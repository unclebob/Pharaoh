(ns pharaoh.contracts-test
  (:require [clojure.test :refer :all]
            [pharaoh.contracts :as ct]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest make-players-creates-10
  (let [rng (r/make-rng 42)
        players (ct/make-players rng)]
    (is (== 10 (count players)))
    (doseq [p players]
      (is (<= 0.5 (:pay-k p) 1.0))
      (is (<= 0.5 (:ship-k p) 1.0))
      (is (<= 0.95 (:default-k p) 1.0)))))

(deftest make-contract-has-required-fields
  (let [rng (r/make-rng 42)
        state (st/initial-state)
        c (ct/make-contract rng state 0)]
    (is (contains? c :type))
    (is (contains? c :who))
    (is (contains? c :what))
    (is (contains? c :amount))
    (is (contains? c :price))
    (is (contains? c :duration))
    (is (#{:buy :sell} (:type c)))
    (is (#{:wheat :slaves :oxen :horses :manure :land} (:what c)))
    (is (pos? (:amount c)))
    (is (pos? (:price c)))
    (is (<= 12 (:duration c) 36))))

(deftest make-contract-price-uses-base-plus-exponential
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state)
                :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                         :horses 500.0 :oxen 300.0 :land 5000.0})
        prices (for [seed (range 100)]
                 (:price (ct/make-contract (r/make-rng seed) state 0)))]
    (is (every? pos? prices))))

(deftest new-offers-fills-slots
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :cont-offers []
                :players (ct/make-players (r/make-rng 1)))
        result (ct/new-offers rng state)]
    (is (== 15 (count (:cont-offers result))))))

(deftest new-offers-ages-surviving
  (let [rng (r/make-rng 42)
        offer {:type :buy :who 0 :what :wheat :amount 100.0
               :price 10.0 :duration 24 :active true :pct 0.0}
        state (assoc (st/initial-state)
                :cont-offers (vec (repeat 15 offer))
                :players (ct/make-players (r/make-rng 1)))
        result (ct/new-offers rng state)
        surviving (filter #(and (:active %) (== 24 (:duration %)))
                          (:cont-offers result))]
    (is (<= (count surviving) 15))))

(deftest accept-contract-moves-to-pending
  (let [offer {:type :buy :who 0 :what :wheat :amount 100.0
               :price 10.0 :duration 24 :active true :pct 0.0}
        state (assoc (st/initial-state) :cont-offers [offer] :cont-pend [])
        result (ct/accept-contract state 0)]
    (is (== 1 (count (:cont-pend result))))
    (is (not (:active (first (:cont-offers result)))))))

(deftest cannot-exceed-max-pending
  (let [offer {:type :buy :who 0 :what :wheat :amount 100.0
               :price 10.0 :duration 24 :active true :pct 0.0}
        pend (vec (repeat 10 offer))
        state (assoc (st/initial-state) :cont-offers [offer] :cont-pend pend)
        result (ct/accept-contract state 0)]
    (is (:error result))))

(deftest buy-contract-fulfillment-adds-goods
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0 :wheat 100.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (> (:wheat (:state result)) 100.0))))

(deftest sell-contract-fulfillment-adds-gold
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (< (:wheat (:state result)) 500.0))
    (is (> (:gold (:state result)) 50000.0))))

(deftest default-cancels-contract
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 0.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (not (:active (:contract result))))))

(deftest contract-progress-processes-all-pending
  (let [rng (r/make-rng 42)
        players (ct/make-players (r/make-rng 1))
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state)
                :gold 50000.0 :wheat 100.0
                :cont-pend [contract] :players players)
        result (ct/contract-progress rng state)]
    (is (vector? (:cont-pend result)))))
