(ns fingerprints.fingerprinter.fingerprinter
  (:require [automation.core :as api]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [fingerprints.fingerprinter.fingerprint-runner :as runner]
            [fingerprints.git :as git]
            [clojure.tools.logging :as log]))

(defn- get-languages
  [token owner repo-name]
  (-> (client/get
       (format "https://api.github.com/repos/%s/%s/languages" owner repo-name)
       {:headers {"Authorization" (str "token " token)}
        :as      :json})
      :body
      keys))

(defn- send-fingerprint-data [d team-id]
  (if (and (:fingerprints d) (not-any? :could_not_clone (:errors d)))
    (let [callback-url (format "https://webhook.atomist.com/atomist/fingerprints/teams/%s" team-id)]
      (log/infof "post fingerprint data back to Atomist %s" callback-url)
      (log/info (with-out-str (clojure.pprint/pprint d)))
      (try
        (log/info
         (client/post callback-url {:body (json/json-str d) :content-type "application/json"}))
        (catch Throwable t (log/error t (format "problem posting data back to %s" callback-url)))))))

(defn
  ^{:event {:name         "CommitHappened"
            :secrets      ["github://org_token?scopes=repo"]
            :description  "this is nothing but ensuring that I can setup subscriptions correctly"
            :subscription "subscription CommitHappened { Commit {sha repo {name org {owner ownerType}}}}"}}
  compute-fingerprint
  "Subscribes to Commit events. For each commit checks out the project and scans it to generate
   fingerprints."
  [event]
  (log/info "CommitHappened:  " (-> event :data :Commit first))
  (let [owner (-> event :data :Commit first :repo :org :owner)
        team-id (-> event :correlation_context :team :id)
        repo-name (-> event :data :Commit first :repo :name)
        token (api/get-secret-value event "github://org_token?scopes=repo")
        data {:commit {:owner owner
                       :repo  repo-name
                       :sha   (-> event :data :Commit first :sha)
                       :token token}}]
    (let [langs (get-languages token owner repo-name)]
      (log/infof "GitHub detected languages for %s/%s -> %s" owner repo-name langs)
      (if (some #{:Clojure} langs)
        (do
          (log/infof "detected Clojure project %s/%s" owner repo-name)
          (log/infof "run all fingerprints against %s" data)
          (-> (git/run-with-cloned-workspace data runner/run-all)
              (assoc :commit (dissoc (:commit data) :token))
              (send-fingerprint-data team-id)))))))
