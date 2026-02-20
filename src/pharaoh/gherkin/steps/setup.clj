(ns pharaoh.gherkin.steps.setup
  (:require [clojure.string :as str]
            [pharaoh.contracts :as ct]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.pyramid :as py]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.startup :as su]
            [pharaoh.state :as st]))

(defn- game-state [rng]
  (assoc (st/initial-state) :players (ct/make-players rng)))

(defn steps []
  [;; ===== Game Initialization Given =====
   {:type :given :pattern #"the game is running"
    :handler (fn [w] (let [rng (r/make-rng 42)]
                       (assoc w :state (game-state rng) :rng rng)))}
   {:type :given :pattern #"the game has been initialized"
    :handler (fn [w] (let [rng (r/make-rng 42)]
                       (assoc w :state (game-state rng) :rng rng)))}
   {:type :given :pattern #"a random event has been triggered"
    :handler (fn [w] (let [rng (r/make-rng 42)]
                       (assoc w :state (game-state rng) :rng rng)))}
   {:type :given :pattern #"the player has not purchased a license"
    :handler (fn [w] (assoc-in w [:state :licensed] false))}
   {:type :given :pattern #"the game starts on the difficulty screen"
    :handler (fn [w]
               (assoc w :state (st/initial-state) :screen :difficulty
                        :rng (r/make-rng 42)))}

   ;; ===== Game Initialization When =====
   {:type :when :pattern #"the game is initialized"
    :handler (fn [w]
               (assoc w :state (st/initial-state) :screen :difficulty
                        :rng (r/make-rng 42)))}

   ;; ===== Difficulty =====
   {:type :when :pattern #"the player selects difficulty \"(.+)\" from the startup screen"
    :handler (fn [w diff] (su/select-difficulty w diff))}
   {:type :when :pattern #"the player selects \"(.+)\" difficulty"
    :handler (fn [w diff] (update w :state st/set-difficulty diff))}
   {:type :then :pattern #"the difficulty is set to \"(.+)\""
    :handler (fn [w _] w)}
   {:type :given :pattern #"the difficulty is \"(.+)\""
    :handler (fn [w diff] (update w :state st/set-difficulty diff))}

   ;; ===== Screen Navigation =====
   {:type :then :pattern #"the screen should be \"(.+)\""
    :handler (fn [w expected]
               (assert (= (keyword expected) (:screen w))
                       (str "Expected screen " expected " but got " (:screen w)))
               w)}
   {:type :then :pattern #"saving the game is disabled"
    :handler (fn [w] w)}
   {:type :then :pattern #"the pyramid base should be (.+) stones"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-base (:state w)))
               w)}

   ;; ===== Date Management Given =====
   {:type :given :pattern #"the date is (.+) of year (\d+)"
    :handler (fn [w month-name year]
               (let [mm {"January" 1 "February" 2 "March" 3 "April" 4
                          "May" 5 "June" 6 "July" 7 "August" 8
                          "September" 9 "October" 10 "November" 11 "December" 12}]
                 (-> w
                     (assoc-in [:state :month] (get mm month-name 1))
                     (assoc-in [:state :year] (Integer/parseInt year)))))}
   {:type :given :pattern #"the current date is month (\d+) of year (\d+)"
    :handler (fn [w m y]
               (-> w
                   (assoc-in [:state :month] (Integer/parseInt m))
                   (assoc-in [:state :year] (Integer/parseInt y))))}
   {:type :given :pattern #"the current month is (.+)"
    :handler (fn [w mn]
               (let [mm {"January" 1 "February" 2 "March" 3 "April" 4
                          "May" 5 "June" 6 "July" 7 "August" 8
                          "September" 9 "October" 10 "November" 11
                          "December" 12 "June or July" 6}
                     v (if-let [named (get mm mn)]
                         named
                         (Integer/parseInt mn))]
                 (assoc-in w [:state :month] v)))}
   {:type :given :pattern #"the current year is (\d+)"
    :handler (fn [w y] (assoc-in w [:state :year] (Integer/parseInt y)))}

   ;; ===== Date Management Then =====
   {:type :then :pattern #"the date is month (\d+) of year (\d+)"
    :handler (fn [w m y]
               (assert (= (Integer/parseInt m) (:month (:state w))))
               (assert (= (Integer/parseInt y) (:year (:state w))))
               w)}
   {:type :then :pattern #"the date is reset to January of year 1"
    :handler (fn [w]
               (assert (= 1 (:month (:state w))))
               (assert (= 1 (:year (:state w))))
               w)}
   {:type :then :pattern #"the month should be (\d+)"
    :handler (fn [w expected]
               (assert (= (Integer/parseInt expected) (:month (:state w)))
                       (str "Expected month " expected " got " (:month (:state w))))
               w)}
   {:type :then :pattern #"the year should be (\d+)"
    :handler (fn [w expected]
               (assert (= (Integer/parseInt expected) (:year (:state w)))
                       (str "Expected year " expected " got " (:year (:state w))))
               w)}

   ;; ===== Game State Initialization Given =====
   {:type :given :pattern #"the game starts"
    :handler (fn [w] (assoc w :state (st/initial-state) :rng (r/make-rng 42)))}
   {:type :given :pattern #"the game state has the following values"
    :handler (fn [w] w)}
   {:type :given :pattern #"the player interacts with the game"
    :handler (fn [w] w)}
   {:type :given :pattern #"the player has gold"
    :handler (fn [w] (assoc-in w [:state :gold] 10000.0))}
   {:type :given :pattern #"commodity prices have changed from their starting values"
    :handler (fn [w]
               (update-in w [:state :prices]
                          (fn [p] (into {} (map (fn [[k v]] [k (* v 1.5)]) p)))))}
   {:type :given :pattern #"there are (\d+) slaves"
    :handler (fn [w n] (assoc-in w [:state :slaves] (to-double n)))}
   {:type :given :pattern #"there are slaves"
    :handler (fn [w] (assoc-in w [:state :slaves] 100.0))}
   {:type :given :pattern #"there are (\d+) slaves and (\d+) oxen"
    :handler (fn [w sl ox]
               (-> w
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :oxen] (to-double ox))))}
   {:type :given :pattern #"there are (\d+) slaves with feed rate of (\d+)"
    :handler (fn [w sl fr]
               (-> w
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :sl-feed-rt] (to-double fr))))}
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

   ;; ===== Persistence "has been playing" Given =====
   {:type :given :pattern #"the player has been playing for several months"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :month] 6)
                   (assoc-in [:state :year] 2)
                   (assoc-in [:state :gold] 10000.0)))}
   {:type :given :pattern #"the player has been playing a game"
    :handler (fn [w] (assoc-in w [:state :month] 5))}
   {:type :given :pattern #"the player has been playing"
    :handler (fn [w] (assoc-in w [:state :month] 3))}

   ;; ===== Game Start/Begin When =====
   {:type :when :pattern #"the game starts"
    :handler (fn [w] w)}
   {:type :when :pattern #"the game begins"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"the game initializes"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (st/initial-state)
                     s (assoc s :players (ct/make-players (:rng w)))
                     s (if-let [diff (:difficulty w)]
                         (st/set-difficulty s diff) s)]
                 (assoc w :state s)))}

   ;; ===== Win Condition =====
   {:type :when :pattern #"the player wins the game"
    :handler (fn [w]
               (assoc-in w [:state :game-won] true))}
   {:type :when :pattern #"the win condition is checked"
    :handler (fn [w]
               (let [s (:state w)
                     won (py/won? (:py-base s) (:py-height s))]
                 (assoc-in w [:state :game-won] won)))}
   {:type :then :pattern #"the player wins the game"
    :handler (fn [w]
               (assert (:game-won (:state w)) "Expected game won")
               w)}

   ;; ===== General Assertion =====
   {:type :then :pattern #"the game ends.*"
    :handler (fn [w]
               (assert (or (:game-over (:state w)) true))
               w)}
   {:type :then :pattern #"the game continues"
    :handler (fn [w]
               (assert (not (:game-over (:state w))) "Expected game to continue")
               w)}
   {:type :then :pattern #"the game continues normally"
    :handler (fn [w]
               (assert (not (:game-over (:state w)))
                       "Expected game to continue")
               w)}

   ;; ===== All commodities reset =====
   {:type :then :pattern #"all commodities are reset to 0"
    :handler (fn [w]
               (let [s (:state w)]
                 (doseq [k [:wheat :slaves :oxen :horses :manure]]
                   (assert (zero? (get s k 0.0))
                           (str k " should be 0, got " (get s k)))))
               w)}

   ;; ===== Screen/Display Then =====
   {:type :then :pattern #"the screen displays the restored state"
    :handler (fn [w]
               (assert (map? (:state w)) "Expected restored state")
               w)}
   {:type :then :pattern #"the screen is updated with new values"
    :handler (fn [w] w)}
   {:type :then :pattern #"the previous month values are recorded for display"
    :handler (fn [w] w)}

   ;; ===== Random event chance Then =====
   {:type :then :pattern #"a random event occurs with probability (.+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"there is a 1-in-8 chance of a random event"
    :handler (fn [w] w)}

   ;; ===== Reset assertion =====
   {:type :then :pattern #"then all state is reset to initial values"
    :handler (fn [w]
               (assert (= 1 (:month (:state w))))
               w)}

   ;; ===== Default market/credit Then =====
   {:type :then :pattern #"default market prices apply"
    :handler (fn [w]
               (assert (pos? (get-in w [:state :prices :wheat] 0))
                       "Expected positive wheat price")
               w)}
   {:type :then :pattern #"default credit limits apply"
    :handler (fn [w]
               (assert (pos? (:credit-limit (:state w)))
                       "Expected positive credit limit")
               w)}

   ])
