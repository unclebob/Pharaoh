(ns pharaoh.pyramid)

(def ^:const ROOT3 1.732050808)

(defn py-max [b]
  (* (/ ROOT3 2.0) b))

(defn py-height [b a]
  (let [max-h (py-max b)
        max-a (* (/ ROOT3 4.0) b b)]
    (cond
      (<= a 0) 0.0
      (>= a max-a) max-h
      :else
      (let [det (- (* b b) (* 4.0 (/ a ROOT3)))]
        (if (< det 0)
          max-h
          (min (/ (- b (Math/sqrt det)) (/ 2.0 ROOT3)) max-h))))))

(defn won? [base height]
  (> (+ height 1) (py-max base)))
