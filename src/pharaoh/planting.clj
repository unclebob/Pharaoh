(ns pharaoh.planting
  (:require [pharaoh.random :as r]
            [pharaoh.tables :as t]
            [pharaoh.util :as u]))

(defn land-cycle [state sew-rt sl-eff]
  (let [ln-grow-rt (:ln-sewn state)
        ln-ripe-rt (:ln-grown state)
        ln-hvsted (:ln-ripe state)
        ln-fallow (+ (:ln-fallow state) ln-hvsted (- sew-rt))
        ln-sewn (+ (:ln-sewn state) sew-rt (- ln-grow-rt))
        ln-grown (+ (:ln-grown state) ln-grow-rt (- ln-ripe-rt))
        ln-ripe (+ (:ln-ripe state) ln-ripe-rt (- ln-hvsted))]
    {:ln-fallow ln-fallow :ln-sewn ln-sewn
     :ln-grown ln-grown :ln-ripe ln-ripe :ln-hvsted ln-hvsted}))

(defn wheat-yield [rng month mn-ln]
  (* (t/interpolate mn-ln t/wheat-yield)
     (r/abs-gaussian rng 1.0 0.1)
     (t/interpolate (double month) t/seasonal-yield)))

(defn wheat-cycle [state rng wt-yield sew-rt sl-eff wt-to-sew]
  (let [wt-sew-rt (* wt-yield wt-to-sew)
        wt-grow-rt (:wt-sewn state)
        wt-ripe-rt (:wt-grown state)
        sythed (* (:wt-ripe state) sl-eff)
        wt-lost (* (- 1 sl-eff) (:wt-ripe state))
        wt-sewn (+ (:wt-sewn state) wt-sew-rt (- wt-grow-rt))
        wt-grown (+ (:wt-grown state) wt-grow-rt (- wt-ripe-rt))
        wt-ripe (+ (:wt-ripe state) wt-ripe-rt (- sythed) (- wt-lost))]
    {:wt-sewn wt-sewn :wt-grown wt-grown :wt-ripe wt-ripe
     :sythed sythed :wt-lost wt-lost}))

(defn wheat-rot [rng wheat rot-rt]
  (* wheat rot-rt (r/abs-gaussian rng 1.0 0.1)))
