(ns pharaoh.overseers
  (:require [pharaoh.messages :as msg]
            [pharaoh.random :as r]))

(defn hire [state n]
  (update state :overseers + n))

(defn fire [state n]
  (let [new-count (- (:overseers state) n)]
    (if (< new-count 0)
      {:error "Cannot fire more overseers than employed"}
      (assoc state :overseers new-count))))

(defn obtain [state n]
  (assoc state :overseers n))

(defn overseer-stress [state wk-deff-sl]
  (let [ov-stress (if (> wk-deff-sl 0) (min 1 (/ wk-deff-sl 10)) 0)
        ov-relax (if (> wk-deff-sl 0) 0 (* (:ov-press state) 0.3))]
    (+ (:ov-press state) ov-stress (- ov-relax))))

(defn overseers-quit [rng state]
  (let [raise-pct (r/gaussian rng 20.0 2.0)
        text (format (msg/pick rng msg/missed-payroll-messages) raise-pct)]
    (-> state
      (assoc :overseers 0 :message text)
      (update :ov-pay + (* (:ov-pay state) (/ raise-pct 100))))))
