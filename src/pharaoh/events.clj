(ns pharaoh.events
  (:require [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.tables :as t]
            [pharaoh.util :as u]))

(defn event-type [roll]
  (cond
    (< roll 2)  :locusts
    (< roll 6)  :plagues
    (< roll 8)  :acts-of-god
    (< roll 20) :acts-of-mobs
    (< roll 21) :war
    (< roll 30) :revolt
    (< roll 45) :workload
    (< roll 60) :health-events
    (< roll 65) :labor-event
    (< roll 75) :wheat-event
    (< roll 85) :gold-event
    :else        :economy-event))

(defn locusts [rng state]
  (let [lt (st/total-land state)]
    (if (zero? lt)
      state
      (-> state
          (update :ln-fallow + (:ln-sewn state) (:ln-grown state) (:ln-ripe state))
          (assoc :ln-sewn 0.0 :ln-grown 0.0 :ln-ripe 0.0
                 :wt-sewn 0.0 :wt-grown 0.0 :wt-ripe 0.0
                 :wk-addition (+ (* 15.0 (:slaves state))
                                 (* (r/gaussian rng 5.0 1.0) lt)))))))

(defn plagues [rng state]
  (if (zero? (+ (:slaves state) (:oxen state) (:horses state)))
    state
    (-> state
        (update :sl-health * (r/uniform rng 0.2 0.9))
        (update :ox-health * (r/uniform rng 0.2 0.9))
        (update :hs-health * (r/uniform rng 0.2 0.9))
        (update :slaves * (r/uniform rng 0.7 0.95))
        (update :oxen * (r/uniform rng 0.7 0.95))
        (update :horses * (r/uniform rng 0.7 0.95)))))

(defn- multiply-resources [rng state factor-fn]
  (let [k-ln (factor-fn rng) k-gr (factor-fn rng) k-sw (factor-fn rng)
        k-rp (factor-fn rng)]
    (-> state
        (update :ln-fallow * (factor-fn rng))
        (update :ln-grown * k-gr) (update :wt-grown * k-gr)
        (update :ln-sewn * k-sw) (update :wt-sewn * k-sw)
        (update :ln-ripe * k-rp) (update :wt-ripe * k-rp)
        (update :slaves * (factor-fn rng))
        (update :oxen * (factor-fn rng))
        (update :horses * (factor-fn rng))
        (update :wheat * (factor-fn rng))
        (update :manure * (factor-fn rng)))))

