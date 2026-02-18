(ns pharaoh.workload-test
  (:require [clojure.test :refer :all]
            [pharaoh.workload :as wk]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest work-components-ox-tending
  (let [state (assoc (st/initial-state) :oxen 50.0)
        comps (wk/work-components state)]
    (is (== 50.0 (:ox-tend comps)))))

(deftest work-components-manure-spreading
  (let [state (assoc (st/initial-state) :mn-to-sprd 100.0)
        comps (wk/work-components state)]
    (is (== 6400.0 (:mn-spread comps)))))

(deftest work-components-wheat-sewing
  (let [state (assoc (st/initial-state) :ln-to-sew 50.0)
        comps (wk/work-components state)]
    (is (== 1500.0 (:wt-sew comps)))))

(deftest work-components-horse-tending
  (let [state (assoc (st/initial-state) :horses 20.0)
        comps (wk/work-components state)]
    (is (== 20.0 (:hs-tend comps)))))

(deftest efficiency-full-when-work-met
  (let [result (wk/compute-efficiency 100 12.0 1000.0)]
    (is (== 1.0 (:sl-eff result)))))

(deftest efficiency-reduced-when-overloaded
  (let [result (wk/compute-efficiency 100 8.0 1000.0)]
    (is (== 0.8 (:sl-eff result)))
    (is (== 800.0 (:tot-wk result)))))

(deftest efficiency-handles-zero-slaves
  (let [result (wk/compute-efficiency 0 0 1000.0)]
    (is (== 1.0 (:sl-eff result)))))

(deftest work-deficit-when-overloaded
  (let [result (wk/compute-efficiency 100 8.0 1200.0)]
    (is (> (:wk-deff-sl result) 0))))

(deftest no-work-deficit-when-meeting-quota
  (let [result (wk/compute-efficiency 100 15.0 1000.0)]
    (is (== 0 (:wk-deff-sl result)))))
