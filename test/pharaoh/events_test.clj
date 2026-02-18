(ns pharaoh.events-test
  (:require [clojure.test :refer :all]
            [pharaoh.events :as ev]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(defn state-with [kvs]
  (merge (st/initial-state) kvs))

(deftest event-dispatch-returns-known-types
  (let [types #{:locusts :plagues :acts-of-god :acts-of-mobs :war
                :revolt :workload :health-events :labor-event
                :wheat-event :gold-event :economy-event}]
    (doseq [roll (range 100)]
      (is (contains? types (ev/event-type roll))))))

(deftest event-type-ranges
  (is (= :locusts (ev/event-type 0)))
  (is (= :locusts (ev/event-type 1)))
  (is (= :plagues (ev/event-type 2)))
  (is (= :plagues (ev/event-type 5)))
  (is (= :acts-of-god (ev/event-type 6)))
  (is (= :acts-of-mobs (ev/event-type 8)))
  (is (= :war (ev/event-type 20)))
  (is (= :revolt (ev/event-type 21)))
  (is (= :revolt (ev/event-type 29)))
  (is (= :workload (ev/event-type 30)))
  (is (= :health-events (ev/event-type 45)))
  (is (= :labor-event (ev/event-type 60)))
  (is (= :wheat-event (ev/event-type 65)))
  (is (= :gold-event (ev/event-type 75)))
  (is (= :economy-event (ev/event-type 85))))

(deftest event-type-boundary-thresholds
  ;; Exact boundary transitions
  (is (= :locusts (ev/event-type 0)))
  (is (= :locusts (ev/event-type 1)))
  (is (= :plagues (ev/event-type 2)))
  (is (= :plagues (ev/event-type 5)))
  (is (= :acts-of-god (ev/event-type 6)))
  (is (= :acts-of-god (ev/event-type 7)))
  (is (= :acts-of-mobs (ev/event-type 8)))
  (is (= :acts-of-mobs (ev/event-type 19)))
  (is (= :war (ev/event-type 20)))
  (is (= :revolt (ev/event-type 21)))
  (is (= :revolt (ev/event-type 29)))
  (is (= :workload (ev/event-type 30)))
  (is (= :workload (ev/event-type 44)))
  (is (= :health-events (ev/event-type 45)))
  (is (= :health-events (ev/event-type 59)))
  (is (= :labor-event (ev/event-type 60)))
  (is (= :labor-event (ev/event-type 64)))
  (is (= :wheat-event (ev/event-type 65)))
  (is (= :wheat-event (ev/event-type 74)))
  (is (= :gold-event (ev/event-type 75)))
  (is (= :gold-event (ev/event-type 84)))
  (is (= :economy-event (ev/event-type 85)))
  (is (= :economy-event (ev/event-type 99))))

(deftest event-message-generates-strings
  (let [rng (r/make-rng 42)]
    (doseq [etype [:acts-of-god :acts-of-mobs :plagues :locusts
                    :health-events :economy-event]]
      (is (string? (ev/event-message rng etype nil))
          (str "event-message for " etype)))
    (is (string? (ev/event-message rng :workload 500.0)))
    (is (string? (ev/event-message rng :labor-event 20)))
    (is (string? (ev/event-message rng :wheat-event 30)))
    (is (string? (ev/event-message rng :gold-event 35)))
    (is (string? (ev/event-message rng :revolt 25)))
    (is (string? (ev/event-message rng :war 1.5)))
    (is (string? (ev/event-message rng :war 0.5)))))

(deftest locusts-destroy-crops
  (let [rng (r/make-rng 42)
        state (state-with {:ln-fallow 50.0 :ln-sewn 30.0 :ln-grown 20.0
                           :ln-ripe 10.0 :wt-sewn 100.0 :wt-grown 80.0
                           :wt-ripe 60.0 :slaves 20.0})
        result (ev/locusts rng state)]
    (is (== 110.0 (:ln-fallow result)))
    (is (== 0.0 (:ln-sewn result)))
    (is (== 0.0 (:ln-grown result)))
    (is (== 0.0 (:ln-ripe result)))
    (is (== 0.0 (:wt-sewn result)))
    (is (== 0.0 (:wt-grown result)))
    (is (== 0.0 (:wt-ripe result)))
    (is (> (:wk-addition result) 0))))

(deftest locusts-noop-no-land
  (let [rng (r/make-rng 42)
        state (state-with {:ln-fallow 0.0 :ln-sewn 0.0 :ln-grown 0.0
                           :ln-ripe 0.0})
        result (ev/locusts rng state)]
    (is (= state result))))

(deftest plagues-reduce-health-and-pop
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 100.0 :oxen 50.0 :horses 30.0
                           :sl-health 0.9 :ox-health 0.8 :hs-health 0.7})
        result (ev/plagues rng state)]
    (is (< (:sl-health result) 0.9))
    (is (< (:ox-health result) 0.8))
    (is (< (:hs-health result) 0.7))
    (is (< (:slaves result) 100.0))
    (is (< (:oxen result) 50.0))
    (is (< (:horses result) 30.0))))

(deftest plagues-noop-no-pop
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 0.0 :oxen 0.0 :horses 0.0})
        result (ev/plagues rng state)]
    (is (= state result))))

