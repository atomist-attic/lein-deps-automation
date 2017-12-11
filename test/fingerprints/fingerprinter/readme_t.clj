(ns fingerprints.fingerprinter.readme-t
  (:require [clojure.test :refer :all]
            [fingerprints.fingerprinter.readme :refer :all])
  (:import (java.io File)))

(deftest content-tests
  (is (= (content-sha (File. "dev-resources/project-1.clj"))
         (content-sha (File. "dev-resources/project-1.clj"))))
  (is (not (= (content-sha (File. "dev-resources/project-1.clj"))
              (content-sha (File. "dev-resources/project-2.clj")))))
  (is (= {:name "readme.md", :version "0.0.1", :abbreviation "readme", :sha "4f8116a9a428d2fcb5af08aa7a8592bed9251487d66d5900fdba81e8fc686041"}
         (run (File. "dev-resources")))))
