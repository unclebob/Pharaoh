(ns pharaoh.state-test
  (:require [clojure.test :refer :all]
            [pharaoh.state :as st]))

(deftest initial-state-has-required-keys
  (let [s (st/initial-state)]
    (is (= 1 (:month s)))
    (is (= 1 (:year s)))
    (is (== 0.0 (:gold s)))
    (is (== 0.0 (:wheat s)))
    (is (== 0.0 (:slaves s)))
    (is (== 0.0 (:oxen s)))
    (is (== 0.0 (:horses s)))
    (is (== 0.0 (:manure s)))
    (is (== 0.8 (:sl-health s)))
    (is (== 0.8 (:ox-health s)))
    (is (== 0.8 (:hs-health s)))
    (is (== 0.0 (:loan s)))
    (is (== 1.0 (:credit-rating s)))
    (is (== 0.0 (:py-stones s)))
    (is (== 0.0 (:py-height s)))
    (is (false? (:game-over s)))
    (is (false? (:game-won s)))))

(deftest initial-state-has-prices
  (let [p (:prices (st/initial-state))]
    (is (pos? (:wheat p)))
    (is (pos? (:slaves p)))
    (is (pos? (:horses p)))
    (is (pos? (:oxen p)))
    (is (pos? (:manure p)))
    (is (pos? (:land p)))))

(deftest initial-state-has-supply-and-demand
  (let [s (st/initial-state)]
    (doseq [k [:wheat :slaves :horses :oxen :manure :land]]
      (is (pos? (get-in s [:supply k])) (str k " supply"))
      (is (pos? (get-in s [:demand k])) (str k " demand"))
      (is (pos? (get-in s [:production k])) (str k " production")))))

(deftest total-land-sums-all-stages
  (let [s (assoc (st/initial-state)
            :ln-fallow 100.0 :ln-sewn 50.0
            :ln-grown 30.0 :ln-ripe 20.0)]
    (is (== 200.0 (st/total-land s)))))

(deftest total-land-zero-initial
  (is (== 0.0 (st/total-land (st/initial-state)))))

(deftest set-difficulty-easy
  (let [s (st/set-difficulty (st/initial-state) "Easy")]
    (is (== 115.47 (:py-base s)))
    (is (== 5e6 (:credit-limit s)))
    (is (== 5e6 (:credit-lower s)))
    (is (== 0.15 (:world-growth s)))))

(deftest set-difficulty-normal
  (let [s (st/set-difficulty (st/initial-state) "Normal")]
    (is (== 346.41 (:py-base s)))
    (is (== 5e5 (:credit-limit s)))
    (is (== 5e5 (:credit-lower s)))
    (is (== 0.10 (:world-growth s)))))

(deftest set-difficulty-hard
  (let [s (st/set-difficulty (st/initial-state) "Hard")]
    (is (== 1154.7 (:py-base s)))))

(deftest set-difficulty-invalid-returns-unchanged
  (let [s (st/initial-state)
        result (st/set-difficulty s "Impossible")]
    (is (= s result))))
