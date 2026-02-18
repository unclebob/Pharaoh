(ns pharaoh.health
  (:require [pharaoh.random :as r]
            [pharaoh.tables :as t]
            [pharaoh.util :as u]))

(defn slave-health-update [rng state sl-fed sl-lash-rt wk-sl ox-mult]
  (let [sl-nourish (* (t/interpolate sl-fed t/slave-nourishment)
                      (r/abs-gaussian rng 1.0 0.1))
        sl-diet sl-nourish
        sl-labor (if (pos? ox-mult) (/ wk-sl ox-mult) 0)
        lash-sick (* (t/interpolate sl-lash-rt t/lash-sickness)
                     (r/abs-gaussian rng 1.0 0.1))
        wk-sick (t/interpolate sl-labor t/work-sickness)
        sl-sick-rt (if (<= (:sl-health state) 0) 0 (+ wk-sick lash-sick))
        new-health (+ (:sl-health state) sl-diet (- sl-sick-rt))]
    {:sl-health (u/clamp new-health 0.0 1.0)
     :sl-nourish sl-nourish :sl-sick-rt sl-sick-rt
     :sl-labor sl-labor :lash-sick lash-sick :wk-sick wk-sick}))

(defn oxen-health-update [rng state ox-fed]
  (let [ox-nourish (* (t/interpolate ox-fed t/oxen-nourishment)
                      (r/abs-gaussian rng 1.0 0.1))
        ox-diet (if (< (:ox-health state) 1.0) ox-nourish 0)
        ox-age (if (<= (:ox-health state) 0) 0 0.05)
        new-health (+ (:ox-health state) ox-diet (- ox-age))]
    {:ox-health (u/clamp new-health 0.0 1.0)}))

(defn horse-health-update [rng state hs-fed]
  (let [hs-nourish (* (t/interpolate hs-fed t/horse-nourishment)
                      (r/abs-gaussian rng 1.0 0.1))
        hs-diet (if (< (:hs-health state) 1.0) hs-nourish 0)
        hs-age (if (<= (:hs-health state) 0) 0 0.08)
        new-health (+ (:hs-health state) hs-diet (- hs-age))]
    {:hs-health (u/clamp new-health 0.0 1.0)}))

(defn population-update [population birth-k death-k]
  (let [births (* birth-k population)
        deaths (* death-k population)]
    (max 0.0 (+ population births (- deaths)))))
