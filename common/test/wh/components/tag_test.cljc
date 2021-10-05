(ns wh.components.tag-test
  (:require [clojure.test :refer :all]
            [wh.components.tag :refer :all]))

(def default-tags [{:id      1
                    :label   "Clojure"
                    :type    :tech
                    :subtype :software}
                   {:id      2
                    :label   "Common Lisp"
                    :type    :tech
                    :subtype :software}
                   {:id      3
                    :label   "Smartphone"
                    :type    :tech
                    :subtype :gadget}
                   {:id    -1
                    :label "Cat"}])

(deftest select-tags-test
  (testing "An empty search text returns all `tags`"
    (is (= default-tags
           (select-tags "" default-tags {:include-ids #{}}))))

  (testing "Tags should be searchable regardless of text case"
    (is (= [{:id 1, :label "Clojure", :type :tech, :subtype :software}]
           (select-tags "clojure" default-tags {:include-ids #{}}))))

  (testing "Tags w/ included IDs are picked up and marked `selected`"
    (is (= [{:id 2, :label "Common Lisp", :type :tech, :subtype :software, :selected true}]
           (select-tags "???" default-tags {:include-ids #{2}}))))

  (testing "Selected tags come first, before non-selected matching ones"
    (is (= [{:id 2, :label "Common Lisp", :type :tech, :subtype :software, :selected true}
            {:id 1, :label "Clojure", :type :tech, :subtype :software}]
           (select-tags "Clojure" default-tags {:include-ids #{2}}))))

  (testing "It is possible to filter tags by their type and/or subtype"
    (is (empty? (select-tags "Clojure"
                             default-tags
                             {:include-ids #{}
                              :type        :non-tech})))
    (is (= [{:id 1, :label "Clojure", :type :tech, :subtype :software}
            {:id 2, :label "Common Lisp", :type :tech, :subtype :software}
            {:id 3, :label "Smartphone", :type :tech, :subtype :gadget}]
           (select-tags ""
                        default-tags
                        {:include-ids #{}
                         :type        :tech})))
    (is (= [{:id 1, :label "Clojure", :type :tech, :subtype :software}
            {:id 2, :label "Common Lisp", :type :tech, :subtype :software}]
           (select-tags ""
                        default-tags
                        {:include-ids #{}
                         :type        :tech
                         :subtype     :software})))
    (is (= [{:id 1, :label "Clojure", :type :tech, :subtype :software}]
           (select-tags "Clo"
                        default-tags
                        {:include-ids #{}
                         :type        :tech})))))
