(ns pharaoh.gherkin.steps.helpers
  (:require [pharaoh.random :as r]))

(defn near? [a b & [tol]]
  (< (Math/abs (- (double a) (double b))) (or tol 0.01)))

(defn assert-near [expected actual & [tol]]
  (let [tolerance (or tol 1.0)]
    (when-not (near? expected actual tolerance)
      (throw (AssertionError.
               (str "Expected " expected " but got " actual
                    " (tolerance " tolerance ")"))))))

(defn to-double [s] (Double/parseDouble (str s)))

(defn ensure-rng [w]
  (if (:rng w) w (assoc w :rng (r/make-rng 42))))

(defn snap [w]
  (assoc w :state-before (:state w)))
