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
    (is (== 1.0 (:sl-health s)))
    (is (== 1.0 (:ox-health s)))
    (is (== 1.0 (:hs-health s)))
    (is (== 0.0 (:loan s)))
    (is (== 1.0 (:credit-rating s)))
    (is (== 0.0 (:py-stones s)))
    (is (== 0.0 (:py-height s)))
    (is (false? (:game-over s)))
    (is (false? (:game-won s)))))

(deftest initial-state-defaults-match-c
  (let [s (st/initial-state)]
    (is (== 0.5 (:interest s)))
    (is (== 0.001 (:inflation s)))
    (is (== 0.05 (:wt-rot-rt s)))
    (is (== 300.0 (:ov-pay s)))
    (is (== 300.0 (:py-base s)))
    (is (== 0.05 (:world-growth s)))
    (is (== 50000.0 (:credit-limit s)))
    (is (== 500000.0 (:credit-lower s)))))

(deftest initial-state-prices-match-c
  (let [p (:prices (st/initial-state))]
    (is (== 2.0 (:wheat p)))
    (is (== 500.0 (:slaves p)))
    (is (== 100.0 (:horses p)))
    (is (== 90.0 (:oxen p)))
    (is (== 20.0 (:manure p)))
    (is (== 10000.0 (:land p)))))

(deftest initial-state-supply-demand-match-c
  (let [s (st/initial-state)]
    (is (== 1e6 (get-in s [:supply :wheat])))
    (is (== 1e7 (get-in s [:demand :wheat])))
    (is (== 1e7 (get-in s [:production :wheat])))
    (is (== 1e3 (get-in s [:supply :slaves])))
    (is (== 1e4 (get-in s [:demand :slaves])))
    (is (== 1e4 (get-in s [:production :slaves])))
    (is (== 1e4 (get-in s [:supply :horses])))
    (is (== 1e5 (get-in s [:demand :horses])))
    (is (== 1e5 (get-in s [:production :horses])))
    (is (== 1e4 (get-in s [:supply :oxen])))
    (is (== 1e5 (get-in s [:demand :oxen])))
    (is (== 1e5 (get-in s [:production :oxen])))
    (is (== 1e2 (get-in s [:supply :land])))
    (is (== 1e3 (get-in s [:demand :land])))
    (is (== 1e3 (get-in s [:production :land])))
    (is (== 1e4 (get-in s [:supply :manure])))
    (is (== 1e5 (get-in s [:demand :manure])))
    (is (== 1e5 (get-in s [:production :manure])))))

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
    (is (== 0.15 (:world-growth s)))
    (is (== 1000.0 (get-in s [:prices :land])))
    (is (== 10.0 (get-in s [:prices :wheat])))
    (is (== 1000.0 (get-in s [:prices :slaves])))))

(deftest set-difficulty-normal
  (let [s (st/set-difficulty (st/initial-state) "Normal")]
    (is (== 346.41 (:py-base s)))
    (is (== 5e5 (:credit-limit s)))
    (is (== 5e5 (:credit-lower s)))
    (is (== 0.10 (:world-growth s)))
    (is (== 5000.0 (get-in s [:prices :land])))
    (is (== 8.0 (get-in s [:prices :wheat])))
    (is (== 800.0 (get-in s [:prices :slaves])))))

(deftest set-difficulty-hard
  (let [s (st/set-difficulty (st/initial-state) "Hard")]
    (is (== 1154.7 (:py-base s)))
    ;; C Hard only sets pyBase; credit uses defaults
    (is (== 50000.0 (:credit-limit s)))
    (is (== 500000.0 (:credit-lower s)))))

(deftest set-difficulty-invalid-returns-unchanged
  (let [s (st/initial-state)
        result (st/set-difficulty s "Impossible")]
    (is (= s result))))

(deftest set-difficulty-easy-sets-commodities
  (let [s (st/set-difficulty (st/initial-state) "Easy")]
    (is (== 100.0 (:slaves s)))
    (is (== 50.0 (:oxen s)))
    (is (== 7.0 (:horses s)))
    (is (== 20000.0 (:wheat s)))
    (is (== 400.0 (:manure s)))
    (is (== 80.0 (:ln-fallow s)))))

(deftest set-difficulty-easy-sets-overseers-and-loan
  (let [s (st/set-difficulty (st/initial-state) "Easy")]
    (is (== 7 (:overseers s)))
    (is (== 393200.0 (:loan s)))
    (is (== 40000.0 (:gold s)))))

(deftest set-difficulty-easy-sets-feed-rates
  (let [s (st/set-difficulty (st/initial-state) "Easy")]
    (is (== 10.0 (:sl-feed-rt s)))
    (is (== 70.0 (:ox-feed-rt s)))
    (is (== 55.0 (:hs-feed-rt s)))))

(deftest set-difficulty-easy-sets-planting
  (let [s (st/set-difficulty (st/initial-state) "Easy")]
    (is (== 50.0 (:mn-to-sprd s)))
    (is (== 10.0 (:ln-to-sew s)))))

(deftest initial-state-has-dirty-flag
  (let [s (st/initial-state)]
    (is (false? (:dirty s)))
    (is (nil? (:save-path s)))))
