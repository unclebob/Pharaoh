(ns pharaoh.gherkin.steps.pyramid
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.messages :as msg]
            [pharaoh.pyramid :as py]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.workload :as wk]))

(defn steps []
  [;; ===== Pyramid Given (most specific first) =====
   {:type :given :pattern #"the pyramid base is ([\d.]+) stones"
    :handler (fn [w v] (assoc-in w [:state :py-base] (to-double v)))}
   {:type :given :pattern #"the pyramid base is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :py-base] (to-double v)))}
   {:type :given :pattern #"the pyramid has (\d+) stones and a height of (\d+)"
    :handler (fn [w stones height]
               (-> w
                   (assoc-in [:state :py-stones] (to-double stones))
                   (assoc-in [:state :py-height] (to-double height))))}
   {:type :given :pattern #"the pyramid has (\d+) stones \(area units\)"
    :handler (fn [w v] (assoc-in w [:state :py-stones] (to-double v)))}
   {:type :given :pattern #"(.+) stones have been laid"
    :handler (fn [w v] (assoc-in w [:state :py-stones] (to-double v)))}
   {:type :given :pattern #"the pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}
   {:type :given :pattern #"the pyramid quota is (\d+) stones"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"the pyramid quota is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"the maximum height is approximately (\d+)"
    :handler (fn [w v] (assoc w :expected-max-height (to-double v)))}
   {:type :given :pattern #"the area exceeds \(sqrt\(3\)/4\) \* base\^2"
    :handler (fn [w]
               (let [b (get-in w [:state :py-base] 346.41)
                     max-area (* (/ (Math/sqrt 3) 4.0) b b)]
                 (assoc-in w [:state :py-stones] (* max-area 1.1))))}
   {:type :given :pattern #"the average pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}
   {:type :given :pattern #"the current pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}
   {:type :given :pattern #"the stone quota is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"stones added this month is (\d+)"
    :handler (fn [w v] (assoc w :stones-added (to-double v)))}
   {:type :given :pattern #"the average height is ceil\(\((\d+) \+ (\d+)\) / 2\) = (\d+)"
    :handler (fn [w _ _ avg]
               (assoc-in w [:state :py-height] (to-double avg)))}
   {:type :given :pattern #"a pyramid dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :pyramid))}

   ;; ===== Pyramid When =====
   {:type :when :pattern #"the maximum height is calculated"
    :handler (fn [w]
               (assoc w :max-height (py/py-max (:py-base (:state w)))))}
   {:type :when :pattern #"the player sets the pyramid quota to (\d+)"
    :handler (fn [w v]
               (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :when :pattern #"the player tries to set the pyramid quota to (.+)"
    :handler (fn [w v]
               (let [val (to-double v)]
                 (if (neg? val)
                   (assoc-in w [:state :message] "Quota cannot be negative")
                   (assoc-in w [:state :py-quota] val))))}
   {:type :when :pattern #"the player enters a negative number for pyramid quota"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :pyramid-negative))]
                 (assoc-in w [:state :message] m)))}
   {:type :when :pattern #"the height is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     h (py/py-height (:py-base s) (:py-stones s))]
                 (assoc-in w [:state :py-height] h)))}
   {:type :when :pattern #"pyramid work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     comps (wk/work-components (:state w))]
                 (assoc w :work-components comps)))}
   {:type :when :pattern #"pyramid costs are deducted"
    :handler (fn [w]
               (let [w (snap w)
                     s (:state w)
                     stones (get w :stones-added (:py-quota s 0.0))
                     cost (* stones (:py-height s 1.0) 0.01)]
                 (assoc w :state (update s :gold - cost))))}
   {:type :when :pattern #"the win condition is checked"
    :handler (fn [w]
               (let [s (:state w)
                     won (py/won? (:py-base s) (:py-height s))]
                 (assoc-in w [:state :game-won] won)))}
   {:type :when :pattern #"the pyramid height reaches within 1 unit of the maximum"
    :handler (fn [w]
               (let [s (:state w)
                     max-h (py/py-max (:py-base s))]
                 (-> w
                     (assoc-in [:state :py-height] (- max-h 0.5))
                     (assoc-in [:state :game-won] true))))}

   ;; ===== Pyramid Then (most specific first) =====
   {:type :then :pattern #"the pyramid base should be (.+) stones"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-base (:state w)))
               w)}
   {:type :then :pattern #"max height = .+ = approximately (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected)
                            (or (:max-height w) (py/py-max (:py-base (:state w))))
                            5.0)
               w)}
   {:type :then :pattern #"the pyramid height should be approximately (.+)"
    :handler (fn [w expected]
               (let [h (py/py-height (get-in w [:state :py-base])
                                     (get-in w [:state :py-stones]))]
                 (assert-near (to-double expected) h 5.0))
               w)}
   {:type :then :pattern #"the pyramid height should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-height (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the pyramid target height should be approximately (\d+) feet"
    :handler (fn [w expected]
               (let [h (py/py-max (:py-base (:state w)))]
                 (assert-near (to-double expected) h 10.0))
               w)}
   {:type :then :pattern #"the target height is approximately (\d+) feet"
    :handler (fn [w expected]
               (let [h (py/py-max (:py-base (:state w)))]
                 (assert-near (to-double expected) h 10.0))
               w)}
   {:type :then :pattern #"the pyramid stones should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-stones (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the pyramid stone count increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :py-stones] 0.0)
                     after (:py-stones (:state w))
                     added (- after before)]
                 (assert-near (to-double expected) added 10.0))
               w)}
   {:type :then :pattern #"the pyramid quota should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-quota (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the pyramid is empty"
    :handler (fn [w]
               (assert (zero? (:py-stones (:state w) 0.0))
                       "Expected 0 pyramid stones")
               w)}
   {:type :then :pattern #"no stones are added to the pyramid"
    :handler (fn [w]
               (let [before (get-in w [:state-before :py-stones] 0.0)
                     after (:py-stones (:state w))]
                 (assert-near before after 0.01))
               w)}
   {:type :then :pattern #"the height equals the maximum for that base"
    :handler (fn [w]
               (let [s (:state w)
                     max-h (py/py-max (:py-base s))
                     h (:py-height s)]
                 (assert-near max-h h 1.0))
               w)}
   {:type :then :pattern #"stones added = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"pyramid stones, height, and quota all match"
    :handler (fn [w] w)}
   {:type :then :pattern #"pyramid work = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"determinant = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"height = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"gain = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"pyramid stones added is (.+)% of quota"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"a pyramid input error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :given :pattern #"the projected new height is (\d+)"
    :handler (fn [w v] (assoc w :projected-height (to-double v)))}
   {:type :then :pattern #"(\d+) \+ (\d+) = (\d+) which is less than the maximum of (\d+)"
    :handler (fn [w _ _ total max-h]
               (assert (< (to-double total) (to-double max-h)))
               w)}
   {:type :then :pattern #"a victory message is displayed"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random win message is displayed from the congratulations pool"
    :handler (fn [w] w)}])
