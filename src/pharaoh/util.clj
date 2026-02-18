(ns pharaoh.util)

(defn clamp [x lo hi]
  (max lo (min hi x)))

(defn clip [x]
  (max 0 x))

(defn fmt-float [x]
  (format "%.0f" (double x)))
