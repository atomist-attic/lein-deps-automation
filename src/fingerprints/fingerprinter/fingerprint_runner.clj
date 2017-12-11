(ns fingerprints.fingerprinter.fingerprint-runner
  (:require [fingerprints.fingerprinter.project-deps]
            [fingerprints.fingerprinter.readme]
            [mount.core :as mount]
            [clojure.tools.logging :as log])
  (:import (java.io File)))

(def fingerprints [#'fingerprints.fingerprinter.project-deps/run
                   #'fingerprints.fingerprinter.readme/run])

(defn run-all [^File f]
  (log/infof "running fingerprints on %s" (.getCanonicalPath f))
  {:fingerprints (->> (map #(apply % [f]) fingerprints)
                      (filter #(and (map? %) (contains? % :name) (contains? % :sha)))
                      (into []))})
