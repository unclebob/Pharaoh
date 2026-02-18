(ns pharaoh.feeding
  (:require [pharaoh.random :as r]
            [pharaoh.util :as u]))

(defn wheat-usage [state sl-eff]
  (let [ox-fed (* (:ox-feed-rt state) sl-eff)
        hs-fed (* (:hs-feed-rt state) sl-eff)
        sl-fed (:sl-feed-rt state)
        sew-rt (min (* (:ln-to-sew state) sl-eff) (:ln-fallow state))
        wt-to-sew (* (:wt-sewn-ln state) sew-rt)
        wt-fed-hs (* hs-fed (:horses state) sl-eff)
        wt-fed-ox (* ox-fed (:oxen state) sl-eff)
        wt-fed-sl (* (:slaves state) sl-fed)
        total (+ wt-to-sew wt-fed-hs wt-fed-ox wt-fed-sl)]
    {:ox-fed ox-fed :hs-fed hs-fed :sl-fed sl-fed
     :sew-rt sew-rt :wt-to-sew wt-to-sew
     :wt-fed-hs wt-fed-hs :wt-fed-ox wt-fed-ox :wt-fed-sl wt-fed-sl
     :total total}))

(defn apply-wheat-shortage [usage wheat-after-rot]
  (let [total (:total usage)
        wt-eff (if (and (< wheat-after-rot total) (pos? total))
                 (/ wheat-after-rot total)
                 1.0)]
    (if (< wt-eff 1.0)
      (-> usage
        (update :wt-to-sew * wt-eff)
        (update :wt-fed-hs * wt-eff)
        (update :wt-fed-ox * wt-eff)
        (update :wt-fed-sl * wt-eff)
        (update :ox-fed * wt-eff)
        (update :hs-fed * wt-eff)
        (update :sl-fed * wt-eff)
        (update :total * wt-eff)
        (update :sew-rt * wt-eff)
        (assoc :wt-eff wt-eff))
      (assoc usage :wt-eff 1.0))))

(defn manure-made [rng wt-eaten]
  (* (/ wt-eaten 100.0) (r/abs-gaussian rng 1.0 0.1)))
