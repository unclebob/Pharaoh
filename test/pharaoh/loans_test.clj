(ns pharaoh.loans-test
  (:require [clojure.test :refer :all]
            [pharaoh.loans :as ln]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest borrow-within-limit
  (let [state (assoc (st/initial-state) :credit-limit 100000.0 :loan 0.0 :gold 5000.0)
        result (ln/borrow state 50000.0)]
    (is (== 50000.0 (:loan result)))
    (is (== 55000.0 (:gold result)))))

(deftest borrow-near-limit-increases-interest
  (let [state (assoc (st/initial-state)
                :credit-limit 100000.0 :loan 85000.0
                :gold 5000.0 :int-addition 0.0)
        result (ln/borrow state 5000.0)]
    (is (== 90000.0 (:loan result)))
    (is (== 0.2 (:int-addition result)))))

(deftest borrow-exceeding-limit-returns-error
  (let [state (assoc (st/initial-state)
                :credit-limit 100000.0 :loan 80000.0 :gold 5000.0)
        result (ln/borrow state 30000.0)]
    (is (= "Exceeds credit limit" (:error result)))))

(deftest credit-check-recalculates-limit
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state)
                :credit-rating 0.8 :credit-lower 500000.0
                :gold 100000.0 :wheat 1000.0 :slaves 50.0
                :oxen 20.0 :horses 10.0
                :sl-health 0.8 :ox-health 0.9 :hs-health 0.7
                :ln-fallow 100.0
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0}
                :manure 200.0 :loan 50000.0)
        result (ln/credit-check rng state)]
    (is (> (:credit-limit result) 0))
    (is (>= (:credit-limit result) (:credit-lower state)))))

(deftest full-payoff
  (let [state (assoc (st/initial-state)
                :loan 50000.0 :gold 60000.0
                :credit-rating 0.7 :int-addition 1.0)
        result (ln/repay state 50000.0)]
    (is (== 0.0 (:loan result)))
    (is (== 10000.0 (:gold result)))
    (is (> (:credit-rating result) 0.7))
    (is (< (:int-addition result) 1.0))))

(deftest full-payoff-credit-formula
  (let [state (assoc (st/initial-state)
                :loan 50000.0 :gold 60000.0
                :credit-rating 0.7 :int-addition 1.0)
        result (ln/repay state 50000.0)]
    (is (< (Math/abs (- (:credit-rating result) 0.8)) 0.001))
    (is (< (Math/abs (- (:int-addition result) 0.8)) 0.001))))

(deftest partial-repayment
  (let [state (assoc (st/initial-state)
                :loan 100000.0 :gold 50000.0
                :credit-rating 0.8 :int-addition 1.0)
        result (ln/repay state 30000.0)]
    (is (== 70000.0 (:loan result)))
    (is (== 20000.0 (:gold result)))
    (is (>= (:credit-rating result) 0.8))))

(deftest cannot-repay-more-than-gold
  (let [state (assoc (st/initial-state) :loan 20000.0 :gold 10000.0)
        result (ln/repay state 20000.0)]
    (is (:error result))))

(deftest monthly-interest-deducted
  (let [state (assoc (st/initial-state)
                :loan 100000.0 :gold 50000.0
                :interest 5.0 :int-addition 2.0)
        result (ln/deduct-interest state)]
    (is (< (Math/abs (- (:gold result) (- 50000.0 (/ (* 100000.0 7.0) 1200.0)))) 0.01))))

(deftest credit-decays-with-loan
  (let [state (assoc (st/initial-state)
                :loan 50000.0 :credit-rating 0.8 :int-addition 1.0)
        result (ln/credit-update state)]
    (is (< (Math/abs (- (:credit-rating result) (* 0.8 0.96))) 0.001))
    (is (< (Math/abs (- (:int-addition result) (* 1.0 1.02))) 0.001))))

(deftest credit-recovers-without-loan
  (let [state (assoc (st/initial-state)
                :loan 0.0 :credit-rating 0.7 :int-addition 1.0)
        result (ln/credit-update state)]
    (is (< (Math/abs (- (:credit-rating result) (+ 0.7 (/ 0.3 10.0)))) 0.001))
    (is (< (Math/abs (- (:int-addition result) (* 1.0 0.95))) 0.001))))

(deftest emergency-loan-when-gold-negative
  (let [state (assoc (st/initial-state)
                :gold -5000.0 :loan 10000.0
                :credit-rating 0.8 :int-addition 0.5
                :credit-limit 100000.0)
        result (ln/emergency-loan state)]
    (is (>= (:gold result) 0.0))
    (is (> (:loan result) 10000.0))
    (is (< (:credit-rating result) 0.8))
    (is (> (:int-addition result) 0.5))))

(deftest no-emergency-when-gold-positive
  (let [state (assoc (st/initial-state) :gold 5000.0 :loan 0.0)
        result (ln/emergency-loan state)]
    (is (== 5000.0 (:gold result)))))

(deftest foreclosure-check
  (let [state (assoc (st/initial-state)
                :loan 500000.0 :credit-rating 0.1
                :gold 1000.0 :slaves 0.0 :oxen 0.0 :horses 0.0
                :ln-fallow 0.0 :wheat 0.0 :manure 0.0
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0})]
    (is (true? (ln/foreclosed? state)))))

(deftest no-foreclosure-with-assets
  (let [state (assoc (st/initial-state)
                :loan 50000.0 :credit-rating 0.8
                :gold 100000.0 :slaves 100.0 :oxen 50.0
                :ln-fallow 100.0
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0})]
    (is (false? (ln/foreclosed? state)))))

(deftest debt-warning-true-above-80-pct
  (let [state (assoc (st/initial-state)
                :loan 400000.0 :credit-rating 0.1
                :gold 1000.0 :slaves 1.0 :oxen 0.0 :horses 0.0
                :ln-fallow 1.0 :wheat 0.0 :manure 0.0
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0})]
    (is (true? (ln/debt-warning? state)))))

(deftest debt-warning-false-below-80-pct
  (let [state (assoc (st/initial-state)
                :loan 1000.0 :credit-rating 0.8
                :gold 100000.0 :slaves 100.0 :oxen 50.0
                :ln-fallow 100.0
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0})]
    (is (false? (ln/debt-warning? state)))))
