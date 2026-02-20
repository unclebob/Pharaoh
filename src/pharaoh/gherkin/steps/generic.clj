(ns pharaoh.gherkin.steps.generic
  (:require [clojure.string :as str]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]))

;; These catch-all patterns MUST be last in the step concatenation.
;; They use broad regexes (.+) that would shadow more specific
;; patterns in domain files if placed earlier.

(defn steps []
  [;; ===== Generic Given catch-alls =====
   {:type :given :pattern #"the player has (.+) gold"
    :handler (fn [w v]
               (-> w
                   (assoc-in [:state :gold] (to-double v))
                   (assoc :gold-explicitly-set true)))}
   {:type :given :pattern #"the player has (\d+) tons of (.+)"
    :handler (fn [w amt commodity]
               (assoc-in w [:state (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the player has (\d+) (.+)"
    :handler (fn [w amount commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state k] (to-double amount))))}

   ;; ===== Generic Then catch-alls =====
   {:type :then :pattern #"the player should have ([\d.]+) gold"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:gold (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the player should have (\d+) slaves"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:slaves (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the player should have (\d+) (.+)"
    :handler (fn [w expected commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assert-near (to-double expected) (get (:state w) k 0.0) 1.0))
               w)}

   ;; ===== Generic message pool catch-all =====
   {:type :then :pattern #"a random (.+) message is displayed (?:from the pool|with .+)"
    :handler (fn [w _]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}

   ;; ===== Generic gold change catch-all =====
   {:type :then :pattern #"gold decreases by (.+)"
    :handler (fn [w expr]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (< after before) "Expected gold to decrease"))
               w)}

   ;; ===== Generic assertion catch-alls =====
   {:type :then :pattern #"slaves = (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:slaves (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the gold received is .+"
    :handler (fn [w]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (> after before) "Expected gold to increase"))
               w)}])
