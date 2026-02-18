(ns pharaoh.ui.screen-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.screen]))

;; Access private functions via var references
(def delta-pct #'pharaoh.ui.screen/delta-pct)
(def fmt #'pharaoh.ui.screen/fmt)
(def fmt1 #'pharaoh.ui.screen/fmt1)

;; ---- fmt ----

(deftest fmt-rounds-to-integer
  (is (= "100" (fmt 100.0)))
  (is (= "100" (fmt 100.4)))
  (is (= "101" (fmt 100.5)))
  (is (= "0" (fmt 0.0))))

(deftest fmt-large-numbers
  (is (= "1000000" (fmt 1000000.0))))

(deftest fmt-negative-numbers
  (is (= "-5" (fmt -5.0)))
  (is (= "-100" (fmt -99.6))))

(deftest fmt-integer-input
  (is (= "42" (fmt 42))))

;; ---- fmt1 ----

(deftest fmt1-one-decimal-place
  (is (= "5.0" (fmt1 5.0)))
  (is (= "5.5" (fmt1 5.5)))
  (is (= "5.3" (fmt1 5.25))))

(deftest fmt1-zero
  (is (= "0.0" (fmt1 0.0))))

(deftest fmt1-negative
  (is (= "-3.2" (fmt1 -3.2))))

(deftest fmt1-rounds-correctly
  (is (= "1.7" (fmt1 1.65)))
  (is (= "99.9" (fmt1 99.94))))

(deftest fmt1-integer-input
  (is (= "10.0" (fmt1 10))))

;; ---- delta-pct ----

(deftest delta-pct-increase
  (is (= "+100%" (delta-pct 200.0 100.0)))
  (is (= "+50%" (delta-pct 150.0 100.0))))

(deftest delta-pct-decrease
  (is (= "-50%" (delta-pct 50.0 100.0)))
  (is (= "-25%" (delta-pct 75.0 100.0))))

(deftest delta-pct-no-change
  (is (= "" (delta-pct 100.0 100.0))))

(deftest delta-pct-old-is-zero
  (is (= "" (delta-pct 50.0 0.0))))

(deftest delta-pct-old-is-negative
  (is (= "" (delta-pct 50.0 -10.0))))

(deftest delta-pct-both-same-nonzero
  (is (= "" (delta-pct 42.0 42.0))))

(deftest delta-pct-small-increase
  (is (= "+10%" (delta-pct 110.0 100.0))))

(deftest delta-pct-small-decrease
  (is (= "-10%" (delta-pct 90.0 100.0))))

(deftest delta-pct-large-increase
  (is (= "+900%" (delta-pct 1000.0 100.0))))

(deftest delta-pct-fractional-result
  ;; 33/100 = 33% increase, rounded to 0 decimals
  (is (= "+33%" (delta-pct 133.0 100.0))))

;; ---- fmt-pending ----

(def fmt-pending #'pharaoh.ui.screen/fmt-pending)

(deftest fmt-pending-buy-with-months-left
  (let [c {:type :buy :amount 100.0 :what :wheat
           :price 1000.0 :months-left 12}]
    (is (= "buy 100 wheat @ 1000 gold 12mo" (fmt-pending c)))))

(deftest fmt-pending-sell-with-months-left
  (let [c {:type :sell :amount 50.0 :what :slaves
           :price 5000.0 :months-left 6}]
    (is (= "sell 50 slaves @ 5000 gold 6mo" (fmt-pending c)))))

(deftest fmt-pending-falls-back-to-duration
  (let [c {:type :buy :amount 200.0 :what :oxen
           :price 300.0 :duration 24}]
    (is (= "buy 200 oxen @ 300 gold 24mo" (fmt-pending c)))))
