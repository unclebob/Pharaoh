(ns pharaoh.random-test
  (:require [clojure.test :refer :all]
            [pharaoh.random :as r]))

(deftest uniform-returns-within-range
  (let [rng (r/make-rng 42)]
    (dotimes [_ 1000]
      (let [v (r/uniform rng 3.0 7.0)]
        (is (>= v 3.0))
        (is (< v 7.0))))))

(deftest uniform-swaps-when-b-less-than-a
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (let [v (r/uniform rng 7.0 3.0)]
        (is (>= v 3.0))
        (is (< v 7.0))))))

(deftest gaussian-has-correct-mean
  (let [rng (r/make-rng 42)
        samples (repeatedly 10000 #(r/gaussian rng 5.0 1.0))
        mean (/ (reduce + samples) (count samples))]
    (is (< (Math/abs (- mean 5.0)) 0.1))))

(deftest gaussian-has-correct-spread
  (let [rng (r/make-rng 42)
        samples (repeatedly 10000 #(r/gaussian rng 0.0 2.0))
        mean (/ (reduce + samples) (count samples))
        variance (/ (reduce + (map #(* (- % mean) (- % mean)) samples))
                    (count samples))
        sigma (Math/sqrt variance)]
    (is (< (Math/abs (- sigma 2.0)) 0.2))))

(deftest abs-gaussian-never-negative
  (let [rng (r/make-rng 42)]
    (dotimes [_ 1000]
      (is (>= (r/abs-gaussian rng 1.0 0.5) 0.0)))))

(deftest abs-gaussian-has-correct-mean
  (let [rng (r/make-rng 42)
        samples (repeatedly 5000 #(r/abs-gaussian rng 5.0 0.5))
        mean (/ (reduce + samples) (count samples))]
    (is (< (Math/abs (- mean 5.0)) 0.2))))

(deftest exponential-always-positive
  (let [rng (r/make-rng 42)]
    (dotimes [_ 1000]
      (is (> (r/exponential rng 1.0) 0.0)))))

(deftest exponential-has-correct-mean
  (let [rng (r/make-rng 42)
        samples (repeatedly 10000 #(r/exponential rng 3.0))
        mean (/ (reduce + samples) (count samples))]
    (is (< (Math/abs (- mean 3.0)) 0.2))))

(deftest max-random-returns-largest
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (let [v (r/max-random rng 5 0.0 1.0)]
        (is (>= v 0.0))
        (is (< v 1.0))))))

(deftest max-random-biased-high
  (let [rng (r/make-rng 42)
        samples (repeatedly 5000 #(r/max-random rng 5 0.0 1.0))
        mean (/ (reduce + samples) (count samples))]
    (is (> mean 0.7))))

(deftest deterministic-with-same-seed
  (let [rng1 (r/make-rng 123)
        rng2 (r/make-rng 123)
        v1 (repeatedly 10 #(r/uniform rng1 0.0 1.0))
        v2 (repeatedly 10 #(r/uniform rng2 0.0 1.0))]
    (is (= v1 v2))))
