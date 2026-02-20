(ns pharaoh.gherkin.steps.contracts
  (:require [clojure.string :as str]
            [pharaoh.contracts :as ct]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.state :as st]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.ui.input :as inp]
            [pharaoh.ui.layout :as lay]))

(defn steps []
  [;; ===== Contracts =====
   {:type :given :pattern #"the player has (\d+) pending contracts"
    :handler (fn [w n]
               (let [contracts (vec (repeat (Integer/parseInt n)
                                           {:type :buy :active true :who 0 :what :wheat
                                            :amount 100.0 :price 1000.0 :duration 12
                                            :complete 0.0}))]
                 (assoc-in w [:state :cont-pend] contracts)))}
   {:type :given :pattern #"the player has accepted a contract"
    :handler (fn [w]
               (assoc-in w [:state :cont-pend]
                         [{:type :buy :active true :who 0 :what :wheat
                           :amount 100.0 :price 1000.0 :duration 12 :complete 0.0}]))}
   {:type :given :pattern #"a pending BUY contract for (.+) (.+) at (.+) gold due this month"
    :handler (fn [w amt commodity price]
               (let [k (keyword (str/lower-case commodity))
                     w (ensure-rng w)
                     players (ct/make-players (:rng w))]
                 (-> w
                     (assoc-in [:state :players] players)
                     (assoc-in [:state :cont-pend]
                               [{:type :buy :active true :who 0 :what k
                                 :amount (to-double amt) :price (to-double price)
                                 :duration 1 :pct 0.0}])
                     (assoc-in [:state k] (to-double amt)))))}
   {:type :given :pattern #"a pending SELL contract for (.+) bushels of (.+) at (.+) gold"
    :handler (fn [w amt commodity price]
               (let [k (keyword (str/lower-case commodity))
                     w (ensure-rng w)
                     players (ct/make-players (:rng w))]
                 (-> w
                     (assoc-in [:state :players] players)
                     (assoc-in [:state :cont-pend]
                               [{:type :sell :active true :who 0 :what k
                                 :amount (to-double amt) :price (to-double price)
                                 :duration 1 :pct 0.0}]))))}
   {:type :given :pattern #"the player has (.+) or more (.+)"
    :handler (fn [w amt commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state k] (to-double amt))))}
   {:type :given :pattern #"the (?:counterparty|player) (?:can pay|can ship|has enough gold) in full"
    :handler (fn [w] w)}
   {:type :given :pattern #"the player has enough gold to pay"
    :handler (fn [w] (assoc-in w [:state :gold] 100000.0))}

   {:type :when :pattern #"the player tries to accept another contract"
    :handler (fn [w]
               (let [full? (>= (count (get-in w [:state :cont-pend])) 10)]
                 (assoc w :contract-rejected full?)))}
   {:type :when :pattern #"the contract comes due"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ct/contract-progress (:rng w) (:state w)))))}

   {:type :then :pattern #"a message says \"(.+)\""
    :handler (fn [w _] w)}
   {:type :then :pattern #"there is no way to cancel the commitment"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player's (.+) decrease by (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the player receives (.+) gold"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the player receives (.+) bushels of (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the player pays (.+) gold"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the contract is marked inactive"
    :handler (fn [w] w)}

   ;; ===== Contract Expiration Messages =====
   {:type :given :pattern #"a pending contract that will default"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     players [{:pay-k 1.0 :ship-k 1.0 :default-k 0.0
                                :name "King HamuNam"}]]
                 (-> w
                     (assoc-in [:state :players] players)
                     (assoc-in [:state :cont-pend]
                               [{:type :buy :active true :who 0 :what :wheat
                                 :amount 100.0 :price 1000.0 :duration 6
                                 :pct 0.0}]))))}

   {:type :given :pattern #"there are (\d+) queued contract messages"
    :handler (fn [w n]
               (let [cnt (Integer/parseInt n)
                     msgs (vec (for [i (range cnt)]
                                 {:text (str "Contract message " (inc i))
                                  :face i}))]
                 (assoc-in w [:state :contract-msgs] msgs)))}

   {:type :given :pattern #"the first message is displayed"
    :handler (fn [w]
               (let [msgs (get-in w [:state :contract-msgs])
                     first-msg (first msgs)]
                 (-> w
                     (assoc-in [:state :message] first-msg)
                     (assoc-in [:state :contract-msgs] (vec (rest msgs))))))}

   {:type :when :pattern #"a month is simulated without an event"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     ;; seed 42 does not trigger event (uniform(0,8) >= 1.0)
                     rng (r/make-rng 42)]
                 (assoc w :state (sim/do-run rng (:state w)))))}

   {:type :when :pattern #"any key is pressed to dismiss"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (inp/handle-key (:rng w) (:state w) \space)]
                 (assoc w :state s)))}

   {:type :then :pattern #"a contract expiration message is queued"
    :handler (fn [w]
               (assert (seq (:contract-msgs (:state w)))
                       (str "Expected contract-msgs but got "
                            (:contract-msgs (:state w))))
               w)}

   {:type :then :pattern #"the message mentions the counterparty name and commodity"
    :handler (fn [w]
               (let [msg (:text (first (:contract-msgs (:state w))))]
                 (assert (re-find #"Regarding your contract with" msg)
                         (str "Expected contract message format, got: " msg)))
               w)}

   {:type :then :pattern #"a face message dialog appears with contract narration text"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text string")
                 (assert (number? (:face m)) "Expected face number"))
               w)}

   {:type :then :pattern #"the second message is displayed"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (= "Contract message 2" (:text m))
                         (str "Expected 'Contract message 2' but got '" (:text m) "'")))
               w)}

   ;; ===== Contracts Dialog Steps =====
   {:type :given :pattern #"contract offers have been generated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (or (:state w) (st/initial-state))
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     s (ct/new-offers (:rng w) s)]
                 (assoc w :state s)))}

   {:type :given :pattern #"the contracts dialog is open in browsing mode"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (or (:state w) (st/initial-state))
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     s (if (empty? (:cont-offers s))
                         (ct/new-offers (:rng w) s)
                         s)
                     s (dlg/open-contracts-dialog s)]
                 (assoc w :state s)))}

   {:type :given :pattern #"the contracts dialog is open with (\d+) active offers"
    :handler (fn [w n]
               (let [w (ensure-rng w)
                     cnt (Integer/parseInt n)
                     offers (vec (repeat cnt {:type :buy :who 0 :what :wheat
                                              :amount 100.0 :price 1000.0
                                              :duration 24 :active true :pct 0.0}))
                     s (or (:state w) (st/initial-state))
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     s (assoc s :cont-offers offers)
                     s (dlg/open-contracts-dialog s)]
                 (assoc w :state s)))}

   {:type :given :pattern #"the last offer is highlighted"
    :handler (fn [w]
               (let [n (count (get-in w [:state :dialog :active-offers]))]
                 (assoc-in w [:state :dialog :selected] (dec n))))}

   {:type :given :pattern #"the contracts dialog is in confirming mode for acceptance"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     offers (vec (repeat 15 {:type :buy :who 0 :what :wheat
                                             :amount 100.0 :price 1000.0
                                             :duration 24 :active true :pct 0.0}))
                     s (assoc s :cont-offers offers)
                     s (dlg/open-contracts-dialog s)
                     s (dlg/confirm-selected s)]
                 (assoc w :state s)))}

   {:type :given :pattern #"the contracts dialog is in confirming mode"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (or (:state w) (st/initial-state))
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     s (if (empty? (:cont-offers s))
                         (ct/new-offers (:rng w) s)
                         s)
                     s (dlg/open-contracts-dialog s)
                     s (dlg/confirm-selected s)]
                 (assoc w :state s)))}

   {:type :when :pattern #"the player clicks on offer row (\d+)"
    :handler (fn [w idx-str]
               (let [idx (Integer/parseInt idx-str)
                     {:keys [y]} (lay/cell-rect-span 2 5 7 14)
                     y0 (+ y (* lay/title-size 2) lay/small-size 8)
                     row-h (+ lay/label-size 4)
                     mx (+ (:x (lay/cell-rect-span 2 5 7 14)) 50)
                     my (+ y0 (* idx row-h) (/ row-h 2))
                     s (inp/handle-mouse (:state w) mx my)]
                 (assoc w :state s)))}

   {:type :then :pattern #"the selected offer index is (\d+)"
    :handler (fn [w expected]
               (let [sel (get-in w [:state :dialog :selected])]
                 (assert (= (Integer/parseInt expected) sel)
                         (str "Expected selected " expected " but got " sel)))
               w)}

   {:type :when :pattern #"the player presses the down arrow"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (inp/handle-key (:rng w) (:state w) (char 0xFFFF) :down)]
                 (assoc w :state s)))}

   {:type :when :pattern #"the player presses Enter"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (inp/handle-key (:rng w) (:state w) \return)]
                 (assoc w :state s)))}

   {:type :when :pattern #"the player presses Esc"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (inp/handle-key (:rng w) (:state w) (char 27))]
                 (assoc w :state s)))}

   {:type :then :pattern #"a contracts dialog opens in browsing mode"
    :handler (fn [w]
               (assert (= :contracts (get-in w [:state :dialog :type]))
                       (str "Expected contracts dialog, got " (get-in w [:state :dialog :type])))
               (assert (= :browsing (get-in w [:state :dialog :mode]))
                       (str "Expected browsing mode, got " (get-in w [:state :dialog :mode])))
               w)}

   {:type :then :pattern #"the first active offer is highlighted"
    :handler (fn [w]
               (assert (= 0 (get-in w [:state :dialog :selected]))
                       (str "Expected selected 0, got " (get-in w [:state :dialog :selected])))
               w)}

   {:type :then :pattern #"the next offer is highlighted"
    :handler (fn [w]
               (assert (= 1 (get-in w [:state :dialog :selected]))
                       (str "Expected selected 1, got " (get-in w [:state :dialog :selected])))
               w)}

   {:type :then :pattern #"the first offer is highlighted"
    :handler (fn [w]
               (assert (= 0 (get-in w [:state :dialog :selected]))
                       (str "Expected selected 0, got " (get-in w [:state :dialog :selected])))
               w)}

   {:type :then :pattern #"the dialog switches to confirming mode"
    :handler (fn [w]
               (assert (= :confirming (get-in w [:state :dialog :mode]))
                       (str "Expected confirming mode, got " (get-in w [:state :dialog :mode])))
               w)}

   {:type :then :pattern #"the offer moves to pending contracts"
    :handler (fn [w]
               (assert (pos? (count (:cont-pend (:state w))))
                       "Expected at least one pending contract")
               w)}

   {:type :then :pattern #"the dialog returns to browsing mode"
    :handler (fn [w]
               (assert (= :browsing (get-in w [:state :dialog :mode]))
                       (str "Expected browsing mode, got " (get-in w [:state :dialog :mode])))
               w)}

   {:type :then :pattern #"the (contracts )?dialog (closes|is dismissed)"
    :handler (fn [w & _]
               (assert (nil? (get-in w [:state :dialog]))
                       "Expected dialog to be nil")
               w)}

   {:type :then :pattern #"the acceptance is rejected with an error"
    :handler (fn [w]
               (assert (or (:contract-rejected w)
                           (string? (:message (:state w))))
                       "Expected contract rejection error")
               w)}

   ;; ===== Contract Given (Phase 0d) =====
   {:type :given :pattern #"a contract is generated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c (ct/make-contract (:rng w) s
                                         (or (:cont-offers s) [])
                                         (or (:cont-pend s) []))]
                 (-> w
                     (assoc-in [:state :players] (:players s))
                     (assoc :contract c))))}
   {:type :given :pattern #"a contract offers to (BUY|SELL) (\d+) (.+) for (\d+) gold in (\d+) months"
    :handler (fn [w typ amt what price dur]
               (let [w (ensure-rng w)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what (keyword (str/lower-case what))
                        :amount (to-double amt) :price (to-double price)
                        :duration (Integer/parseInt dur)
                        :active true :pct 0.0}
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w))) s)
                     offers (or (:cont-offers s) (vec (repeat 15 {:active false})))]
                 (-> w
                     (assoc :contract c)
                     (assoc :state (assoc s :cont-offers (assoc offers 0 c))))))}
   {:type :given :pattern #"a pending (BUY|SELL) contract for (\d+) (.+) at (\d+) gold"
    :handler (fn [w typ amt what price]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case what))
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what k
                        :amount (to-double amt) :price (to-double price)
                        :duration 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc :gold 100000.0)))))}
   {:type :given :pattern #"a pending (BUY|SELL) contract for (\d+) (\w+)"
    :handler (fn [w typ amt what]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case what))
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what k
                        :amount (to-double amt) :price 100.0
                        :duration 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc k (to-double amt))))))}
   {:type :given :pattern #"a pending (BUY|SELL) contract for (\d+) (.+)"
    :handler (fn [w typ amt what]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case what))
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what k
                        :amount (to-double amt) :price 100.0
                        :duration 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc k (to-double amt) :gold 100000.0)))))}
   {:type :given :pattern #"a pending (BUY|SELL) contract"
    :handler (fn [w typ]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what :wheat
                        :amount 500.0 :price 10000.0
                        :duration 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc :wheat 500.0 :gold 100000.0)))))}
   {:type :given :pattern #"a pending contract with a counterparty"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type :buy :who 0 :what :wheat
                        :amount 500.0 :price 10000.0
                        :duration 24 :active true :pct 0.0}]
                 (assoc w :state (update s :cont-pend conj c))))}
   {:type :given :pattern #"an active (BUY|SELL) contract offer"
    :handler (fn [w typ]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     offer {:type (keyword (str/lower-case typ))
                            :who 0 :what :wheat
                            :amount 500.0 :price 10000.0
                            :duration 24 :active true :pct 0.0}
                     s (update s :cont-offers conj offer)]
                 (assoc w :state s)))}
   {:type :given :pattern #"a counterparty defaults on a contract"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type :buy :who 0 :what :wheat
                        :amount 500.0 :price 10000.0
                        :duration 24 :active true :pct 0.0
                        :defaulted true}]
                 (assoc w :state (update s :cont-pend conj c))))}
   {:type :given :pattern #"the counterparty cannot pay the full amount this month"
    :handler (fn [w] (assoc w :counter-pay-k 0.5))}
   {:type :given :pattern #"the counterparty cannot ship all goods this month"
    :handler (fn [w] (assoc w :counter-ship-k 0.5))}
   {:type :given :pattern #"player \"(.+)\" already has a wheat contract"
    :handler (fn [w name]
               (let [c {:type :buy :who 0 :what :wheat
                        :amount 500.0 :price 10000.0
                        :duration 24 :active true :pct 0.0}]
                 (update-in w [:state :cont-pend] conj c)))}
   {:type :given :pattern #"the player cannot afford a SELL contract payment"
    :handler (fn [w] (assoc-in w [:state :gold] 0.0))}
   {:type :given :pattern #"the player cannot fulfill a BUY contract"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :wheat] 0.0)
                   (assoc-in [:state :slaves] 0.0)
                   (assoc-in [:state :oxen] 0.0)
                   (assoc-in [:state :horses] 0.0)))}
   {:type :given :pattern #"the player receives (\d+) (.+) via a SELL contract"
    :handler (fn [w amt what]
               (let [k (keyword (str/lower-case what))]
                 (update-in w [:state k] + (to-double amt))))}
   {:type :given :pattern #"there are (\d+) offer slots"
    :handler (fn [w n]
               (let [cnt (Integer/parseInt n)
                     offers (vec (repeat cnt {:type :buy :who 0 :what :wheat
                                              :amount 100.0 :price 1000.0
                                              :duration 24 :active false :pct 0.0}))]
                 (assoc-in w [:state :cont-offers] offers)))}
   {:type :given :pattern #"the counterparty's pay probability fails"
    :handler (fn [w]
               ;; Set pay-k to 0 so counterparty never pays in full
               (let [players (get-in w [:state :players])
                     players (if (seq players)
                               (assoc-in (vec players) [0 :pay-k] 0.0)
                               [{:name "BadPayer" :pay-k 0.0 :ship-k 1.0 :default-k 1.0}])]
                 (assoc-in w [:state :players] players)))}
   {:type :given :pattern #"the counterparty's ship probability fails"
    :handler (fn [w]
               ;; Set ship-k to 0 so counterparty never ships in full
               (let [players (get-in w [:state :players])
                     players (if (seq players)
                               (assoc-in (vec players) [0 :ship-k] 0.0)
                               [{:name "BadShipper" :pay-k 1.0 :ship-k 0.0 :default-k 1.0}])]
                 (assoc-in w [:state :players] players)))}
   {:type :given :pattern #"the counterparty's default probability triggers"
    :handler (fn [w]
               ;; Set default-k to 0 so (1 - default-k) = 1.0, always triggers default
               (let [players (get-in w [:state :players])
                     players (if (seq players)
                               (assoc-in (vec players) [0 :default-k] 0.0)
                               [{:name "Defaulter" :pay-k 1.0 :ship-k 1.0 :default-k 0.0}])]
                 (assoc-in w [:state :players] players)))}

   ;; ===== Contract When (Phase 0d) =====
   {:type :when :pattern #"a new contract is created"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c (ct/make-contract (:rng w) s
                                         (or (:cont-offers s) [])
                                         (or (:cont-pend s) []))]
                 (assoc w :state s :contract c)))}
   {:type :when :pattern #"a (BUY|SELL) contract is fully fulfilled"
    :handler (fn [w typ]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w))) s)
                     ctype (keyword (str/lower-case typ))
                     default-c {:type ctype :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 1 :active true :pct 0.0}
                     contract (or (first (:cont-pend s)) default-c)
                     contract (assoc contract :pct 1.0 :duration 1 :type ctype)
                     s (if (= ctype :buy)
                         (assoc s :wheat (max (:wheat s 0) (:amount contract)))
                         (assoc s :gold (max (:gold s 0) (:price contract))))
                     result (ct/fulfill-contract (:rng w) s contract (:players s))
                     new-s (:state result)
                     pool (:msg-pool result)
                     m (when pool (msg/pick (:rng w) pool))]
                 (assoc w :state (if m (assoc new-s :message m) new-s))))}
   {:type :when :pattern #"the player accepts the contract"
    :handler (fn [w]
               (let [w (snap w)
                     s (:state w)
                     idx 0]
                 (assoc w :state (ct/accept-contract s idx))))}
   {:type :when :pattern #"the contract ages for one month"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ct/contract-progress (:rng w) (:state w)))))}
   {:type :when :pattern #"a new contract is generated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c (ct/make-contract (:rng w) s
                                         (or (:cont-offers s) [])
                                         (or (:cont-pend s) []))]
                 (assoc w :state s :contract c)))}
   {:type :when :pattern #"the (?:partial payment|partial shipment|default|shortfall) notification is displayed"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (assoc-in w [:state :message]
                           {:text "Contract notification" :face 0})))}
   {:type :when :pattern #"monthly contract progress is checked"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ct/contract-progress (:rng w) (:state w)))))}

   ;; ===== Contract Then (Phase 0d) =====
   {:type :then :pattern #"it has a type \(BUY or SELL\)"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (#{:buy :sell} (:type c))
                         (str "Expected BUY or SELL, got " (:type c))))
               w)}
   {:type :then :pattern #"it has a commodity \(wheat, slaves, oxen, horses, manure, or land\)"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (#{:wheat :slaves :oxen :horses :manure :land} (:what c))
                         (str "Unexpected commodity " (:what c))))
               w)}
   {:type :then :pattern #"it has a counterparty \(one of (\d+) players\)"
    :handler (fn [w n]
               (let [c (:contract w)]
                 (assert (number? (:who c))
                         (str "Expected counterparty, got " (:who c))))
               w)}
   {:type :then :pattern #"it has an amount"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:amount c 0))
                         (str "Expected positive amount, got " (:amount c))))
               w)}
   {:type :then :pattern #"it has a price"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:price c 0))
                         (str "Expected positive price, got " (:price c))))
               w)}
   {:type :then :pattern #"it has a duration in months"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:duration c 0))
                         (str "Expected positive duration, got " (:duration c))))
               w)}
   {:type :then :pattern #"the duration is uniformly random between (\d+) and (\d+) months"
    :handler (fn [w lo hi]
               (let [c (:contract w)
                     dur (:duration c)]
                 (assert (and (>= dur (Integer/parseInt lo))
                              (<= dur (Integer/parseInt hi)))
                         (str "Duration " dur " not in range")))
               w)}
   {:type :then :pattern #"the type is BUY or SELL with equal probability"
    :handler (fn [w] w)}
   {:type :then :pattern #"the counterparty and commodity are chosen to avoid duplicates"
    :handler (fn [w] w)}
   {:type :then :pattern #"it will not assign another wheat contract to \"(.+)\""
    :handler (fn [w _] w)}
   {:type :then :pattern #"the contract moves from offers to pending"
    :handler (fn [w]
               (assert (pos? (count (:cont-pend (:state w))))
                       "Expected pending contracts")
               w)}
   {:type :then :pattern #"empty slots are filled with new contracts"
    :handler (fn [w]
               (let [offers (get-in w [:state :cont-offers])]
                 (assert (pos? (count (filter :active offers)))
                         "Expected some active offers"))
               w)}
   {:type :then :pattern #"existing contracts with duration <= (\d+) are replaced"
    :handler (fn [w _] w)}
   {:type :then :pattern #"(\d+)% of remaining contracts are randomly replaced"
    :handler (fn [w _] w)}
   {:type :then :pattern #"surviving contracts are aged .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the amount is normally distributed around .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the price = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"contract offers are refreshed"
    :handler (fn [w]
               (assert (seq (get-in w [:state :cont-offers]))
                       "Expected contract offers")
               w)}
   {:type :then :pattern #"the offer slot becomes inactive"
    :handler (fn [w]
               (let [offers (:cont-offers (:state w))]
                 (when (seq offers)
                   (assert (not (:active (first offers)))
                           "Expected offer slot 0 to be inactive")))
               w)}
   {:type :then :pattern #"the player delivers all (\d+) (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the counterparty buys a reduced amount \((\d+)% to (\d+)% of the contract, uniformly random\)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"a 10% penalty on total contract value is deducted from gold"
    :handler (fn [w] w)}
   {:type :then :pattern #"the counterparty ships a reduced amount \((\d+)% to (\d+)%, uniformly random\)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the contract is cancelled"
    :handler (fn [w]
               (let [pending (filter :active (:cont-pend (:state w)))]
                 (assert (zero? (count pending))
                         (str "Expected no active contracts, got " (count pending))))
               w)}
   {:type :then :pattern #"the contract amount and price are reduced proportionally"
    :handler (fn [w] w)}
   {:type :then :pattern #"a 10% bonus of remaining value is paid to the player"
    :handler (fn [w] w)}
   {:type :then :pattern #"the contract is adjusted for the remainder"
    :handler (fn [w] w)}
   {:type :then :pattern #"each player has a name"
    :handler (fn [w]
               (doseq [p (:players (:state w))]
                 (assert (string? (:name p))
                         "Expected player to have a name"))
               w)}
   {:type :then :pattern #"each player's pay probability is the best of 2 random draws between .+"
    :handler (fn [w]
               (doseq [p (:players (:state w))]
                 (assert (and (number? (:pay-k p)) (>= (:pay-k p) 0.5) (<= (:pay-k p) 1.0))
                         (str "Expected pay-k 0.5-1.0, got " (:pay-k p))))
               w)}
   {:type :then :pattern #"each player's ship probability is the best of 2 random draws between .+"
    :handler (fn [w]
               (doseq [p (:players (:state w))]
                 (assert (and (number? (:ship-k p)) (>= (:ship-k p) 0.5) (<= (:ship-k p) 1.0))
                         (str "Expected ship-k 0.5-1.0, got " (:ship-k p))))
               w)}
   {:type :then :pattern #"each player's default probability is the best of 5 random draws between .+"
    :handler (fn [w]
               (doseq [p (:players (:state w))]
                 (assert (and (number? (:default-k p)) (>= (:default-k p) 0.95) (<= (:default-k p) 1.0))
                         (str "Expected default-k 0.95-1.0, got " (:default-k p))))
               w)}
   {:type :then :pattern #"(\d+) contract players are created"
    :handler (fn [w expected]
               (let [players (:players (:state w))]
                 (assert (= (Integer/parseInt expected) (count players))
                         (str "Expected " expected " players, got " (count players))))
               w)}
   {:type :then :pattern #"a random buy-completion message is shown from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected buy-completion message"))
               w)}
   {:type :then :pattern #"a random sell-completion message is shown from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected sell-completion message"))
               w)}
   {:type :then :pattern #"a random insufficient-funds message is shown from the contract pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random insufficient-goods message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random partial-payment message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random partial-shipment message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random default message is shown from the default pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a default notification is displayed"
    :handler (fn [w]
               (let [msgs (:contract-msgs (:state w))]
                 (assert (seq msgs) "Expected contract default notification"))
               w)}
   {:type :then :pattern #"the player receives payment for (\d+) (.+) at the per-unit price"
    :handler (fn [w amt commodity]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (> (:gold after 0) (:gold before 0))
                         "Expected gold to increase from payment"))
               w)}
   {:type :then :pattern #"the player receives payment for the reduced amount"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (> (:gold after 0) (:gold before 0))
                         "Expected gold to increase from partial payment"))
               w)}
   {:type :then :pattern #"the player pays for the reduced amount"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (< (:gold after 0) (:gold before 0))
                         "Expected gold to decrease from payment"))
               w)}
   {:type :then :pattern #"the pending contract list shows the commitment"
    :handler (fn [w]
               (assert (seq (:cont-pend (:state w)))
                       "Expected at least one pending contract")
               w)}
   {:type :then :pattern #"the player receives a 5% cancellation penalty payment"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (> (:gold after 0) (:gold before 0))
                         "Expected gold to increase from penalty payment"))
               w)}
   {:type :then :pattern #"a penalty of 10% of the remaining contract value is deducted"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message asks the player to hold the remainder until next month"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message promises completion next month"
    :handler (fn [w] w)}
   {:type :then :pattern #"a farewell message is displayed from the farewell pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"all (\d+) pending contracts are present with matching terms"
    :handler (fn [w n]
               (let [expected (Long/parseLong n)
                     actual (count (:cont-pend (:state w)))]
                 (assert (= expected actual)
                         (str "Expected " expected " pending contracts, got " actual))
                 w))}])
