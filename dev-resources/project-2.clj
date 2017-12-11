(defproject fpimagesrv "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [quil "2.6.0"]
                 [metosin/compojure-api        "1.1.8"]
                 [ring/ring-jetty-adapter      "1.5.0"]
                 [org.clojure/data.json        "0.2.6"]
                 [mount "0.1.11"]
                 [rewrite-clj "0.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [cljfmt "0.5.6" :exclusions [org.clojure/tools.reader]]
                 [com.atomist/clj-git-lib "0.2.7"]
                 [environ "1.0.0"]
                 [digest "1.4.5"]]

  :plugins [[lein-metajar "0.1.1"]
            [lein-codox "0.9.4"]
            [clj-plugin   "0.1.16"]
            [lein-dynamodb-local "0.2.8"]
            [lein-environ "1.1.0"]
            [clj-local-secrets "0.3.0"]
            [environ/environ.lein "0.3.1"]]

  :uberjar-name "fpimagesrv.jar"

  :min-lein-version "2.6.1"

  :profiles {:uberjar {:aot :all}
             :production {:env {:production true}}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [environ "1.1.0"]
                                  [clj-local-secrets "0.3.0"]
                                  [clj-http                     "3.3.0"]
                                  [org.clojure/tools.nrepl      "0.2.12"]
                                  [org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}}}

  :repositories [["releases" {:url      "https://sforzando.artifactoryonline.com/sforzando/libs-release-local"
                              :username [:gpg :env/artifactory_user]
                              :password [:gpg :env/artifactory_pwd]}]
                 ["plugins" {:url      "https://sforzando.artifactoryonline.com/sforzando/plugins-release"
                             :username [:gpg :env/artifactory_user]
                             :password [:gpg :env/artifactory_pwd]}]])
