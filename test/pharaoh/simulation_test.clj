(ns pharaoh.simulation-test
  (:require [clojure.test :refer :all]
            [pharaoh.simulation :as sim]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.contracts :as ct]))

(defn game-state []
  (let [rng (r/make-rng 1)]
    (assoc (st/initial-state)
      :gold 100000.0 :wheat 5000.0 :slaves 50.0
      :oxen 20.0 :horses 10.0 :manure 1000.0
      :ln-fallow 100.0 :sl-feed-rt 8.0
      :ox-feed-rt 60.0 :hs-feed-rt 50.0
      :ln-to-sew 20.0 :mn-to-sprd 5.0
      :overseers 3.0 :ov-pay 100.0
      :py-quota 10.0
      :players (ct/make-players rng))))

(deftest run-month-advances-date
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (or (and (== 2 (:month result)) (== 1 (:year result)))
            (and (== 1 (:month result)) (== 2 (:year result)))))))

(deftest run-month-records-old-values
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (== 100000.0 (:old-gold result)))
    (is (== 5000.0 (:old-wheat result)))
    (is (== 50.0 (:old-slaves result)))))

(deftest run-month-modifies-gold
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (not= 100000.0 (:gold result)))))

(deftest run-month-adjusts-health
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (<= 0.0 (:sl-health result) 1.0))
    (is (<= 0.0 (:ox-health result) 1.0))
    (is (<= 0.0 (:hs-health result) 1.0))))

(deftest run-month-updates-prices
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (not= (:prices state) (:prices result)))))

(deftest run-month-handles-pyramid
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (>= (:py-stones result) 0))
    (is (>= (:py-height result) 0))))

(deftest run-month-handles-overseer-costs
  (let [rng (r/make-rng 42)
        state (assoc (game-state) :overseers 5.0 :ov-pay 1000.0)
        result (sim/run-month rng state)]
    (is (< (:gold result) 100000.0))))

(deftest run-month-year-wraps
  (let [rng (r/make-rng 42)
        state (assoc (game-state) :month 12)
        result (sim/run-month rng state)]
    (is (== 1 (:month result)))
    (is (== 2 (:year result)))))

(deftest run-month-updates-populations
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (>= (:slaves result) 0))
    (is (>= (:oxen result) 0))
    (is (>= (:horses result) 0))))

(deftest run-month-wheat-changes
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/run-month rng state)]
    (is (not= (:wheat state) (:wheat result)))))

(deftest run-month-resets-wk-addition
  (let [rng (r/make-rng 42)
        state (assoc (game-state) :wk-addition 500.0)
        result (sim/run-month rng state)]
    (is (== 0.0 (:wk-addition result)))))

(deftest run-month-updates-credit
  (let [rng (r/make-rng 42)
        state (assoc (game-state) :loan 50000.0 :credit-rating 0.8)
        result (sim/run-month rng state)]
    (is (not= 0.8 (:credit-rating result)))))

(deftest run-month-checks-win
  (let [rng (r/make-rng 42)
        state (assoc (game-state) :py-base 10.0 :py-stones 100000.0
                :py-height 8.66)
        result (sim/run-month rng state)]
    (is (:game-won result))))

(deftest run-month-deterministic
  (let [state (game-state)
        r1 (sim/run-month (r/make-rng 42) state)
        r2 (sim/run-month (r/make-rng 42) state)]
    (is (== (:gold r1) (:gold r2)))
    (is (== (:slaves r1) (:slaves r2)))))

(deftest do-run-includes-event-check
  (let [rng (r/make-rng 42)
        state (game-state)
        result (sim/do-run rng state)]
    (is (map? result))
    (is (contains? result :gold))))

(deftest multi-month-simulation
  (let [state (game-state)]
    (loop [s state n 0]
      (when (< n 12)
        (let [result (sim/do-run (r/make-rng (+ 42 n)) s)]
          (is (map? result))
          (is (>= (:slaves result) 0))
          (is (>= (:gold result) Double/NEGATIVE_INFINITY))
          (recur result (inc n)))))))
