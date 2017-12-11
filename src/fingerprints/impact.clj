(ns fingerprints.impact
  (:require [automation.core :as api]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [fingerprints.git :as git]
            [fingerprints.fingerprinter.project-deps :as deps]
            [clojure.data])
  (:import [java.io File]))

(defn has-goal?
  "Does the set of goals contain the supplied goal name?"
  [goals k]
  ((into #{} (keys goals)) k))

(defn get-team-goals
  "Returns the goals for a team"
  [team-id]
  (if-let [pref (->>
                  (-> (api/run-query team-id "{ChatTeam {id preferences {name value}}}")
                      :data
                      :ChatTeam
                      first
                      :preferences)
                  (filter #(= "atomist:fingerprints:clojure:library-goals" (:name %)))
                  first
                  :value)]
    (json/read-str pref)))

(defn update-goals
  "Update the goals for a team. Takes the map to replace the existing goals with"
  [team-id goals]
  (let [goals-string (clojure.string/replace (json/write-str goals) "\"" "\\\"")
        query (format "mutation {setTeamPreference(name: \"%s\", value: \"%s\") {name value}}"
                      "atomist:fingerprints:clojure:library-goals"
                      goals-string)]
    (log/info query)
    (api/run-query team-id query)))

(defn add-team-goal
  "Add a new goal to the existing set"
  [team-id goal-name goal-version]
  (let [goals (get-team-goals team-id)
        goals (if (map? goals) goals {})]
    (update-goals team-id (assoc goals goal-name goal-version))))

(defn- push-impact?
  [x]
  (->> x
       (map #(second %))
       (apply +)
       (< 0)))

(defn- get-repo-details [event]
  (cond
    (-> event :data :PushImpact)
    [(-> event :data :PushImpact first :push :after :repo :org :owner)
     (-> event :data :PushImpact first :push :after :repo :name)
     (-> event :data :PushImpact first :push :after :repo :channels first :name)]))

(defn- create-library-editor-choice
  [event channel-name {:keys [name version]} current owner repo]
  (api/actionable-message
    (api/channel event channel-name)
    {:text (format "%s/%s Should we update %s from %s to %s?" owner repo name current version)
     :attachments
           [{:callback_id "callbackid1"
             :text        ""
             :markdwn_in  ["text"]
             :actions     [{:text            "Update"
                            :type            "button"
                            :name            "rug"
                            :atomist/command {:rug        {:type "command_handler" :name "confirm-update"}
                                              :parameters [{:name  "owner"
                                                            :value owner}
                                                           {:name  "repo"
                                                            :value repo}
                                                           {:name  "library.name"
                                                            :value name}
                                                           {:name  "library.version"
                                                            :value version}]}}
                           {:text            (str "Set goal to " current)
                            :type            "button"
                            :name            "rug"
                            :atomist/command {:rug        {:type "command_handler" :name "set-team-library-goal"}
                                              :parameters [{:name  "library.name"
                                                            :value name}
                                                           {:name  "library.version"
                                                            :value current}]}}]}]}))

(defn- perform
  [event action]
  (let [[owner repo channel-name] (get-repo-details event)]
    (assert (and owner repo))
    (cond
      (= (:action action) :update-library)
      (let [{:keys [library current]} action]
        (create-library-editor-choice event channel-name library current owner repo))

      (= (:action action) :readme-changed)
      (api/simple-message event (format "readme.md changed in %s/%s" owner repo))

      (= (:action action) :none)
      nil

      :else (log/info "Unknown action: " action))))

(defn- check-library [goals n v & args]
  (log/infof "check %s %s and %s" n v goals)
  (cond
    (and
      (has-goal? goals n)
      (not (= v (get goals n))))
    {:action  :update-library
     :library {:name n :version (get goals n)}
     :current v}
    :else
    {:action :none}))

(defn- diff-fp [team-id s from to]
  (let [goals (get-team-goals team-id)
        data (fn [x] (-> x :data (json/read-str :key-fn keyword)))
        diff (fn [a b]
               (zipmap [:from :to]
                       (clojure.data/diff (into #{} (data a)) (into #{} (data b)))))]
    (case s
      "project-deps"
      (concat
        (when (not (= (:sha from) (:sha to)))
          [{:action :display-dep-differences
            :data   (diff from to)}])
        (map #(apply check-library goals %) (data to)))
      "readme.md"
      (when (not (= (:sha from) (:sha to)))
        [{:action :readme-changed}])

      [{:action :none}])))

(defn- check-push-impact
  "check if this commit had an important impact on it's parent"
  [team-id d]
  (let [fp-data (-> d :data :PushImpact first)
        sha-impacts? (push-impact? (-> fp-data :data (json/read-str :key-fn keyword)))]
    (let [to (-> fp-data :push :after)
          from (-> fp-data :push :before)
          fp-names (map :name (:fingerprints to))]
      (if (and from to)
        (->> fp-names
             (mapcat
               (fn [fp] (diff-fp team-id fp
                                 (->> from :fingerprints (some #(if (= fp (:name %)) %)))
                                 (->> to :fingerprints (some #(if (= fp (:name %)) %)))))))
        (log/info "Missing some data (is this our first fingerprint commit?)")))))

(defn
  ^{:event {:name         "PushImpactEvent"
            :secrets      ["github://org_token?scopes=repo"]
            :description  "watch for changes in a Push"
            :subscription (slurp "resources/push-impact.graphql")}}
  see-push-impact
  "Subscribe to PushImpacts. These are fired when a push contains fingerprint changes. By subscribing
   to these we can act when a fingerprint change occurs. See the resources file for the information we
   collect on that event."
  [event]
  (let [team-id (-> event :correlation_context :team :id)
        trace (fn [x] (when-not (= (:action x) :none) (log/info x)) x)]
    (try
      (->> (check-push-impact team-id event)
           (filter identity)
           (map trace)
           (map (partial perform event))
           (doall))
      (catch Throwable t
        (log/error t (.getMessage t))
        (log/error "processing " (-> event :data :ParentImpact first :data))))))

(defn
  ^{:command {:name        "confirm-update"
              :description "run editor to update library"
              :secrets     ["github://user_token?scopes=repo"]
              :parameters  [{:name "owner" :pattern ".*" :required true}
                            {:name "repo" :pattern ".*" :required true}
                            {:name "library.name" :pattern ".*" :required true}
                            {:name "library.version" :pattern ".*" :required true}]}}
  handler-confirm-update
  "user confirmation (via button) that the library should be updated"
  [o]
  (let [owner (api/get-parameter-value o "owner")
        repo (api/get-parameter-value o "repo")
        library-name (api/get-parameter-value o "library.name")
        library-version (api/get-parameter-value o "library.version")
        token (api/get-secret-value o "github://user_token?scopes=repo")]
    (api/simple-message o (format "run the library update editor on %s/%s %s %s"
                                  owner
                                  repo
                                  library-name
                                  library-version))
    (git/raise-PR-in-a-cloned-workspace
      {:commit {:owner owner :repo repo :token token}}
      (fn [f]
        (deps/edit-library f library-name library-version))
      (format "Add library feature %s/%s" library-version library-name)
      (format "update %s/%s to use version %s of %s" owner repo library-version library-name))))

(defn
  ^{:command {:name        "set-team-library-goal"
              :intent      ["set library goal"]
              :description "set a team library goal"
              :parameters  [{:name "library.name" :pattern ".*" :required true}
                            {:name "library.version" :pattern ".*" :required true}]}}
  handler-set-library-goal
  ""
  [o]
  (let [team-id (-> o :correlation_context :team :id)
        lib-name (api/get-parameter-value o "library.name")
        n (name (symbol lib-name))
        v (api/get-parameter-value o "library.version")]

    (api/simple-message o (format "set the new team-wide library version preference for %s to be %s" n v))
    (add-team-goal team-id (name (symbol n)) v)
    (log/info (get-team-goals team-id))))

(defn
  ^{:command {:name        (name :choose-team-library-goal)
              :description "set a team library goal"
              :parameters  [{:name "library" :pattern ".*" :required true}]}}
  handler-choose-team-library-goal
  "Responds to calls to select a library from the dropdown"
  [o]
  (let [team-id (-> o :correlation_context :team :id)
        [n v] (if-let [d (api/get-parameter-value o "library")] (rest (re-find #"(.*):(.*)" d)))]
    (if (and n v)
      (add-team-goal team-id n v)
      (log/warn "library is in wrong form:  " (api/get-parameter-value o "library")))))

(defn- get-options [goals libs]
  (let [current-goals (->> goals (keys) (into #{}))]
    (log/info "current goals " current-goals)
    (->> (filter (fn [x] ((complement current-goals) (-> x first name))) libs)
         (map (fn [[symbol-name version & x]] {:text  (format "%s %s" (name symbol-name) version)
                                               :value (format "%s:%s" (name symbol-name) version)}))
         (into []))))

(defn
  ^{:command {:name              (name :get-goals)
              :description       "Select goals from the current channels project"
              :intent            ["get goals"]
              :secrets           ["github://user_token?scopes=repo"]
              :mapped_parameters [{:local_key "repo" :foreign_key "atomist://github/repository" :required true}
                                  {:local_key "owner" :foreign_key "atomist://github/repository/owner" :required true}]}}
  handler-show-goals
  "Creates a handler that provides a drop down of libraries from this project to select as library goals.
   Also provides an example of drop down boxes."
  [o]

  (let [team-id (-> o :correlation_context :team :id)
        goals (get-team-goals team-id)
        owner (api/mapped-parameter-value o "owner")
        repo (api/mapped-parameter-value o "repo")
        token (api/get-secret-value o "github://user_token?scopes=repo")]
    (api/simple-message o (format "```%s```" (with-out-str (clojure.pprint/pprint goals))))
    (if (and owner repo)
      (git/run-with-cloned-workspace
        {:commit {:owner owner :repo repo :token token}}
        (fn [d]
          (if-let [libs (deps/project-dependencies (File. d "project.clj"))]
            (let [options (get-options goals libs)]
              (api/actionable-message
                o
                {:text "Leiningen Library Fingerprint"
                 :attachments
                       [{:callback_id "callbackid1"
                         :text        (format "Would you like to set a team-wide goal dependency based on current dependencies in %s/%s?" owner repo)
                         :markdwn_in  ["text"]
                         :actions     [{:text            "Choose Goal ..."
                                        :type            "select"
                                        :name            "rug"
                                        :options         options
                                        :atomist/command {:rug            {:type "command_handler"
                                                                           :name (name :choose-team-library-goal)}
                                                          :parameter_name "library"
                                                          :parameters     []}}]}]}))))))))
