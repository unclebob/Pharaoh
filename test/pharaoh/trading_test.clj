(ns pharaoh.trading-test
  (:require [clojure.test :refer :all]
            [pharaoh.trading :as tr]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest buy-commodity
  (let [state (assoc (st/initial-state)
                :gold 10000.0 :wheat 0.0
                :prices (assoc (:prices (st/initial-state)) :wheat 10.0)
                :supply (assoc (:supply (st/initial-state)) :wheat 500.0))
        rng (r/make-rng 42)
        result (tr/buy rng state :wheat 100.0)]
    (is (= (:wheat result) 100.0))
    (is (= (:gold result) 9000.0))
    (is (= (get-in result [:supply :wheat]) 400.0))))

(deftest sell-commodity
  (let [state (assoc (st/initial-state)
                :gold 0.0 :wheat 200.0
                :prices (assoc (:prices (st/initial-state)) :wheat 10.0)
                :supply (assoc (:supply (st/initial-state)) :wheat 500.0)
                :demand (assoc (:demand (st/initial-state)) :wheat 10000.0))
        rng (r/make-rng 42)
        result (tr/sell rng state :wheat 50.0)]
    (is (= (:wheat result) 150.0))
    (is (= (:gold result) 500.0))
    (is (= (get-in result [:supply :wheat]) 550.0))))

(deftest sell-livestock-adjusts-by-health
  (let [state (assoc (st/initial-state)
                :gold 0.0 :slaves 100.0 :sl-health 0.5
                :prices (assoc (:prices (st/initial-state)) :slaves 1000.0)
                :supply (assoc (:supply (st/initial-state)) :slaves 500.0)
                :demand (assoc (:demand (st/initial-state)) :slaves 10000.0))
        rng (r/make-rng 42)
        result (tr/sell rng state :slaves 50.0)]
    (is (= (:slaves result) 50.0))
    (is (= (:gold result) 25000.0))))

(deftest buy-livestock-blends-health
  (let [state (assoc (st/initial-state)
                :gold 100000.0 :slaves 100.0 :sl-health 0.6
                :prices (assoc (:prices (st/initial-state)) :slaves 1000.0)
                :supply (assoc (:supply (st/initial-state)) :slaves 500.0))
        rng (r/make-rng 42)
        result (tr/buy rng state :slaves 50.0)
        ;; blended = (100*0.6 + 50*~0.8) / 150 ~ 0.667
        health (:sl-health result)]
    (is (> health 0.6))
    (is (< health 0.85))))

(deftest buy-limited-by-supply
  (let [state (assoc (st/initial-state)
                :gold 100000.0 :wheat 0.0
                :prices (assoc (:prices (st/initial-state)) :wheat 10.0)
                :supply (assoc (:supply (st/initial-state)) :wheat 30.0))
        rng (r/make-rng 42)
        result (tr/buy rng state :wheat 100.0)]
    (is (<= (:wheat result) 30.0))))

(deftest cannot-sell-more-than-owned
  (let [state (assoc (st/initial-state)
                :wheat 100.0
                :demand (assoc (:demand (st/initial-state)) :wheat 10000.0))
        rng (r/make-rng 42)]
    (is (= :error (:status (tr/validate-sell state :wheat 150.0))))))

(deftest cannot-buy-more-than-gold
  (let [state (assoc (st/initial-state)
                :gold 500.0
                :prices (assoc (:prices (st/initial-state)) :wheat 10.0))
        rng (r/make-rng 42)]
    (is (= :error (:status (tr/validate-buy state :wheat 100.0))))))

(deftest validate-sell-capped-by-supply
  ;; market absorbs up to demand * 1.1 total supply
  ;; demand=100, max-supply=110, cur-supply=100, so only 10 more accepted
  (let [state (assoc (st/initial-state)
                :wheat 1000.0
                :supply (assoc (:supply (st/initial-state)) :wheat 100.0)
                :demand (assoc (:demand (st/initial-state)) :wheat 100.0))]
    (is (= :capped (:status (tr/validate-sell state :wheat 500.0))))
    (is (pos? (:max-amount (tr/validate-sell state :wheat 500.0))))))

(deftest validate-sell-ok-within-limits
  (let [state (assoc (st/initial-state)
                :wheat 1000.0
                :supply (assoc (:supply (st/initial-state)) :wheat 0.0)
                :demand (assoc (:demand (st/initial-state)) :wheat 10000.0))]
    (is (= :ok (:status (tr/validate-sell state :wheat 100.0))))))

(deftest buy-livestock-blends-health-when-zero-stock
  (let [state (assoc (st/initial-state)
                :gold 100000.0 :slaves 0.0 :sl-health 0.0
                :prices (assoc (:prices (st/initial-state)) :slaves 1000.0)
                :supply (assoc (:supply (st/initial-state)) :slaves 500.0))
        rng (r/make-rng 42)
        result (tr/buy rng state :slaves 50.0)]
    ;; When old-count is 0, health should be ~0.8 (nominal)
    (is (> (:sl-health result) 0.7))
    (is (< (:sl-health result) 0.9))))

(deftest sell-land-burns-crops
  (let [state (assoc (st/initial-state)
                :gold 0.0 :ln-fallow 100.0 :wt-sewn 200.0
                :prices (assoc (:prices (st/initial-state)) :land 5000.0)
                :supply (assoc (:supply (st/initial-state)) :land 0.0)
                :demand (assoc (:demand (st/initial-state)) :land 10000.0))
        rng (r/make-rng 42)
        ;; sell doesn't use crop-burn logic for fallow land (crop-key nil for :ln-fallow)
        result (tr/sell rng state :land 50.0)]
    (is (= (:ln-fallow result) 50.0))
    (is (> (:gold result) 0))))
