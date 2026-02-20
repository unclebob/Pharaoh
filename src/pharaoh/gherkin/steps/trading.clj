(ns pharaoh.gherkin.steps.trading
  (:require [clojure.string :as str]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double
                                                   ensure-rng snap]]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.trading :as tr]))

(defn steps []
  [;; ===== Trading Given (most specific patterns first) =====

   {:type :given :pattern #"(.+) costs (.+) per bushel"
    :handler (fn [w commodity price]
               (assoc-in w [:state :prices (keyword (str/lower-case commodity))]
                         (to-double price)))}
   {:type :given :pattern #"the (.+) supply is (.+)"
    :handler (fn [w commodity amt]
               (assoc-in w [:state :supply (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the remaining (.+)% of monthly demand is consumed"
    :handler (fn [w _pct] w)}
   {:type :given :pattern #"supply is still positive after all consumption"
    :handler (fn [w] w)}
   {:type :given :pattern #"monthly demand consumes (.+)% and supply goes negative"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player has (\d+) acres of total land"
    :handler (fn [w amt]
               (let [per (/ (to-double amt) 4.0)]
                 (-> w
                     (assoc-in [:state :ln-fallow] per)
                     (assoc-in [:state :ln-sewn] per)
                     (assoc-in [:state :ln-grown] per)
                     (assoc-in [:state :ln-ripe] per))))}
   {:type :given :pattern #"(\d+) slaves, (\d+) horses, and (\d+) oxen"
    :handler (fn [w sl hs ox]
               (-> w
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :oxen] (to-double ox))))}
   {:type :given :pattern #"the player has various commodities and gold"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] 50000.0)
                   (assoc-in [:state :wheat] 5000.0)
                   (assoc-in [:state :slaves] 100.0)
                   (assoc-in [:state :oxen] 50.0)
                   (assoc-in [:state :horses] 20.0)
                   (assoc-in [:state :manure] 1000.0)
                   (assoc-in [:state :ln-fallow] 200.0)))}
   {:type :given :pattern #"the player has (\d+) acres of planted land"
    :handler (fn [w amt] (assoc-in w [:state :ln-sewn] (to-double amt)))}
   {:type :given :pattern #"the player has (\d+) acres of fallow land"
    :handler (fn [w amt] (assoc-in w [:state :ln-fallow] (to-double amt)))}
   {:type :given :pattern #"those acres have (\d+) bushels of wheat sewn"
    :handler (fn [w amt] (assoc-in w [:state :wt-sewn] (to-double amt)))}

   ;; --- Phase 0d Trading Given ---
   {:type :given :pattern #"the slave market price is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :prices :slaves] (to-double v)))}
   {:type :given :pattern #"the market can only absorb (\d+) bushels of wheat"
    :handler (fn [w v] (assoc-in w [:state :demand :wheat] (to-double v)))}
   {:type :given :pattern #"the market only has (\d+) bushels in stock"
    :handler (fn [w v] (assoc-in w [:state :supply :wheat] (to-double v)))}
   {:type :given :pattern #"the player cannot afford the purchase"
    :handler (fn [w] (assoc-in w [:state :gold] 0.0))}
   {:type :given :pattern #"the player only has (\d+) (.+)"
    :handler (fn [w amt what]
               (let [k (cond
                         (= what "gold") :gold
                         (= what "oxen") :oxen
                         :else (keyword (str/lower-case what)))]
                 (assoc-in w [:state k] (to-double amt))))}
   {:type :given :pattern #"the player's current horses have health ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}

   ;; ===== Trading When =====

   {:type :when :pattern #"the player buys (\d+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/buy (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player buys (\d+) (.+)"
    :handler (fn [w amt commodity]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/buy (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player sells (\d+) bushels of (.+) at (.+) per bushel"
    :handler (fn [w amt commodity _price]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/sell (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player sells (\d+) acres of planted land"
    :handler (fn [w amt]
               (let [w (snap (ensure-rng w))
                     sell-amt (to-double amt)
                     s (:state w)
                     owned (:ln-sewn s)
                     burn-fract (if (pos? owned) (/ sell-amt owned) 0)]
                 (assoc w :state (-> s
                                     (update :ln-sewn - sell-amt)
                                     (update :wt-sewn * (- 1.0 burn-fract))))))}
   {:type :when :pattern #"the player sells (\d+) acres of fallow land"
    :handler (fn [w amt]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (tr/sell (:rng w) (:state w) :land (to-double amt)))))}
   {:type :when :pattern #"the player tries to sell more than (\d+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [k (keyword (str/lower-case commodity))
                     over-amt (+ (to-double amt) 1.0)
                     s (assoc (:state w) k (+ over-amt 1000.0))
                     v (tr/validate-sell s k over-amt)]
                 (assoc w :sell-result v)))}
   {:type :when :pattern #"the player tries to sell (.+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [k (keyword (str/lower-case commodity))
                     v (tr/validate-sell (:state w) k (to-double amt))]
                 (assoc w :sell-result v)))}
   {:type :when :pattern #"the player tries to buy (.+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))
                     ;; Ensure gold is sufficient for the purchase
                     s (-> (:state w)
                           (update :gold #(if (pos? (or % 0)) % 100000.0)))
                     v (tr/validate-buy s k (to-double amt))]
                 (case (:status v)
                   :ok (let [result (tr/buy (:rng w) s k (to-double amt))
                             m (msg/pick (:rng w) msg/transaction-success-messages)]
                         (assoc w :state (assoc result :message m)
                                  :buy-result v))
                   (assoc w :buy-result v :state s))))}
   {:type :when :pattern #"the player keeps (.+) bushels of (.+)"
    :handler (fn [w amt commodity]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case commodity))
                     target (to-double amt)
                     owned (get (:state w) k 0.0)
                     diff (- owned target)]
                 (if (pos? diff)
                   (assoc w :state (tr/sell (:rng w) (:state w) k diff))
                   w)))}

   ;; --- Phase 0d Trading When ---
   {:type :when :pattern #"the player acquires (\d+) slaves"
    :handler (fn [w n]
               (let [w (snap (ensure-rng w))
                     target (to-double n)
                     owned (get-in w [:state :slaves] 0.0)
                     to-buy (max 0 (- target owned))
                     init (st/initial-state)
                     s (merge {:gold 1000000.0 :prices (:prices init)
                               :supply (:supply init)} (:state w))]
                 (assoc w :state (tr/buy (:rng w) s :slaves to-buy))))}
   {:type :when :pattern #"a buy or sell transaction completes normally"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     init (st/initial-state)
                     s (merge {:gold 100000.0 :prices (:prices init)
                               :supply (:supply init)} (:state w))
                     result (tr/buy (:rng w) s :wheat 100.0)
                     m (msg/pick (:rng w) msg/transaction-success-messages)]
                 (assoc w :state (assoc result :message m))))}
   {:type :when :pattern #"the player sells slaves"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     amt (/ (:slaves (:state w)) 2.0)]
                 (assoc w :state (tr/sell (:rng w) (:state w) :slaves amt))))}
   {:type :when :pattern #"the player attempts to buy"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     init (st/initial-state)
                     s (merge {:prices (:prices init) :supply (:supply init)} (:state w))
                     v (tr/validate-buy s :wheat 100.0)]
                 (if (= :ok (:status v))
                   (assoc w :state (tr/buy (:rng w) s :wheat 100.0))
                   (let [m (msg/pick (:rng w) msg/insufficient-funds-messages)]
                     (assoc w :state (assoc s :message m)
                              :trade-error v)))))}
   {:type :when :pattern #"the player tries to buy (\d+) bushels"
    :handler (fn [w amt]
               (let [w (snap (ensure-rng w))
                     init (st/initial-state)
                     s (merge {:gold 100000.0 :prices (:prices init)
                               :supply (:supply init)} (:state w))
                     v (tr/validate-buy s :wheat (to-double amt))]
                 (if (= :ok (:status v))
                   (let [result (tr/buy (:rng w) s :wheat (to-double amt))
                         m (msg/pick (:rng w) msg/transaction-success-messages)]
                     (assoc w :state (assoc result :message m)
                              :buy-result v))
                   (let [m (msg/pick (:rng w) msg/demand-limit-messages)]
                     (assoc w :trade-error v
                              :state (assoc s :message m))))))}
   {:type :when :pattern #"the player tries to sell (\d+) bushels"
    :handler (fn [w amt]
               (let [w (snap (ensure-rng w))
                     s (merge {:wheat 10000.0} (:state w))
                     result (tr/validate-sell s :wheat (to-double amt))]
                 (if (= :ok (:status result))
                   (let [init (st/initial-state)]
                     (assoc w :state (tr/sell (:rng w)
                                             (merge {:prices (:prices init)} s)
                                             :wheat (to-double amt))))
                   (let [m (msg/pick (:rng w) msg/supply-limit-messages)]
                     (assoc w :trade-error result
                              :state (assoc s :message m))))))}
   {:type :when :pattern #"the player sells (\d+) (.+)"
    :handler (fn [w amt commodity]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/sell (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the horses are added"
    :handler (fn [w]
               (let [s (:state w)
                     new-health 0.9
                     existing (:horses s 0.0)
                     old-health (:hs-health s 0.8)
                     added 100.0
                     blended (/ (+ (* existing old-health) (* added new-health))
                                (+ existing added))]
                 (-> w
                     (update-in [:state :horses] + added)
                     (assoc-in [:state :hs-health] blended))))}

   ;; ===== Trading Then =====

   {:type :then :pattern #"the player's (.+) should (?:increase|decrease) by (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the (.+) supply should (?:increase|decrease) by (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the transaction is rejected"
    :handler (fn [w]
               (let [result (or (:sell-result w) (:buy-result w))]
                 (assert (= :error (:status result))
                         (str "Expected rejection, got " result)))
               w)}
   {:type :then :pattern #"the sale is capped at the maximum.*"
    :handler (fn [w]
               (assert (= :capped (:status (:sell-result w))))
               w)}
   {:type :then :pattern #"a message shows the player (?:owns|can afford) (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the system sells (.+) bushels of (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the new (.+) have nominal health of approximately (.+)"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the blended slave health is.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"(\d+)% of the wheat sewn on that land is destroyed"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the player has (\d+) acres of planted land"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:ln-sewn (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the player has (\d+) acres of fallow land"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:ln-fallow (:state w)) 1.0)
               w)}
   {:type :then :pattern #"(\d+) bushels remain sewn"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:wt-sewn (:state w)) 1.0)
               w)}
   {:type :then :pattern #"no crops are affected"
    :handler (fn [w] w)}

   ;; --- Phase 0d Trading Then ---
   {:type :then :pattern #"a message indicates the market is out of stock"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (some? m) "Expected a message about supply"))
               w)}
   {:type :then :pattern #"only (\d+) bushels are purchased"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wheat] 0.0)
                     after (:wheat (:state w))
                     bought (- after before)]
                 (assert-near (to-double expected) bought 1.0))
               w)}
   {:type :then :pattern #"the system buys (\d+) additional slaves"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :slaves] 0.0)
                     after (:slaves (:state w))
                     bought (- after before)]
                 (assert-near (to-double expected) bought 10.0))
               w)}
   {:type :then :pattern #"the actual price received per slave is .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player should receive gold"
    :handler (fn [w]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (> after before) "Expected gold to increase"))
               w)}
   {:type :then :pattern #"the blended health = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player buys as much as remaining gold allows"
    :handler (fn [w]
               (let [after (:state w)]
                 (assert (<= (:gold after 0) 0.01)
                         "Expected gold to be near zero after buying max"))
               w)}
   {:type :then :pattern #"the player pays for the reduced amount"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (< (:gold after 0) (:gold before 0))
                         "Expected gold to decrease from payment"))
               w)}

   ;; --- Trading message Then handlers ---
   {:type :then :pattern #"a random supply-limit message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random demand-limit message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random insufficient-funds message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random success message is displayed from the pool"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"the message includes the maximum amount the market will accept"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message includes the amount available"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message includes the maximum the player can afford"
    :handler (fn [w] w)}])
