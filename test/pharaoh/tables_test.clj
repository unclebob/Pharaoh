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

;; --- C-exact table value tests (from vars.c) ---

(deftest seasonal-yield-matches-c
  (is (== 0.2 (t/interpolate 1.0 t/seasonal-yield)))   ; minX
  (is (== 1.5 (t/interpolate 6.5 t/seasonal-yield)))   ; peak at index 5
  (is (== 0.25 (t/interpolate 12.0 t/seasonal-yield))) ; maxX
  (is (= 11 (count (:y-vector t/seasonal-yield)))))     ; exactly 11 elements

(deftest wheat-yield-matches-c
  (is (== 20.0 (t/interpolate 0.0 t/wheat-yield)))     ; bushels at minX
  (is (== 200.0 (t/interpolate 5.0 t/wheat-yield)))    ; peak
  (is (== 0.0 (t/interpolate 10.0 t/wheat-yield))))    ; over-manuring kills yield

(deftest slave-nourishment-matches-c
  (is (== -1.0 (t/interpolate 0.0 t/slave-nourishment)))  ; starvation
  (is (== 0.036 (t/interpolate 3.0 t/slave-nourishment))) ; positive at idx 3
  (is (== 10.0 (:max-x t/slave-nourishment))))             ; X range 0-10

(deftest oxen-nourishment-matches-c
  (is (== -1.0 (t/interpolate 0.0 t/oxen-nourishment)))  ; starvation
  (is (== 0.1 (t/interpolate 100.0 t/oxen-nourishment))) ; maxX value
  (is (== 100.0 (:max-x t/oxen-nourishment))))

(deftest horse-nourishment-matches-c
  (is (== -1.0 (t/interpolate 0.0 t/horse-nourishment)))  ; starvation
  (is (== 0.1 (t/interpolate 75.0 t/horse-nourishment)))  ; maxX value
  (is (== 75.0 (:max-x t/horse-nourishment))))

(deftest slave-birth-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/slave-birth)))
  (is (== 0.14 (t/interpolate 1.0 t/slave-birth))))     ; peaks at 14%

(deftest slave-death-matches-c
  (is (== 1.0 (t/interpolate 0.0 t/slave-death)))       ; 100% death at health=0
  (is (== 0.002 (t/interpolate 1.0 t/slave-death))))

(deftest oxen-birth-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/oxen-birth)))
  (is (== 0.07 (t/interpolate 1.0 t/oxen-birth))))      ; 7% peak

(deftest oxen-death-matches-c
  (is (== 1.0 (t/interpolate 0.0 t/oxen-death)))        ; 100% death at health=0
  (is (== 0.004 (t/interpolate 1.0 t/oxen-death))))

(deftest horse-birth-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/horse-birth)))
  (is (== 0.07 (t/interpolate 1.0 t/horse-birth))))     ; 7% peak

(deftest horse-death-matches-c
  (is (== 1.0 (t/interpolate 0.0 t/horse-death)))       ; 100% death at health=0
  (is (== 0.005 (t/interpolate 1.0 t/horse-death))))

(deftest work-ability-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/work-ability)))
  (is (== 20.0 (t/interpolate 1.0 t/work-ability))))    ; max 20 hours

(deftest ox-mult-matches-c
  (is (== 1.0 (t/interpolate 0.0 t/ox-mult)))
  (is (== 4.0 (t/interpolate 1.0 t/ox-mult)))           ; max 4x multiplier
  (is (== 1.0 (:max-x t/ox-mult))))                     ; X range 0-1

(deftest stress-lash-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/stress-lash)))
  (is (== 1000.0 (t/interpolate 10.0 t/stress-lash)))   ; output 0-1000
  (is (== 10.0 (:max-x t/stress-lash))))

(deftest lash-sickness-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/lash-sickness)))
  (is (== 1.0 (t/interpolate 100.0 t/lash-sickness)))   ; X range 0-100
  (is (== 100.0 (:max-x t/lash-sickness))))

(deftest work-sickness-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/work-sickness)))
  (is (== 1.0 (t/interpolate 24.0 t/work-sickness)))    ; fatal overwork
  (is (== 24.0 (:max-x t/work-sickness))))

(deftest negative-motive-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/negative-motive)))
  (is (== 0.5 (t/interpolate 100.0 t/negative-motive))) ; X range 0-100
  (is (== 100.0 (:max-x t/negative-motive))))

(deftest positive-motive-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/positive-motive)))
  (is (== 0.7 (t/interpolate 0.1 t/positive-motive)))   ; X range 0-0.1
  (is (== 0.1 (:max-x t/positive-motive))))

(deftest overseer-effectiveness-matches-c
  (is (== 0.3 (t/interpolate 0.0 t/overseer-effectiveness)))
  (is (== 0.997 (t/interpolate 1.0 t/overseer-effectiveness)))
  (is (== 1.0 (:max-x t/overseer-effectiveness))))

(deftest oxen-efficiency-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/oxen-efficiency)))
  (is (== 1.0 (t/interpolate 1.0 t/oxen-efficiency)))
  ;; Non-linear: index 1 = 0.2, index 2 = 0.1 (dip)
  (let [v (t/interpolate 0.1 t/oxen-efficiency)]
    (is (< (Math/abs (- v 0.2)) 0.001))))

(deftest horse-efficiency-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/horse-efficiency)))
  (is (== 1.0 (t/interpolate 1.0 t/horse-efficiency)))
  ;; Steep sigmoid: near 0 at health 0.1
  (let [v (t/interpolate 0.1 t/horse-efficiency)]
    (is (< v 0.01))))

(deftest debt-support-matches-c
  (is (== 0.0 (t/interpolate 0.0 t/debt-support)))      ; starts at 0
  (is (== 3.0 (t/interpolate 1.0 t/debt-support))))     ; peaks at 3.0

(deftest repay-index-table-matches-original
  (is (== 1.0 (t/interpolate 0.0 t/repay-index)))
  (is (== 1.3 (t/interpolate 0.1 t/repay-index))))
