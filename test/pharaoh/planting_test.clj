(ns pharaoh.planting-test
  (:require [clojure.test :refer :all]
            [pharaoh.planting :as pl]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest land-cycle-conserves-total
  (let [state (assoc (st/initial-state)
                :ln-fallow 100.0 :ln-sewn 30.0
                :ln-grown 20.0 :ln-ripe 10.0)
        total-before (+ 100 30 20 10)
        result (pl/land-cycle state 40.0 1.0)
        total-after (+ (:ln-fallow result) (:ln-sewn result)
                       (:ln-grown result) (:ln-ripe result))]
    (is (< (Math/abs (- total-before total-after)) 0.001))))

(deftest land-cycle-fallow-to-planted
  (let [state (assoc (st/initial-state)
                :ln-fallow 100.0 :ln-sewn 0.0
                :ln-grown 0.0 :ln-ripe 0.0)
        result (pl/land-cycle state 50.0 1.0)]
    (is (== 50.0 (:ln-fallow result)))
    (is (== 50.0 (:ln-sewn result)))))

(deftest wheat-yield-better-in-summer
  (let [rng1 (r/make-rng 42)
        rng2 (r/make-rng 42)
        july-yield (pl/wheat-yield rng1 7 3.0)
        jan-yield (pl/wheat-yield rng2 1 3.0)]
    (is (> july-yield jan-yield))))

(deftest wheat-yield-better-with-manure
  (let [rng1 (r/make-rng 42)
        rng2 (r/make-rng 42)
        high (pl/wheat-yield rng1 6 5.0)
        low (pl/wheat-yield rng2 6 0.0)]
    (is (> high low))))

(deftest wheat-rot-proportional
  (let [rng (r/make-rng 42)
        rot (pl/wheat-rot rng 10000.0 0.02)]
    (is (> rot 100.0))
    (is (< rot 400.0))))
