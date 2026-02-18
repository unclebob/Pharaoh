(ns pharaoh.tables-test
  (:require [clojure.test :refer :all]
            [pharaoh.tables :as t]))

(def test-table
  (t/make-table 0.0 10.0 [0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0]))

(deftest interpolate-at-exact-points
  (is (== 0.0 (t/interpolate 0.0 test-table)))
  (is (== 5.0 (t/interpolate 5.0 test-table)))
  (is (== 10.0 (t/interpolate 10.0 test-table))))

(deftest interpolate-between-points
  (is (== 2.5 (t/interpolate 2.5 test-table)))
  (is (== 7.5 (t/interpolate 7.5 test-table))))

(deftest interpolate-clamps-below-min
  (is (== 0.0 (t/interpolate -5.0 test-table))))

(deftest interpolate-clamps-above-max
  (is (== 10.0 (t/interpolate 15.0 test-table))))

(def nonlinear-table
  (t/make-table 0.0 10.0 [0.0 0.1 0.4 0.9 1.6 2.5 3.6 4.9 6.4 8.1 10.0]))

(deftest interpolate-nonlinear-midpoints
  (let [v (t/interpolate 0.5 nonlinear-table)]
    (is (< (Math/abs (- v 0.05)) 0.001)))
  (let [v (t/interpolate 5.5 nonlinear-table)]
    (is (< (Math/abs (- v 3.05)) 0.001))))

(deftest interpolate-at-min-boundary
  (is (== 0.0 (t/interpolate 0.0 nonlinear-table))))

(deftest interpolate-at-max-boundary
  (is (== 10.0 (t/interpolate 10.0 nonlinear-table))))

(deftest seasonal-yield-table-exists
  (is (some? t/seasonal-yield))
  (is (> (t/interpolate 6.0 t/seasonal-yield)
         (t/interpolate 1.0 t/seasonal-yield))))

(deftest yield-table-exists
  (is (some? t/wheat-yield))
  (is (> (t/interpolate 5.0 t/wheat-yield)
         (t/interpolate 0.0 t/wheat-yield))))

(deftest repay-index-table-matches-original
  (is (== 1.0 (t/interpolate 0.0 t/repay-index)))
  (is (== 1.3 (t/interpolate 0.1 t/repay-index))))

(deftest debt-support-table-exists
  (is (some? t/debt-support)))