(deftest acts-of-god-reduce-everything
  (let [rng (r/make-rng 42)
        state (state-with {:ln-fallow 100.0 :ln-sewn 50.0 :ln-grown 40.0
                           :ln-ripe 30.0 :wt-sewn 200.0 :wt-grown 150.0
                           :wt-ripe 100.0 :slaves 80.0 :oxen 40.0
                           :horses 20.0 :wheat 5000.0 :manure 3000.0})
        result (ev/acts-of-god rng state)]
    (is (< (:ln-fallow result) 100.0))
    (is (< (:slaves result) 80.0))
    (is (< (:wheat result) 5000.0))
    (is (> (:wk-addition result) 0))))

(deftest acts-of-mobs-damage
  (let [rng (r/make-rng 42)
        state (state-with {:wt-sewn 200.0 :wt-grown 150.0 :wt-ripe 100.0
                           :slaves 80.0 :oxen 40.0 :horses 20.0
                           :wheat 5000.0 :manure 1000.0
                           :ln-fallow 100.0 :ln-sewn 50.0 :ln-grown 40.0 :ln-ripe 30.0})
        result (ev/acts-of-mobs rng state)]
    (is (< (:slaves result) 80.0))
    (is (< (:wheat result) 5000.0))
    (is (>= (:manure result) 1000.0))))

(deftest war-uses-overseers
  (let [rng (r/make-rng 42)
        state (state-with {:overseers 10.0 :slaves 50.0 :oxen 20.0
                           :horses 10.0 :wheat 1000.0 :manure 500.0
                           :ln-fallow 50.0 :ln-sewn 20.0 :ln-grown 10.0
                           :ln-ripe 5.0 :wt-sewn 100.0 :wt-grown 50.0
                           :wt-ripe 30.0})
        result (ev/war rng state)]
    (is (number? (:slaves result)))
    (is (> (:wk-addition result) 0))))

(deftest revolt-based-on-lashing
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 100.0 :sl-health 0.4 :overseers 5.0
                           :oxen 20.0 :horses 10.0 :wheat 1000.0
                           :manure 500.0 :ln-fallow 50.0 :ln-sewn 20.0
                           :ln-grown 10.0 :ln-ripe 5.0
                           :wt-sewn 100.0 :wt-grown 50.0 :wt-ripe 30.0
                           :sl-lash-rt 0.5})
        result (ev/revolt rng state)]
    (is (< (:slaves result) 100.0))
    (is (> (:wk-addition result) 0))))

(deftest revolt-noop-no-slaves
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 0.0})
        result (ev/revolt rng state)]
    (is (= state result))))

(deftest workload-event-adds-work
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 50.0 :ln-fallow 100.0 :ln-sewn 50.0
                           :ln-grown 30.0 :ln-ripe 20.0})
        result (ev/workload-event rng state)]
    (is (> (:wk-addition result) 0))))

(deftest workload-noop-no-slaves
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 0.0})
        result (ev/workload-event rng state)]
    (is (= state result))))

(deftest health-event-reduces-health
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 50.0 :oxen 20.0 :horses 10.0
                           :sl-health 0.9 :ox-health 0.8 :hs-health 0.7})
        result (ev/health-event rng state)]
    (is (< (:sl-health result) 0.9))
    (is (< (:ox-health result) 0.8))
    (is (< (:hs-health result) 0.7))))

(deftest health-noop-no-pop
  (let [rng (r/make-rng 42)
        state (state-with {:slaves 0.0 :oxen 0.0 :horses 0.0})
        result (ev/health-event rng state)]
    (is (= state result))))

(deftest labor-event-raises-pay
  (let [rng (r/make-rng 42)
        state (state-with {:overseers 10.0 :ov-pay 100.0 :ov-press 0.3})
        result (ev/labor-event rng state)]
    (is (> (:ov-pay result) 100.0))
    (is (< (:overseers result) 10.0))
    (is (> (:ov-press result) 0.3))))

(deftest labor-noop-no-overseers
  (let [rng (r/make-rng 42)
        state (state-with {:overseers 0.0})
        result (ev/labor-event rng state)]
    (is (= state result))))

(deftest wheat-event-damages-crops
  (let [rng (r/make-rng 42)
        state (state-with {:wheat 5000.0 :wt-sewn 200.0 :wt-grown 150.0
                           :wt-ripe 100.0 :ln-sewn 10.0 :ln-grown 8.0 :ln-ripe 5.0})
        result (ev/wheat-event rng state)]
    (is (< (:wheat result) 5000.0))
    (is (< (:wt-sewn result) 200.0))))

(deftest wheat-noop-no-crops
  (let [rng (r/make-rng 42)
        state (state-with {:ln-sewn 0.0 :ln-grown 0.0 :ln-ripe 0.0})
        result (ev/wheat-event rng state)]
    (is (= state result))))

(deftest gold-event-reduces-gold
  (let [rng (r/make-rng 42)
        state (state-with {:gold 50000.0})
        result (ev/gold-event rng state)]
    (is (< (:gold result) 50000.0))
    (is (> (:gold result) 0.0))))

(deftest gold-noop-no-gold
  (let [rng (r/make-rng 42)
        state (state-with {:gold 0.0})
        result (ev/gold-event rng state)]
    (is (= state result))))

(deftest economy-event-shifts-prices
  (let [rng (r/make-rng 42)
        state (state-with {:prices {:wheat 10.0 :oxen 300.0 :horses 500.0
                                    :slaves 1000.0 :manure 5.0 :land 5000.0}
                           :inflation 0.0})
        result (ev/economy-event rng state)]
    (is (not= (:prices result) (:prices state)))))
