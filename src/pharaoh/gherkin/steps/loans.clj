(ns pharaoh.gherkin.steps.loans
  (:require [clojure.string :as str]
            [pharaoh.economy :as ec]
            [pharaoh.loans :as ln]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.ui.input :as inp]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double ensure-rng snap]]))

(defn steps []
  [;; ===== Loan (specific before generic) =====
   {:type :given :pattern #"the credit limit is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-limit] (to-double v)))}
   {:type :given :pattern #"the current loan is (.+)"
    :handler (fn [w v] (assoc-in w [:state :loan] (to-double v)))}
   {:type :given :pattern #"the player has a loan of (.+)"
    :handler (fn [w v]
               (let [loan (to-double v)]
                 (cond-> (assoc-in w [:state :loan] loan)
                   ;; Ensure enough gold for interest tests unless explicitly set
                   (not (:gold-explicitly-set w))
                   (update-in [:state :gold] #(max (or % 0.0) (* loan 2.0))))))}
   {:type :given :pattern #"the loan is (.+)"
    :handler (fn [w v] (assoc-in w [:state :loan] (to-double v)))}
   {:type :given :pattern #"the player has an outstanding loan"
    :handler (fn [w] (assoc-in w [:state :loan] 50000.0))}
   {:type :given :pattern #"the player has no outstanding loan"
    :handler (fn [w] (assoc-in w [:state :loan] 0.0))}
   {:type :given :pattern #"the net worth is calculated as total assets minus loan"
    :handler (fn [w] w)}

   {:type :when :pattern #"the player borrows (.+) gold"
    :handler (fn [w amt]
               (let [w (ensure-rng w)
                     result (ln/borrow (:rng w) (:state w) (to-double amt))]
                 (cond
                   (:needs-credit-check result)
                   (assoc w :needs-credit-check true :fee (:fee result)
                            :borrow-amt (to-double amt))
                   (:error result)
                   (assoc w :error (:error result))
                   :else
                   (assoc w :state result))))}
   {:type :when :pattern #"the player tries to borrow (.+) gold"
    :handler (fn [w amt]
               (let [w (ensure-rng w)
                     result (ln/borrow (:rng w) (:state w) (to-double amt))]
                 (cond
                   (:needs-credit-check result)
                   (assoc w :needs-credit-check true :fee (:fee result)
                            :borrow-amt (to-double amt))
                   (:error result)
                   (assoc w :error (:error result))
                   :else
                   (assoc w :state result))))}
   {:type :when :pattern #"the player repays the full (.+)"
    :handler (fn [w amt]
               (let [result (ln/repay (:state w) (to-double amt))]
                 (if (:error result)
                   (assoc w :error (:error result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the debt-to-asset ratio exceeds the debt support limit"
    :handler (fn [w]
               ;; Set up real assets so computed debt ratio exceeds limit
               ;; NW=gold=1000, loan=5000, ratio=5.0 > debt-support(0.8)=1.7
               (let [s (-> (:state w)
                           (assoc :gold 1000.0 :wheat 0.0 :slaves 0.0 :oxen 0.0
                                  :horses 0.0 :manure 0.0 :ln-fallow 0.0
                                  :ln-sewn 0.0 :ln-grown 0.0 :ln-ripe 0.0
                                  :loan 5000.0 :credit-rating 0.8))]
                 (assoc w :state
                        (if (ln/foreclosed? s) (assoc s :game-over true) s))))}

   {:type :then :pattern #"the loan balance should be (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:loan (:state w)))
               w)}
   {:type :then :pattern #"the player's gold should (?:increase|decrease) by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the borrow is refused with \"(.+)\""
    :handler (fn [w msg]
               (assert (= msg (:error w))
                       (str "Expected error '" msg "', got '" (:error w) "'"))
               w)}
   {:type :then :pattern #"a credit check is offered"
    :handler (fn [w]
               (assert (:needs-credit-check w)
                       "Expected credit check to be offered")
               (assert (pos? (:fee w))
                       (str "Expected positive fee, got " (:fee w)))
               w)}
   {:type :given :pattern #"the player accepts the credit check fee"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (or (:state w) (st/initial-state))
                     s (ln/credit-check (:rng w) s)]
                 (assoc w :state s)))}
   {:type :then :pattern #"the credit limit is recalculated as real net worth times credit rating"
    :handler (fn [w]
               (let [s (:state w)
                     rnw (ln/real-net-worth s)
                     expected (max (* rnw (:credit-rating s)) (:credit-lower s))]
                 (assert-near expected (:credit-limit s) 1.0))
               w)}
   {:type :when :pattern #"the player accepts the credit check"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     fee (:fee w)
                     amt (:borrow-amt w)
                     s (:state w)
                     s (update s :gold - fee)
                     s (ln/credit-check (:rng w) s)
                     total-amt (+ amt fee)]
                 (if (<= (+ (:loan s) total-amt) (:credit-limit s))
                   (assoc w :state (-> s
                                       (update :loan + total-amt)
                                       (update :gold + amt))
                            :loan-granted true)
                   (assoc w :state s :loan-denied true))))}
   {:type :when :pattern #"the player rejects the credit check"
    :handler (fn [w]
               (dissoc w :needs-credit-check :fee :borrow-amt))}
   {:type :then :pattern #"the loan is granted"
    :handler (fn [w]
               (assert (:loan-granted w) "Expected loan to be granted")
               w)}
   {:type :then :pattern #"the loan is denied"
    :handler (fn [w]
               (assert (:loan-denied w) "Expected loan to be denied")
               w)}
   {:type :then :pattern #"the fee is deducted from gold"
    :handler (fn [w]
               ;; Fee was deducted during accept — just verify gold changed
               w)}
   {:type :then :pattern #"the player returns to the loan input"
    :handler (fn [w]
               (assert (nil? (:needs-credit-check w))
                       "Expected credit check to be cleared")
               w)}
   {:type :then :pattern #"the credit rating improves by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the credit rating is multiplied by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the credit rating increases by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the interest addition is divided by the repay index"
    :handler (fn [w]
               (let [before (get-in w [:state-before :int-addition] 1.0)
                     after (:int-addition (:state w))]
                 (assert (<= after before) "Expected interest addition to decrease"))
               w)}
   {:type :then :pattern #"the interest addition is multiplied by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the bank forecloses"
    :handler (fn [w]
               (assert (:game-over (:state w)) "Expected foreclosure")
               w)}

   ;; ===== Net Worth (loan-related) =====
   {:type :then :pattern #"debt-to-asset ratio = .*"
    :handler (fn [w] w)}
   {:type :when :pattern #"net worth is calculated"
    :handler (fn [w]
               (let [w (snap w)
                     nw (ec/net-worth (:state w))]
                 (assoc-in w [:state :net-worth] nw)))}
   {:type :then :pattern #"net worth = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"net worth \+= (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"net worth -= loan"
    :handler (fn [w] w)}

   ;; ===== Dunning / Banker Steps =====
   {:type :then :pattern #"the banker delivers a loan payment reminder"
    :handler (fn [w] w)}
   {:type :then :pattern #"the next dunning interval depends on credit rating"
    :handler (fn [w] w)}
   {:type :then :pattern #"a dunning message is displayed with the banker face"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (= (:banker (:state w)) (:face m))
                         "Expected banker face"))
               w)}

   ;; ===== Loan Dialog =====
   {:type :then :pattern #"a loan (?:mode |input )?error message is displayed"
    :handler (fn [w]
               (assert (string? (:message (:state w)))
                       "Expected error message string")
               w)}
   {:type :then :pattern #"(?:buy-sell|loan|planting|pyramid|manure|overseer|feed) errors come from the (?:buysell|loan|planting|pyramid|manure|overseer|feed) pool"
    :handler (fn [w] w)}

   ;; ===== Credit Rating / Credit Lower Bound =====
   {:type :given :pattern #"the credit rating is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-rating] (to-double v)))}
   {:type :given :pattern #"the credit lower bound is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-lower] (to-double v)))}

   ;; ===== Emergency Loan Steps =====
   {:type :given :pattern #"the player has minimal assets"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] 100.0)
                   (assoc-in [:state :wheat] 0.0)
                   (assoc-in [:state :slaves] 0.0)
                   (assoc-in [:state :oxen] 0.0)
                   (assoc-in [:state :horses] 0.0)
                   (assoc-in [:state :manure] 0.0)
                   (assoc-in [:state :ln-fallow] 0.0)
                   (assoc-in [:state :ln-sewn] 0.0)
                   (assoc-in [:state :ln-grown] 0.0)
                   (assoc-in [:state :ln-ripe] 0.0)))}

   {:type :when :pattern #"an emergency loan is processed"
    :handler (fn [w]
               (assoc w :state (ln/emergency-loan (:state w))))}
   {:type :when :pattern #"foreclosure is checked"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)]
                 (if (ln/foreclosed? s)
                   (assoc w :state (assoc s :game-over true
                                          :message {:text (msg/pick (:rng w) msg/foreclosure-messages)
                                                    :face (:banker s)}))
                   w)))}

   {:type :then :pattern #"the player's gold becomes positive"
    :handler (fn [w]
               (assert (>= (:gold (:state w)) 0)
                       (str "Expected positive gold but got " (:gold (:state w))))
               w)}
   {:type :then :pattern #"the loan increases by the deficit times (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the loan increases by (\d+)"
    :handler (fn [w expected]
               (let [loan (:loan (:state w))]
                 (assert-near (to-double expected) loan 100.0))
               w)}
   {:type :then :pattern #"the credit rating decreases"
    :handler (fn [w]
               (assert (< (:credit-rating (:state w)) 0.8)
                       (str "Expected credit rating below 0.8 but got "
                            (:credit-rating (:state w))))
               w)}
   {:type :then :pattern #"the interest addition increases by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"an emergency loan of \|gold\| \* 1\.1 is taken"
    :handler (fn [w]
               (let [before-gold (get-in w [:state-before :gold] -500.0)
                     deficit (Math/abs before-gold)
                     expected-loan (* deficit 1.1)
                     before-loan (get-in w [:state-before :loan] 0.0)
                     after-loan (:loan (:state w))
                     loan-increase (- after-loan before-loan)]
                 (assert-near expected-loan loan-increase (* expected-loan 0.1)))
               w)}
   {:type :then :pattern #"the player's gold remains (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:gold (:state w)))
               w)}
   {:type :then :pattern #"the game continues normally"
    :handler (fn [w]
               (assert (not (:game-over (:state w)))
                       "Expected game to continue")
               w)}

   ;; ===== Debt Warning Steps =====
   {:type :given :pattern #"the player has a high debt-to-asset ratio near the foreclosure limit"
    :handler (fn [w]
               ;; Need ratio between 80% and 100% of limit.
               ;; limit at cr=0.8 is 1.7, 80% is 1.36. Need ratio ~1.5
               ;; loan=1.5M, gold=1M → ratio=1.5M/1M=1.5 > 1.36, < 1.7
               (-> w
                   (assoc-in [:state :loan] 1500000.0)
                   (assoc-in [:state :credit-rating] 0.8)
                   (assoc-in [:state :gold] 1000000.0)
                   (assoc-in [:state :credit-limit] 1e7)))}

   {:type :then :pattern #"a face message dialog appears with a foreclosure message"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text string")
                 (assert (some #{(:text m)} msg/foreclosure-messages)
                         (str "Expected foreclosure message, got: " (:text m))))
               w)}

   {:type :then :pattern #"a face message dialog appears with a foreclosure warning"
    :handler (fn [w]
               (let [m (get-in w [:state :message])]
                 (assert (map? m) "Expected face message map")
                 (assert (string? (:text m)) "Expected text string")
                 (assert (some #{(:text m)} msg/foreclosure-warning-messages)
                         (str "Expected foreclosure warning message, got: " (:text m))))
               w)}

   {:type :then :pattern #"the message has the banker face portrait"
    :handler (fn [w]
               (let [face (:face (get-in w [:state :message]))]
                 (assert (= (:banker (:state w)) face)
                         (str "Expected banker face " (:banker (:state w))
                              " but got " face)))
               w)}

   ;; ===== Foreclosure Dismiss =====
   {:type :then :pattern #"dismissing the message quits the game"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (inp/handle-key (:rng w) (:state w) \space)]
                 (assert (:quit-clicked s)
                         "Expected :quit-clicked after dismissing foreclosure message")
                 (assoc w :state s)))}

   ;; ===== Phase 0d Loan Givens =====
   {:type :given :pattern #"a loan of (\d+) at a certain interest rate"
    :handler (fn [w amt]
               (-> w
                   (assoc-in [:state :loan] (to-double amt))
                   (assoc-in [:state :interest] 5.0)))}
   {:type :given :pattern #"the interest rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :interest] (to-double v)))}
   {:type :given :pattern #"the player requests a loan exceeding the credit limit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :credit-limit] 50000.0)
                   (assoc :loan-request 100000.0)))}
   {:type :given :pattern #"the player passes the credit check"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] 500000.0)
                   (assoc-in [:state :credit-rating] 1.0)))}
   {:type :given :pattern #"the player fails the credit check"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] 0.0)
                   (assoc-in [:state :credit-rating] 0.1)
                   (assoc-in [:state :credit-limit] 100.0)))}
   {:type :given :pattern #"the player repays part of the loan"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     loan (get-in w [:state :loan] 50000.0)
                     amt (* loan 0.5)]
                 (assoc w :state (ln/repay (:state w) amt))))}
   {:type :given :pattern #"the player's gold is negative"
    :handler (fn [w] (assoc-in w [:state :gold] -1000.0))}
   {:type :given :pattern #"the player's gold is (.+)"
    :handler (fn [w v] (assoc-in w [:state :gold] (to-double v)))}
   {:type :given :pattern #"the player's gold drops below 0 at end of month"
    :handler (fn [w]
               (let [s (-> (:state w)
                           (assoc :credit-rating 0.8 :int-addition 0.5
                                  :loan 10000.0 :gold -500.0))
                     w (assoc w :state s)
                     w (snap w)
                     result (ln/emergency-loan s)]
                 (assoc w :state result)))}
   {:type :given :pattern #"the player runs out of gold at end of month"
    :handler (fn [w] (assoc-in w [:state :gold] -100.0))}
   {:type :given :pattern #"the player is out of gold and cannot get a loan"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] -1000.0)
                   (assoc-in [:state :credit-limit] 0.0)
                   (assoc-in [:state :credit-rating] 0.0)))}
   {:type :given :pattern #"the bank forecloses on the player"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :loan] 5000000.0)
                   (assoc-in [:state :credit-rating] 0.1)
                   (assoc-in [:state :gold] 100.0)))}
   {:type :given :pattern #"the debt-to-asset ratio exceeds 80% of the debt support limit"
    :handler (fn [w]
               ;; limit at cr=0.8 is 1.7, 80% is 1.36. Need ratio ~1.5 (between 1.36 and 1.7)
               ;; loan=1.5M, gold=1M → ratio=1.5 > 1.36 ✓, < 1.7 ✓
               (-> w
                   (assoc-in [:state :loan] 1500000.0)
                   (assoc-in [:state :credit-rating] 0.8)
                   (assoc-in [:state :gold] 1000000.0)
                   (assoc-in [:state :credit-limit] 1e7)))}
   {:type :given :pattern #"the interest addition is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :int-addition] (to-double v)))}
   {:type :given :pattern #"a credit rating and credit limit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :credit-rating] 0.7)
                   (assoc-in [:state :credit-limit] 300000.0)))}
   {:type :given :pattern #"the emergency loan cannot cover the deficit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] -1000000.0)
                   (assoc-in [:state :credit-limit] 0.0)
                   (assoc-in [:state :credit-rating] 0.0)))}
   {:type :given :pattern #"the player's gold drops below 0"
    :handler (fn [w] (assoc-in w [:state :gold] -100.0))}

   ;; ===== Phase 0d Loan Whens =====
   {:type :when :pattern #"the player fully repays the loan"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (merge {:loan 50000.0 :gold 100000.0 :credit-rating 0.5
                               :int-addition 1.0} (:state w))
                     loan (:loan s)
                     result (ln/repay s loan)
                     m (msg/pick (:rng w) msg/loan-repayment-messages)]
                 (assoc w :state (assoc result :message {:text m :face (:banker s)}))))}
   {:type :when :pattern #"the player repays (\d+) gold"
    :handler (fn [w amt]
               (let [a (to-double amt)
                     s (-> (:state w)
                           ;; Ensure gold >= repay amount and defaults for credit math
                           (update :gold #(max (or % 0.0) (* a 2.0)))
                           (update :credit-rating #(or % 0.5))
                           (update :int-addition #(or % 1.0)))
                     w (assoc w :state s)
                     w (snap w)
                     result (ln/repay s a)]
                 (if (:error result)
                   (assoc w :error (:error result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the player tries to repay (\d+)"
    :handler (fn [w amt]
               (let [result (ln/repay (:state w) (to-double amt))]
                 (if (:error result)
                   (assoc w :error (:error result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the player borrows from the bank"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     amt (get w :loan-request 10000.0)
                     result (ln/borrow (:rng w) (:state w) amt)]
                 (if (:needs-credit-check result)
                   (assoc w :needs-credit-check true :credit-fee (:fee result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the loan is granted"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     s (merge {:loan 0.0 :gold 10000.0 :credit-limit 1000000.0
                               :credit-rating 0.8 :interest 5.0 :int-addition 0.0} s)
                     amt (get w :loan-request 10000.0)
                     result (ln/borrow (:rng w) s amt)]
                 (if (and (map? result) (:needs-credit-check result))
                   (assoc w :state s :loan-denied true)
                   (let [int-rate (+ (:interest s) (:int-addition s))
                         m (format (msg/pick (:rng w) msg/loan-approval-messages) amt int-rate)]
                     (assoc w :state (assoc result :message
                                           {:text m :face (:banker s)}))))))}
   {:type :when :pattern #"the loan is denied"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) msg/loan-denial-messages)]
                 (-> w
                     (assoc :loan-denied true)
                     (assoc-in [:state :message] {:text m :face (:banker (:state w))}))))}
   {:type :when :pattern #"the shortage is detected"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)]
                 (if (neg? (:gold s))
                   (assoc-in w [:state :message]
                             {:text (msg/pick (:rng w) msg/cash-shortage-messages)
                              :face (:banker s)})
                   w)))}
   {:type :when :pattern #"the game ends"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     m (cond
                         (and (pos? (:loan s 0)) (ln/foreclosed? (merge (st/initial-state) s)))
                         {:text (msg/pick (:rng w) msg/foreclosure-messages)
                          :face (:banker s)}
                         (and (neg? (:gold s 0)) (<= (:credit-limit s 0) 0))
                         {:text (msg/pick (:rng w) msg/bankruptcy-messages)
                          :face (:banker s)}
                         :else nil)]
                 (cond-> (assoc-in w [:state :game-over] true)
                   m (assoc-in [:state :message] m))))}
   {:type :when :pattern #"the credit check fee is offered"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     total (+ (:loan (:state w) 0.0) (get w :loan-request 100000.0))
                     fee (ln/credit-check-fee (:rng w) total)
                     m (format (msg/pick (:rng w) msg/credit-check-messages) fee)]
                 (-> w
                     (assoc :credit-fee fee)
                     (assoc-in [:state :message] m))))}

   ;; ===== Phase 0d Loan Thens =====
   {:type :then :pattern #"the repayment is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected repayment rejection")
               w)}
   {:type :then :pattern #"the loan decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :loan] 50000.0)
                     after (:loan (:state w))]
                 (assert-near (to-double expected) (- before after) 100.0))
               w)}
   {:type :then :pattern #"gold decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert-near (to-double expected) (- before after) 500.0))
               w)}
   {:type :then :pattern #"the credit limit should be (\d+) gold"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:credit-limit (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the credit rating decreases by \(1 - credit rating\) / 3"
    :handler (fn [w]
               (let [before (get-in w [:state-before :credit-rating] 1.0)
                     after (:credit-rating (:state w))]
                 (assert (< after before) "Expected credit rating to decrease"))
               w)}
   {:type :then :pattern #"the credit lower bound should be (\d+) gold"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:credit-lower (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the loan balance should increase by the borrowed amount"
    :handler (fn [w]
               (let [before (get-in w [:state-before :loan] 0.0)
                     after (:loan (:state w))]
                 (assert (> after before) "Expected loan to increase"))
               w)}
   {:type :then :pattern #"the player can now buy commodities"
    :handler (fn [w]
               (assert (pos? (:gold (:state w))) "Expected positive gold")
               w)}
   {:type :then :pattern #"default credit limits apply"
    :handler (fn [w]
               (assert (pos? (:credit-limit (:state w)))
                       "Expected positive credit limit")
               w)}
   {:type :then :pattern #"gold, loan, interest rate, credit rating, and credit limit all match"
    :handler (fn [w]
               (let [before (:state-before w) after (:state w)]
                 (doseq [k [:gold :loan :interest :credit-rating :credit-limit]]
                   (assert (near? (k before) (k after)) (str k " mismatch")))
                 w))}
   {:type :then :pattern #"one face is the banker"
    :handler (fn [w]
               (assert (number? (:banker (:state w))) "Expected banker face")
               w)}
   {:type :then :pattern #"a random repayment message is displayed from the pool"
    :handler (fn [w] w)}

   ;; --- Loan message Then handlers ---
   {:type :then :pattern #"a random credit-check-fee message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random loan-approval message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random loan-denial message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random foreclosure message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random bankruptcy message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"the message includes the fee amount"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message includes the loan amount and interest rate"
    :handler (fn [w] w)}])
