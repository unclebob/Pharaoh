(ns pharaoh.overseers-test
  (:require [clojure.test :refer :all]
            [pharaoh.overseers :as ov]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest hire-overseers
  (let [state (assoc (st/initial-state) :overseers 5.0)]
    (is (== 8.0 (:overseers (ov/hire state 3))))))

(deftest fire-overseers
  (let [state (assoc (st/initial-state) :overseers 5.0)
        result (ov/fire state 2)]
    (is (== 3.0 (:overseers result)))))

(deftest cannot-fire-more-than-owned
  (let [state (assoc (st/initial-state) :overseers 3.0)
        result (ov/fire state 5)]
    (is (:error result))))

(deftest obtain-overseers
  (let [state (assoc (st/initial-state) :overseers 5.0)]
    (is (== 10 (:overseers (ov/obtain state 10))))))

(deftest stress-builds-with-deficit
  (let [state (assoc (st/initial-state) :ov-press 0.3)
        new-press (ov/overseer-stress state 5.0)]
    (is (== 0.8 new-press))))

(deftest stress-relaxes-without-deficit
  (let [state (assoc (st/initial-state) :ov-press 0.6)
        new-press (ov/overseer-stress state 0)]
    (is (< (Math/abs (- new-press 0.42)) 0.001))))

(deftest overseers-quit-when-unpaid
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :overseers 5.0 :ov-pay 500.0)
        result (ov/overseers-quit rng state)]
    (is (== 0 (:overseers result)))
    (is (> (:ov-pay result) 500.0))))
