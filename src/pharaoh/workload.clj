(ns pharaoh.workload
  (:require [pharaoh.random :as r]
            [pharaoh.tables :as t]
            [pharaoh.pyramid :as py]))

(defn work-components [state]
  (let [new-height (py/py-height (:py-base state)
                     (+ (:py-stones state) (:py-quota state)))
        avg-height (Math/ceil (/ (+ (:py-height state) new-height) 2.0))]
    {:ox-tend (* (:oxen state) 1)
     :mn-spread (* (:mn-to-sprd state) 64)
     :wt-sew (* (:ln-to-sew state) 30)
     :wt-tend (+ (* (:ln-sewn state) 20) (* (:ln-grown state) 15))
     :wt-harvest (+ (* (:wt-ripe state) 0.1) (* (:ln-ripe state) 20))
     :hs-tend (* (:horses state) 1)
     :py-work (* (:py-quota state) avg-height 12)
     :avg-py-height avg-height}))

(defn required-work [rng state]
  (let [comps (work-components state)
        base (+ (:ox-tend comps) (:mn-spread comps) (:wt-sew comps)
                (:wt-tend comps) (:wt-harvest comps) (:hs-tend comps)
                (:py-work comps) (:wk-addition state))]
    {:req-work (* base (r/abs-gaussian rng 1.0 0.1))
     :avg-py-height (:avg-py-height comps)}))

(defn slave-capacity [rng state]
  (let [sl-dth-k (* (t/interpolate (:sl-health state) t/slave-death)
                    (r/abs-gaussian rng 1.0 0.1))
        sl-brth-k (* (t/interpolate (:sl-health state) t/slave-birth)
                     (r/abs-gaussian rng 1.0 0.1))
        wk-able (* (t/interpolate (:sl-health state) t/work-ability)
                   (r/abs-gaussian rng 1.0 0.1))
        hs-eff (* (t/interpolate (:hs-health state) t/horse-efficiency)
                  (r/abs-gaussian rng 1.0 0.1))
        hs-dth-k (* (t/interpolate (:hs-health state) t/horse-death)
                    (r/abs-gaussian rng 1.0 0.1))
        hs-brth-k (* (t/interpolate (:hs-health state) t/horse-birth)
                     (r/abs-gaussian rng 1.0 0.1))
        ox-eff (* (t/interpolate (:ox-health state) t/oxen-efficiency)
                  (r/abs-gaussian rng 1.0 0.1))
        ox-dth-k (* (t/interpolate (:ox-health state) t/oxen-death)
                    (r/abs-gaussian rng 1.0 0.1))
        ox-brth-k (* (t/interpolate (:ox-health state) t/oxen-birth)
                     (r/abs-gaussian rng 1.0 0.1))
        ox-sl (if (pos? (:slaves state)) (/ (:oxen state) (:slaves state)) 0)
        sl-ov (/ (:slaves state) (+ (:overseers state) 1))
        hs-ov (if (pos? (:overseers state))
                (/ (:horses state) (:overseers state)) 0)
        hs-eff-ov (* hs-ov hs-eff)
        ov-eff (* (t/interpolate hs-eff-ov t/overseer-effectiveness)
                  (r/abs-gaussian rng 1.0 0.1))
        ov-eff-sl (if (pos? sl-ov) (/ ov-eff sl-ov) 0)
        stress-lash (* (t/interpolate (:ov-press state) t/stress-lash)
                       (r/abs-gaussian rng 1.0 0.1))
        sl-lash-rt (* stress-lash ov-eff-sl)
        pos-motive (* (t/interpolate ov-eff-sl t/positive-motive)
                      (r/abs-gaussian rng 1.0 0.1))
        neg-motive (t/interpolate sl-lash-rt t/negative-motive)
        motive (+ pos-motive neg-motive)
        ox-mult-k (t/interpolate ox-sl t/ox-mult)
        ox-mult (max (* ox-mult-k ox-eff) 1)
        max-wk-sl (* motive wk-able ox-mult)]
    {:sl-dth-k sl-dth-k :sl-brth-k sl-brth-k :wk-able wk-able
     :hs-eff hs-eff :hs-dth-k hs-dth-k :hs-brth-k hs-brth-k
     :ox-eff ox-eff :ox-dth-k ox-dth-k :ox-brth-k ox-brth-k
     :ov-eff ov-eff :ov-eff-sl ov-eff-sl
     :stress-lash stress-lash :sl-lash-rt sl-lash-rt
     :pos-motive pos-motive :neg-motive neg-motive :motive motive
     :ox-mult-k ox-mult-k :ox-mult ox-mult :max-wk-sl max-wk-sl}))

(defn compute-efficiency [slaves max-wk-sl req-work]
  (if (or (zero? slaves) (zero? req-work))
    {:wk-sl 0 :wk-deff-sl 0 :tot-wk 0 :sl-eff 1.0}
    (let [req-wk-sl (/ req-work slaves)
          wk-sl (min max-wk-sl req-wk-sl)
          wk-deff-sl (if (< wk-sl req-wk-sl) (- req-wk-sl wk-sl) 0)
          tot-wk (* wk-sl slaves)
          sl-eff (if (pos? req-work) (/ tot-wk req-work) 1.0)]
      {:wk-sl wk-sl :wk-deff-sl wk-deff-sl
       :tot-wk tot-wk :sl-eff sl-eff :req-wk-sl req-wk-sl})))
