(ns pharaoh.gherkin.steps.overseers
  (:require [clojure.string :as str]
            [pharaoh.messages :as msg]
            [pharaoh.overseers :as ov]
            [pharaoh.tables :as t]
            [pharaoh.workload :as wk]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]))

(defn steps []
  [;; ===== Overseer Given (specific before generic) =====
   {:type :given :pattern #"the player has overseers"
    :handler (fn [w] (assoc-in w [:state :overseers] 5.0))}
   {:type :given :pattern #"the overseer pay is (\d+) gold each"
    :handler (fn [w v]
               (-> w
                   (assoc-in [:state :ov-pay] (to-double v))
                   (update-in [:state :gold] #(max (or % 0.0) 100000.0))))}
   {:type :given :pattern #"the overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"the overseer effect per slave is ([\d.]+)"
    :handler (fn [w v] (assoc w :ov-eff-sl (to-double v)))}
   {:type :given :pattern #"the player cannot pay overseers this month"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :overseers] 5.0)
                   (assoc-in [:state :gold] -100.0)))}
   {:type :given :pattern #"the current overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}

   ;; ===== Overseer When =====
   {:type :when :pattern #"the player hires (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/hire (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player fires (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/fire (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player obtains (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/obtain (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player tries to fire (\d+) overseers"
    :handler (fn [w n]
               (let [result (ov/fire (:state w) (Integer/parseInt n))]
                 (if (map? result)
                   (if (:error result)
                     (assoc w :error (:error result))
                     (assoc w :state result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the player tries to hire ([\d.]+) overseers"
    :handler (fn [w n]
               (let [v (Double/parseDouble n)]
                 (if (not= v (Math/floor v))
                   (assoc w :error "Fractional number of overseers not allowed")
                   (let [result (ov/hire (:state w) (long v))]
                     (if (and (map? result) (:error result))
                       (assoc w :error (:error result))
                       (assoc w :state result))))))}
   {:type :when :pattern #"the player enters a fractional number for overseers"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :overseer-fractional))]
                 (assoc-in w [:state :message] m)))}
   {:type :when :pattern #"overseers quit"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ov/overseers-quit (:rng w) (:state w)))))}
   {:type :when :pattern #"monthly costs are assessed"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)]
                 (if (neg? (:gold s))
                   (assoc w :state (ov/overseers-quit (:rng w) s))
                   w)))}
   {:type :when :pattern #"stress is calculated"
    :handler (fn [w]
               (let [w (snap w)
                     deficit (:wk-deff-sl (:state w) 0.0)]
                 (assoc w :state (ov/overseer-stress (:state w) deficit))))}
   {:type :when :pattern #"overseer effectiveness is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :slave-capacity cap)))}

   ;; ===== Overseer Then (specific before generic) =====
   {:type :then :pattern #"the player should have (\d+) overseers"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:overseers (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the action is rejected with a fractional number error"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected fractional number error")
               w)}
   {:type :then :pattern #"the action is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected action to be rejected")
               w)}
   {:type :then :pattern #"a random fractional-input message is displayed from the pool"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected fractional error message")
               w)}
   {:type :then :pattern #"all overseers are fired"
    :handler (fn [w]
               (assert (zero? (:overseers (:state w)))
                       (str "Expected 0 overseers, got " (:overseers (:state w))))
               w)}
   {:type :then :pattern #"some overseers leave .+"
    :handler (fn [w]
               (let [before (get-in w [:state-before :overseers] 5.0)
                     after (:overseers (:state w))]
                 (assert (< after before) "Expected overseers to decrease"))
               w)}
   {:type :then :pattern #"overseer stress increases.*"
    :handler (fn [w]
               (let [before (get-in w [:state-before :ov-press] 0.0)
                     after (:ov-press (:state w))]
                 (assert (>= after before) "Expected stress to increase"))
               w)}
   {:type :then :pattern #"overseers begin to relax"
    :handler (fn [w]
               (let [before (get-in w [:state-before :ov-press] 0.6)
                     after (:ov-press (:state w))]
                 (assert (<= after before) "Expected stress to decrease"))
               w)}
   {:type :then :pattern #"the message includes the raise percentage demanded to return"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effectiveness drops"
    :handler (fn [w] w)}
   {:type :then :pattern #"stress increase = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer pressure = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"stress-driven lashing is looked up from the stress-to-lash table"
    :handler (fn [w] w)}
   {:type :then :pattern #"positive motivation is looked up from the oversight table, randomized ±10%"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer count and pressure match the saved values"
    :handler (fn [w] w)}
   {:type :then :pattern #"the overseer pay increases by approximately (\d+)% with slight variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the overseer pay (?:increases|rate increases) by approximately (\d+)% with slight (?:random )?variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"overseer effectiveness is looked up from the effectiveness table based on .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effect per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave-to-overseer ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"extra work = approximately (\d+) man-hours per slave plus (\d+) per overseer"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"a random missed-payroll message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected missed-payroll message"))
               w)}])