(defn acts-of-god [rng state]
  (let [lt (st/total-land state)
        result (multiply-resources rng state #(r/uniform % 0.3 0.8))]
    (assoc result :wk-addition
           (+ (* (r/gaussian rng 11.0 3.0) (:slaves state))
              (* (r/gaussian rng 5.0 1.0) lt)))))

(defn acts-of-mobs [rng state]
  (let [lt (st/total-land state)]
    (-> state
        (update :wt-grown * (r/uniform rng 0.6 0.8))
        (update :wt-sewn * (r/uniform rng 0.6 0.8))
        (update :wt-ripe * (r/uniform rng 0.6 0.8))
        (update :slaves * (r/uniform rng 0.6 0.8))
        (update :oxen * (r/uniform rng 0.6 0.8))
        (update :horses * (r/uniform rng 0.6 0.8))
        (update :wheat * (r/uniform rng 0.6 0.8))
        (update :manure * (r/uniform rng 1.05 1.20))
        (update :manure + (r/uniform rng (* lt 0.5) (* lt 3.0)))
        (assoc :wk-addition
               (+ (* (r/uniform rng 5.0 10.0) (:slaves state))
                  (* (r/gaussian rng 5.0 1.0) lt))))))

(defn war [rng state]
  (let [my-army (+ (:overseers state) 1.0)
        his-army (+ (* (min 1e5 (:overseers state))
                       (r/abs-gaussian rng 1.0 0.2)) 1.0)
        my-dice (* my-army (r/abs-gaussian rng 1.0 0.3))
        his-dice (* his-army (r/abs-gaussian rng 1.0 0.3))
        gain (if (< his-dice 0.001) 1.0 (/ my-dice his-dice))
        max-gain (/ (+ his-army my-army) my-army)
        gain (min gain max-gain)
        apply-gain (fn [rng s k]
                     (update s k * (* gain (r/abs-gaussian rng 1.0 0.2))))]
    (-> (reduce (partial apply-gain rng) state
                [:ln-fallow :ln-grown :wt-grown :ln-sewn :wt-sewn
                 :ln-ripe :wt-ripe :slaves :oxen :horses :wheat :manure])
        (assoc :wk-addition
               (+ (* (r/gaussian rng 15.0 3.0) (:slaves state))
                  (* his-army (r/gaussian rng 5.0 1.0)))))))

(defn revolt [rng state]
  (if (zero? (:slaves state))
    state
    (let [lash-rt (get state :sl-lash-rt 0.0)
          suffering (t/interpolate lash-rt t/revolt-suffering)
          sickness (t/interpolate (:sl-health state) t/revolt-sickness)
          hatred (/ (+ suffering sickness) 2.0)
          destruction (* (t/interpolate hatred t/revolt-destruction)
                        (r/abs-gaussian rng 1.0 0.2))
          gain (u/clamp (- 1.0 destruction) 0.0 1.0)]
      (-> (reduce (fn [s k] (update s k * gain)) state
                  [:ln-fallow :ln-grown :wt-grown :ln-sewn :wt-sewn
                   :ln-ripe :wt-ripe :slaves :oxen :horses :wheat :manure])
          (assoc :wk-addition
                 (+ (* (r/gaussian rng 18.0 3.0) (:slaves state))
                    (* (r/gaussian rng 30.0 5.0) (:overseers state))))))))

(defn workload-event [rng state]
  (if (zero? (:slaves state))
    state
    (let [lt (st/total-land state)]
      (assoc state :wk-addition
             (+ (* (r/gaussian rng 10.0 3.0) (:slaves state))
                (* (r/gaussian rng 8.0 2.0) lt))))))

(defn health-event [rng state]
  (if (zero? (+ (:slaves state) (:oxen state) (:horses state)))
    state
    (-> state
        (update :sl-health * (r/gaussian rng 0.6 0.1))
        (update :ox-health * (r/gaussian rng 0.6 0.1))
        (update :hs-health * (r/gaussian rng 0.6 0.1)))))

(defn labor-event [rng state]
  (if (zero? (:overseers state))
    state
    (let [raise (max 1.01 (r/gaussian rng 1.20 0.05))]
      (-> state
          (update :ov-pay * raise)
          (update :overseers #(Math/floor (* % (r/gaussian rng 0.9 0.03))))
          (update :ov-press + (r/gaussian rng 0.5 0.1))))))

(defn wheat-event [rng state]
  (if (zero? (+ (:ln-sewn state) (:ln-grown state) (:ln-ripe state)))
    state
    (let [loss (min 0.99 (r/gaussian rng 0.7 0.07))]
      (-> state
          (update :wheat * loss)
          (update :wt-sewn * loss)
          (update :wt-grown * loss)
          (update :wt-ripe * loss)))))

(defn gold-event [rng state]
  (if (zero? (:gold state))
    state
    (let [loss (min 0.99 (r/gaussian rng 0.65 0.1))]
      (update state :gold * loss))))

(defn economy-event [rng state]
  (let [shift #(* % (r/gaussian rng 1.0 0.15))]
    (-> state
        (update-in [:prices :wheat] shift)
        (update-in [:prices :oxen] shift)
        (update-in [:prices :horses] shift)
        (update-in [:prices :slaves] shift)
        (update-in [:prices :manure] shift)
        (update :inflation + (r/gaussian rng 0.0 0.01)))))

(def event-fns
  {:locusts      locusts      :plagues     plagues
   :acts-of-god  acts-of-god  :acts-of-mobs acts-of-mobs
   :war          war          :revolt      revolt
   :workload       workload-event :health-events health-event
   :labor-event    labor-event    :wheat-event   wheat-event
   :gold-event     gold-event     :economy-event economy-event})

(defn random-event [rng state]
  (let [roll (long (r/uniform rng 0.0 100.0))
        etype (event-type roll)
        handler (event-fns etype)]
    {:type etype :state (handler rng state)}))
