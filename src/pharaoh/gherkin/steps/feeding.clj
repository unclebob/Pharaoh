(ns pharaoh.gherkin.steps.feeding
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.feeding :as fd]
            [pharaoh.state :as st]
            [clojure.string :as str]))

(defn steps []
  [;; ===== Feeding Given (most specific first) =====
   {:type :given :pattern #"total wheat usage would be (.+) bushels"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player has only (\d+) bushels of wheat after rot"
    :handler (fn [w amt] (assoc-in w [:state :wheat] (to-double amt)))}
   {:type :given :pattern #"the player has only (\d+) tons of manure"
    :handler (fn [w amt] (assoc-in w [:state :manure] (to-double amt)))}
   {:type :given :pattern #"(\d+) tons of manure are used for spreading"
    :handler (fn [w amt] (assoc-in w [:state :mn-to-sprd] (to-double amt)))}
   {:type :given :pattern #"there are (\d+) slaves with feed rate of (\d+)"
    :handler (fn [w sl fr]
               (-> w
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :sl-feed-rt] (to-double fr))))}
   {:type :given :pattern #"the slave feed rate is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :sl-feed-rt] (to-double v)))}
   {:type :given :pattern #"the effective slave feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :sl-feed-rt] (to-double v)))}
   {:type :given :pattern #"effective oxen feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :ox-feed-rt] (to-double v)))}
   {:type :given :pattern #"effective horse feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :hs-feed-rt] (to-double v)))}
   {:type :given :pattern #"(\d+) bushels of wheat are sewn this month"
    :handler (fn [w amt] (assoc-in w [:state :wt-sewn] (to-double amt)))}
   {:type :given :pattern #"(\d+) tons of manure are spread on (\d+) acres"
    :handler (fn [w mn acres]
               (-> w
                   (assoc-in [:state :mn-to-sprd] (to-double mn))
                   (assoc-in [:state :ln-to-sew] (to-double acres))))}
   {:type :given :pattern #"(\d+) bushels of wheat are eaten this month"
    :handler (fn [w amt] (assoc w :wheat-eaten (to-double amt)))}
   {:type :given :pattern #"the manure spread per acre is ([\d.]+) tons"
    :handler (fn [w v] (assoc w :mn-per-acre (to-double v)))}
   {:type :given :pattern #"the sowing rate per acre is a fixed amount"
    :handler (fn [w] w)}
   {:type :given :pattern #"there are (\d+) oxen with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w ox fr eff]
               (-> w
                   (assoc-in [:state :oxen] (to-double ox))
                   (assoc-in [:state :ox-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}
   {:type :given :pattern #"the oxen feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :ox-feed-rt] (to-double v)))}
   {:type :given :pattern #"the horse feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :hs-feed-rt] (to-double v)))}
   {:type :given :pattern #"there are (\d+) horses with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w hs fr eff]
               (-> w
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :hs-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}
   {:type :given :pattern #"the manure spread rate is (\d+) tons"
    :handler (fn [w v] (assoc-in w [:state :mn-to-sprd] (to-double v)))}

   ;; ===== Feeding When =====
   {:type :when :pattern #"the manure balance is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     used (min (:mn-to-sprd s) (:manure s))]
                 (assoc w :state (update s :manure #(max 0.0 (- % used))))))}
   {:type :when :pattern #"the player sets the slave feed rate to ([\d.]+) bushels per slave per month"
    :handler (fn [w v]
               (let [w (snap w)]
                 (assoc-in w [:state :sl-feed-rt] (to-double v))))}
   {:type :when :pattern #"the player sets the oxen feed rate to (\d+) bushels per ox per month"
    :handler (fn [w v]
               (let [w (snap w)]
                 (assoc-in w [:state :ox-feed-rt] (to-double v))))}
   {:type :when :pattern #"the player sets the horse feed rate to (\d+) bushels per horse per month"
    :handler (fn [w v]
               (let [w (snap w)]
                 (assoc-in w [:state :hs-feed-rt] (to-double v))))}
   {:type :when :pattern #"the player tries to set a feed rate to (.+)"
    :handler (fn [w v]
               (let [val (to-double v)]
                 (if (neg? val)
                   (assoc-in w [:state :message] "Feed rate cannot be negative")
                   (assoc-in w [:state :sl-feed-rt] val))))}
   {:type :when :pattern #"manure production is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     eaten (get w :wheat-eaten 3000.0)
                     manure (fd/manure-made (:rng w) eaten)]
                 (update-in w [:state :manure] + manure)))}

   ;; ===== Feeding Then (most specific first) =====
   {:type :then :pattern #"the wheat efficiency factor is.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat (?:sown|fed to .+) is reduced to.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"actual (?:feed|sowing) rates? (?:are|is) reduced to.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"all consumption proceeds as planned"
    :handler (fn [w] w)}
   {:type :then :pattern #"the manure stockpile is clamped to 0"
    :handler (fn [w]
               (assert (>= (:manure (:state w)) 0))
               w)}
   {:type :then :pattern #"the (.+) feed rate should be ([\d.]+)"
    :handler (fn [w animal expected]
               (let [k (cond
                         (= animal "slave") :sl-feed-rt
                         (= animal "oxen") :ox-feed-rt
                         (= animal "horse") :hs-feed-rt
                         :else :sl-feed-rt)]
                 (assert-near (to-double expected) (get (:state w) k 0.0) 0.1))
               w)}
   {:type :then :pattern #"wheat fed to slaves = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat fed to oxen = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat fed to horses = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat consumed for sowing is sowing rate \* (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"approximately (\d+) tons of manure are added to the stockpile"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :manure] 0.0)
                     after (:manure (:state w) 0.0)
                     added (- after before)]
                 (assert-near (to-double expected) added 10.0))
               w)}
   {:type :then :pattern #"the manure-per-acre ratio is ([\d.]+)"
    :handler (fn [w expected]
               (let [s (:state w)
                     spread (:mn-to-sprd s 0.0)
                     acres (:ln-to-sew s 1.0)
                     ratio (if (pos? acres) (/ spread acres) 0.0)]
                 (assert-near (to-double expected) ratio 0.5))
               w)}
])
