(ns pharaoh.trading
  (:require [pharaoh.random :as r]))

(def commodity-keys
  {:wheat :wheat :manure :manure :slaves :slaves
   :horses :horses :oxen :oxen :land :ln-fallow})

(def health-keys
  {:slaves :sl-health :horses :hs-health :oxen :ox-health})

(def crop-keys
  {:ln-sewn :wt-sewn :ln-grown :wt-grown :ln-ripe :wt-ripe})

(defn commodity-key [commodity]
  (get commodity-keys commodity commodity))

(defn health-key [commodity]
  (get health-keys commodity))

(defn validate-sell [state commodity amount]
  (let [ck (commodity-key commodity)
        owned (get state ck 0.0)
        max-supply (* (get-in state [:demand commodity] 10000.0) 1.1)
        cur-supply (get-in state [:supply commodity] 0.0)]
    (cond
      (> amount (+ owned 1e-4))
      {:status :error :max-amount owned}
      (> (+ cur-supply amount) max-supply)
      {:status :capped :max-amount (max 1.0 (- max-supply cur-supply))}
      :else {:status :ok})))

(defn validate-buy [state commodity amount]
  (let [price (get-in state [:prices commodity] 0.0)
        cost (* amount price)
        gold (:gold state)]
    (if (> cost gold)
      {:status :error :max-amount (/ gold price)}
      {:status :ok})))

(defn buy [rng state commodity amount]
  (let [ck (commodity-key commodity)
        hk (health-key commodity)
        price (get-in state [:prices commodity])
        supply (get-in state [:supply commodity])
        actual (min amount (max supply 1.0))
        cost (* actual price)
        state (-> state
                (update ck + actual)
                (update :gold - cost)
                (assoc-in [:supply commodity] (- supply actual)))]
    (if hk
      (let [old-count (- (get state ck) actual)
            old-health (get state hk 0.8)
            nom-health (r/gaussian rng 0.8 0.02)
            blended (if (> (+ old-count actual) 0)
                      (/ (+ (* old-count old-health) (* actual nom-health))
                         (+ old-count actual))
                      nom-health)]
        (assoc state hk blended))
      state)))

(defn sell [rng state commodity amount]
  (let [ck (commodity-key commodity)
        hk (health-key commodity)
        price (get-in state [:prices commodity])
        health (if hk (get state hk 1.0) 1.0)
        revenue (* amount price (if hk health 1.0))
        crop-key (get crop-keys ck)
        state (-> state
                (update ck - amount)
                (update :gold + revenue)
                (update-in [:supply commodity] + amount))]
    (if (and crop-key (< amount 0))
      ;; selling land with crops
      (let [owned (+ (get state ck) amount)
            burn-fract (if (> owned 0) (/ (- amount) owned) 0)]
        (update state crop-key * (- 1 burn-fract)))
      state)))
