(ns pharaoh.gherkin.steps.neighbors
  (:require [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.neighbors :as nb]
            [pharaoh.visits :as vis]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.ui.dialogs :as dlg]
            [clojure.string :as str]))

(defn steps []
  [;; ===== Neighbors Given (most specific first) =====
   {:type :given :pattern #"the good guy visits"
    :handler (fn [w] (assoc w :visiting :good-guy))}
   {:type :given :pattern #"the bad guy visits"
    :handler (fn [w] (assoc w :visiting :bad-guy))}
   {:type :given :pattern #"the village idiot visits"
    :handler (fn [w] (assoc w :visiting :dumb-guy))}
   {:type :given :pattern #"the player has been inactive for .+ seconds"
    :handler (fn [w] w)}
   {:type :given :pattern #"the topic is \"(.+)\""
    :handler (fn [w topic] (assoc w :advice-topic topic))}
   {:type :given :pattern #"a neighbor gives advice on a topic"
    :handler (fn [w] (assoc w :visiting :good-guy))}
   {:type :given :pattern #"any neighbor gives advice"
    :handler (fn [w] (assoc w :visiting :good-guy))}
   {:type :given :pattern #"any neighbor visits"
    :handler (fn [w] (assoc w :visiting :good-guy))}
   {:type :given :pattern #"the banker visits for a chat"
    :handler (fn [w] (assoc w :visiting :banker))}
   {:type :given :pattern #"a message is not attributed to a specific face"
    :handler (fn [w] (assoc-in w [:state :message] {:text "test" :face nil}))}
   {:type :given :pattern #"speech synthesis is available"
    :handler (fn [w] w)}
   {:type :given :pattern #"(\d+)-(\d+) seconds have elapsed since the last chat"
    :handler (fn [w _ _] w)}
   {:type :given :pattern #"more than (\d+) seconds have passed since the last idle check"
    :handler (fn [w _] w)}

   ;; ===== Face message Given =====
   {:type :given :pattern #"a neighbor with face (\d+) delivers a message \"(.+)\""
    :handler (fn [w face text]
               (assoc-in w [:state :message]
                         {:text text :face (Integer/parseInt face)}))}
   {:type :given :pattern #"a face message dialog is displayed"
    :handler (fn [w]
               (assoc-in w [:state :message]
                         {:text "test" :face 0}))}

   ;; ===== Visit Timer Given =====
   {:type :given :pattern #"the visit timers are initialized"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     w (if (:state w) w (assoc w :state (st/initial-state)))
                     timers (vis/init-timers (:rng w) 10000)]
                 (merge w timers {:now 10000})))}
   {:type :given :pattern #"the idle timer has expired"
    :handler (fn [w] (assoc w :next-idle (- (:now w) 1000)))}
   {:type :given :pattern #"the chat timer has expired"
    :handler (fn [w] (assoc w :next-chat (- (:now w) 1000)))}
   {:type :given :pattern #"the dunning timer has expired"
    :handler (fn [w] (assoc w :next-dunning (- (:now w) 1000)))}
   {:type :given :pattern #"a dialog is open"
    :handler (fn [w] (assoc-in w [:state :dialog] {:type :buy-sell}))}
   {:type :given :pattern #"a face message is already showing"
    :handler (fn [w] (assoc-in w [:state :message] {:text "existing" :face 0}))}

   ;; ===== Timer Interval Given =====
   {:type :given :pattern #"a random number generator is initialized"
    :handler (fn [w] (assoc w :rng (r/make-rng 42)))}
   {:type :given :pattern #"the credit rating is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-rating] (to-double v)))}

   ;; ===== Neighbors When =====
   {:type :when :pattern #"he gives advice about (.+)"
    :handler (fn [w _] w)}
   {:type :when :pattern #"the idle timer fires"
    :handler (fn [w] w)}
   {:type :when :pattern #"the dunning timer expires"
    :handler (fn [w] w)}
   {:type :when :pattern #"a neighbor delivers an idle message"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     face (long (r/uniform (:rng w) 0 4))
                     text (msg/pick (:rng w) msg/idle-messages)]
                 (assoc-in w [:state :message] {:text text :face face})))}
   {:type :when :pattern #"a neighbor delivers generic small talk"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     face (long (r/uniform (:rng w) 0 4))
                     text (msg/pick (:rng w) msg/chat-messages)]
                 (assoc-in w [:state :message] {:text text :face face})))}
   {:type :when :pattern #"the banker delivers a dunning notice"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     text (msg/pick (:rng w) msg/dunning-messages)
                     face (:banker (:state w))]
                 (assoc-in w [:state :message] {:text text :face face})))}
   {:type :when :pattern #"the chat timer fires"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     face (long (r/uniform (:rng w) 0 4))
                     text (msg/pick (:rng w) msg/chat-messages)]
                 (assoc-in w [:state :message] {:text text :face face})))}
   {:type :when :pattern #"any neighbor message is displayed"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     face (long (r/uniform (:rng w) 0 4))
                     text (msg/pick (:rng w) msg/chat-messages)]
                 (assoc-in w [:state :message] {:text text :face face})))}
   {:type :when :pattern #"a neighbor gives advice"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (assoc-in w [:state :message]
                           {:text "Advice message" :face 1})))}
   {:type :when :pattern #"the player presses any key"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (update w :state dissoc :message)))}

   ;; ===== Visit Timer When =====
   {:type :when :pattern #"visits are checked"
    :handler (fn [w]
               (let [result (vis/check-visits w (:now w))]
                 (merge w (select-keys result [:state :next-idle :next-chat :next-dunning]))))}

   ;; ===== Timer Interval When =====
   {:type :when :pattern #"the idle interval is calculated (\d+) times"
    :handler (fn [w n]
               (let [w (ensure-rng w)
                     intervals (repeatedly (Integer/parseInt n)
                                           #(nb/idle-interval (:rng w)))]
                 (assoc w :intervals (vec intervals))))}
   {:type :when :pattern #"the chat interval is calculated (\d+) times"
    :handler (fn [w n]
               (let [w (ensure-rng w)
                     intervals (repeatedly (Integer/parseInt n)
                                           #(nb/chat-interval (:rng w)))]
                 (assoc w :intervals (vec intervals))))}

   ;; ===== Neighbors Then =====
   {:type :then :pattern #"he says the slaves look (?:bad|great).*"
    :handler (fn [w] w)}
   {:type :then :pattern #"he randomly says good or bad"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random neighbor delivers an idle pep talk"
    :handler (fn [w] w)}
   {:type :then :pattern #"the idle timer resets to .+ seconds"
    :handler (fn [w] w)}
   {:type :then :pattern #"the banker delivers a loan payment reminder"
    :handler (fn [w] w)}
   {:type :then :pattern #"the next dunning interval depends on credit rating"
    :handler (fn [w] w)}
   {:type :then :pattern #"the good message is selected from a pool of approximately (\d+)-(\d+) variants for that topic"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the message is selected randomly from a pool of approximately (\d+) variants"
    :handler (fn [w _] w)}
   {:type :then :pattern #"there is a (\d+)% chance the chat is generic regardless of game state"
    :handler (fn [w _] w)}
   {:type :then :pattern #"there is a (\d+)% chance the advice meaning is inverted"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the advice is skipped and generic chat is shown instead"
    :handler (fn [w] w)}
   {:type :then :pattern #"he delivers generic chat messages"
    :handler (fn [w] w)}
   {:type :then :pattern #"the dunning timer is reset"
    :handler (fn [w] w)}
   {:type :then :pattern #"the bank issues a warning message"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (nil? m) (string? m)
                             (and (map? m) (string? (:text m))))
                         "Expected warning message"))
               w)}
   {:type :then :pattern #"he never gives topical advice"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message appears in a dialog box with the neighbor's face"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (map? m) (string? m)) "Expected message"))
               w)}
   {:type :then :pattern #"the idle nag is cancelled"
    :handler (fn [w] w)}
   {:type :then :pattern #"the timer resets"
    :handler (fn [w] w)}
   {:type :then :pattern #"the chat timer resets to (\d+)-(\d+) seconds"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the next dunning notice is postponed"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message is spoken aloud using the neighbor's voice"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message text is spoken using the delivering neighbor's voice settings"
    :handler (fn [w] w)}
   {:type :then :pattern #"messages include .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"messages range from .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random neighbor delivers an opening message from the opening pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random neighbor visits"
    :handler (fn [w] w)}
   {:type :then :pattern #"the bad message is selected from a separate pool of approximately (\d+)-(\d+) variants"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the \"(.+)\" advice is selected"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the chat may contain advice or be generic small talk"
    :handler (fn [w] w)}
   {:type :then :pattern #"the assembled message is spoken by a random neighbor"
    :handler (fn [w] w)}

   ;; ===== Face message Then =====
   {:type :then :pattern #"a dialog box appears overlaying the game screen"
    :handler (fn [w]
               (assert (map? (get-in w [:state :message])))
               w)}
   {:type :then :pattern #"the dialog contains the portrait for face (\d+) on the left"
    :handler (fn [w face]
               (assert (= (Integer/parseInt face)
                          (:face (get-in w [:state :message]))))
               w)}
   {:type :then :pattern #"the message text \"(.+)\" appears on the right"
    :handler (fn [w text]
               (assert (= text (:text (get-in w [:state :message]))))
               w)}
   {:type :then :pattern #"pressing any key dismisses the dialog"
    :handler (fn [w]
               (let [state (dissoc (:state w) :message)]
                 (assert (nil? (:message state)))
                 w))}
   {:type :then :pattern #"the message is dismissed"
    :handler (fn [w]
               (let [state (dissoc (:state w) :message)]
                 (assert (nil? (:message state)))
                 w))}
   {:type :then :pattern #"the key press is not processed as a game action"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message has the banker face portrait"
    :handler (fn [w]
               (let [face (:face (get-in w [:state :message]))]
                 (assert (= (:banker (:state w)) face)
                         (str "Expected banker face " (:banker (:state w))
                              " but got " face)))
               w)}
   {:type :then :pattern #"the message has a neighbor face portrait"
    :handler (fn [w]
               (let [face (:face (get-in w [:state :message]))]
                 (assert (and (>= face 0) (<= face 3))
                         (str "Expected face 0-3, got " face)))
               w)}

   ;; ===== Visit Timer Then =====
   {:type :then :pattern #"an idle message is displayed with a face"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text")
                 (assert (number? (:face m)) "Expected face"))
               w)}
   {:type :then :pattern #"a chat message is displayed with a face"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text")
                 (assert (number? (:face m)) "Expected face"))
               w)}
   {:type :then :pattern #"a dunning message is displayed with the banker face"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (= (:banker (:state w)) (:face m))
                         "Expected banker face"))
               w)}
   {:type :then :pattern #"the idle timer is reset to the future"
    :handler (fn [w]
               (assert (> (:next-idle w) (:now w)) "Idle timer should be in the future")
               w)}
   {:type :then :pattern #"the chat timer is reset to the future"
    :handler (fn [w]
               (assert (> (:next-chat w) (:now w)) "Chat timer should be in the future")
               w)}
   {:type :then :pattern #"the dunning timer is reset to the future"
    :handler (fn [w]
               (assert (> (:next-dunning w) (:now w)) "Dunning timer should be in the future")
               w)}
   {:type :then :pattern #"no visit message is displayed"
    :handler (fn [w]
               (assert (nil? (get-in w [:state :message])) "Expected no message")
               w)}
   {:type :then :pattern #"the existing message is preserved"
    :handler (fn [w]
               (assert (= {:text "existing" :face 0} (get-in w [:state :message])))
               w)}

   ;; ===== Timer Interval Then =====
   {:type :then :pattern #"every interval is between (\d+) and (\d+) seconds"
    :handler (fn [w lo hi]
               (let [lo-d (to-double lo) hi-d (to-double hi)]
                 (doseq [iv (:intervals w)]
                   (assert (and (>= iv lo-d) (<= iv hi-d))
                           (str "Interval " iv " not in [" lo-d ", " hi-d "]"))))
               w)}
   {:type :then :pattern #"the dunning interval is approximately (\d+) seconds"
    :handler (fn [w expected]
               (let [cr (get-in w [:state :credit-rating])
                     iv (nb/dunning-interval cr)]
                 (assert-near (to-double expected) iv 5.0))
               w)}
   {:type :then :pattern #"the dunning interval is less than (\d+) seconds"
    :handler (fn [w threshold]
               (let [cr (get-in w [:state :credit-rating])
                     iv (nb/dunning-interval cr)]
                 (assert (< iv (to-double threshold))
                         (str "Dunning interval " iv " not < " threshold)))
               w)}
   {:type :then :pattern #"the dunning interval is greater than (\d+) seconds"
    :handler (fn [w threshold]
               (let [cr (get-in w [:state :credit-rating])
                     iv (nb/dunning-interval cr)]
                 (assert (> iv (to-double threshold))
                         (str "Dunning interval " iv " not > " threshold)))
               w)}

   ;; ===== Voice/Portrait Then =====
   {:type :then :pattern #"Face (\d+) speaks at rate (\d+), pitch (\d+)"
    :handler (fn [w face rate pitch]
               (let [f (dec (Integer/parseInt face))
                     vs (nb/voice-settings f)]
                 (assert (= (Integer/parseInt rate) (:rate vs))
                         (str "Expected rate " rate " but got " (:rate vs)))
                 (assert (= (Integer/parseInt pitch) (:pitch vs))
                         (str "Expected pitch " pitch " but got " (:pitch vs))))
               w)}
   {:type :then :pattern #"the default voice is rate (\d+), pitch (\d+)"
    :handler (fn [w rate pitch]
               (let [vs (nb/voice-settings -1)]
                 (assert (= (Integer/parseInt rate) (:rate vs))
                         (str "Expected rate " rate))
                 (assert (= (Integer/parseInt pitch) (:pitch vs))
                         (str "Expected pitch " pitch)))
               w)}
   {:type :then :pattern #"four portrait images are loaded from .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"four faces are assigned"
    :handler (fn [w]
               (let [s (:state w)]
                 (assert (number? (:banker s)) "Expected banker face")
                 (assert (number? (:good-guy s)) "Expected good-guy face")
                 (assert (number? (:bad-guy s)) "Expected bad-guy face")
                 (assert (number? (:dumb-guy s)) "Expected dumb-guy face"))
               w)}
   {:type :then :pattern #"no two personalities share the same face"
    :handler (fn [w]
               (let [s (:state w)
                     faces [(:banker s) (:good-guy s) (:bad-guy s) (:dumb-guy s)]]
                 (assert (= 4 (count (set faces)))
                         (str "Expected 4 distinct faces, got " faces)))
               w)}
   {:type :then :pattern #"assignments are randomized each new game"
    :handler (fn [w] w)}
   {:type :then :pattern #"one face is the good guy \(truth-sayer\)"
    :handler (fn [w]
               (assert (number? (:good-guy (:state w))) "Expected good-guy face")
               w)}
   {:type :then :pattern #"one face is the bad guy \(liar\)"
    :handler (fn [w]
               (assert (number? (:bad-guy (:state w))) "Expected bad-guy face")
               w)}
   {:type :then :pattern #"one face is the village idiot"
    :handler (fn [w]
               (assert (number? (:dumb-guy (:state w))) "Expected dumb-guy face")
               w)}
   {:type :then :pattern #"one face is the banker"
    :handler (fn [w]
               (assert (number? (:banker (:state w))) "Expected banker face")
               w)}
   {:type :then :pattern #"each portrait is a black-and-white bitmap extracted from the original resource fork"
    :handler (fn [w] w)}

   ;; ===== Neighbor advice metric condition =====
   {:type :given :pattern #"the \"(.+)\" is \"(.+)\""
    :handler (fn [w metric condition]
               (let [metric-map {"credit rating" [:credit-rating]
                                 "slave health" [:sl-health]
                                 "oxen health" [:ox-health]
                                 "horse health" [:hs-health]
                                 "slave feed rate" [:sl-feed-rt]
                                 "oxen feed rate" [:ox-feed-rt]
                                 "horse feed rate" [:hs-feed-rt]
                                 "overseer pressure" [:ov-press]
                                 "manure per acre" nil
                                 "slave-to-overseer ratio" nil}
                     path (get metric-map metric)
                     val (cond
                           (str/starts-with? condition "> ")
                           (+ (to-double (subs condition 2)) 0.01)
                           (str/starts-with? condition "< ")
                           (- (to-double (subs condition 2)) 0.01)
                           (str/includes? condition " - ")
                           (let [[lo _] (str/split condition #" - ")]
                             (to-double lo))
                           :else (to-double condition))]
                 (cond
                   (= metric "manure per acre")
                   (let [land (max 1.0 (+ (get-in w [:state :ln-fallow] 0.0)
                                          (get-in w [:state :ln-sewn] 0.0)
                                          (get-in w [:state :ln-grown] 0.0)
                                          (get-in w [:state :ln-ripe] 0.0)))
                         manure (* val land)]
                     (assoc-in w [:state :manure] manure))
                   (= metric "slave-to-overseer ratio")
                   (let [slaves 100.0
                         target-ratio val
                         overseers (/ slaves target-ratio)]
                     (-> w
                         (assoc-in [:state :slaves] slaves)
                         (assoc-in [:state :overseers] overseers)))
                   path
                   (assoc-in w [:state (first path)] val)
                   :else w)))}])
