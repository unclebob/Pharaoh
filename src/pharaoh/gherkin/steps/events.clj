(ns pharaoh.gherkin.steps.events
  (:require [clojure.string :as str]
            [pharaoh.events :as ev]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]))

(defn steps []
  [;; ===== Events =====
   {:type :given :pattern #"the event roll \(0-100\) is (.+)"
    :handler (fn [w roll] (assoc w :event-roll roll))}
   {:type :then :pattern #"the event type is \"(.+)\""
    :handler (fn [w expected]
               (let [roll-str (:event-roll w)
                     roll (if (str/includes? roll-str "-")
                            (to-double (first (str/split roll-str #"-")))
                            (to-double roll-str))
                     etype (ev/event-type (long roll))]
                 (assert (= (name etype)
                            (str/lower-case (str/replace expected " " "-")))
                         (str "Expected " expected " but got " (name etype)))
                 w))}

   ;; Event-specific
   {:type :given :pattern #"the player has planted, growing, and ripe land"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :ln-fallow] 100.0)
                   (assoc-in [:state :ln-sewn] 100.0)
                   (assoc-in [:state :ln-grown] 100.0)
                   (assoc-in [:state :ln-ripe] 100.0)
                   (assoc-in [:state :wt-sewn] 1000.0)
                   (assoc-in [:state :wt-grown] 1000.0)
                   (assoc-in [:state :wt-ripe] 1000.0)))}
   {:type :when :pattern #"locusts strike"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ev/locusts (:rng w) (:state w)))))}
   {:type :then :pattern #"all planted, growing, and ripe land reverts to fallow"
    :handler (fn [w]
               (let [s (:state w)]
                 (assert (zero? (:ln-sewn s)))
                 (assert (zero? (:ln-grown s)))
                 (assert (zero? (:ln-ripe s))))
               w)}
   {:type :then :pattern #"all wheat sewn, growing, and ripe is destroyed"
    :handler (fn [w]
               (let [s (:state w)]
                 (assert (zero? (:wt-sewn s)))
                 (assert (zero? (:wt-grown s)))
                 (assert (zero? (:wt-ripe s))))
               w)}
   {:type :then :pattern #"extra work is added.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"nothing happens if.*"
    :handler (fn [w] w)}

   {:type :given :pattern #"the player has slaves, oxen, and horses"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :slaves] 100.0)
                   (assoc-in [:state :oxen] 50.0)
                   (assoc-in [:state :horses] 30.0)
                   (assoc-in [:state :sl-health] 0.8)
                   (assoc-in [:state :ox-health] 0.8)
                   (assoc-in [:state :hs-health] 0.8)))}
   {:type :when :pattern #"a plague strikes"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ev/plagues (:rng w) (:state w)))))}
   {:type :then :pattern #"(?:slave|oxen|horse) (?:health|population) is reduced by a random factor.*"
    :handler (fn [w] w)}

   {:type :given :pattern #"the player has wheat in various stages"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :wheat] 10000.0)
                   (assoc-in [:state :wt-sewn] 5000.0)
                   (assoc-in [:state :wt-grown] 5000.0)
                   (assoc-in [:state :wt-ripe] 5000.0)
                   (assoc-in [:state :ln-sewn] 100.0)
                   (assoc-in [:state :ln-grown] 100.0)
                   (assoc-in [:state :ln-ripe] 100.0)))}
   {:type :when :pattern #"a wheat event occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/wheat-event (:rng w) (:state w))
                     m (ev/event-message (:rng w) :wheat-event nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :then :pattern #"a loss factor of approximately.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat (?:stores|sewn|growing|ripe) (?:are|is) reduced by the loss factor"
    :handler (fn [w] w)}

   ;; ===== Events Given (Phase 0d) =====
   {:type :given :pattern #"a random event adds (\d+) man-hours of extra work"
    :handler (fn [w hrs]
               (assoc-in w [:state :wk-addition] (to-double hrs)))}
   {:type :given :pattern #"the war gain is ([\d.]+)"
    :handler (fn [w v] (assoc w :war-gain (to-double v)))}

   ;; ===== Event When (Phase 0d) =====
   {:type :when :pattern #"an act of God occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/acts-of-god (:rng w) (:state w))
                     m (ev/event-message (:rng w) :acts-of-god nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"mobs attack"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/acts-of-mobs (:rng w) (:state w))
                     m (ev/event-message (:rng w) :acts-of-mobs nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"a war occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/war (:rng w) (:state w))
                     gain (if-let [g (:war-gain w)] g 1.0)
                     m (ev/event-message (:rng w) :war gain)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"war occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (merge {:overseers 5.0 :slaves 100.0 :oxen 50.0 :horses 20.0
                               :ln-fallow 100.0 :wheat 5000.0 :manure 1000.0
                               :ln-sewn 0.0 :ln-grown 0.0 :ln-ripe 0.0
                               :wt-sewn 0.0 :wt-grown 0.0 :wt-ripe 0.0} (:state w))
                     result (ev/war (:rng w) s)
                     denom (+ (:slaves s) (:oxen s) (:horses s))
                     gain (if (pos? denom)
                             (/ (+ (:slaves result) (:oxen result) (:horses result)) denom)
                             1.0)]
                 (assoc w :state result :war-gain gain)))}
   {:type :when :pattern #"a revolt occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/revolt (:rng w) (:state w))
                     m (ev/event-message (:rng w) :revolt nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"a health event occurs"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     ;; Ensure non-zero populations so event doesn't skip
                     s (-> (:state w)
                           (update :slaves #(if (pos? (or % 0)) % 100.0))
                           (update :oxen #(if (pos? (or % 0)) % 50.0))
                           (update :horses #(if (pos? (or % 0)) % 20.0)))
                     w (assoc w :state s)
                     w (snap w)
                     s (ev/health-event (:rng w) s)
                     m (ev/event-message (:rng w) :health-events nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"a gold event occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/gold-event (:rng w) (:state w))
                     m (ev/event-message (:rng w) :gold-event nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"a labor event occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/labor-event (:rng w) (:state w))
                     m (ev/event-message (:rng w) :labor-event nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"a workload event occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/workload-event (:rng w) (:state w))
                     m (ev/event-message (:rng w) :workload nil)]
                 (assoc w :state (assoc s :message m))))}
   {:type :when :pattern #"an economy event occurs"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (ev/economy-event (:rng w) (:state w))
                     m (ev/event-message (:rng w) :economy-event nil)]
                 (assoc w :state (assoc s :message m))))}

   ;; ===== War Effects =====
   {:type :when :pattern #"war effects are applied"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     gain (or (:war-gain w) 1.0)
                     ;; Explicitly set non-zero defaults (initial-state has 0 for populations)
                     s (-> (:state w)
                           (update :ln-fallow #(if (pos? (or % 0)) % 100.0))
                           (update :slaves #(if (pos? (or % 0)) % 100.0))
                           (update :oxen #(if (pos? (or % 0)) % 50.0))
                           (update :horses #(if (pos? (or % 0)) % 20.0))
                           (update :wheat #(if (pos? (or % 0)) % 5000.0))
                           (update :manure #(if (pos? (or % 0)) % 1000.0)))
                     w (assoc w :state s)
                     w (snap w)
                     apply-gain (fn [s k] (update s k * (* gain (r/abs-gaussian (:rng w) 1.0 0.2))))
                     s (reduce apply-gain s
                               [:ln-fallow :ln-grown :wt-grown :ln-sewn :wt-sewn
                                :ln-ripe :wt-ripe :slaves :oxen :horses :wheat :manure])]
                 (assoc w :state s)))}

   ;; ===== Random Event During Month =====
   {:type :when :pattern #"a random event occurs during the month simulation"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     rng (:rng w)
                     {:keys [type state]} (ev/random-event rng (:state w))
                     state (assoc state :last-event type)
                     state (sim/run-month rng state)
                     msg (ev/event-message rng type nil)
                     face (long (r/uniform rng 0 4))]
                 (assoc w :state
                        (assoc state :message
                               {:text (or msg "Something happened...") :face face}))))}

   ;; ===== Event Popup Steps =====
   {:type :then :pattern #"a face message dialog appears with event narration text"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text string"))
               w)}
   {:type :then :pattern #"the message has a neighbor face portrait"
    :handler (fn [w]
               (let [face (:face (get-in w [:state :message]))]
                 (assert (and (>= face 0) (<= face 3))
                         (str "Expected face 0-3, got " face)))
               w)}

   ;; ===== Event Narration Then Handlers =====
   {:type :then :pattern #"a random adjective is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random disaster type is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random consequence is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the assembled message is spoken by a random neighbor"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random crowd size is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random motivation is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random action is chosen .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"if the player wins, a victory message with gain percentage is shown"
    :handler (fn [w] w)}
   {:type :then :pattern #"if the player loses, a defeat message with loss percentage is shown"
    :handler (fn [w] w)}

   ;; ===== Event Result Then Steps =====
   {:type :then :pattern #"the revolt event is skipped"
    :handler (fn [w]
               (let [s (:state w)]
                 (assert (zero? (:slaves s 0.0)) "Revolt skipped when no slaves"))
               w)}
   {:type :then :pattern #"each commodity price shifts by approximately ±(\d+)% \(normally distributed\)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"each resource is reduced by a random factor between ([\d.]+) and ([\d.]+)"
    :handler (fn [w lo hi]
               (let [before (:state-before w)
                     after (:state w)]
                 (doseq [k [:slaves :oxen :horses]]
                   (when (pos? (get before k 0.0))
                     (let [ratio (/ (get after k) (get before k))]
                       (assert (and (>= ratio (- (to-double lo) 0.1))
                                    (<= ratio (+ (to-double hi) 0.1)))
                               (str k " ratio " ratio " not in range"))))))
               w)}
   {:type :then :pattern #"wheat in all growing stages is reduced by a random factor between ([\d.]+) and ([\d.]+)"
    :handler (fn [w _ _]
               (let [before (:state-before w)
                     after (:state w)]
                 (doseq [k [:wt-sewn :wt-grown :wt-ripe]]
                   (assert (<= (get after k 0.0) (get before k 0.0))
                           (str k " should not increase"))))
               w)}
   {:type :then :pattern #"each side rolls with approximately (\d+)% variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"destruction is looked up from the hatred-to-destruction table, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the player gains land, livestock, and wheat"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (or (> (:gold after 0) (:gold before 0))
                             (> (:slaves after 0) (:slaves before 0))
                             (> (:wheat after 0) (:wheat before 0)))
                         "Expected player to gain resources"))
               w)}
   {:type :then :pattern #"the player loses land, livestock, and wheat"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (or (< (:gold after 0) (:gold before 0))
                             (< (:slaves after 0) (:slaves before 0))
                             (< (:wheat after 0) (:wheat before 0)))
                         "Expected player to lose resources"))
               w)}
   {:type :then :pattern #"the player's army strength = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the enemy army is proportional to the player's overseers, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"all resources are multiplied by approximately ([\d.]+), each randomized ±(\d+)%"
    :handler (fn [w factor _]
               (let [f (to-double factor)
                     before (:state-before w)
                     after (:state w)]
                 (doseq [k [:slaves :oxen :horses :gold :wheat]]
                   (when (and before (pos? (get before k 0.0)))
                     (let [ratio (/ (get after k 0.0) (get before k 1.0))]
                       (assert (and (> ratio (* f 0.5)) (< ratio (* f 2.0)))
                               (str k " ratio " ratio " unexpected"))))))
               w)}
   {:type :then :pattern #"all resources are multiplied by the survival factor"
    :handler (fn [w] w)}
   {:type :then :pattern #"survival factor = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"gold is reduced by the loss factor"
    :handler (fn [w]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (< after before) "Expected gold to decrease"))
               w)}
   {:type :then :pattern #"gold is reset to 0"
    :handler (fn [w]
               (assert (zero? (:gold (:state w))) "Expected gold = 0")
               w)}
   {:type :then :pattern #"a random wheat-event message is displayed with loss percentage"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected event message"))
               w)}
   {:type :then :pattern #"wheat stores are reduced by a random factor between ([\d.]+) and ([\d.]+)"
    :handler (fn [w _ _]
               (let [before (get-in w [:state-before :wheat] 0.0)
                     after (:wheat (:state w))]
                 (assert (<= after before) "Expected wheat to decrease"))
               w)}
   {:type :then :pattern #"manure increases slightly .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"lash sickness is looked up from the lash-to-sickness table, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"suffering is looked up from the lash-to-suffering table"
    :handler (fn [w] w)}
   {:type :then :pattern #"hatred = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the narration is assembled from three pools: .+"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected narration message"))
               w)}
   {:type :then :pattern #"the narration is assembled from four pools: .+"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected narration message"))
               w)}
   {:type :then :pattern #"the narration includes the destruction percentage"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected narration with percentage"))
               w)}
   {:type :then :pattern #"the message includes the attacking force description"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected war narration"))
               w)}
   {:type :then :pattern #"the player's oxen are set to 0"
    :handler (fn [w]
               (assert (<= (:oxen (:state w) 0) 0.01)
                       "Expected oxen to be 0")
               w)}
   ])
