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
        state (assoc (st/initial-state) :wheat 1000.0 :slaves 50.0
                :oxen 30.0 :horses 20.0 :manure 500.0 :ln-fallow 100.0
                :players (ct/make-players (r/make-rng 1)))
        c (ct/make-contract rng state [] [])]
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

(deftest make-contract-price-is-total-value
  ;; C: price = ceil(amount * unit_price * (0.4 + exponential(0.6)))
  ;; Price should be total contract value, not per-unit
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :wheat 1000.0 :slaves 50.0
                :oxen 30.0 :horses 20.0 :manure 500.0 :ln-fallow 100.0
                :players (ct/make-players (r/make-rng 1)))
        c (ct/make-contract rng state [] [])]
    ;; price should be > amount (for most commodities, since unit price > 1)
    (is (pos? (:price c)))
    ;; amount should be ceil'd
    (is (== (Math/ceil (:amount c)) (:amount c)))))

(deftest new-offers-fills-slots
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :cont-offers []
                :players (ct/make-players (r/make-rng 1)))
        result (ct/new-offers rng state)]
    (is (== 15 (count (:cont-offers result))))))

(deftest new-offers-decrements-duration-and-replaces
  ;; C: active offers with duration <= 8 are replaced; others have duration decremented
  (let [rng (r/make-rng 42)
        old-offer {:type :buy :who 0 :what :wheat :amount 100.0
                   :price 5000.0 :duration 24 :active true :pct 0.0}
        state (assoc (st/initial-state)
                :wheat 1000.0 :slaves 50.0 :oxen 30.0
                :horses 20.0 :manure 500.0 :ln-fallow 100.0
                :cont-offers (vec (repeat 15 old-offer))
                :players (ct/make-players (r/make-rng 1)))
        result (ct/new-offers rng state)
        surviving (filter #(and (:active %) (= 23 (:duration %)))
                          (:cont-offers result))]
    ;; Some offers should have been replaced (20% random), others aged (duration--)
    (is (pos? (count surviving)))))

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
  ;; C: gold += c->price (total value)
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 5000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 0.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (== 0.0 (:wheat (:state result))))
    (is (== 5000.0 (:gold (:state result))))
    (is (not (:active (:contract result))))))

(deftest settle-buy-insufficient-goods
  ;; C: ppu=price/amount, gold += myAmount*ppu - 0.1*remaining_price
  ;; price=5000, amount=500, ppu=10
  ;; myAmount=floor(300)=300, gold += 300*10=3000
  ;; remaining_price = 5000 * (1 - 300/500) = 2000
  ;; penalty = 0.1 * 2000 = 200, gold -= 200
  ;; net gold = 3000 - 200 = 2800
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 5000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 0.0 :wheat 300.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    (is (== 0.0 (:wheat s)))
    (is (== 2800.0 (:gold s)))
    (is (:active c))
    (is (== 200.0 (:amount c)))
    (is (== 2000.0 (:price c)))))

(deftest settle-buy-partial-pay
  ;; C: canBuy = ceil(amount * uniform(0.5, 0.95)), gold += canBuy*ppu + remaining*0.1
  (let [rng (r/make-rng 99)
        player {:pay-k 0.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 5000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 0.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    (is (< (:wheat s) 500.0))
    (is (> (:wheat s) 0.0))
    (is (> (:gold s) 0.0))
    (is (:active c))
    (is (< (:amount c) 500.0))))

(deftest settle-sell-full
  ;; C: gold -= c->price (total), receive full amount
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 100000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    (is (== 1000.0 (:wheat s)))
    (is (== 80000.0 (:gold s)))
    (is (not (:active c)))))

(deftest settle-sell-insufficient-gold
  ;; C: penalty = 0.1*c->price, then buy what you can with remaining gold
  ;; price=20000 (total), ppu=20, penalty=2000
  ;; gold after penalty = 15000-2000 = 13000
  ;; myAmount = floor(max(13000,0)/20) = 650
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 15000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    (is (== 650.0 (:wheat s)))
    (is (< (:gold s) 15000.0))
    (is (:active c))
    (is (== 350.0 (:amount c)))))

