(ns pharaoh.util-test
  (:require [clojure.test :refer :all]
            [pharaoh.util :as u]))

(deftest clamp-within-range
  (is (= 5 (u/clamp 5 0 10))))

(deftest clamp-below-min
  (is (= 0 (u/clamp -3 0 10))))

(deftest clamp-above-max
  (is (= 10 (u/clamp 15 0 10))))

(deftest clamp-at-min
  (is (= 0 (u/clamp 0 0 10))))

(deftest clamp-at-max
  (is (= 10 (u/clamp 10 0 10))))

(deftest clamp-negative-range
  (is (= -5 (u/clamp -5 -10 -1))))

(deftest clamp-float-values
  (is (== 0.5 (u/clamp 0.5 0.0 1.0)))
  (is (== 0.0 (u/clamp -0.1 0.0 1.0)))
  (is (== 1.0 (u/clamp 1.5 0.0 1.0))))

(deftest clip-positive-unchanged
  (is (= 5 (u/clip 5))))

(deftest clip-zero-unchanged
  (is (= 0 (u/clip 0))))

(deftest clip-negative-becomes-zero
  (is (= 0 (u/clip -3))))

(deftest clip-float-values
  (is (== 0.5 (u/clip 0.5)))
  (is (== 0 (u/clip -0.1))))

(deftest fmt-float-integer-format
  (is (= "42" (u/fmt-float 42.0)))
  (is (= "42" (u/fmt-float 42.4)))
  (is (= "43" (u/fmt-float 42.5)))
  (is (= "0" (u/fmt-float 0.0)))
  (is (= "-5" (u/fmt-float -5.0)))
  (is (= "1000000" (u/fmt-float 1e6))))
