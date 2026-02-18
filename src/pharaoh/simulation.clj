(ns pharaoh.simulation
  (:require [pharaoh.contracts :as ct]
            [pharaoh.economy :as ec]
            [pharaoh.events :as ev]
            [pharaoh.feeding :as fd]
            [pharaoh.health :as hl]
            [pharaoh.loans :as ln]
            [pharaoh.overseers :as ov]
            [pharaoh.planting :as pl]
            [pharaoh.pyramid :as py]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.tables :as t]
            [pharaoh.util :as u]
            [pharaoh.workload :as wk]))

(defn- advance-date [state]
  (if (>= (:month state) 12)
    (assoc state :month 1 :year (inc (:year state)))
    (update state :month inc)))

(defn- record-old [state]
  (assoc state
    :old-gold (:gold state) :old-wheat (:wheat state)
    :old-slaves (:slaves state) :old-oxen (:oxen state)
    :old-horses (:horses state) :old-manure (:manure state)))

(defn- compute-workload [state rng]
  (let [{:keys [req-work avg-py-height]} (wk/required-work rng state)
        cap (wk/slave-capacity rng state)
        {:keys [wk-sl wk-deff-sl sl-eff] :as eff}
        (wk/compute-efficiency (:slaves state) (:max-wk-sl cap) req-work)]
    (merge state cap eff
           {:req-work req-work :avg-py-height avg-py-height
            :sl-eff (min sl-eff 1.0)})))

(defn- compute-feeding [state rng]
  (let [sl-eff (:sl-eff state)
        usage (fd/wheat-usage state sl-eff)
        wt-rotted (pl/wheat-rot rng (:wheat state) (:wt-rot-rt state))
        wheat-left (- (:wheat state) wt-rotted)
        usage (fd/apply-wheat-shortage usage wheat-left)
        wt-eaten (+ (:wt-fed-sl usage) (:wt-fed-ox usage) (:wt-fed-hs usage))
        mn-made (fd/manure-made rng wt-eaten)
        mn-spread (min (:mn-to-sprd state) (:manure state))
        mn-used (* mn-spread sl-eff)
        mn-ln (if (pos? (:ln-to-sew state))
                (/ mn-spread (:ln-to-sew state)) 0)]
    (merge state usage
           {:wt-rotted wt-rotted :wt-eaten wt-eaten
            :mn-made mn-made :mn-spread mn-spread :mn-used mn-used
            :mn-ln mn-ln})))

(defn- apply-planting [state rng]
  (let [sl-eff (:sl-eff state)
        sew-rt (:sew-rt state)
        land (pl/land-cycle state sew-rt sl-eff)
        wt-yield (pl/wheat-yield rng (:month state) (:mn-ln state))
        wt-cycle (pl/wheat-cycle state rng wt-yield sew-rt sl-eff
                                 (:wt-to-sew state))
        wt-harvested (:sythed wt-cycle)
        tot-wt-used (+ (:total state) (:wt-rotted state))]
    (merge state land wt-cycle
           {:wt-harvested wt-harvested
            :wheat (max 0.0 (+ (:wheat state) wt-harvested (- tot-wt-used)))
            :manure (max 0.0 (+ (:manure state) (:mn-made state) (- (:mn-used state))))})))

(defn- apply-populations [state]
  (let [sl-new (hl/population-update (:slaves state) (:sl-brth-k state) (:sl-dth-k state))
        ox-new (hl/population-update (:oxen state) (:ox-brth-k state) (:ox-dth-k state))
        hs-new (hl/population-update (:horses state) (:hs-brth-k state) (:hs-dth-k state))]
    (assoc state :slaves sl-new :oxen ox-new :horses hs-new)))

(defn- apply-health [state rng]
  (let [sl-h (hl/slave-health-update rng state (:sl-fed state)
                                     (get state :sl-lash-rt 0)
                                     (get state :wk-sl 0)
                                     (get state :ox-mult 1))
        ox-h (hl/oxen-health-update rng state (:ox-fed state))
        hs-h (hl/horse-health-update rng state (:hs-fed state))]
    (merge state sl-h ox-h hs-h)))

