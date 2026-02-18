(ns pharaoh.health-test
  (:require [clojure.test :refer :all]
            [pharaoh.health :as h]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest slave-health-capped-at-1
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :sl-health 0.98)
        result (h/slave-health-update rng state 10.0 0.0 0.0 1.0)]
    (is (<= (:sl-health result) 1.0))))

(deftest slave-health-clamped-at-0
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :sl-health 0.01)
        result (h/slave-health-update rng state 0.0 0.5 10.0 1.0)]
    (is (>= (:sl-health result) 0.0))))

(deftest slave-health-no-sickness-when-dead
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :sl-health 0.0)
        result (h/slave-health-update rng state 0.0 0.5 10.0 1.0)]
    (is (== 0.0 (:sl-sick-rt result)))))

(deftest oxen-health-ages
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :ox-health 1.0)
        result (h/oxen-health-update rng state 0.0)]
    (is (== 0.95 (:ox-health result)))))

(deftest oxen-health-no-aging-when-dead
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :ox-health 0.0)
        result (h/oxen-health-update rng state 0.0)]
    (is (== 0.0 (:ox-health result)))))

(deftest horse-health-ages-faster-than-oxen
  (let [rng1 (r/make-rng 42)
        rng2 (r/make-rng 42)
        state (assoc (st/initial-state) :ox-health 1.0 :hs-health 1.0)
        ox-result (h/oxen-health-update rng1 state 0.0)
        hs-result (h/horse-health-update rng2 state 0.0)]
    (is (< (:hs-health hs-result) (:ox-health ox-result)))))

(deftest population-grows-with-good-health
  (let [pop (h/population-update 100.0 0.04 0.01)]
    (is (> pop 100.0))))

(deftest population-declines-with-poor-health
  (let [pop (h/population-update 100.0 0.01 0.04)]
    (is (< pop 100.0))))

(deftest population-cannot-go-negative
  (let [pop (h/population-update 5.0 0.0 0.5)]
    (is (>= pop 0.0))))
