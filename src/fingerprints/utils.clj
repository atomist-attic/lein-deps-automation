(ns fingerprints.utils
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.zip.whitespace :as w]
            [clojure.tools.logging :as log]))

(defn- indent
  "Find position of node that matches matcher. -2 because of one-based counting and default whitespace insert"
  ([zipper]
   (indent zipper (constantly true)))
  ([zipper matcher]
   (- (second (z/position (z/find-depth-first zipper matcher)))
      2)))

(defn- to-seq
  "Make sure x is a vector. Create one containing x if it isn't"
  [x]
  (if (sequential? x)
    x
    [x]))

(defn files->zipper
  [xs]
  (->> xs
       (map #(try (z/of-file %) (catch Throwable t (log/warn %))))
       (filter identity)))

(defn all-clj-files
  [dir]
  (->> (file-seq dir)
       (filter #(.endsWith (.getName %) ".clj"))))

(defn find-loc
  "Return the key/loc pair (if any) that matches one of the keys in somekey-or-keys. If only a key, then return only the value"
  [zipper somekey-or-keys]
  (loop [thekeys (to-seq somekey-or-keys)]
    (when-let [thekey (first thekeys)]
      (if-let [val (z/find-depth-first zipper #(= (z/sexpr %) thekey))]
        val
        (when (not-empty thekeys)
          (recur (rest thekeys)))))))

(defn insert-if
  "Like insert on the zipper, but only inserts if the key isn't already there. Return zipper at zloc of key. Also anchors to indentation to anchor"
  [zipper anchor somekey-or-keys default-value]
  (if-let [value (find-loc zipper somekey-or-keys)]
    (z/right value)
    (let [anchor-zloc (find-loc zipper anchor)]
      (->
        anchor-zloc
        z/right
        (z/insert-right (first (to-seq somekey-or-keys)))
        (w/insert-space-right (indent anchor-zloc))
        w/insert-newline-right
        z/right
        (z/insert-right default-value)
        z/right))))

(defn assoc-if
  "If it's an empty map, just assoc. Otherwise find an anchor and use insert-if"
  [zipper somekey someval]
  (let [s (z/sexpr zipper)]
    (when-not (map? s)
      (throw (RuntimeException. "assoc-if only works on maps")))
    (if (empty? s)
      (->
        (z/assoc zipper somekey someval)
        z/down
        (z/find-depth-first #(= (z/sexpr %) somekey))
        z/right)
      (insert-if zipper (last (keys s)) somekey someval))))
