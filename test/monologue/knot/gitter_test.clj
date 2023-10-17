(ns monologue.knot.gitter-test
  (:require [monologue.knot.transfer :as gitter]
            [clojure.test :refer [deftest is]]))


(deftest parse-target-test
  (is (= true (gitter/parse-target? "asdf.md")))
  (is (= true (gitter/parse-target? "2023/asdf.md")))
  (is (= false (gitter/parse-target? "files/asdf.md")))
  (is (= false (gitter/parse-target? ".asdf.md")))
  (is (= false (gitter/parse-target? "-asdf.md"))))