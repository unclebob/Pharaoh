(ns pharaoh.feeding-test
  (:require [clojure.test :refer :all]
            [pharaoh.feeding :as f]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest wheat-usage-basic
  (let [state (assoc (st/initial-state)
                :slaves 100.0 :oxen 50.0 :horses 20.0
                :sl-feed-rt 10.0 :ox-feed-rt 60.0 :hs-feed-rt 50.0)
        usage (f/wheat-usage state 1.0)]
    (is (== 1000.0 (:wt-fed-sl usage)))
    (is (== 3000.0 (:wt-fed-ox usage)))
    (is (== 1000.0 (:wt-fed-hs usage)))))

(deftest wheat-usage-with-efficiency
  (let [state (assoc (st/initial-state)
                :slaves 100.0 :oxen 50.0 :horses 20.0
                :sl-feed-rt 10.0 :ox-feed-rt 60.0 :hs-feed-rt 50.0)
        usage (f/wheat-usage state 0.8)]
    ;; ox-fed = 60*0.8 = 48, wt-fed-ox = 48*50*0.8 = 1920
    (is (== 1920.0 (:wt-fed-ox usage)))))

(deftest wheat-shortage-proportioning
  (let [usage {:total 5000.0 :wt-to-sew 1000.0 :wt-fed-hs 1000.0
               :wt-fed-ox 1500.0 :wt-fed-sl 1500.0
               :ox-fed 60.0 :hs-fed 50.0 :sl-fed 10.0 :sew-rt 50.0}
        result (f/apply-wheat-shortage usage 3000.0)]
    (is (== 0.6 (:wt-eff result)))
    (is (== 600.0 (:wt-to-sew result)))
    (is (== 900.0 (:wt-fed-sl result)))))

(deftest no-shortage-when-sufficient
  (let [usage {:total 3000.0 :wt-to-sew 500.0 :wt-fed-hs 500.0
               :wt-fed-ox 1000.0 :wt-fed-sl 1000.0
               :ox-fed 60.0 :hs-fed 50.0 :sl-fed 10.0 :sew-rt 50.0}
        result (f/apply-wheat-shortage usage 5000.0)]
    (is (== 1.0 (:wt-eff result)))))

(deftest manure-production
  (let [rng (r/make-rng 42)
        mn (f/manure-made rng 3000.0)]
    (is (> mn 20.0))
    (is (< mn 50.0))))
