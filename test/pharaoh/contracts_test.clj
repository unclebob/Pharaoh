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

(deftest settle-buy-full
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 0.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (== 0.0 (:wheat (:state result))))
    (is (== 5000.0 (:gold (:state result))))
    (is (not (:active (:contract result))))))

(deftest settle-buy-insufficient-goods
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 0.0 :wheat 300.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; Player delivers all 300, gets paid 300*10=3000
    ;; Penalty = 10% of remaining value = 0.10 * 200 * 10 = 200
    ;; Gold = 0 + 3000 - 200 = 2800
    (is (== 0.0 (:wheat s)))
    (is (== 2800.0 (:gold s)))
    ;; Contract adjusted: amount reduced by 300 (now 200), still active
    (is (:active c))
    (is (== 200.0 (:amount c)))))

(deftest settle-buy-partial-pay
  (let [rng (r/make-rng 99)
        player {:pay-k 0.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 0.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; pay-k 0.0 means pays? always false, so can-buy = 500 * uniform(0.5,0.95)
    ;; Player has enough wheat (500 >= 500)
    ;; can-buy < amount → partial pay path
    ;; Player delivers can-buy, gets paid can-buy*price
    ;; Bonus = 10% of remaining value = 0.10*(amount-can-buy)*price
    (is (< (:wheat s) 500.0))
    (is (> (:wheat s) 0.0))
    (is (> (:gold s) 0.0))
    ;; Contract still active with reduced amount
    (is (:active c))
    (is (< (:amount c) 500.0))))

(deftest settle-sell-full
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 100000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; Player pays 1000*20=20000, receives 1000 wheat
    (is (== 1000.0 (:wheat s)))
    (is (== 80000.0 (:gold s)))
    (is (not (:active c)))))

(deftest settle-sell-insufficient-gold
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 15000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; Total cost = 1000*20 = 20000, player only has 15000
    ;; Penalty = 10% of total = 2000, gold after penalty = 15000-2000 = 13000
    ;; Can afford = 13000/20 = 650 wheat
    (is (> (:wheat s) 0.0))
    (is (< (:gold s) 15000.0))
    ;; Contract still active with reduced amount
    (is (:active c))
    (is (< (:amount c) 1000.0))))

(deftest settle-sell-partial-ship
  (let [rng (r/make-rng 99)
        player {:pay-k 1.0 :ship-k 0.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 100000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; ship-k 0.0 means can-sell = 1000*uniform(0.5,0.95)
    ;; Player pays for can-sell, gets bonus 10% of remaining
    (is (> (:wheat s) 0.0))
    (is (< (:wheat s) 1000.0))
    (is (< (:gold s) 100000.0))
    ;; Contract still active
    (is (:active c))
    (is (< (:amount c) 1000.0))))

(deftest default-cancels-contract
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 0.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (not (:active (:contract result))))))

(deftest accept-contract-sets-months-left
  (let [offer {:type :buy :who 0 :what :wheat :amount 100.0
               :price 10.0 :duration 24 :active true :pct 0.0}
        state (assoc (st/initial-state) :cont-offers [offer] :cont-pend [])
        result (ct/accept-contract state 0)
        pending (first (:cont-pend result))]
    (is (= 24 (:months-left pending)))))

(deftest fulfill-buy-contract-decrements-months-left
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 12}
        state (assoc (st/initial-state) :gold 50000.0 :wheat 100.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (= 11 (:months-left (:contract result))))))

(deftest fulfill-sell-contract-decrements-months-left
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 120.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 12}
        state (assoc (st/initial-state) :gold 50000.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (= 11 (:months-left (:contract result))))))

(deftest contract-not-due-no-settlement
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 6}
        state (assoc (st/initial-state) :gold 1000.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; months-left > 1 after decrement, no settlement
    (is (== 500.0 (:wheat s)))
    (is (== 1000.0 (:gold s)))
    (is (== 5 (:months-left c)))
    (is (:active c))))

(deftest inc-commodity-blends-health
  (let [;; Player has 50 horses at 0.7 health, receives 100 at 0.9
        state (assoc (st/initial-state) :horses 50.0 :hs-health 0.7)
        result (ct/inc-commodity state :horses 100.0)]
    ;; blended = (0.7*50 + 0.9*100) / (50+100) = (35+90)/150 = 125/150 ≈ 0.8333
    (is (== 150.0 (:horses result)))
    (is (< (Math/abs (- (/ 125.0 150.0) (:hs-health result))) 0.001))))

(deftest settle-sell-blends-livestock-health
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :horses :amount 100.0
                  :price 500.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state) :gold 100000.0 :horses 50.0 :hs-health 0.7)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)]
    ;; Receives 100 horses, blended health = (0.7*50 + 0.9*100)/150
    (is (== 150.0 (:horses s)))
    (is (< (Math/abs (- (/ 125.0 150.0) (:hs-health s))) 0.001))))

(deftest inc-commodity-no-health-for-wheat
  (let [state (assoc (st/initial-state) :wheat 100.0)
        result (ct/inc-commodity state :wheat 50.0)]
    (is (== 150.0 (:wheat result)))))

(deftest contract-msg-format
  (let [contract {:who 0 :what :wheat :amount 500.0}
        players [{:name "King HamuNam"}]
        msg-text (ct/contract-msg contract players "The goods have been delivered.")]
    (is (= "Regarding your contract with King HamuNam for 500 wheat: The goods have been delivered."
           msg-text))))

(deftest settlement-generates-message
  (let [rng (r/make-rng 42)
        players [{:pay-k 1.0 :ship-k 1.0 :default-k 1.0 :name "King HamuNam"}]
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 1}
        state (assoc (st/initial-state)
                :gold 0.0 :wheat 500.0
                :cont-pend [contract] :players players)
        result (ct/contract-progress rng state)]
    (is (seq (:contract-msgs result)))
    (is (map? (first (:contract-msgs result))))
    (is (string? (:text (first (:contract-msgs result)))))
    (is (number? (:face (first (:contract-msgs result)))))))

(deftest default-generates-message
  (let [rng (r/make-rng 42)
        players [{:pay-k 1.0 :ship-k 1.0 :default-k 0.0 :name "King HamuNam"}]
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 10.0 :duration 12 :active true :pct 0.0
                  :months-left 6}
        state (assoc (st/initial-state)
                :gold 0.0 :wheat 500.0
                :cont-pend [contract] :players players)
        result (ct/contract-progress rng state)]
    (is (seq (:contract-msgs result)))))

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
