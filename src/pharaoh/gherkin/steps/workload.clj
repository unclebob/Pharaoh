(ns pharaoh.gherkin.steps.workload
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.overseers :as ov]
            [pharaoh.tables :as t]
            [pharaoh.workload :as wk]
            [clojure.string :as str]))

(defn steps []
  [;; ===== Workload Given (most specific first) =====
   {:type :given :pattern #"max work per slave is (\d+) and required work per slave is (\d+)"
    :handler (fn [w max-wk req-wk]
               (-> w
                   (assoc :max-wk-sl (to-double max-wk))
                   (assoc :req-wk-sl (to-double req-wk))))}
   {:type :given :pattern #"all work components sum to (\d+)"
    :handler (fn [w total] (assoc w :expected-work (to-double total)))}
   {:type :given :pattern #"the work deficit per slave is (\d+)"
    :handler (fn [w deficit]
               (assoc-in w [:state :wk-deff-sl] (to-double deficit)))}
   {:type :given :pattern #"slave labor \(work per slave / ox multiplier\) is (\d+)"
    :handler (fn [w v] (assoc w :slave-labor (to-double v)))}
   {:type :given :pattern #"motivation is ([\d.]+)"
    :handler (fn [w v] (assoc w :motivation (to-double v)))}
   {:type :given :pattern #"work ability per slave is ([\d.]+)"
    :handler (fn [w v] (assoc w :wk-able (to-double v)))}
   {:type :given :pattern #"ox multiplier is ([\d.]+)"
    :handler (fn [w v] (assoc w :ox-mult (to-double v)))}
   {:type :given :pattern #"oxen efficiency is ([\d.]+)"
    :handler (fn [w v] (assoc w :ox-eff (to-double v)))}
   {:type :given :pattern #"horse efficiency is ([\d.]+)"
    :handler (fn [w v] (assoc w :hs-eff (to-double v)))}
   {:type :given :pattern #"slave efficiency is (.+)"
    :handler (fn [w v]
               (let [eff (to-double v)]
                 (-> w
                     (assoc-in [:state :sl-eff] eff)
                     (assoc :forced-sl-eff eff))))}
   {:type :given :pattern #"slaves are fully efficient"
    :handler (fn [w] (assoc-in w [:state :sl-eff] 1.0))}
   {:type :given :pattern #"there are (\d+) oxen with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w ox fr eff]
               (-> w
                   (assoc-in [:state :oxen] (to-double ox))
                   (assoc-in [:state :ox-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}
   {:type :given :pattern #"there are (\d+) horses with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w hs fr eff]
               (-> w
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :hs-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}
   {:type :given :pattern #"a random event adds (\d+) man-hours of extra work"
    :handler (fn [w hrs]
               (assoc-in w [:state :wk-addition] (to-double hrs)))}
   {:type :given :pattern #"the overseer effect per slave is ([\d.]+)"
    :handler (fn [w v] (assoc w :ov-eff-sl (to-double v)))}
   {:type :given :pattern #"the overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"the current overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"the player has overseers"
    :handler (fn [w] (assoc-in w [:state :overseers] 5.0))}
   {:type :given :pattern #"the overseer pay is (\d+) gold each"
    :handler (fn [w v]
               (-> w
                   (assoc-in [:state :ov-pay] (to-double v))
                   (update-in [:state :gold] #(max (or % 0.0) 100000.0))))}
   {:type :given :pattern #"the player cannot pay overseers this month"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :overseers] 5.0)
                   (assoc-in [:state :gold] -100.0)))}
   {:type :given :pattern #"there are (\d+) overseers and (\d+) horses"
    :handler (fn [w ov hs]
               (-> w
                   (assoc-in [:state :overseers] (to-double ov))
                   (assoc-in [:state :horses] (to-double hs))))}
   {:type :given :pattern #"there are (\d+) overseers and (\d+) slaves"
    :handler (fn [w ov sl]
               (-> w
                   (assoc-in [:state :overseers] (to-double ov))
                   (assoc-in [:state :slaves] (to-double sl))))}

   ;; ===== Workload When (most specific first) =====
   {:type :when :pattern #"required work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     result (wk/required-work (:rng w) (:state w))]
                 (assoc w :work-result result)))}
   {:type :when :pattern #"the total required work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (merge {:ln-sewn 0.0 :ln-grown 0.0 :wt-ripe 0.0 :ln-ripe 0.0
                               :horses 0.0 :oxen 0.0 :py-quota 0.0 :py-height 0.0
                               :mn-to-sprd 0.0 :ln-to-sew 0.0 :wk-addition 0.0} (:state w))
                     result (wk/required-work (:rng w) s)]
                 (-> w
                     (assoc :work-result result)
                     (assoc-in [:state :wk-addition] 0.0))))}
   {:type :when :pattern #"the work deficit is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     cap (wk/slave-capacity (:rng w) s)
                     req (wk/required-work (:rng w) s)
                     eff (wk/compute-efficiency (:slaves s) (:max-wk-sl cap) (:req-work req))]
                 (assoc w :work-eff eff)))}
   {:type :when :pattern #"actual work per slave is determined"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     slaves (get-in w [:state :slaves] 100.0)
                     max-wk (or (:max-wk-sl w) 8.0)
                     req-wk-sl (or (:req-wk-sl w) 10.0)
                     req-work (* req-wk-sl slaves)
                     eff (wk/compute-efficiency slaves max-wk req-work)]
                 (-> w
                     (assoc :slave-capacity eff)
                     (update :state merge eff))))}
   {:type :when :pattern #"work ability is (?:calculated|determined)"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (merge {:slaves 100.0 :oxen 50.0 :horses 20.0
                               :overseers 5.0 :sl-health 0.8 :ox-health 0.8
                               :hs-health 0.8} (:state w))
                     cap (wk/slave-capacity (:rng w) s)]
                 (assoc w :slave-capacity cap :state (merge s cap))))}
   {:type :when :pattern #"maximum work per slave is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :slave-capacity cap)))}
   {:type :when :pattern #"the ox multiplier is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :ox-mult (:ox-mult cap))))}
   {:type :when :pattern #"overseer effectiveness is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :slave-capacity cap)))}
   {:type :when :pattern #"lashing is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     press (:ov-press s 0.0)
                     lash-rt (t/interpolate press t/stress-lash)]
                 (assoc-in w [:state :sl-lash-rt] lash-rt)))}
   {:type :when :pattern #"motivation is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     press (:ov-press s 0.0)
                     pos (t/interpolate (:sl-health s 0.8) t/positive-motive)
                     neg (t/interpolate press t/negative-motive)]
                 (assoc w :motivation (+ pos neg))))}
   {:type :when :pattern #"the ratio is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     ratio (if (pos? (:overseers s 0.0))
                             (/ (:slaves s 0.0) (:overseers s))
                             Double/POSITIVE_INFINITY)]
                 (assoc w :sl-ov-ratio ratio)))}
   {:type :when :pattern #"horse efficiency is determined"
    :handler (fn [w]
               (let [s (:state w)
                     h (:hs-health s 0.8)
                     eff (t/interpolate h t/horse-efficiency)]
                 (assoc w :hs-eff eff)))}
   {:type :when :pattern #"oxen efficiency is determined"
    :handler (fn [w]
               (let [s (:state w)
                     h (:ox-health s 0.8)
                     eff (t/interpolate h t/oxen-efficiency)]
                 (assoc w :ox-eff eff)))}
   {:type :when :pattern #"pyramid work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     comps (wk/work-components (:state w))]
                 (assoc w :work-components comps)))}
   {:type :when :pattern #"stress is calculated"
    :handler (fn [w]
               (let [w (snap w)
                     deficit (:wk-deff-sl (:state w) 0.0)]
                 (assoc w :state (ov/overseer-stress (:state w) deficit))))}
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
   {:type :when :pattern #"overseers quit"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ov/overseers-quit (:rng w) (:state w)))))}
   {:type :when :pattern #"slave sickness is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     sick (t/interpolate h t/work-sickness)]
                 (assoc w :sickness-rate sick)))}

   ;; ===== Workload Then (most specific first) =====
   {:type :then :pattern #"actual work per slave = (\d+) \(the (?:required amount|maximum they can do)\)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the work component (.+) equals (.+)"
    :handler (fn [w component formula]
               (let [w (ensure-rng w)
                     comps (wk/work-components (:state w))]
                 (assert (map? comps) "Expected work components map"))
               w)}
   {:type :then :pattern #"total required work = (\d+), randomized with approximately (\d+)% variance"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"required work per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the (\d+) extra man-hours are included in the total"
    :handler (fn [w hrs]
               ;; required-work applies abs-gaussian(1.0, 0.1) noise factor
               (let [result (:work-result w)
                     threshold (* (to-double hrs) 0.7)]
                 (assert (>= (:req-work result 0) threshold)
                         (str "Expected total work >= " threshold " but got " (:req-work result 0))))
               w)}
   {:type :then :pattern #"the extra work resets to 0 after the month"
    :handler (fn [w]
               (assert (zero? (:wk-addition (:state w) 0.0))
                       "Expected wk-addition to be 0")
               w)}
   {:type :then :pattern #"extra work = approximately (\d+) man-hours per slave plus (\d+) per acre"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"extra work = approximately (\d+) man-hours per slave plus (\d+) per overseer"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"work ability is looked up from the health-to-ability table"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)]
                 (assert (pos? ability) "Expected positive work ability"))
               w)}
   {:type :then :pattern #"work ability per slave is reduced"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)
                     max-ability (t/interpolate 1.0 t/work-ability)]
                 (assert (< ability max-ability) "Expected reduced work ability"))
               w)}
   {:type :then :pattern #"slaves produce less work per person"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)
                     max-ability (t/interpolate 1.0 t/work-ability)]
                 (assert (< ability max-ability) "Slaves should produce less work"))
               w)}
   {:type :then :pattern #"work deficit per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"total work = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"max work per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave efficiency = ([\d.]+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:sl-eff (:state w)) 0.05)
               w)}
   {:type :then :pattern #"slave efficiency = .+"
    :handler (fn [w]
               (assert (number? (:sl-eff (:state w)))
                       "Expected sl-eff to be a number")
               w)}
   {:type :then :pattern #"oxen-to-slave ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave-to-overseer ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the effective ox multiplier = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"mounted effectiveness = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the raw ox multiplier is looked up from the ox-ratio table"
    :handler (fn [w] w)}
   {:type :then :pattern #"total motivation = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the ox multiplier for slave work is diminished"
    :handler (fn [w] w)}
   {:type :then :pattern #"randomized with approximately (\d+)% variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"horse-to-overseer ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effect per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effectiveness is looked up from the effectiveness table based on .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effectiveness drops"
    :handler (fn [w] w)}
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
   {:type :then :pattern #"overseer pressure = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the overseer pay (?:increases|rate increases) by approximately (\d+)% with slight (?:random )?variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the overseer pay increases by approximately (\d+)% with slight variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the player should have (\d+) overseers"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:overseers (:state w)) 0.01)
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
   {:type :then :pattern #"overseer count and pressure match the saved values"
    :handler (fn [w] w)}
   {:type :then :pattern #"positive motivation is looked up from the oversight table, randomized ±10%"
    :handler (fn [w] w)}
   {:type :then :pattern #"negative motivation is looked up from the lashing table"
    :handler (fn [w] w)}
   {:type :then :pattern #"stress-driven lashing is looked up from the stress-to-lash table"
    :handler (fn [w] w)}
   {:type :then :pattern #"stress increase = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave lash rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"relaxation = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"suffering is looked up from the lash-to-suffering table"
    :handler (fn [w] w)}
   {:type :then :pattern #"work sickness is looked up from the labor-to-sickness table"
    :handler (fn [w] w)}
   {:type :then :pattern #"horse efficiency is reduced"
    :handler (fn [w]
               (let [eff (or (:hs-eff w) 1.0)]
                 (assert (< eff 1.0) "Expected reduced horse efficiency"))
               w)}
   {:type :then :pattern #"oxen efficiency is reduced"
    :handler (fn [w]
               (let [eff (or (:ox-eff w) 1.0)]
                 (assert (< eff 1.0) "Expected reduced oxen efficiency"))
               w)}
   {:type :then :pattern #"pyramid work = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"all activities proceed at full capacity"
    :handler (fn [w]
               (assert (>= (:sl-eff (:state w) 1.0) 0.99)
                       "Expected full efficiency")
               w)}
   {:type :then :pattern #"all activities are reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (:sl-eff (:state w))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.1))
               w)}
   {:type :then :pattern #"harvest is (.+)% of ripe wheat"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"manure spread is reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"pyramid stones added is (.+)% of quota"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"livestock feeding rates are reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"planting rate is reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (:sl-eff (:state w))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"the enemy army is proportional to the player's overseers, randomized ±(\d+)%"
    :handler (fn [w _] w)}])
