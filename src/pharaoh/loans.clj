(ns pharaoh.loans
  (:require [pharaoh.economy :as ec]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.tables :as t]))

(defn credit-check-fee [rng total-debt]
  (* total-debt (r/gaussian rng 0.05 0.01)))

(defn real-net-worth [state]
  (let [p (:prices state)
        lt (st/total-land state)]
    (+ (* (:slaves state) (:slaves p) (:sl-health state))
       (* (:oxen state) (:oxen p) (:ox-health state))
       (* (:horses state) (:horses p) (:hs-health state))
       (* lt (:land p))
       (* (:manure state) (:manure p))
       (* (:wheat state) (:wheat p))
       (:gold state)
       (- (:loan state)))))

(defn borrow [rng state amt]
  (if (> (+ (:loan state) amt) (:credit-limit state))
    (let [total-debt (+ (:loan state) amt)]
      {:needs-credit-check true
       :fee (credit-check-fee rng total-debt)})
    (let [new-loan (+ (:loan state) amt)
          headroom (/ (- (:credit-limit state) new-loan)
                      (:credit-limit state))
          state (-> state
                    (assoc :loan new-loan)
                    (update :gold + amt))]
      (if (< headroom 0.2)
        (update state :int-addition + 0.2)
        state))))

(defn credit-check [rng state]
  (let [rnw (real-net-worth state)
        new-limit (* rnw (:credit-rating state))]
    (assoc state :credit-limit
           (max new-limit (:credit-lower state)))))

(defn repay [state amt]
  (cond
    (> amt (:gold state))
    {:error "Cannot repay more than available gold"}

    (>= amt (- (:loan state) 0.001))
    (-> state
        (update :gold - (:loan state))
        (assoc :loan 0.0)
        (update :credit-rating
                (fn [cr] (+ cr (/ (- 1.0 cr) 3.0))))
        (update :int-addition * 0.80))

    :else
    (let [ratio (/ amt (:loan state))
          repay-idx (t/interpolate ratio t/repay-index)]
      (-> state
          (update :gold - amt)
          (update :loan - amt)
          (update :credit-rating
                  (fn [cr] (min 1.0 (* cr repay-idx))))
          (update :int-addition / repay-idx)))))

(defn deduct-interest [state]
  (let [monthly-rate (/ (+ (:interest state) (:int-addition state))
                        1200.0)
        payment (* (:loan state) monthly-rate)]
    (update state :gold - payment)))

(defn credit-update [state]
  (if (pos? (:loan state))
    (-> state
        (update :credit-rating * 0.96)
        (update :int-addition * 1.02))
    (-> state
        (update :credit-rating
                (fn [cr] (+ cr (/ (- 1.0 cr) 10.0))))
        (update :int-addition * 0.95))))

(defn emergency-loan [state]
  (if (>= (:gold state) 0)
    state
    (let [deficit (Math/abs (:gold state))
          em-loan (* deficit 1.1)]
      (-> state
          (update :gold + em-loan)
          (update :loan + em-loan)
          (update :credit-rating
                  (fn [cr] (- cr (/ (- 1.0 cr) 3.0))))
          (update :int-addition + 0.2)))))

(defn foreclosed? [state]
  (if (<= (:loan state) 0)
    false
    (let [nw (ec/net-worth state)
          debt-asset (if (pos? nw) (/ (:loan state) nw) 100.0)
          limit (t/interpolate (:credit-rating state) t/debt-support)]
      (> debt-asset limit))))

(defn debt-warning? [state]
  (if (<= (:loan state) 0)
    false
    (let [nw (ec/net-worth state)
          debt-asset (if (pos? nw) (/ (:loan state) nw) 100.0)
          limit (t/interpolate (:credit-rating state) t/debt-support)]
      (> debt-asset (* limit 0.8)))))
