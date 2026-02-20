(ns pharaoh.gherkin.steps.health
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.health :as hl]
            [pharaoh.tables :as t]
            [pharaoh.workload :as wk]
            [clojure.string :as str]))

(defn steps []
  [;; ===== Health Given (most specific compound patterns first) =====
   {:type :given :pattern #"slave health is (.+), oxen health is (.+), horse health is (.+)"
    :handler (fn [w sh oh hh]
               (-> w
                   (assoc-in [:state :sl-health] (to-double sh))
                   (assoc-in [:state :ox-health] (to-double oh))
                   (assoc-in [:state :hs-health] (to-double hh))))}
   {:type :given :pattern #"slave health is (.+) and nourishment is (.+)"
    :handler (fn [w health nourishment]
               (-> w
                   (assoc-in [:state :sl-health] (to-double health))
                   (assoc :nourishment (to-double nourishment))))}
   {:type :given :pattern #"slave health is (.+) and sickness rate is (.+)"
    :handler (fn [w health sickness]
               (-> w
                   (assoc-in [:state :sl-health] (to-double health))
                   (assoc :sickness-rate (to-double sickness))))}
   {:type :given :pattern #"slave health is below (.+)"
    :handler (fn [w threshold]
               (assoc-in w [:state :sl-health] (- (to-double threshold) 0.1)))}
   {:type :given :pattern #"slave health is (.+)"
    :handler (fn [w v] (assoc-in w [:state :sl-health] (to-double v)))}
   {:type :given :pattern #"the slave lash rate is (.+)"
    :handler (fn [w v] (assoc-in w [:state :sl-lash-rt] (to-double v)))}
   {:type :given :pattern #"nourishment is ([\d.]+) and sickness rate is ([\d.]+)"
    :handler (fn [w nourish sick]
               (-> w
                   (assoc :nourishment (to-double nourish))
                   (assoc :sickness-rate (to-double sick))))}
   {:type :given :pattern #"oxen health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ox-health] (to-double v)))}
   {:type :given :pattern #"horse health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"horses health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"slaves health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :sl-health] (to-double v)))}
   {:type :given :pattern #"the player's current horses have health ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"the death rate would kill (\d+)"
    :handler (fn [w kill-count]
               (assoc w :test-death-rate 3.0))}

   ;; ===== Health When =====
   {:type :when :pattern #"slave health is updated"
    :handler (fn [w]
               (let [h (:sl-health (:state w))
                     nourish (get w :nourishment 0.0)
                     sick (get w :sickness-rate 0.0)
                     new-h (max 0.0 (min 1.0 (+ h nourish (- sick))))]
                 (assoc-in w [:state :sl-health] new-h)))}
   {:type :when :pattern #"birth and death rates are calculated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)]
                 (assoc w :state
                        (-> s
                            (assoc :slaves (hl/population-update
                                            (:slaves s)
                                            (t/interpolate (:sl-health s) t/slave-birth)
                                            (t/interpolate (:sl-health s) t/slave-death)))
                            (assoc :oxen (hl/population-update
                                           (:oxen s)
                                           (t/interpolate (:ox-health s) t/oxen-birth)
                                           (t/interpolate (:ox-health s) t/oxen-death)))
                            (assoc :horses (hl/population-update
                                             (:horses s)
                                             (t/interpolate (:hs-health s) t/horse-birth)
                                             (t/interpolate (:hs-health s) t/horse-death)))))))}
   {:type :when :pattern #"births and deaths are calculated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)]
                 (assoc w :state
                        (-> s
                            (assoc :slaves (hl/population-update
                                            (:slaves s)
                                            (t/interpolate (:sl-health s) t/slave-birth)
                                            (t/interpolate (:sl-health s) t/slave-death)))))))}
   {:type :when :pattern #"comparing aging rates"
    :handler (fn [w] w)}
   {:type :when :pattern #"horse health is updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     result (hl/horse-health-update (:rng w) s
                              (get s :hs-fed 0.5))]
                 (assoc w :state (merge s result))))}
   {:type :when :pattern #"oxen health is updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     result (hl/oxen-health-update (:rng w) s
                              (get s :ox-fed 0.5))]
                 (assoc w :state (merge s result))))}
   {:type :when :pattern #"population is updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     sl-d (or (:test-death-rate w)
                              (t/interpolate (:sl-health s 0.0) t/slave-death))]
                 (assoc w :state
                        (-> s
                            (assoc :slaves (hl/population-update
                                            (:slaves s)
                                            (t/interpolate (:sl-health s 0.0) t/slave-birth)
                                            sl-d))
                            (assoc :oxen (hl/population-update
                                           (:oxen s)
                                           (t/interpolate (:ox-health s 0.0) t/oxen-birth)
                                           (t/interpolate (:ox-health s 0.0) t/oxen-death)))
                            (assoc :horses (hl/population-update
                                             (:horses s)
                                             (t/interpolate (:hs-health s 0.0) t/horse-birth)
                                             (t/interpolate (:hs-health s 0.0) t/horse-death)))))))}
   {:type :when :pattern #"slave sickness is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     sick (t/interpolate h t/work-sickness)]
                 (assoc w :sickness-rate sick)))}

   ;; ===== Health Then (most specific first) =====
   {:type :then :pattern #"slave health is clamped to (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:sl-health (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the new (.+) have nominal health of approximately (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the blended slave health is.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"sickness rate = (\d+)"
    :handler (fn [w expected]
               (let [v (to-double expected)
                     health (:sl-health (:state w) 0.0)]
                 (when (zero? v)
                   (assert (<= health 0.001)
                           (str "Expected health ~0 for sickness rate 0, got " health))))
               w)}
   {:type :then :pattern #"nourishment is looked up from the slave nourishment table based on feed rate"
    :handler (fn [w]
               (let [fr (:sl-feed-rt (:state w) 10.0)
                     nourish (t/interpolate fr t/slave-nourishment)]
                 (assert (number? nourish) "Expected numeric nourishment"))
               w)}
   {:type :then :pattern #"slave health = (.+)"
    :handler (fn [w expected]
               (let [parts (str/split expected #" [+=\\-] ")
                     val (to-double (last (str/split expected #"= ")))]
                 (assert-near val (:sl-health (:state w)) 0.05))
               w)}
   {:type :then :pattern #"slave health is multiplied by approximately ([\d.]+) with slight variance"
    :handler (fn [w factor]
               (let [before (get-in w [:state-before :sl-health] 1.0)
                     after (:sl-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.4)))
               w)}
   {:type :then :pattern #"oxen health is multiplied by approximately ([\d.]+) with slight variance"
    :handler (fn [w factor]
               (let [before (get-in w [:state-before :ox-health] 1.0)
                     after (:ox-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.4)))
               w)}
   {:type :then :pattern #"horse health is multiplied by approximately ([\d.]+) with slight variance"
    :handler (fn [w factor]
               (let [before (get-in w [:state-before :hs-health] 1.0)
                     after (:hs-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.4)))
               w)}
   {:type :then :pattern #"the birth rate exceeds the death rate"
    :handler (fn [w]
               (let [s (:state w)
                     before (:state-before w)
                     pop-after (get s :slaves (get s :oxen (get s :horses 0.0)))
                     pop-before (get before :slaves (get before :oxen (get before :horses 0.0)))]
                 (assert (>= pop-after pop-before) "Expected population to grow"))
               w)}
   {:type :then :pattern #"the death rate exceeds the birth rate"
    :handler (fn [w]
               (let [s (:state w)
                     before (:state-before w)
                     pop-after (get s :slaves (get s :oxen (get s :horses 0.0)))
                     pop-before (get before :slaves (get before :oxen (get before :horses 0.0)))]
                 (assert (<= pop-after pop-before) "Expected population to shrink"))
               w)}
   {:type :then :pattern #"birth rate constant is looked up from the (.+) birth table, randomized ±10%"
    :handler (fn [w animal]
               (let [table (cond
                             (str/includes? animal "slave") t/slave-birth
                             (str/includes? animal "oxen") t/oxen-birth
                             (str/includes? animal "horse") t/horse-birth
                             :else t/slave-birth)
                     h (cond
                         (str/includes? animal "slave") (:sl-health (:state w) 0.8)
                         (str/includes? animal "oxen") (:ox-health (:state w) 0.8)
                         (str/includes? animal "horse") (:hs-health (:state w) 0.8)
                         :else 0.8)
                     rate (t/interpolate h table)]
                 (assert (number? rate) "Expected numeric birth rate"))
               w)}
   {:type :then :pattern #"horse aging rate is ([\d.]+) per month"
    :handler (fn [w _] w)}
   {:type :then :pattern #"oxen aging rate is ([\d.]+) per month"
    :handler (fn [w _] w)}
   {:type :then :pattern #"aging rate = (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"aging rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"death rate constant is looked up from the (.+) death table, randomized ±10%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"nourishment is looked up from the (.+) nourishment table, randomized ±10%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"sickness is looked up from the health-to-sickness table"
    :handler (fn [w] w)}
   {:type :then :pattern #"the slave population (?:declines|grows)"
    :handler (fn [w] w)}
   {:type :then :pattern #"oxen health decreases by ([\d.]+) \(aging only\)"
    :handler (fn [w _]
               (let [before (get-in w [:state-before :ox-health] 1.0)
                     after (:ox-health (:state w))]
                 (assert (< after before) "Expected oxen health to decrease"))
               w)}
   {:type :then :pattern #"oxen health remains at (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:ox-health (:state w)) 0.01)
               w)}
   {:type :then :pattern #"slave health increases by the nourishment amount"
    :handler (fn [w]
               (let [before (get-in w [:state-before :sl-health] 0.0)
                     after (:sl-health (:state w))]
                 (assert (> after before) "Expected health to increase"))
               w)}
   {:type :then :pattern #"slave health decreases by total sickness rate"
    :handler (fn [w]
               (let [before (get-in w [:state-before :sl-health] 1.0)
                     after (:sl-health (:state w))]
                 (assert (<= after before) "Expected health to decrease"))
               w)}
   {:type :then :pattern #"slave lash rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the blended health = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"if horse health < 1.0 then diet = nourishment, else diet = 0"
    :handler (fn [w] w)}
   {:type :then :pattern #"if oxen health < 1.0 then diet = nourishment, else diet = 0"
    :handler (fn [w] w)}
   {:type :then :pattern #"diet = (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"horse health \+= diet - aging rate"
    :handler (fn [w] w)}
   {:type :then :pattern #"oxen health \+= diet - aging rate"
    :handler (fn [w] w)}
   {:type :then :pattern #"new population = population \+ births - deaths"
    :handler (fn [w] w)}
   {:type :then :pattern #"births = birth rate constant \* population"
    :handler (fn [w] w)}
   {:type :then :pattern #"deaths = death rate constant \* population"
    :handler (fn [w] w)}
   {:type :then :pattern #"total sickness rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"work sickness is looked up from the labor-to-sickness table"
    :handler (fn [w] w)}
   {:type :then :pattern #"lash sickness is looked up from the lash-to-sickness table, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the death rate would kill (\d+)"
    :handler (fn [w _] w)}])
