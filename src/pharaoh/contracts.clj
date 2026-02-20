(ns pharaoh.contracts
  (:require [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(def max-offers 15)
(def max-pend 10)
(def max-players 10)

(def commodities [:wheat :slaves :oxen :horses :manure :land])

(defn make-players [rng]
  (vec (for [i (range max-players)]
         {:pay-k (r/max-random rng 2 0.5 1.0)
          :ship-k (r/max-random rng 2 0.5 1.0)
          :default-k (r/max-random rng 5 0.95 1.0)
          :name (get msg/player-names i
                     (str "King " i))})))

(defn- commodity-ptr [what]
  (case what
    :wheat :wheat :slaves :slaves :oxen :oxen
    :horses :horses :manure :manure :land :ln-fallow))

(defn- health-ptr [what]
  (case what
    :slaves :sl-health :oxen :ox-health :horses :hs-health nil))

(defn inc-commodity [state what added]
  (let [ptr (commodity-ptr what)
        h-ptr (health-ptr what)
        existing (get state ptr 0.0)]
    (if (and h-ptr (pos? added))
      (let [old-h (get state h-ptr 0.8)
            new-h (/ (+ (* old-h existing) (* 0.9 added))
                     (+ existing added))]
        (-> state
            (update ptr + added)
            (assoc h-ptr new-h)))
      (update state ptr + added))))

;; C: AlreadyTrading checks both contOffers and contPend arrays
(defn- already-trading? [offers pending who what]
  (or (some #(and (== (:who %) who) (= (:what %) what)) offers)
      (some #(and (== (:who %) who) (= (:what %) what)) pending)))

;; C: MakeContract — amount from stock, price is total contract value
(defn make-contract [rng state offers pending]
  (let [n-players (count (:players state))
        max-combos (* n-players (count commodities))]
    (loop [tries 0]
      (if (>= tries max-combos)
        nil
      (let [who (long (r/uniform rng 0.0 (- n-players 0.01)))
            what (commodities (long (r/uniform rng 0 5.999)))]
        (if (already-trading? offers pending who what)
          (recur (inc tries))
          (let [typ (if (< (r/uniform rng 0.0 1.0) 0.5) :buy :sell)
                ptr (commodity-ptr what)
                stock (get state ptr 0.0)
                unit-price (get-in state [:prices what] 100.0)
                min-amount (/ 200000.0 unit-price)
                working (r/gaussian rng (* stock 6.0) (* stock 2.0))
                amount (Math/ceil (max working min-amount))
                price (Math/ceil (* amount unit-price
                                    (+ 0.4 (r/exponential rng 0.6))))
                duration (long (r/uniform rng 12.0 36.0))]
            {:type typ :who who :what what :amount amount
             :price price :duration duration :active true :pct 0.0})))))))

;; C: NewOffers — slot-by-slot: replace if duration<=8 or 20% random, else age
(defn new-offers [rng state]
  (if (empty? (:players state))
    state
  (let [pending (:cont-pend state)
        init-offers (let [o (or (:cont-offers state) [])]
                      (vec (take max-offers
                                 (concat o (repeat (- max-offers (count o))
                                                   {:active false :who -1 :what nil})))))]
    (assoc state :cont-offers
           (loop [i 0 offers init-offers]
             (if (>= i max-offers)
               offers
               (let [offer (nth offers i)]
                 (if (:active offer)
                   (if (or (<= (:duration offer) 8)
                           (< (r/uniform rng 0.0 1.0) 0.2))
                     (let [temp (assoc offers i {:active false :who -1 :what nil})
                           c (make-contract rng state temp pending)]
                       (recur (inc i) (assoc offers i (or c offer))))
                     (let [drift (if (= :buy (:type offer))
                                  (r/uniform rng 1.01 1.1)
                                  (r/uniform rng 0.90 0.99))]
                       (recur (inc i)
                              (assoc offers i (-> offer
                                                  (update :duration dec)
                                                  (update :price * drift))))))
                   (let [c (make-contract rng state offers pending)]
                     (recur (inc i) (if c (assoc offers i c) offers)))))))))))


;; C: movmem(c, &contPend[i]) — just copy the offer to pending
(defn accept-contract [state offer-idx]
  (if (>= (count (filter :active (:cont-pend state))) max-pend)
    {:error "Maximum pending contracts reached"}
    (let [offer (get (:cont-offers state) offer-idx)]
      (if (not (:active offer))
        {:error "Contract not active"}
        (-> state
            (update :cont-offers assoc offer-idx
                    (assoc offer :active false))
            (update :cont-pend conj offer))))))

(defn contract-msg [contract players pool-msg]
  (let [name (get-in players [(:who contract)] {:name "Unknown"})
        name (if (string? name) name (:name name))]
    (str "Regarding your contract with " name
         " for " (long (:amount contract)) " " (clojure.core/name (:what contract))
         ": " pool-msg)))

;; C: ContProg BUY settlement — ppu = price/amount, C-exact math
(defn- settle-buy [rng state contract player]
  (let [ptr (commodity-ptr (:what contract))
        amount (:amount contract)
        price (:price contract)
        ppu (/ price amount)
        pays? (< (r/uniform rng 0 1) (:pay-k player))
        can-buy (if pays? amount (Math/ceil (* amount (r/uniform rng 0.5 0.95))))
        my-amount (Math/floor (get state ptr 0.0))]
    (cond
      (< my-amount can-buy)
      (let [new-price (* price (- 1.0 (/ my-amount amount)))
            new-amount (- amount my-amount)]
        {:state (-> state (assoc ptr 0.0)
                    (update :gold + (* my-amount ppu))
                    (update :gold - (* 0.1 new-price)))
         :contract (assoc contract :price new-price :amount new-amount)
         :msg-pool msg/contract-insufficient-goods-messages})

      (< can-buy amount)
      (let [new-price (* price (- 1.0 (/ can-buy amount)))
            new-amount (- amount can-buy)]
        {:state (-> state (update ptr - can-buy)
                    (update :gold + (* can-buy ppu) (* 0.1 new-price)))
         :contract (assoc contract :price new-price :amount new-amount)
         :msg-pool msg/contract-partial-pay-messages})

      :else
      {:state (-> state (update ptr - can-buy) (update :gold + price))
       :contract (assoc contract :active false)
       :msg-pool msg/buy-complete-messages})))

;; C: ContProg SELL settlement — ppu = price/amount, C-exact math
(defn- settle-sell [rng state contract player]
  (let [what (:what contract)
        amount (:amount contract)
        price (:price contract)
        ppu (/ price amount)
        gold (:gold state)
        ships? (< (r/uniform rng 0 1) (:ship-k player))
        can-sell (if ships? amount (Math/ceil (* amount (r/uniform rng 0.5 0.95))))
        sell-price (* can-sell ppu)]
    (cond
      (< gold sell-price)
      (let [gold-after (- gold (* price 0.1))
            my-amount (Math/floor (/ (max gold-after 0.0) ppu))
            new-price (* price (- 1.0 (/ my-amount amount)))
            new-amount (- amount my-amount)]
        {:state (-> state
                    (inc-commodity what my-amount)
                    (assoc :gold (- gold-after (* my-amount ppu))))
         :contract (assoc contract :price new-price :amount new-amount)
         :msg-pool msg/contract-insufficient-funds-messages})

      (< can-sell amount)
      (let [new-price (* price (- 1.0 (/ can-sell amount)))
            new-amount (- amount can-sell)]
        {:state (-> state
                    (update :gold + (* price 0.1))
                    (update :gold - (* can-sell ppu))
                    (inc-commodity what can-sell))
         :contract (assoc contract :price new-price :amount new-amount)
         :msg-pool msg/contract-partial-ship-messages})

      :else
      {:state (-> state (inc-commodity what amount)
                  (update :gold - price))
       :contract (assoc contract :active false)
       :msg-pool msg/contract-complete-messages})))

;; C: ContProg — default check, then --duration <= 0 triggers settlement
(defn fulfill-contract [rng state contract players]
  (let [player (get players (:who contract))
        defaults? (< (r/uniform rng 0 1) (- 1.0 (:default-k player)))]
    (if defaults?
      {:state (update state :gold + (* (:price contract) 0.05))
       :contract (assoc contract :active false)
       :msg-pool msg/contract-default-messages}
      (let [dur (dec (:duration contract))
            contract (assoc contract :duration dur)]
        (if (<= dur 0)
          (if (= :buy (:type contract))
            (settle-buy rng state contract player)
            (settle-sell rng state contract player))
          {:state state :contract contract})))))

(defn contract-progress [rng state]
  (let [players (:players state)]
    (loop [pend (:cont-pend state)
           idx 0
           s state
           new-pend []
           msgs (vec (:contract-msgs state))]
      (if (>= idx (count pend))
        (assoc s :cont-pend (vec (filter :active new-pend))
                 :contract-msgs msgs)
        (let [c (nth pend idx)]
          (if (not (:active c))
            (recur pend (inc idx) s (conj new-pend c) msgs)
            (let [{:keys [state contract msg-pool]}
                  (fulfill-contract rng s c players)
                  msgs (if msg-pool
                         (let [txt (contract-msg c players (msg/pick rng msg-pool))
                               face (mod (:who c) 4)]
                           (conj msgs {:text txt :face face}))
                         msgs)]
              (recur pend (inc idx) state (conj new-pend contract) msgs))))))))
