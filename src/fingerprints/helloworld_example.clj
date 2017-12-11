(ns fingerprints.helloworld-example
  (:require [automation.core :as api]))

(defn
  ^{:command {:name         "HelloClojureWorld"
              :description  "Cheerful greetings"
              :intent      ["hello world"]
              :parameters   [{:name "username" :pattern ".*" :required true}]}}
  hello-clojure-world
  "A very simple handler that responds to `@atomist hello world` asks the user in a thread for a username
   then responds `hello $username!`"
  [o]
  (let [user (api/get-parameter-value o "username")]
    (api/simple-message o (format "Hello %s!" user))))
