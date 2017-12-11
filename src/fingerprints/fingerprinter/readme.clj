(ns fingerprints.fingerprinter.readme
  (:require [digest :as digest])
  (:import (java.io File)))

(defn content-sha
  "Returns a sha of the content of the supplied file"
  [f]
  (-> (slurp f) (digest/sha-256)))

(defn run [^File f]
  "Tests to see if the readme file has changed. A very basic fingerprint."
  (let [readme (File. f "README.md")]
    (if (.exists readme)
      {:name "readme.md" :sha (content-sha readme) :version "0.0.1" :abbreviation "readme"})))
