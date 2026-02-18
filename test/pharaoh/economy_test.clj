(ns pharaoh.economy-test
  (:require [clojure.test :refer :all]
            [pharaoh.economy :as econ]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest inflation-random-walk
  (let [rng (r/make-rng 42)
        inf1 (econ/update-inflation rng 0.0)
        inf2 (econ/update-inflation rng 0.02)]
    (is (number? inf1))
    (is (number? inf2))))

(deftest market-price-update
  (let [rng (r/make-rng 42)
        p (econ/update-price rng 100.0 0.02)]
    (is (> p 0))
    (is (< (Math/abs (- p 102.0)) 10.0))))

(deftest adjust-production-demand-grows
  (let [rng (r/make-rng 42)
        result (econ/adjust-production rng
                 {:supply 10000.0 :demand 10000.0
                  :production 10000.0 :price 10.0}
                 0.10)]
    (is (> (:demand result) 10000.0))))

(deftest net-worth-calculation
  (let [state (assoc (st/initial-state)
                :slaves 100.0 :oxen 50.0 :horses 20.0
                :ln-fallow 500.0 :manure 200.0 :wheat 1000.0
                :gold 5000.0)]
    (is (> (econ/net-worth state) 0))))

(deftest ownership-cost-positive
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state)
                :ln-fallow 100.0 :slaves 50.0
                :horses 10.0 :oxen 20.0)]
    (is (> (econ/ownership-cost rng state) 0))))

(deftest ownership-cost-zero-when-nothing-owned
  (let [rng (r/make-rng 42)
        state (st/initial-state)]
    (is (== 0.0 (econ/ownership-cost rng state)))))
