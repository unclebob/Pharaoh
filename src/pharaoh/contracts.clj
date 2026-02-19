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

(defn make-contract [rng state who]
  (let [what (commodities (long (r/uniform rng 0 5.999)))
        base-price (get-in state [:prices what] 100.0)
        price (* base-price (+ 0.4 (r/exponential rng 0.6)))
        typ (if (< (r/uniform rng 0 1) 0.5) :buy :sell)
        amount (max 1.0 (r/exponential rng (* 10.0 (+ 1.0 (r/uniform rng 0 2)))))
        duration (long (r/uniform rng 12 36))]
    {:type typ :who who :what what :amount amount
     :price price :duration duration :active true :pct 0.0}))

(defn- already-trading? [offers who what]
  (some #(and (:active %) (== (:who %) who) (= (:what %) what)) offers))

(defn- age-offer [rng offer]
  (if (not (:active offer))
    offer
    (let [pct (r/uniform rng 0.01 0.10)]
      (if (= :buy (:type offer))
        (update offer :price * (+ 1.0 pct))
        (update offer :price * (- 1.0 pct))))))

(defn new-offers [rng state]
  (let [players (:players state)
        offers (:cont-offers state)
        aged (mapv (partial age-offer rng) offers)
        n-existing (count (filter :active aged))
        n-needed (- max-offers n-existing)
        new-contracts
        (loop [acc [] tries 0]
          (if (or (>= (count acc) n-needed) (> tries (* n-needed 3)))
            acc
            (let [who (long (r/uniform rng 0 (dec (count players))))
                  c (make-contract rng state who)
                  all-offers (concat aged acc)]
              (if (already-trading? all-offers who (:what c))
                (recur acc (inc tries))
                (recur (conj acc c) (inc tries))))))
        ;; Replace inactive slots with new contracts
        result (loop [offers aged new-list new-contracts]
                 (if (empty? new-list)
                   offers
                   (let [idx (first (keep-indexed
                                     (fn [i o] (when (not (:active o)) i))
                                     offers))]
                     (if idx
                       (recur (assoc offers idx (first new-list))
                              (rest new-list))
                       ;; Append if no inactive slots
                       (into offers new-list)))))]
    (assoc state :cont-offers
           (vec (take max-offers
                      (if (< (count result) max-offers)
                        (into result (repeat (- max-offers (count result))
                                            {:active false}))
                        result))))))

(defn accept-contract [state offer-idx]
  (if (>= (count (filter :active (:cont-pend state))) max-pend)
    {:error "Maximum pending contracts reached"}
    (let [offer (get (:cont-offers state) offer-idx)]
      (if (not (:active offer))
        {:error "Contract not active"}
        (-> state
            (update :cont-offers assoc offer-idx
                    (assoc offer :active false))
            (update :cont-pend conj
                    (assoc offer :months-left (:duration offer))))))))

(defn contract-msg [contract players pool-msg]
  (let [name (get-in players [(:who contract)] {:name "Unknown"})
        name (if (string? name) name (:name name))]
    (str "Regarding your contract with " name
         " for " (long (:amount contract)) " " (clojure.core/name (:what contract))
         ": " pool-msg)))

(defn- settle-buy [rng state contract player]
  (let [ptr (commodity-ptr (:what contract))
        amount (:amount contract)
        price (:price contract)
        pays? (< (r/uniform rng 0 1) (:pay-k player))
        can-buy (if pays? amount (* amount (r/uniform rng 0.5 0.95)))
        available (get state ptr 0.0)]
    (cond
      (< available amount)
      (let [paid (* available price)
            penalty (* 0.10 (* (- amount available) price))]
        {:state (-> state (assoc ptr 0.0) (update :gold + paid (- penalty)))
         :contract (-> contract (update :amount - available) (assoc :months-left 1))
         :msg-pool msg/contract-insufficient-goods-messages})

      (< can-buy amount)
      (let [paid (* can-buy price)
            bonus (* 0.10 (* (- amount can-buy) price))]
        {:state (-> state (update ptr - can-buy) (update :gold + paid bonus))
         :contract (-> contract (update :amount - can-buy) (assoc :months-left 1))
         :msg-pool msg/contract-partial-pay-messages})

      :else
      (let [total (* price amount)]
        {:state (-> state (update ptr - amount) (update :gold + total))
         :contract (assoc contract :active false)
         :msg-pool msg/buy-complete-messages}))))

(defn- settle-sell [rng state contract player]
  (let [what (:what contract)
        amount (:amount contract)
        price (:price contract)
        total (* price amount)
        gold (:gold state)
        ships? (< (r/uniform rng 0 1) (:ship-k player))
        can-sell (if ships? amount (* amount (r/uniform rng 0.5 0.95)))]
    (cond
      (< gold total)
      (let [penalty (* 0.10 total)
            gold-left (- gold penalty)
            can-afford (max 0.0 (/ gold-left price))]
        {:state (-> state (inc-commodity what can-afford)
                    (update :gold - (* can-afford price) penalty))
         :contract (-> contract (update :amount - can-afford) (assoc :months-left 1))
         :msg-pool msg/contract-insufficient-funds-messages})

      (< can-sell amount)
      (let [paid (* can-sell price)
            bonus (* 0.10 (* (- amount can-sell) price))]
        {:state (-> state (inc-commodity what can-sell)
                    (update :gold - paid (- bonus)))
         :contract (-> contract (update :amount - can-sell) (assoc :months-left 1))
         :msg-pool msg/contract-partial-ship-messages})

      :else
      {:state (-> state (inc-commodity what amount) (update :gold - total))
       :contract (assoc contract :active false)
       :msg-pool msg/contract-complete-messages})))

(defn fulfill-contract [rng state contract players]
  (let [player (get players (:who contract))
        defaults? (< (r/uniform rng 0 1) (- 1.0 (:default-k player)))]
    (if defaults?
      (let [penalty (* (:amount contract) (:price contract) 0.05)]
        {:state (update state :gold + penalty)
         :contract (assoc contract :active false)
         :msg-pool msg/contract-default-messages})
      (let [ml (or (:months-left contract) (:duration contract))
            contract (assoc contract :months-left (dec ml))]
        (if (<= (:months-left contract) 0)
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
