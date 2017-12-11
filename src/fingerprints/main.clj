(ns fingerprints.main
  (:require [automation.core]
            [mount.core :as mount]
            [clojure.tools.logging :as log]))

(defn on-stop
  []
  (fn []
    (mount/stop)
    (log/info "Finished shutdown handler")))

(defn -main [& args]
  (log/info
    (-> (mount/start)))
  (.. Runtime getRuntime (addShutdownHook (Thread. (on-stop))))
  (.. Thread currentThread join))
