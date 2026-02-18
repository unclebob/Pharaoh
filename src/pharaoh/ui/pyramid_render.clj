(ns pharaoh.ui.pyramid-render
  (:require [quil.core :as q]
            [pharaoh.pyramid :as py]))

(defn draw-pyramid [x y w h base stones]
  (let [max-h (py/py-max base)
        cur-h (py/py-height base stones)
        pct (if (pos? max-h) (/ cur-h max-h) 0)
        tri-h (* h pct)
        half-base (* (/ w 2.0) pct)]
    (q/fill 180 150 80)
    (q/stroke 100 80 40)
    (q/triangle (+ x (/ w 2.0)) (- (+ y h) tri-h)
                (- (+ x (/ w 2.0)) half-base) (+ y h)
                (+ (+ x (/ w 2.0)) half-base) (+ y h))
    ;; Max outline
    (q/no-fill)
    (q/stroke 200 200 200)
    (q/triangle (+ x (/ w 2.0)) y
                x (+ y h)
                (+ x w) (+ y h))))
