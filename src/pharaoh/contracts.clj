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

(defn- monthly-amount [contract]
  (/ (:amount contract) (:duration contract)))

(defn fulfill-contract [rng state contract players]
  (let [player (get players (:who contract))
        defaults? (< (r/uniform rng 0 1) (- 1.0 (:default-k player)))]
    (if defaults?
      (let [penalty (* (:amount contract) (:price contract) 0.05)]
        {:state (update state :gold + penalty)
         :contract (assoc contract :active false)})
      (let [monthly (monthly-amount contract)
            ptr (commodity-ptr (:what contract))
            h-ptr (health-ptr (:what contract))]
        (if (= :buy (:type contract))
          ;; BUY: counterparty ships goods, player pays
          (let [pays? (< (r/uniform rng 0 1) (:pay-k player))
                ships? (< (r/uniform rng 0 1) (:ship-k player))
                ship-amt (if ships? monthly (* monthly (r/uniform rng 0.3 0.8)))
                pay-amt (* ship-amt (:price contract))
                cost (if pays? pay-amt (* pay-amt (r/uniform rng 0.5 0.9)))]
            {:state (-> state
                        (update ptr + ship-amt)
                        (update :gold - cost))
             :contract (-> contract
                          (update :pct + (/ 1.0 (:duration contract)))
                          (update :months-left #(some-> % dec)))})
          ;; SELL: player ships goods, counterparty pays
          (let [available (get state ptr 0.0)
                ship-amt (min monthly available)
                pays? (< (r/uniform rng 0 1) (:pay-k player))
                pay-amt (* ship-amt (:price contract))
                income (if pays? pay-amt (* pay-amt (r/uniform rng 0.5 0.9)))]
            {:state (-> state
                        (update ptr - ship-amt)
                        (update :gold + income))
             :contract (-> contract
                          (update :pct + (/ 1.0 (:duration contract)))
                          (update :months-left #(some-> % dec)))}))))))

(defn contract-progress [rng state]
  (let [players (:players state)]
    (loop [pend (:cont-pend state)
           idx 0
           s state
           new-pend []]
      (if (>= idx (count pend))
        (assoc s :cont-pend (vec (filter :active new-pend)))
        (let [c (nth pend idx)]
          (if (not (:active c))
            (recur pend (inc idx) s (conj new-pend c))
            (let [{:keys [state contract]}
                  (fulfill-contract rng s c players)
                  done? (>= (:pct contract) 0.999)
                  contract (if done? (assoc contract :active false) contract)]
              (recur pend (inc idx) state (conj new-pend contract)))))))))
