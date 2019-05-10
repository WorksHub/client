(ns wh.util-test
  (:require
    [clojure.test :refer :all]
    [clojure.test.check]
    [orchestra.spec.test :as st]
    [wh.test-common :as tc]
    [wh.util :as util]))

(deftest remove-nils-test
  (st/instrument [`util/remove-nils])
  (is (true? (tc/check `util/remove-nils))))

(deftest ->vec-test
  (st/instrument [`util/->vec])
  (is (true? (tc/check `util/->vec))))

(deftest fix-order-unit-test
  (let [order (range 5)]
    (is (= (map :id (util/fix-order order :id (map #(hash-map :id %) (reverse order)))) order))))

(deftest fix-order-test
  (st/instrument [`util/fix-order])
  (is (true? (tc/check `util/fix-order))))
