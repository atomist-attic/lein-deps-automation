(defproject fingerprint "0.0.1-SNAPSHOT"
  :description "An atomist automation in Clojure that updates project dependencies"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]

                 [automation-api-clj "0.2.5"]

                 ;; fingerprints
                 [rewrite-clj "0.6.0"]
                 [digest "1.4.5"]
                 [cljfmt "0.5.6" :exclusions [org.clojure/tools.reader]]

                 ;; atomistd git
                 [tentacles "0.5.1"]
                 [com.atomist/clj-git-lib "0.3.0"]]

  :plugins [[lein-metajar "0.1.1"]
            [lein-environ "1.1.0"]
            [environ/environ.lein "0.3.1"]]

  :uberjar-name "fingerprint.jar"

  :min-lein-version "2.6.1"

  :profiles {:uberjar {:aot :all}
             :production {:env {:production true}}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [environ "1.1.0"]
                                  [org.clojure/tools.nrepl      "0.2.12"]
                                  [org.clojure/tools.namespace "0.2.11"]]
                   :source-paths   ["env/dev/clj"]
                   :resource-paths ["env/dev/resources"]}}

  :repositories [["atomist" {:url "https://atomist.jfrog.io/atomist/libs-release-local"}]])
