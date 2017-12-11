(ns fingerprints.fingerprinter.project-deps-t
  (:require [clojure.test :refer :all]
            [fingerprints.fingerprinter.project-deps :refer :all])
  (:import (java.io File)))

(deftest project-deps
  (testing "that the leiningen project dependencies fingerprint skips meaningless changes and notices real ones"
    (is (= (project-dependencies (File. "dev-resources/project-1.clj"))
           (project-dependencies (File. "dev-resources/project-1-same.clj"))))
    (is (not (= (project-dependencies (File. "dev-resources/project-1.clj"))
                (project-dependencies (File. "dev-resources/project-2.clj"))))))
  (testing "that project.clj without dependencies works"
    (is (= (project-dependencies (File. "dev-resources/junk-project.clj"))
           '()))))