(deftest settle-sell-partial-ship
  ;; C: gold += c->price * 0.1 (bonus BEFORE adjustment), then pay for partial
  (let [rng (r/make-rng 99)
        player {:pay-k 1.0 :ship-k 0.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :wheat :amount 1000.0
                  :price 20000.0 :duration 1 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 100000.0 :wheat 0.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    (is (> (:wheat s) 0.0))
    (is (< (:wheat s) 1000.0))
    ;; Gold should include bonus: +0.1*20000=2000, minus partial payment
    (is (< (:gold s) 100000.0))
    (is (:active c))
    (is (< (:amount c) 1000.0))))

(deftest default-cancels-contract
  ;; C: gold += c->price * 0.05
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 0.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 1200.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (not (:active (:contract result))))
    ;; gold += 1200 * 0.05 = 60
    (is (== 50060.0 (:gold (:state result))))))

(deftest accept-contract-copies-to-pending
  (let [offer {:type :buy :who 0 :what :wheat :amount 100.0
               :price 1000.0 :duration 24 :active true :pct 0.0}
        state (assoc (st/initial-state) :cont-offers [offer] :cont-pend [])
        result (ct/accept-contract state 0)
        pending (first (:cont-pend result))]
    (is (= 24 (:duration pending)))))

(deftest fulfill-contract-decrements-duration
  ;; C: if (--(c->duration) <= 0) settle, else continue
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 1200.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 50000.0 :wheat 100.0)
        result (ct/fulfill-contract rng state contract [player])]
    (is (= 11 (:duration (:contract result))))))

(deftest contract-not-due-no-settlement
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :buy :who 0 :what :wheat :amount 500.0
                  :price 5000.0 :duration 6 :active true :pct 0.0}
        state (assoc (st/initial-state) :gold 1000.0 :wheat 500.0)
        result (ct/fulfill-contract rng state contract [player])
        s (:state result)
        c (:contract result)]
    ;; duration 6 → 5 after decrement, not due, no settlement
    (is (== 500.0 (:wheat s)))
    (is (== 1000.0 (:gold s)))
    (is (== 5 (:duration c)))
    (is (:active c))))

(deftest inc-commodity-blends-health
  (let [;; Player has 50 horses at 0.7 health, receives 100 at 0.9
        state (assoc (st/initial-state) :horses 50.0 :hs-health 0.7)
        result (ct/inc-commodity state :horses 100.0)]
    ;; blended = (0.7*50 + 0.9*100) / (50+100) = (35+90)/150 = 125/150 ≈ 0.8333
    (is (== 150.0 (:horses result)))
    (is (< (Math/abs (- (/ 125.0 150.0) (:hs-health result))) 0.001))))

(deftest settle-sell-blends-livestock-health
  ;; C: IncCommodity uses 0.9 nominal health
  (let [rng (r/make-rng 42)
        player {:pay-k 1.0 :ship-k 1.0 :default-k 1.0}
        contract {:type :sell :who 0 :what :horses :amount 100.0
                  :price 10000.0 :duration 1 :active true :pct 0.0}
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
                  :price 5000.0 :duration 1 :active true :pct 0.0}
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
                  :price 5000.0 :duration 6 :active true :pct 0.0}
        state (assoc (st/initial-state)
                :gold 0.0 :wheat 500.0
                :cont-pend [contract] :players players)
        result (ct/contract-progress rng state)]
    (is (seq (:contract-msgs result)))))

(deftest contract-progress-processes-all-pending
  (let [rng (r/make-rng 42)
        players (ct/make-players (r/make-rng 1))
        contract {:type :buy :who 0 :what :wheat :amount 120.0
                  :price 1200.0 :duration 12 :active true :pct 0.0}
        state (assoc (st/initial-state)
                :gold 50000.0 :wheat 100.0
                :cont-pend [contract] :players players)
        result (ct/contract-progress rng state)]
    (is (vector? (:cont-pend result)))))
