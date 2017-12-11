(ns fingerprints.fingerprinter.project-deps
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.zip.whitespace :as w]
            [rewrite-clj.parser :as p]
            [rewrite-clj.node :as n]
            [digest :as digest]
            [fingerprints.utils :as utils]
            [clojure.data.json :as json])
  (:import [java.io File]))

(defn dependencies
  ([zipper]
   (-> zipper
       z/down
       (utils/find-loc :dependencies)
       z/right)))

(defn project-dependencies [^File f]
  (->> (z/of-file f)
       dependencies
       z/child-sexprs
       (sort-by (comp name first))))

(defn edit-library [^File f library-name library-version]
  (let [project (File. f "project.clj")]
    (spit
      project
      (-> (z/of-file project)
          dependencies
          (z/find z/next #(if-let [s (z/sexpr %)]
                            (and (symbol? s) (= library-name (name s)))))
          (z/right)
          (z/replace library-version)
          (z/root-string)))))

(defn run [^File f]
  (let [project (File. f "project.clj")]
    (if (.exists project)
      (let [project-deps (project-dependencies (File. f "project.clj"))]
        {:name "project-deps" :version "0.0.2" :sha (->> project-deps (apply str) (digest/sha-256)) :abbreviation "lein" :value (->> project-deps (into []) (json/json-str))}))))
