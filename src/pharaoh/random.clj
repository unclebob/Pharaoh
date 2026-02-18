(ns pharaoh.random
  (:import [java.util Random]))

(defn make-rng [seed]
  (Random. (long seed)))

(defn uniform [^Random rng a b]
  (let [[lo hi] (if (< b a) [b a] [a b])]
    (+ lo (* (.nextDouble rng) (- hi lo)))))

(defn gaussian [^Random rng mean sigma]
  (+ mean (* sigma (.nextGaussian rng))))

(defn abs-gaussian [^Random rng mean sigma]
  (loop []
    (let [x (gaussian rng mean sigma)]
      (if (>= x 0) x (recur)))))

(defn exponential [^Random rng mean]
  (loop []
    (let [u (uniform rng 0.0 1.0)]
      (if (zero? u)
        (recur)
        (* (- (Math/log u)) mean)))))

(defn max-random [^Random rng n a b]
  (loop [i 0 best a]
    (if (>= i n)
      best
      (recur (inc i) (max best (uniform rng a b))))))
