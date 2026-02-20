(ns pharaoh.gherkin.steps.input-validation
  (:require [clojure.string :as str]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.messages :as msg]
            [pharaoh.trading :as tr]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.ui.input :as inp]))

(defn steps []
  [;; ===== Input Validation Given =====
   {:type :given :pattern #"a buy-sell dialog is open for (.+)"
    :handler (fn [w commodity]
               (let [k (keyword (clojure.string/lower-case commodity))]
                 (update w :state dlg/open-dialog :buy-sell {:commodity k})))}
   {:type :given :pattern #"a loan dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :loan))}
   {:type :given :pattern #"an overseer dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :overseer))}
   {:type :given :pattern #"a planting dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :plant))}
   {:type :given :pattern #"a pyramid dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :pyramid))}
   {:type :given :pattern #"a manure spreading dialog is open"
    :handler (fn [w] (update w :state dlg/open-dialog :spread))}
   {:type :given :pattern #"the dialog input contains \"(.+)\""
    :handler (fn [w input]
               (assoc-in w [:state :dialog :input] input))}
   {:type :given :pattern #"the dialog input contains \"\""
    :handler (fn [w] (assoc-in w [:state :dialog :input] ""))}
   {:type :given :pattern #"the dialog mode is set to (.+)"
    :handler (fn [w mode]
               (update w :state dlg/set-dialog-mode (keyword mode)))}
   {:type :given :pattern #"the player has (\d+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state k] (to-double amt))))}
   {:type :given :pattern #"the (.+) demand is (\d+)"
    :handler (fn [w commodity amt]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state :demand k] (to-double amt))))}
   {:type :given :pattern #"the (.+) supply is (\d+)"
    :handler (fn [w commodity amt]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state :supply k] (to-double amt))))}
   {:type :given :pattern #"there are input error pools for each dialog category"
    :handler (fn [w] w)}

   ;; ===== Input Validation When =====
   {:type :when :pattern #"the player types \"(.+)\""
    :handler (fn [w text]
               (reduce (fn [w ch] (update w :state dlg/update-dialog-input ch))
                       w (seq text)))}
   {:type :when :pattern #"the player presses backspace"
    :handler (fn [w] (update w :state dlg/update-dialog-input \backspace))}
   {:type :when :pattern #"the player presses enter without selecting (?:buy or sell|borrow or repay|hire or fire)"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (update w :state #(dlg/execute-dialog (:rng w) %))))}
   {:type :when :pattern #"the player presses enter"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (update w :state #(dlg/execute-dialog (:rng w) %))))}
   {:type :when :pattern #"the player presses escape"
    :handler (fn [w] (update w :state dlg/close-dialog))}
   {:type :when :pattern #"the player presses '([a-z])'"
    :handler (fn [w ch]
               (let [w (ensure-rng w)
                     c (first ch)
                     s (inp/handle-key (:rng w) (:state w) c)]
                 (if (string? (:message s))
                   (assoc w :state s :contract-rejected true)
                   (assoc w :state s))))}
   {:type :when :pattern #"the player enters non-numeric text in the (.+) dialog"
    :handler (fn [w dialog-name]
               (let [dialog-map {"buy/sell" :buy-sell "loan" :loan "planting" :plant
                                 "pyramid quota" :pyramid "manure spread" :spread
                                 "overseer" :overseer "feed rate" :feed}
                     dtype (get dialog-map dialog-name :buy-sell)
                     s (-> (:state w)
                           (dlg/open-dialog dtype)
                           (assoc-in [:dialog :input] "abc"))
                     w (ensure-rng w)]
                 (assoc w :state (dlg/execute-dialog (:rng w) s))))}
   {:type :when :pattern #"the player enters a negative number for acres to plant"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :planting-negative))]
                 (assoc-in w [:state :message] m)))}
   {:type :when :pattern #"the player enters a negative number for manure to spread"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :manure-negative))]
                 (assoc-in w [:state :message] m)))}
   {:type :when :pattern #"the player enters a negative number for pyramid quota"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :pyramid-negative))]
                 (assoc-in w [:state :message] m)))}

   ;; ===== Input Validation Then =====
   {:type :then :pattern #"the dialog input contains \"(.+)\""
    :handler (fn [w expected]
               (assert (= expected (get-in w [:state :dialog :input]))
                       (str "Expected input '" expected "' but got '"
                            (get-in w [:state :dialog :input]) "'"))
               w)}
   {:type :then :pattern #"the dialog input contains \"\""
    :handler (fn [w]
               (assert (= "" (get-in w [:state :dialog :input]))
                       (str "Expected empty input but got '"
                            (get-in w [:state :dialog :input]) "'"))
               w)}
   {:type :then :pattern #"the dialog mode is set to (.+)"
    :handler (fn [w expected]
               (assert (= (keyword expected) (get-in w [:state :dialog :mode]))
                       (str "Expected mode " expected " but got "
                            (get-in w [:state :dialog :mode])))
               w)}
   {:type :then :pattern #"a buy-sell (?:mode |input )?error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"a loan (?:mode |input )?error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"an overseer (?:mode )?error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"a planting input error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"a pyramid input error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"a manure input error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"the dialog is closed"
    :handler (fn [w]
               (assert (nil? (get-in w [:state :dialog]))
                       "Expected dialog to be nil")
               w)}
   {:type :then :pattern #"(?:buy-sell|loan|planting|pyramid|manure|overseer|feed) errors come from the (?:buysell|loan|planting|pyramid|manure|overseer|feed) pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"the input is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected input rejection")
               w)}
   {:type :then :pattern #"a random input-error message is displayed from the (.+) error pool"
    :handler (fn [w _]
               (assert (string? (:message (:state w)))
                       "Expected input error message")
               w)}
   {:type :then :pattern #"a random negative-input message is displayed from the (.+) error pool"
    :handler (fn [w _]
               (assert (string? (:message (:state w)))
                       "Expected negative input error message")
               w)}
   {:type :then :pattern #"the dialog remains open"
    :handler (fn [w]
               (assert (some? (get-in w [:state :dialog]))
                       "Expected dialog to remain open")
               w)}
   {:type :then :pattern #"an insufficient funds error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected insufficient funds error message")
               w)}
   {:type :then :pattern #"the message includes the max affordable amount of (\d+)"
    :handler (fn [w expected]
               (let [m (:message (:state w))]
                 (assert (and (string? m) (str/includes? m (str (long (to-double expected)))))
                         (str "Expected message to include " expected " but got: " m)))
               w)}
   {:type :then :pattern #"a selling-more error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected selling-more error message")
               w)}
   {:type :then :pattern #"a supply-limit error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected supply-limit error message")
               w)}
   {:type :then :pattern #"the player's gold is non-negative"
    :handler (fn [w]
               (assert (>= (:gold (:state w)) 0)
                       (str "Expected non-negative gold, got " (:gold (:state w))))
               w)}
   {:type :then :pattern #"a demand-limit message is displayed"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (string? m)
                         (str "Expected demand-limit message string, got " (pr-str m)))
                 (assert (some #(let [fmt-pat (clojure.string/replace % "%.0f" "\\d+")]
                                  (re-find (re-pattern fmt-pat) m))
                               pharaoh.messages/demand-limit-messages)
                         (str "Message not from demand-limit pool: " m)))
               w)}])
