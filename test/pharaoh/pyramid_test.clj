(ns pharaoh.pyramid-test
  (:require [clojure.test :refer :all]
            [pharaoh.pyramid :as py]))

(def ROOT3 1.732050808)

(deftest py-max-easy
  (let [h (py/py-max 115.47)]
    (is (< (Math/abs (- h 100.0)) 1.0))))

(deftest py-max-normal
  (let [h (py/py-max 346.41)]
    (is (< (Math/abs (- h 300.0)) 1.0))))

(deftest py-max-hard
  (let [h (py/py-max 1154.7)]
    (is (< (Math/abs (- h 1000.0)) 1.0))))

(deftest py-height-zero-area
  (is (== 0.0 (py/py-height 346.41 0.0))))

(deftest py-height-full-area
  (let [b 115.47
        max-area (* (/ ROOT3 4.0) b b)
        h (py/py-height b max-area)]
    (is (< (Math/abs (- h (py/py-max b))) 1.0))))

(deftest py-height-partial-area
  (let [h (py/py-height 346.41 1000.0)]
    (is (> h 0.0))
    (is (< h 300.0))))

(deftest py-height-excess-area-capped
  (let [b 115.47
        excess-area (* 2 (/ ROOT3 4.0) b b)
        h (py/py-height b excess-area)]
    (is (<= h (py/py-max b)))))

(deftest win-condition-not-met
  (is (false? (py/won? 346.41 200.0))))

(deftest win-condition-met
  (let [b 115.47
        max-h (py/py-max b)]
    (is (true? (py/won? b max-h)))))

(deftest win-condition-within-one
  (let [b 115.47
        max-h (py/py-max b)]
    (is (true? (py/won? b (- max-h 0.5))))))
