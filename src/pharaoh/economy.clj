(ns pharaoh.economy
  (:require [pharaoh.random :as r]
            [pharaoh.state :as st]))

(defn update-inflation [rng inflation]
  (+ inflation (r/gaussian rng 0.0 0.001)))

(defn update-price [rng price inflation]
  (* price (r/abs-gaussian rng (+ 1.0 inflation) 0.02)))

(defn adjust-production [rng {:keys [supply demand production price]} world-growth]
  (let [demand (* demand (+ 1.0 (/ world-growth 12.0)))
        monthly-demand (/ demand 12.0)
        supply (- supply (* monthly-demand 0.8))
        [price production]
        (if (< supply 0)
          [(* price (r/uniform rng 1.0 1.2))
           (* production (r/uniform rng 1.0 1.1))]
          [price production])
        supply (- supply (* monthly-demand 0.2))
        supply (max 0.0 supply)
        [price production]
        (if (> supply 0)
          [(* price (r/uniform rng 0.8 1.0))
           (* production (r/uniform rng 0.9 1.0))]
          [price production])
        production (* production (r/uniform rng 0.95 1.05))
        supply (+ supply (/ production 12.0))]
    {:supply supply :demand demand :production production :price price}))

(defn net-worth [state]
  (let [prices (:prices state)
        lt (st/total-land state)]
    (+ (* (:slaves state) (:slaves prices))
       (* (:oxen state) (:oxen prices))
       (* (:horses state) (:horses prices))
       (* lt (:land prices))
       (* (:manure state) (:manure prices))
       (* (:wheat state) (:wheat prices))
       (:gold state))))

(defn ownership-cost [rng state]
  (let [lt (st/total-land state)
        base (+ (* lt 100) (* (:slaves state) 10)
                (* (:horses state) 5) (* (:oxen state) 3))]
    (if (zero? base)
      0.0
      (* base (+ (r/abs-gaussian rng 0.7 0.3) 0.3)))))
