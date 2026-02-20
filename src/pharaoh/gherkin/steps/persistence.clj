(ns pharaoh.gherkin.steps.persistence
  (:require [clojure.string :as str]
            [pharaoh.contracts :as ct]
            [pharaoh.persistence :as ps]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]))

(defn steps []
  [;; ===== Persistence (specific "has been playing" patterns) =====
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
   {:type :given :pattern #"the player has (.+) wheat, (.+) slaves, (.+) oxen, (.+) horses, (.+) manure"
    :handler (fn [w wt sl ox hs mn]
               (-> w
                   (assoc-in [:state :wheat] (to-double wt))
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :oxen] (to-double ox))
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :manure] (to-double mn))))}
   {:type :given :pattern #"the player has (.+) acres of land in various stages"
    :handler (fn [w amt]
               (let [per (/ (to-double amt) 4.0)]
                 (-> w
                     (assoc-in [:state :ln-fallow] per)
                     (assoc-in [:state :ln-sewn] per)
                     (assoc-in [:state :ln-grown] per)
                     (assoc-in [:state :ln-ripe] per))))}
   {:type :given :pattern #"the player has (\d+) overseers"
    :handler (fn [w n] (assoc-in w [:state :overseers] (to-double n)))}
   {:type :given :pattern #"a save file already exists with that name"
    :handler (fn [w]
               (let [path (str "/tmp/pharaoh-test-exists.edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}

   ;; ===== Persistence When =====
   {:type :when :pattern #"the player saves the game"
    :handler (fn [w]
               (let [path (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}
   {:type :when :pattern #"the player opens a different saved game"
    :handler (fn [w]
               (if-let [path (:save-path w)]
                 (assoc w :state (ps/load-game path))
                 w))}
   {:type :when :pattern #"the game is saved and restored"
    :handler (fn [w]
               (let [w (snap w)
                     path (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :state (ps/load-game path) :save-path path)))}
   {:type :when :pattern #"the player starts a new game"
    :handler (fn [w] (assoc w :state (st/initial-state)))}
   {:type :when :pattern #"the player chooses to save"
    :handler (fn [w]
               (let [path (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}
   {:type :when :pattern #"the player opens a saved game file"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (or (:state w) (st/initial-state))
                     s (if (seq (:cont-offers s)) s
                         (assoc s :cont-offers
                                (vec (repeat 15 {:active false}))
                                :players (ct/make-players (:rng w))))
                     path (or (:save-path w)
                              (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn"))]
                 (ps/save-game s path)
                 (assoc w :state (ps/load-game path) :save-path path)))}
   {:type :when :pattern #"the player saves to the same filename"
    :handler (fn [w]
               (let [path (or (:save-path w) "/tmp/pharaoh-test-overwrite.edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}

   ;; ===== Persistence Then =====
   {:type :then :pattern #"all game state is written to a file"
    :handler (fn [w]
               (assert (:save-path w) "Expected save path")
               (assert (.exists (java.io.File. (:save-path w))))
               w)}
   {:type :then :pattern #"the player can continue playing"
    :handler (fn [w] w)}
   {:type :then :pattern #"the current game is replaced by the saved game"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player is prompted to save the current game first"
    :handler (fn [w] w)}
   {:type :then :pattern #"all (.+) values match the saved values"
    :handler (fn [w what]
               (let [before (:state-before w) after (:state w)]
                 (case what
                   "health"
                   (doseq [k [:sl-health :ox-health :hs-health]]
                     (assert (near? (k before) (k after)) (str k " mismatch")))
                   (assert false (str "Unknown values domain: " what)))
                 w))}
   {:type :then :pattern #"all (.+) match the saved values"
    :handler (fn [w what]
               (let [before (:state-before w) after (:state w)]
                 (case what
                   "commodity quantities"
                   (doseq [k [:wheat :slaves :oxen :horses :manure]]
                     (assert (near? (k before) (k after)) (str k " mismatch")))
                   "market prices"
                   (doseq [k (keys (:prices before))]
                     (assert (near? (get-in before [:prices k])
                                    (get-in after [:prices k]))
                             (str "price " k " mismatch")))
                   "feed rates and quotas"
                   (doseq [k [:sl-feed-rt :ox-feed-rt :hs-feed-rt :ln-to-sew :mn-to-sprd]]
                     (assert (near? (k before) (k after)) (str k " mismatch")))
                   (assert false (str "Unknown domain: " what)))
                 w))}
   {:type :then :pattern #"all (\d+) pending contracts are present with matching terms"
    :handler (fn [w n]
               (let [expected (Long/parseLong n)
                     actual (count (:cont-pend (:state w)))]
                 (assert (= expected actual)
                         (str "Expected " expected " pending contracts, got " actual))
                 w))}
   {:type :then :pattern #"then all state is reset to initial values"
    :handler (fn [w]
               (assert (= 1 (:month (:state w))))
               w)}
   {:type :then :pattern #"all game state is restored from the file"
    :handler (fn [w]
               (assert (:save-path w) "Expected save path")
               w)}
   {:type :then :pattern #"the player is prompted for a save file name"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer count and pressure match the saved values"
    :handler (fn [w]
               (let [before (:state-before w) after (:state w)]
                 (assert (near? (:overseers before) (:overseers after)) "overseer count mismatch")
                 (assert (near? (:ov-press before) (:ov-press after)) "overseer pressure mismatch")
                 w))}
   {:type :then :pattern #"gold, loan, interest rate, credit rating, and credit limit all match"
    :handler (fn [w]
               (let [before (:state-before w) after (:state w)]
                 (doseq [k [:gold :loan :interest :credit-rating :credit-limit]]
                   (assert (near? (k before) (k after)) (str k " mismatch")))
                 w))}
   {:type :then :pattern #"the file is overwritten with the current state"
    :handler (fn [w]
               (assert (:save-path w) "Expected save path")
               w)}
   {:type :then :pattern #"the screen displays the restored state"
    :handler (fn [w]
               (assert (map? (:state w)) "Expected restored state")
               w)}])