(defn- apply-pyramid [state]
  (let [sl-eff (:sl-eff state)
        py-added (* (:py-quota state) sl-eff)
        new-stones (+ (:py-stones state) py-added)
        new-height (py/py-height (:py-base state) new-stones)]
    (assoc state
      :py-stones new-stones
      :py-height new-height
      :py-added py-added)))

(defn- apply-overseer-stress [state]
  (let [wk-deff-sl (get state :wk-deff-sl 0)
        new-press (ov/overseer-stress state wk-deff-sl)]
    (assoc state :ov-press new-press)))

(defn- apply-costs [state rng]
  (let [ov-cost (* (:overseers state) (:ov-pay state))
        own-cost (ec/ownership-cost rng state)
        py-cost (* (get state :avg-py-height 0) (get state :py-added 0))]
    (update state :gold - ov-cost own-cost py-cost)))

(defn- apply-market [state rng]
  (let [inflation (ec/update-inflation rng (:inflation state))
        update-p (fn [prices k]
                   (update prices k #(ec/update-price rng % inflation)))
        new-prices (reduce update-p (:prices state)
                          [:wheat :land :horses :oxen :slaves :manure])
        ov-pay (* (:ov-pay state) (r/abs-gaussian rng (+ 1.0 inflation) 0.02))
        interest (* (:interest state) (r/abs-gaussian rng (+ 1.0 inflation) 0.02))
        update-commodity (fn [state k]
                           (let [cm {:supply (get-in state [:supply k])
                                     :demand (get-in state [:demand k])
                                     :production (get-in state [:production k])
                                     :price (get-in state [:prices k])}
                                 result (ec/adjust-production rng cm (:world-growth state))]
                             (-> state
                                 (assoc-in [:supply k] (:supply result))
                                 (assoc-in [:demand k] (:demand result))
                                 (assoc-in [:production k] (:production result))
                                 (assoc-in [:prices k] (:price result)))))]
    (-> (reduce update-commodity state [:wheat :manure :slaves :horses :oxen :land])
        (assoc :prices new-prices :inflation inflation
               :ov-pay ov-pay :interest interest))))

(defn- check-overseers-unpaid [state rng]
  (if (and (< (:gold state) 0) (> (:overseers state) 0.5))
    (ov/overseers-quit rng state)
    state))

(defn- apply-loan-interest [state]
  (ln/deduct-interest state))

(defn- apply-credit-update [state]
  (ln/credit-update state))

(defn- check-emergency-loan [state]
  (ln/emergency-loan state))

(defn- check-foreclosure [state]
  (if (ln/foreclosed? state)
    (assoc state :game-over true)
    state))

(defn- check-win [state]
  (if (py/won? (:py-base state) (:py-height state))
    (assoc state :game-won true)
    state))

(defn- process-contracts [state rng]
  (ct/contract-progress rng state))

(defn- update-net-worth [state]
  (let [nw (ec/net-worth state)
        debt-asset (if (and (pos? nw) (pos? (:loan state)))
                     (/ (:loan state) nw) 0)]
    (assoc state :net-worth (- nw (:loan state)) :debt-asset debt-asset)))

(defn run-month [rng state]
  (-> state
      record-old
      advance-date
      (compute-workload rng)
      (compute-feeding rng)
      (apply-planting rng)
      apply-populations
      (apply-health rng)
      apply-pyramid
      apply-overseer-stress
      (apply-costs rng)
      (process-contracts rng)
      (check-overseers-unpaid rng)
      apply-loan-interest
      (apply-market rng)
      apply-credit-update
      check-emergency-loan
      update-net-worth
      check-foreclosure
      check-win
      (assoc :wk-addition 0.0)))

(defn do-run [rng state]
  (let [state (record-old state)
        event? (< (r/uniform rng 0.0 8.0) 1.0)
        state (if event?
                (let [{:keys [type state]} (ev/random-event rng state)]
                  (assoc state :last-event type))
                state)]
    (run-month rng state)))
