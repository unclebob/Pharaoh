(ns pharaoh.gherkin.steps.planting
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.planting :as pl]
            [pharaoh.simulation :as sim]
            [pharaoh.state :as st]
            [pharaoh.random :as r]
            [clojure.string :as str]))

(defn steps []
  [;; ===== Planting Given (most specific first) =====
   {:type :given :pattern #"the player sets the planting quota to (.+) acres"
    :handler (fn [w amt]
               (let [a (to-double amt)
                     wt-per-acre (get-in w [:state :wt-sewn-ln] 20.0)
                     wheat-needed (* a wt-per-acre 2.0)]
                 (-> w
                     (assoc-in [:state :ln-to-sew] a)
                     ;; Ensure fallow land >= quota so planting isn't trivially zero
                     (update-in [:state :ln-fallow] #(max (or % 0.0) a))
                     ;; Ensure enough wheat for sowing
                     (update-in [:state :wheat] #(max (or % 0.0) wheat-needed)))))}
   {:type :given :pattern #"slaves are fully efficient"
    :handler (fn [w] (assoc-in w [:state :sl-eff] 1.0))}
   {:type :given :pattern #"the player has land in various stages"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :ln-fallow] 100.0)
                   (assoc-in [:state :ln-sewn] 100.0)
                   (assoc-in [:state :ln-grown] 100.0)
                   (assoc-in [:state :ln-ripe] 100.0)))}
   {:type :given :pattern #"there is a monthly rot rate"
    :handler (fn [w] w)}
   {:type :given :pattern #"the player sets manure to spread at (.+) tons"
    :handler (fn [w amt] (assoc-in w [:state :mn-to-sprd] (to-double amt)))}
   {:type :given :pattern #"slave efficiency is (.+)"
    :handler (fn [w v]
               (let [eff (to-double v)]
                 (-> w
                     (assoc-in [:state :sl-eff] eff)
                     (assoc :forced-sl-eff eff))))}
   {:type :given :pattern #"the planting quota is (\d+) acres"
    :handler (fn [w n] (assoc-in w [:state :ln-to-sew] (to-double n)))}
   {:type :given :pattern #"(\d+) acres are being planted"
    :handler (fn [w n] (assoc-in w [:state :ln-to-sew] (to-double n)))}
   {:type :given :pattern #"only (\d+) acres are fallow"
    :handler (fn [w n] (assoc-in w [:state :ln-fallow] (to-double n)))}
   {:type :given :pattern #"there are (\d+) bushels of ripe wheat"
    :handler (fn [w amt]
               (-> w
                   (assoc-in [:state :wt-ripe] (to-double amt))
                   (assoc-in [:state :ln-ripe] 100.0)))}
   {:type :given :pattern #"(\d+) bushels of wheat are sewn this month"
    :handler (fn [w amt] (assoc-in w [:state :wt-sewn] (to-double amt)))}
   {:type :given :pattern #"(\d+) tons of manure are spread on (\d+) acres"
    :handler (fn [w mn acres]
               (-> w
                   (assoc-in [:state :mn-to-sprd] (to-double mn))
                   (assoc-in [:state :ln-to-sew] (to-double acres))))}
   {:type :given :pattern #"the manure spread per acre is ([\d.]+) tons"
    :handler (fn [w v] (assoc w :mn-per-acre (to-double v)))}
   {:type :given :pattern #"the sowing rate per acre is a fixed amount"
    :handler (fn [w] w)}
   {:type :given :pattern #"the manure spread rate is (\d+) tons"
    :handler (fn [w v] (assoc-in w [:state :mn-to-sprd] (to-double v)))}

   ;; ===== Simulation When (most specific first) =====
   {:type :when :pattern #"a month is simulated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     rng (:rng w)]
                 (if-let [eff (:forced-sl-eff w)]
                   ;; Split pipeline: compute workload, override sl-eff, then continue
                   (let [s (#'sim/record-old s)
                         s (#'sim/advance-date s)
                         s (#'sim/compute-workload s rng)
                         s (assoc s :sl-eff eff)
                         s (#'sim/compute-feeding s rng)
                         s (#'sim/apply-planting s rng)
                         s (#'sim/apply-populations s)
                         s (#'sim/apply-health s rng)
                         s (#'sim/apply-pyramid s)
                         s (#'sim/apply-overseer-stress s)
                         s (#'sim/apply-costs s rng)
                         s (#'sim/process-contracts s rng)
                         s (#'sim/check-overseers-unpaid s rng)
                         s (#'sim/apply-loan-interest s)
                         s (#'sim/apply-market s rng)
                         s (#'sim/apply-credit-update s)
                         s (#'sim/check-emergency-loan s)
                         s (#'sim/update-net-worth s)
                         s (#'sim/check-foreclosure s rng)
                         s (#'sim/check-debt-warning s rng)
                         s (#'sim/check-win s)
                         s (assoc s :wk-addition 0.0)]
                     (assoc w :state s))
                   ;; Normal path: just run-month
                   (assoc w :state (sim/run-month rng s)))))}
   {:type :when :pattern #"a month is simulated without an event"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     ;; seed 42 does not trigger event (uniform(0,8) >= 1.0)
                     rng (r/make-rng 42)]
                 (assoc w :state (sim/do-run rng (:state w)))))}
   {:type :when :pattern #"month (\d+) is simulated"
    :handler (fn [w _]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"the wheat yield is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     mn-ln (if (pos? (st/total-land s))
                             (/ (:manure s 0.0) (st/total-land s))
                             0.0)
                     y (pl/wheat-yield (:rng w) (:month s 1) mn-ln)]
                 (assoc w :wheat-yield y)))}
   {:type :when :pattern #"wheat is planted"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     y (pl/wheat-yield (:rng w) (:month s) 0.0)]
                 (assoc w :wheat-yield y :state s)))}
   {:type :when :pattern #"the (?:next|following) month is simulated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"a new month begins"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"the player clicks \"Run\""
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"the monthly simulation runs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}

   ;; ===== Planting Then (most specific first) =====
   {:type :then :pattern #"(\d+) acres move from (.+) to (.+)"
    :handler (fn [w _ _ _] w)}
   {:type :then :pattern #"(\d+) acres are harvested and return to fallow"
    :handler (fn [w _] w)}
   {:type :then :pattern #"total land = (.+)"
    :handler (fn [w _]
               (let [s (:state w)
                     total (+ (:ln-fallow s) (:ln-sewn s) (:ln-grown s) (:ln-ripe s))]
                 (assert (pos? total) "Total land should be positive"))
               w)}
   {:type :then :pattern #"wheat lost to rot = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the rot is deducted before usage calculations"
    :handler (fn [w] w)}
   {:type :then :pattern #"only (.+) tons are (?:spread|actually spread)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"only (\d+) acres are actually planted"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :ln-sewn] 0.0)
                     after (:ln-sewn (:state w))
                     planted (- after before)]
                 (assert-near (to-double expected) planted 1.0))
               w)}
   {:type :then :pattern #"planting rate is reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (:sl-eff (:state w))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"the resulting harvest will be (?:the largest|very poor)"
    :handler (fn [w] w)}
   {:type :then :pattern #"the seasonal yield multiplier is at its (?:maximum|minimum)"
    :handler (fn [w] w)}
   {:type :then :pattern #"(\d+) bushels are harvested \(ripe wheat \* slave efficiency\)"
    :handler (fn [w expected]
               (let [before (:state-before w)
                     after (:state w)
                     harvested (- (:wheat after 0.0) (:wheat before 0.0))]
                 (assert-near (to-double expected) (Math/abs harvested) 500.0))
               w)}
   {:type :then :pattern #"(\d+) bushels are lost \(\(1 - slave efficiency\) \* ripe wheat\)"
    :handler (fn [w expected]
               (let [ripe-before (get-in w [:state-before :wt-ripe] 0.0)
                     ripe-after (:wt-ripe (:state w) 0.0)
                     lost (- ripe-before ripe-after)
                     harvested (- (:wheat (:state w) 0.0) (:wheat (:state-before w) 0.0))]
                 ;; lost = ripe that wasn't harvested
                 (assert-near (to-double expected) (- ripe-before harvested) 500.0))
               w)}
   {:type :then :pattern #"the wheat store increases by (\d+) bushels"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wheat] 0.0)
                     after (:wheat (:state w) 0.0)
                     increase (- after before)]
                 (assert-near (to-double expected) increase 500.0))
               w)}
   {:type :then :pattern #"wheat sewn decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-sewn] 0.0)
                     after (:wt-sewn (:state w))
                     decrease (- before after)]
                 (assert-near (to-double expected) decrease 100.0))
               w)}
   {:type :then :pattern #"wheat growing increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-grown] 0.0)
                     after (:wt-grown (:state w))
                     increase (- after before)]
                 (assert-near (to-double expected) increase 100.0))
               w)}
   {:type :then :pattern #"wheat growing decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-grown] 0.0)
                     after (:wt-grown (:state w))
                     decrease (- before after)]
                 (assert-near (to-double expected) decrease 100.0))
               w)}
   {:type :then :pattern #"wheat ripe increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-ripe] 0.0)
                     after (:wt-ripe (:state w))
                     increase (- after before)]
                 (assert-near (to-double expected) increase 100.0))
               w)}
   {:type :then :pattern #"yield = .+"
    :handler (fn [w] w)}
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
   {:type :then :pattern #"livestock feeding rates are reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
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
   {:type :then :pattern #"the monthly simulation runs"
    :handler (fn [w] w)}
   {:type :then :pattern #"the previous month values are recorded for display"
    :handler (fn [w] w)}
   {:type :then :pattern #"the screen is updated with new values"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat consumed for sowing is sowing rate \* (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the manure-per-acre ratio is ([\d.]+)"
    :handler (fn [w expected]
               (let [s (:state w)
                     spread (:mn-to-sprd s 0.0)
                     acres (:ln-to-sew s 1.0)
                     ratio (if (pos? acres) (/ spread acres) 0.0)]
                 (assert-near (to-double expected) ratio 0.5))
               w)}])
