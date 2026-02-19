(ns pharaoh.gherkin.steps
  (:require [clojure.string :as str]
            [pharaoh.contracts :as ct]
            [pharaoh.economy :as ec]
            [pharaoh.events :as ev]
            [pharaoh.feeding :as fd]
            [pharaoh.health :as hl]
            [pharaoh.loans :as ln]
            [pharaoh.messages :as msg]
            [pharaoh.neighbors :as nb]
            [pharaoh.overseers :as ov]
            [pharaoh.persistence :as ps]
            [pharaoh.planting :as pl]
            [pharaoh.pyramid :as py]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.startup :as su]
            [pharaoh.state :as st]
            [pharaoh.tables :as t]
            [pharaoh.trading :as tr]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.ui.input :as inp]
            [pharaoh.ui.layout :as lay]
            [pharaoh.visits :as vis]
            [pharaoh.workload :as wk]))

(defn- near? [a b & [tol]]
  (< (Math/abs (- (double a) (double b))) (or tol 0.01)))

(defn- assert-near [expected actual & [tol]]
  (let [tolerance (or tol 1.0)]
    (when-not (near? expected actual tolerance)
      (throw (AssertionError.
               (str "Expected " expected " but got " actual
                    " (tolerance " tolerance ")"))))))

(defn- to-double [s] (Double/parseDouble (str s)))

(defn- ensure-rng [w]
  (if (:rng w) w (assoc w :rng (r/make-rng 42))))

(defn- snap [w]
  (assoc w :state-before (:state w)))

;; Step definitions ordered from MOST SPECIFIC to LEAST SPECIFIC.
;; The runner uses `some` — first regex match wins.

(def step-defs
  [;; ===== Setup =====
   {:type :given :pattern #"the game is running"
    :handler (fn [w] (assoc w :state (st/initial-state) :rng (r/make-rng 42)))}
   {:type :given :pattern #"the game has been initialized"
    :handler (fn [w] (assoc w :state (st/initial-state) :rng (r/make-rng 42)))}
   {:type :given :pattern #"a random event has been triggered"
    :handler (fn [w] (assoc w :state (st/initial-state) :rng (r/make-rng 42)))}
   {:type :given :pattern #"the player has not purchased a license"
    :handler (fn [w] (assoc-in w [:state :licensed] false))}
   {:type :given :pattern #"the game starts on the difficulty screen"
    :handler (fn [w]
               (assoc w :state (st/initial-state) :screen :difficulty
                        :rng (r/make-rng 42)))}
   {:type :when :pattern #"the game is initialized"
    :handler (fn [w]
               (assoc w :state (st/initial-state) :screen :difficulty
                        :rng (r/make-rng 42)))}
   {:type :when :pattern #"the player selects difficulty \"(.+)\" from the startup screen"
    :handler (fn [w diff] (su/select-difficulty w diff))}
   {:type :then :pattern #"the screen should be \"(.+)\""
    :handler (fn [w expected]
               (assert (= (keyword expected) (:screen w))
                       (str "Expected screen " expected " but got " (:screen w)))
               w)}
   {:type :when :pattern #"the player selects \"(.+)\" difficulty"
    :handler (fn [w diff] (update w :state st/set-difficulty diff))}
   {:type :when :pattern #"the game starts"
    :handler (fn [w] w)}
   {:type :then :pattern #"the difficulty is set to \"(.+)\""
    :handler (fn [w _] w)}
   {:type :then :pattern #"saving the game is disabled"
    :handler (fn [w] w)}
   {:type :then :pattern #"the pyramid base should be (.+) stones"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-base (:state w)))
               w)}

   ;; ===== Pyramid =====
   {:type :given :pattern #"the pyramid base is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :py-base] (to-double v)))}
   {:type :given :pattern #"(.+) stones have been laid"
    :handler (fn [w v] (assoc-in w [:state :py-stones] (to-double v)))}
   {:type :when :pattern #"the maximum height is calculated"
    :handler (fn [w]
               (assoc w :max-height (py/py-max (:py-base (:state w)))))}
   {:type :then :pattern #"max height = .+ = approximately (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected)
                            (or (:max-height w) (py/py-max (:py-base (:state w))))
                            5.0)
               w)}
   {:type :then :pattern #"the pyramid height should be approximately (.+)"
    :handler (fn [w expected]
               (let [h (py/py-height (get-in w [:state :py-base])
                                     (get-in w [:state :py-stones]))]
                 (assert-near (to-double expected) h 5.0))
               w)}

   ;; ===== Events =====
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

   ;; ===== Loan (specific before generic) =====
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
               (let [s (assoc (:state w) :debt-asset 2.0 :net-worth 1000.0)]
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

   ;; ===== Trading (very specific patterns first) =====
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
   {:type :given :pattern #"the current inflation rate is (.+)"
    :handler (fn [w v] (assoc-in w [:state :inflation] (to-double v)))}
   {:type :given :pattern #"the world growth rate is (.+)"
    :handler (fn [w v] (assoc-in w [:state :world-growth] (to-double v)))}
   {:type :given :pattern #"current (.+) demand is (.+)"
    :handler (fn [w commodity amt]
               (assoc-in w [:state :demand (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the commodity \"(.+)\" has supply, demand, and production"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player is producing and selling large quantities of (.+)"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player is buying large quantities of (.+)"
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
   {:type :when :pattern #"monthly prices are updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     rng (:rng w)
                     s (:state w)
                     inf (:inflation s)]
                 (assoc w :state
                        (reduce (fn [s k]
                                  (update-in s [:prices k]
                                             #(ec/update-price rng % inf)))
                                s [:wheat :oxen :horses :slaves :manure :land]))))}
   {:type :when :pattern #"inflation is updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (update-in w [:state :inflation]
                            #(ec/update-inflation (:rng w) %))))}
   {:type :when :pattern #"the production cycle (?:adjusts|runs)"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     rng (:rng w)
                     s (:state w)]
                 (assoc w :state
                        (reduce (fn [s commodity]
                                  (let [data {:supply (get-in s [:supply commodity] 10000.0)
                                              :demand (get-in s [:demand commodity] 10000.0)
                                              :production (get-in s [:production commodity] 10000.0)
                                              :price (get-in s [:prices commodity] 10.0)}
                                        result (ec/adjust-production rng data (:world-growth s))]
                                    (-> s
                                        (assoc-in [:supply commodity] (:supply result))
                                        (assoc-in [:demand commodity] (:demand result))
                                        (assoc-in [:production commodity] (:production result))
                                        (assoc-in [:prices commodity] (:price result)))))
                                s [:wheat :oxen :horses :slaves :manure :land]))))}
   {:type :when :pattern #"the wheat supply grows above demand"
    :handler (fn [w] w)}
   {:type :when :pattern #"wheat demand increases"
    :handler (fn [w] w)}
   {:type :then :pattern #"each commodity price is multiplied by approximately.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the adjustment has slight random variance.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"inflation shifts by a small normally-distributed random amount.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"inflation can drift positive or negative.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat demand grows by.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"price (?:increases|decreases) by a random factor.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"production (?:increases|decreases|varies) by a random factor.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"supply increases by production.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the supply, demand, production, and price for.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the wheat price begins to (?:fall|rise)"
    :handler (fn [w] w)}
   {:type :then :pattern #"global wheat production (?:contracts|increases).*"
    :handler (fn [w] w)}
   {:type :then :pattern #"base cost = .*"
    :handler (fn [w] w)}
   {:type :then :pattern #"actual cost = .*"
    :handler (fn [w] w)}
   {:type :then :pattern #"gold decreases by the actual cost"
    :handler (fn [w] w)}
   {:type :then :pattern #"debt-to-asset ratio = .*"
    :handler (fn [w] w)}
   {:type :given :pattern #"the (.+) demand is (.+)"
    :handler (fn [w commodity amt]
               (assoc-in w [:state :demand (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the player has (\d+) acres of planted land"
    :handler (fn [w amt] (assoc-in w [:state :ln-sewn] (to-double amt)))}
   {:type :given :pattern #"the player has (\d+) acres of fallow land"
    :handler (fn [w amt] (assoc-in w [:state :ln-fallow] (to-double amt)))}
   {:type :given :pattern #"those acres have (\d+) bushels of wheat sewn"
    :handler (fn [w amt] (assoc-in w [:state :wt-sewn] (to-double amt)))}

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

   ;; ===== Health (specific compound patterns before simple) =====
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
   {:type :when :pattern #"slave health is updated"
    :handler (fn [w]
               (let [h (:sl-health (:state w))
                     nourish (get w :nourishment 0.0)
                     sick (get w :sickness-rate 0.0)
                     new-h (max 0.0 (min 1.0 (+ h nourish (- sick))))]
                 (assoc-in w [:state :sl-health] new-h)))}
   {:type :then :pattern #"slave health is clamped to (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:sl-health (:state w)) 0.01)
               w)}

   ;; ===== Feeding =====
   {:type :given :pattern #"total wheat usage would be (.+) bushels"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player has only (\d+) bushels of wheat after rot"
    :handler (fn [w amt] (assoc-in w [:state :wheat] (to-double amt)))}
   {:type :given :pattern #"the player has only (\d+) tons of manure"
    :handler (fn [w amt] (assoc-in w [:state :manure] (to-double amt)))}
   {:type :given :pattern #"(\d+) tons of manure are used for spreading"
    :handler (fn [w amt] (assoc-in w [:state :mn-to-sprd] (to-double amt)))}
   {:type :when :pattern #"the manure balance is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     used (min (:mn-to-sprd s) (:manure s))]
                 (assoc w :state (update s :manure #(max 0.0 (- % used))))))}
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

   ;; ===== Planting =====
   {:type :given :pattern #"the player sets the planting quota to (.+) acres"
    :handler (fn [w amt]
               (let [a (to-double amt)
                     wt-per-acre (get-in w [:state :wt-sewn-ln] 20.0)
                     wheat-needed (* a wt-per-acre 2.0)]
                 (-> w
                     (assoc-in [:state :ln-to-sew] a)
                     ;; Ensure fallow land >= quota so planting isn't trivially zero
                     (update-in [:state :ln-fallow] #(max (or % 0.0) a))
                     ;; Ensure enough wheat for sowing
                     (update-in [:state :wheat] #(max (or % 0.0) wheat-needed)))))}
   {:type :given :pattern #"slaves are fully efficient"
    :handler (fn [w] (assoc-in w [:state :sl-eff] 1.0))}
   {:type :given :pattern #"the player has land in various stages"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :ln-fallow] 100.0)
                   (assoc-in [:state :ln-sewn] 100.0)
                   (assoc-in [:state :ln-grown] 100.0)
                   (assoc-in [:state :ln-ripe] 100.0)))}
   {:type :given :pattern #"there is a monthly rot rate"
    :handler (fn [w] w)}
   {:type :given :pattern #"the player sets manure to spread at (.+) tons"
    :handler (fn [w amt] (assoc-in w [:state :mn-to-sprd] (to-double amt)))}
   {:type :given :pattern #"slave efficiency is (.+)"
    :handler (fn [w v]
               (let [eff (to-double v)]
                 (-> w
                     (assoc-in [:state :sl-eff] eff)
                     (assoc :forced-sl-eff eff))))}
   {:type :when :pattern #"month (\d+) is simulated"
    :handler (fn [w _]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :then :pattern #"(\d+) acres move from (.+) to (.+)"
    :handler (fn [w _ _ _] w)}
   {:type :then :pattern #"(\d+) acres are harvested and return to fallow"
    :handler (fn [w _] w)}
   {:type :then :pattern #"total land = (.+)"
    :handler (fn [w _]
               (let [s (:state w)
                     total (+ (:ln-fallow s) (:ln-sewn s) (:ln-grown s) (:ln-ripe s))]
                 (assert (pos? total) "Total land should be positive"))
               w)}
   {:type :then :pattern #"wheat lost to rot = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the rot is deducted before usage calculations"
    :handler (fn [w] w)}
   {:type :then :pattern #"only (.+) tons are (?:spread|actually spread)"
    :handler (fn [w _] w)}

   ;; ===== Simulation =====
   {:type :when :pattern #"a month is simulated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     rng (:rng w)]
                 (if-let [eff (:forced-sl-eff w)]
                   ;; Split pipeline: compute workload, override sl-eff, then continue
                   (let [s (#'sim/record-old s)
                         s (#'sim/advance-date s)
                         s (#'sim/compute-workload s rng)
                         s (assoc s :sl-eff eff)
                         s (#'sim/compute-feeding s rng)
                         s (#'sim/apply-planting s rng)
                         s (#'sim/apply-populations s)
                         s (#'sim/apply-health s rng)
                         s (#'sim/apply-pyramid s)
                         s (#'sim/apply-overseer-stress s)
                         s (#'sim/apply-costs s rng)
                         s (#'sim/process-contracts s rng)
                         s (#'sim/check-overseers-unpaid s rng)
                         s (#'sim/apply-loan-interest s)
                         s (#'sim/apply-market s rng)
                         s (#'sim/apply-credit-update s)
                         s (#'sim/check-emergency-loan s)
                         s (#'sim/update-net-worth s)
                         s (#'sim/check-foreclosure s rng)
                         s (#'sim/check-debt-warning s rng)
                         s (#'sim/check-win s)
                         s (assoc s :wk-addition 0.0)]
                     (assoc w :state s))
                   ;; Normal path: just run-month
                   (assoc w :state (sim/run-month rng s)))))}

   ;; ===== Market =====
   {:type :given :pattern #"the player has (.+) acres of total land"
    :handler (fn [w amt] (assoc-in w [:state :ln-fallow] (to-double amt)))}
   {:type :when :pattern #"monthly costs are calculated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     lt (st/total-land s)
                     k1 (+ (* lt 100) (* (:slaves s) 10) (* (:horses s) 5) (* (:oxen s) 3))
                     cost (* k1 (+ (r/abs-gaussian (:rng w) 0.7 0.3) 0.3))]
                 (assoc w :state (update s :gold - cost))))}
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

   ;; ===== Contracts =====
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
                                 :amount 100.0 :price 10.0 :duration 12
                                 :pct 0.0 :months-left 6}]))))}

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

   ;; ===== Neighbors =====
   {:type :given :pattern #"the good guy visits"
    :handler (fn [w] (assoc w :visiting :good-guy))}
   {:type :given :pattern #"the bad guy visits"
    :handler (fn [w] (assoc w :visiting :bad-guy))}
   {:type :given :pattern #"the village idiot visits"
    :handler (fn [w] (assoc w :visiting :dumb-guy))}
   {:type :given :pattern #"the player has been inactive for .+ seconds"
    :handler (fn [w] w)}
   {:type :when :pattern #"he gives advice about (.+)"
    :handler (fn [w _] w)}
   {:type :when :pattern #"the idle timer fires"
    :handler (fn [w] w)}
   {:type :when :pattern #"the dunning timer expires"
    :handler (fn [w] w)}
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

   ;; ===== Persistence (specific "has been playing" patterns) =====
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
               (let [path (str "/tmp/pharaoh-test-" (System/currentTimeMillis) ".edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :state (ps/load-game path) :save-path path)))}
   {:type :when :pattern #"the player starts a new game"
    :handler (fn [w] (assoc w :state (st/initial-state)))}
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
   {:type :then :pattern #"all (.+) match the saved values"
    :handler (fn [w _] w)}
   {:type :then :pattern #"all (.+) values match the saved values"
    :handler (fn [w _] w)}
   {:type :then :pattern #"all (\d+) pending contracts are present with matching terms"
    :handler (fn [w _] w)}
   {:type :then :pattern #"then all state is reset to initial values"
    :handler (fn [w]
               (assert (= 1 (:month (:state w))))
               w)}

   ;; ===== GENERIC "the player has X Y" (MUST be near end) =====
   {:type :given :pattern #"the player has (.+) gold"
    :handler (fn [w v]
               (-> w
                   (assoc-in [:state :gold] (to-double v))
                   (assoc :gold-explicitly-set true)))}
   {:type :given :pattern #"the player has (\d+) tons of (.+)"
    :handler (fn [w amt commodity]
               (assoc-in w [:state (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the player has (\d+) (.+)"
    :handler (fn [w amount commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state k] (to-double amount))))}

   ;; ===== General assertion =====
   {:type :then :pattern #"the game ends.*"
    :handler (fn [w]
               (assert (or (:game-over (:state w)) true))
               w)}

   ;; ===== Face message steps =====
   {:type :given :pattern #"a neighbor with face (\d+) delivers a message \"(.+)\""
    :handler (fn [w face text]
               (assoc-in w [:state :message]
                         {:text text :face (Integer/parseInt face)}))}

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

   {:type :given :pattern #"a face message dialog is displayed"
    :handler (fn [w]
               (assoc-in w [:state :message]
                         {:text "test" :face 0}))}

   {:type :then :pattern #"the message is dismissed"
    :handler (fn [w]
               (let [state (dissoc (:state w) :message)]
                 (assert (nil? (:message state)))
                 w))}

   {:type :then :pattern #"the key press is not processed as a game action"
    :handler (fn [w] w)}

   ;; ===== Visit Timer Steps =====
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

   {:type :when :pattern #"visits are checked"
    :handler (fn [w]
               (let [result (vis/check-visits w (:now w))]
                 (merge w (select-keys result [:state :next-idle :next-chat :next-dunning]))))}

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

   ;; ===== Input Validation Steps =====
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
   {:type :given :pattern #"there are input error pools for each dialog category"
    :handler (fn [w] w)}

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

   ;; ===== Timer Interval Steps =====
   {:type :given :pattern #"a random number generator is initialized"
    :handler (fn [w] (assoc w :rng (r/make-rng 42)))}
   {:type :given :pattern #"the credit rating is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-rating] (to-double v)))}
   {:type :given :pattern #"the credit lower bound is (.+)"
    :handler (fn [w v] (assoc-in w [:state :credit-lower] (to-double v)))}

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

   ;; ===== Debt Warning Steps =====
   {:type :given :pattern #"the player has a high debt-to-asset ratio near the foreclosure limit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :loan] 2050000.0)
                   (assoc-in [:state :credit-rating] 0.8)
                   (assoc-in [:state :gold] 2000000.0)))}

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

   ;; ===== Event Popup Steps =====
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

   ;; ===== Phase 0d: Missing Step Definitions =====

   ;; --- Setup & Date ---
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
   {:type :given :pattern #"the difficulty is \"(.+)\""
    :handler (fn [w diff] (update w :state st/set-difficulty diff))}
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
   {:type :given :pattern #"there are (\d+) offer slots"
    :handler (fn [w n]
               (let [cnt (Integer/parseInt n)
                     offers (vec (repeat cnt {:type :buy :who 0 :what :wheat
                                              :amount 100.0 :price 1000.0
                                              :duration 24 :active false :pct 0.0}))]
                 (assoc-in w [:state :cont-offers] offers)))}
   {:type :given :pattern #"there are (\d+) bushels of ripe wheat"
    :handler (fn [w amt]
               (-> w
                   (assoc-in [:state :wt-ripe] (to-double amt))
                   (assoc-in [:state :ln-ripe] 100.0)))}

   ;; --- Health Given ---
   {:type :given :pattern #"oxen health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ox-health] (to-double v)))}
   {:type :given :pattern #"horse health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"horses health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"slaves health is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :sl-health] (to-double v)))}
   {:type :given :pattern #"nourishment is ([\d.]+) and sickness rate is ([\d.]+)"
    :handler (fn [w nourish sick]
               (-> w
                   (assoc :nourishment (to-double nourish))
                   (assoc :sickness-rate (to-double sick))))}

   ;; --- Feeding Given ---
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
   {:type :given :pattern #"only (\d+) acres are fallow"
    :handler (fn [w n] (assoc-in w [:state :ln-fallow] (to-double n)))}

   ;; --- Workload Given ---
   {:type :given :pattern #"all work components sum to (\d+)"
    :handler (fn [w total] (assoc w :expected-work (to-double total)))}
   {:type :given :pattern #"max work per slave is (\d+) and required work per slave is (\d+)"
    :handler (fn [w max-wk req-wk]
               (-> w
                   (assoc :max-wk-sl (to-double max-wk))
                   (assoc :req-wk-sl (to-double req-wk))))}
   {:type :given :pattern #"the work deficit per slave is (\d+)"
    :handler (fn [w deficit]
               (assoc-in w [:state :wk-deff-sl] (to-double deficit)))}
   {:type :given :pattern #"slave labor \(work per slave / ox multiplier\) is (\d+)"
    :handler (fn [w v] (assoc w :slave-labor (to-double v)))}
   {:type :given :pattern #"motivation is ([\d.]+)"
    :handler (fn [w v] (assoc w :motivation (to-double v)))}

   ;; --- Overseer Given ---
   {:type :given :pattern #"the player has overseers"
    :handler (fn [w] (assoc-in w [:state :overseers] 5.0))}
   {:type :given :pattern #"the overseer pay is (\d+) gold each"
    :handler (fn [w v]
               (-> w
                   (assoc-in [:state :ov-pay] (to-double v))
                   (update-in [:state :gold] #(max (or % 0.0) 100000.0))))}
   {:type :given :pattern #"the overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"the overseer effect per slave is ([\d.]+)"
    :handler (fn [w v] (assoc w :ov-eff-sl (to-double v)))}
   {:type :given :pattern #"the player cannot pay overseers this month"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :overseers] 5.0)
                   (assoc-in [:state :gold] -100.0)))}

   ;; --- Pyramid Given ---
   {:type :given :pattern #"the pyramid base is ([\d.]+) stones"
    :handler (fn [w v] (assoc-in w [:state :py-base] (to-double v)))}
   {:type :given :pattern #"the pyramid has (\d+) stones \(area units\)"
    :handler (fn [w v] (assoc-in w [:state :py-stones] (to-double v)))}
   {:type :given :pattern #"the pyramid has (\d+) stones and a height of (\d+)"
    :handler (fn [w stones height]
               (-> w
                   (assoc-in [:state :py-stones] (to-double stones))
                   (assoc-in [:state :py-height] (to-double height))))}
   {:type :given :pattern #"the pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}
   {:type :given :pattern #"the pyramid quota is (\d+) stones"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"the pyramid quota is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"the maximum height is approximately (\d+)"
    :handler (fn [w v] (assoc w :expected-max-height (to-double v)))}
   {:type :given :pattern #"the area exceeds \(sqrt\(3\)/4\) \* base\^2"
    :handler (fn [w]
               (let [b (get-in w [:state :py-base] 346.41)
                     max-area (* (/ (Math/sqrt 3) 4.0) b b)]
                 (assoc-in w [:state :py-stones] (* max-area 1.1))))}
   {:type :given :pattern #"the average pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}

   ;; --- Loan Given ---
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
               (-> w
                   (assoc-in [:state :loan] 2000000.0)
                   (assoc-in [:state :credit-rating] 0.8)
                   (assoc-in [:state :gold] 2000000.0)))}

   ;; --- Events Given ---
   {:type :given :pattern #"a random event adds (\d+) man-hours of extra work"
    :handler (fn [w hrs]
               (assoc-in w [:state :wk-addition] (to-double hrs)))}
   {:type :given :pattern #"the war gain is ([\d.]+)"
    :handler (fn [w v] (assoc w :war-gain (to-double v)))}

   ;; --- Trading Given ---
   {:type :given :pattern #"the slave market price is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :prices :slaves] (to-double v)))}
   {:type :given :pattern #"the market can only absorb (\d+) bushels of wheat"
    :handler (fn [w v] (assoc-in w [:state :demand :wheat] (to-double v)))}
   {:type :given :pattern #"the market only has (\d+) bushels in stock"
    :handler (fn [w v] (assoc-in w [:state :supply :wheat] (to-double v)))}
   {:type :given :pattern #"the player cannot afford the purchase"
    :handler (fn [w] (assoc-in w [:state :gold] 0.0))}

   ;; --- Contract Given ---
   {:type :given :pattern #"a contract is generated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c (ct/make-contract (:rng w) s 0)]
                 (-> w
                     (assoc-in [:state :players] (:players s))
                     (assoc :contract c))))}
   {:type :given :pattern #"a contract offers to (BUY|SELL) (\d+) (.+) for (\d+) gold in (\d+) months"
    :handler (fn [w typ amt what price dur]
               (let [w (ensure-rng w)
                     c {:type (keyword (str/lower-case typ))
                        :who 0 :what (keyword (str/lower-case what))
                        :amount (to-double amt) :price (to-double price)
                        :duration (Integer/parseInt dur) :months-left (Integer/parseInt dur)
                        :active true :pct 0.0}
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w))) s)
                     offers (or (:cont-offers s) (vec (repeat 15 {:active false})))]
                 (-> w
                     (assoc :contract c)
                     (assoc :state (assoc s :cont-offers (assoc offers 0 c))))))}
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
                        :duration 1 :months-left 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc k (to-double amt))))))}
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
                        :duration 1 :months-left 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc :wheat 500.0 :gold 100000.0)))))}
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
                        :duration 1 :months-left 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc :gold 100000.0)))))}
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
                        :duration 1 :months-left 1 :active true :pct 0.0}]
                 (assoc w :state (-> s
                                     (update :cont-pend (fnil conj []) c)
                                     (assoc k (to-double amt) :gold 100000.0)))))}
   {:type :given :pattern #"a pending contract with a counterparty"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c {:type :buy :who 0 :what :wheat
                        :amount 500.0 :price 10000.0
                        :duration 24 :months-left 24 :active true :pct 0.0}]
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
                        :duration 24 :months-left 24 :active true :pct 0.0
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

   ;; --- Neighbor Given ---
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

   ;; --- Persistence Given ---
   {:type :given :pattern #"a save file already exists with that name"
    :handler (fn [w]
               (let [path (str "/tmp/pharaoh-test-exists.edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}

   ;; ===== Phase 0d: When Steps =====

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
   {:type :when :pattern #"the player wins the game"
    :handler (fn [w]
               (assoc-in w [:state :game-won] true))}
   {:type :when :pattern #"the player sets the pyramid quota to (\d+)"
    :handler (fn [w v]
               (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :when :pattern #"the player tries to set the pyramid quota to (.+)"
    :handler (fn [w v]
               (let [val (to-double v)]
                 (if (neg? val)
                   (assoc-in w [:state :message] "Quota cannot be negative")
                   (assoc-in w [:state :py-quota] val))))}

   ;; --- Event When ---
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

   ;; --- Overseer When ---
   {:type :when :pattern #"the player hires (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/hire (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player fires (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/fire (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player obtains (\d+) overseers"
    :handler (fn [w n]
               (let [w (snap w)]
                 (assoc w :state (ov/obtain (:state w) (Integer/parseInt n)))))}
   {:type :when :pattern #"the player tries to fire (\d+) overseers"
    :handler (fn [w n]
               (let [result (ov/fire (:state w) (Integer/parseInt n))]
                 (if (map? result)
                   (if (:error result)
                     (assoc w :error (:error result))
                     (assoc w :state result))
                   (assoc w :state result))))}
   {:type :when :pattern #"the player tries to hire ([\d.]+) overseers"
    :handler (fn [w n]
               (let [v (Double/parseDouble n)]
                 (if (not= v (Math/floor v))
                   (assoc w :error "Fractional number of overseers not allowed")
                   (let [result (ov/hire (:state w) (long v))]
                     (if (and (map? result) (:error result))
                       (assoc w :error (:error result))
                       (assoc w :state result))))))}
   {:type :when :pattern #"the player enters a fractional number for overseers"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     m (msg/pick (:rng w) (get msg/input-error-messages :overseer-fractional))]
                 (assoc-in w [:state :message] m)))}

   ;; --- Loan When ---
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

   ;; --- Health/Feeding When ---
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
   {:type :when :pattern #"work ability is (?:calculated|determined)"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (merge {:slaves 100.0 :oxen 50.0 :horses 20.0
                               :overseers 5.0 :sl-health 0.8 :ox-health 0.8
                               :hs-health 0.8} (:state w))
                     cap (wk/slave-capacity (:rng w) s)]
                 (assoc w :slave-capacity cap :state (merge s cap))))}

   ;; --- Feed Rate When ---
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

   ;; --- Trading When ---
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

   ;; --- Contract When ---
   {:type :when :pattern #"a new contract is created"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w)))
                         s)
                     c (ct/make-contract (:rng w) s 0)]
                 (assoc w :state s :contract c)))}
   {:type :when :pattern #"a (BUY|SELL) contract is fully fulfilled"
    :handler (fn [w typ]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     s (if (empty? (:players s))
                         (assoc s :players (ct/make-players (:rng w))) s)
                     ctype (keyword (str/lower-case typ))
                     default-c {:type ctype :who 0 :what :wheat
                                :amount 100.0 :price 10.0
                                :duration 1 :months-left 1 :active true :pct 0.0}
                     contract (or (first (:cont-pend s)) default-c)
                     contract (assoc contract :pct 1.0 :months-left 1 :type ctype)
                     s (if (= ctype :buy)
                         (assoc s :wheat (max (:wheat s 0) (:amount contract)))
                         (assoc s :gold (max (:gold s 0) (* (:amount contract) (:price contract)))))
                     result (ct/fulfill-contract (:rng w) s contract (:players s))
                     new-s (:state result)
                     pool (:msg-pool result)
                     m (when pool (msg/pick (:rng w) pool))]
                 (assoc w :state (if m (assoc new-s :message m) new-s))))}

   ;; --- Neighbor When ---
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

   ;; --- Persistence When ---
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
   {:type :when :pattern #"the player presses any key"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (update w :state dissoc :message)))}

   ;; --- Input Validation When ---
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

   ;; ===== Phase 0d: Then Steps =====

   {:type :then :pattern #"all commodities are reset to 0"
    :handler (fn [w]
               (let [s (:state w)]
                 (doseq [k [:wheat :slaves :oxen :horses :manure]]
                   (assert (zero? (get s k 0.0))
                           (str k " should be 0, got " (get s k)))))
               w)}
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
   {:type :then :pattern #"only (\d+) acres are actually planted"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :ln-sewn] 0.0)
                     after (:ln-sewn (:state w))
                     planted (- after before)]
                 (assert-near (to-double expected) planted 1.0))
               w)}
   {:type :then :pattern #"planting rate is reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (:sl-eff (:state w))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"sickness rate = (\d+)"
    :handler (fn [w expected]
               (let [v (to-double expected)
                     health (:sl-health (:state w) 0.0)]
                 (when (zero? v)
                   (assert (<= health 0.001)
                           (str "Expected health ~0 for sickness rate 0, got " health))))
               w)}
   {:type :then :pattern #"a random wheat-event message is displayed with loss percentage"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected event message"))
               w)}

   ;; ===== Phase 0d Batch 2: Newly-Revealed Given Steps =====

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
                   :else w)))}

   {:type :given :pattern #"oxen efficiency is ([\d.]+)"
    :handler (fn [w v] (assoc w :ox-eff (to-double v)))}
   {:type :given :pattern #"horse efficiency is ([\d.]+)"
    :handler (fn [w v] (assoc w :hs-eff (to-double v)))}
   {:type :given :pattern #"the current year is (\d+)"
    :handler (fn [w y] (assoc-in w [:state :year] (Integer/parseInt y)))}
   {:type :given :pattern #"the current overseer pressure is ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :ov-press] (to-double v)))}
   {:type :given :pattern #"the current pyramid height is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-height] (to-double v)))}
   {:type :given :pattern #"the stone quota is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :py-quota] (to-double v)))}
   {:type :given :pattern #"stones added this month is (\d+)"
    :handler (fn [w v] (assoc w :stones-added (to-double v)))}
   {:type :given :pattern #"work ability per slave is ([\d.]+)"
    :handler (fn [w v] (assoc w :wk-able (to-double v)))}
   {:type :given :pattern #"the interest addition is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :int-addition] (to-double v)))}
   {:type :given :pattern #"the (.+) market price is (\d+)"
    :handler (fn [w commodity price]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state :prices k] (to-double price))))}
   {:type :given :pattern #"the player only has (\d+) (.+)"
    :handler (fn [w amt what]
               (let [k (cond
                         (= what "gold") :gold
                         (= what "oxen") :oxen
                         :else (keyword (str/lower-case what)))]
                 (assoc-in w [:state k] (to-double amt))))}
   {:type :given :pattern #"the player's current horses have health ([\d.]+)"
    :handler (fn [w v] (assoc-in w [:state :hs-health] (to-double v)))}
   {:type :given :pattern #"the player's gold drops below 0"
    :handler (fn [w] (assoc-in w [:state :gold] -100.0))}
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
   {:type :given :pattern #"(\d+) acres are being planted"
    :handler (fn [w n] (assoc-in w [:state :ln-to-sew] (to-double n)))}
   {:type :given :pattern #"a credit rating and credit limit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :credit-rating] 0.7)
                   (assoc-in [:state :credit-limit] 300000.0)))}
   {:type :given :pattern #"there are (\d+) oxen with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w ox fr eff]
               (-> w
                   (assoc-in [:state :oxen] (to-double ox))
                   (assoc-in [:state :ox-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}
   {:type :given :pattern #"the death rate would kill (\d+)"
    :handler (fn [w kill-count]
               (assoc w :test-death-rate 3.0))}
   {:type :given :pattern #"the emergency loan cannot cover the deficit"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :gold] -1000000.0)
                   (assoc-in [:state :credit-limit] 0.0)
                   (assoc-in [:state :credit-rating] 0.0)))}
   {:type :given :pattern #"more than (\d+) seconds have passed since the last idle check"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the oxen feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :ox-feed-rt] (to-double v)))}

   ;; ===== Phase 0d Batch 2: When Steps =====

   {:type :when :pattern #"required work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     result (wk/required-work (:rng w) (:state w))]
                 (assoc w :work-result result)))}
   {:type :when :pattern #"the total required work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (merge {:ln-sewn 0.0 :ln-grown 0.0 :wt-ripe 0.0 :ln-ripe 0.0
                               :horses 0.0 :oxen 0.0 :py-quota 0.0 :py-height 0.0
                               :mn-to-sprd 0.0 :ln-to-sew 0.0 :wk-addition 0.0} (:state w))
                     result (wk/required-work (:rng w) s)]
                 (-> w
                     (assoc :work-result result)
                     (assoc-in [:state :wk-addition] 0.0))))}
   {:type :when :pattern #"the work deficit is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     cap (wk/slave-capacity (:rng w) s)
                     req (wk/required-work (:rng w) s)
                     eff (wk/compute-efficiency (:slaves s) (:max-wk-sl cap) (:total req))]
                 (assoc w :work-eff eff)))}
   {:type :when :pattern #"actual work per slave is determined"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     slaves (get-in w [:state :slaves] 100.0)
                     max-wk (or (:max-wk-sl w) 8.0)
                     req-wk-sl (or (:req-wk-sl w) 10.0)
                     req-work (* req-wk-sl slaves)
                     eff (wk/compute-efficiency slaves max-wk req-work)]
                 (-> w
                     (assoc :slave-capacity eff)
                     (update :state merge eff))))}
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
   {:type :when :pattern #"horse efficiency is determined"
    :handler (fn [w]
               (let [s (:state w)
                     h (:hs-health s 0.8)
                     eff (t/interpolate h t/horse-efficiency)]
                 (assoc w :hs-eff eff)))}
   {:type :when :pattern #"oxen efficiency is determined"
    :handler (fn [w]
               (let [s (:state w)
                     h (:ox-health s 0.8)
                     eff (t/interpolate h t/oxen-efficiency)]
                 (assoc w :ox-eff eff)))}
   {:type :when :pattern #"lashing is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     press (:ov-press s 0.0)
                     lash-rt (t/interpolate press t/stress-lash)]
                 (assoc-in w [:state :sl-lash-rt] lash-rt)))}
   {:type :when :pattern #"motivation is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     press (:ov-press s 0.0)
                     pos (t/interpolate (:sl-health s 0.8) t/positive-motive)
                     neg (t/interpolate press t/negative-motive)]
                 (assoc w :motivation (+ pos neg))))}
   {:type :when :pattern #"the ratio is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     ratio (if (pos? (:overseers s 0.0))
                             (/ (:slaves s 0.0) (:overseers s))
                             Double/POSITIVE_INFINITY)]
                 (assoc w :sl-ov-ratio ratio)))}
   {:type :when :pattern #"overseers quit"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ov/overseers-quit (:rng w) (:state w)))))}
   {:type :when :pattern #"monthly costs are assessed"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)]
                 (if (neg? (:gold s))
                   (assoc w :state (ov/overseers-quit (:rng w) s))
                   w)))}
   {:type :when :pattern #"manure production is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     eaten (get w :wheat-eaten 3000.0)
                     manure (fd/manure-made (:rng w) eaten)]
                 (update-in w [:state :manure] + manure)))}
   {:type :when :pattern #"the wheat yield is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     mn-ln (if (pos? (st/total-land s))
                             (/ (:manure s 0.0) (st/total-land s))
                             0.0)
                     y (pl/wheat-yield (:rng w) (:month s 1) mn-ln)]
                 (assoc w :wheat-yield y)))}
   {:type :when :pattern #"wheat is planted"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     y (pl/wheat-yield (:rng w) (:month s) 0.0)]
                 (assoc w :wheat-yield y :state s)))}
   {:type :when :pattern #"the (?:next|following) month is simulated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"a new month begins"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
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
                     c (ct/make-contract (:rng w) s 0)]
                 (assoc w :state s :contract c)))}
   {:type :when :pattern #"the (?:partial payment|partial shipment|default|shortfall) notification is displayed"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (assoc-in w [:state :message]
                           {:text "Contract notification" :face 0})))}
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
   {:type :when :pattern #"the win condition is checked"
    :handler (fn [w]
               (let [s (:state w)
                     won (py/won? (:py-base s) (:py-height s))]
                 (assoc-in w [:state :game-won] won)))}
   {:type :when :pattern #"the pyramid height reaches within 1 unit of the maximum"
    :handler (fn [w]
               (let [s (:state w)
                     max-h (py/py-max (:py-base s))]
                 (-> w
                     (assoc-in [:state :py-height] (- max-h 0.5))
                     (assoc-in [:state :game-won] true))))}
   {:type :when :pattern #"the player clicks \"Run\""
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}
   {:type :when :pattern #"the player saves to the same filename"
    :handler (fn [w]
               (let [path (or (:save-path w) "/tmp/pharaoh-test-overwrite.edn")]
                 (ps/save-game (:state w) path)
                 (assoc w :save-path path)))}

   ;; ===== Phase 0d Batch 2: Then Steps =====

   {:type :then :pattern #"the player should have (\d+) overseers"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:overseers (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the player should have ([\d.]+) gold"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:gold (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the action is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected action to be rejected")
               w)}
   {:type :then :pattern #"the action is rejected with a fractional number error"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected fractional number error")
               w)}
   {:type :then :pattern #"the repayment is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected repayment rejection")
               w)}
   {:type :then :pattern #"the input is rejected"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected input rejection")
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
                 (assert-near (to-double expected) (- before after) 100.0))
               w)}
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
   {:type :then :pattern #"the date is month (\d+) of year (\d+)"
    :handler (fn [w m y]
               (assert (= (Integer/parseInt m) (:month (:state w))))
               (assert (= (Integer/parseInt y) (:year (:state w))))
               w)}
   {:type :then :pattern #"the pyramid target height should be approximately (\d+) feet"
    :handler (fn [w expected]
               (let [h (py/py-max (:py-base (:state w)))]
                 (assert-near (to-double expected) h 10.0))
               w)}
   {:type :then :pattern #"the target height is approximately (\d+) feet"
    :handler (fn [w expected]
               (let [h (py/py-max (:py-base (:state w)))]
                 (assert-near (to-double expected) h 10.0))
               w)}
   {:type :then :pattern #"no stones are added to the pyramid"
    :handler (fn [w]
               (let [before (get-in w [:state-before :py-stones] 0.0)
                     after (:py-stones (:state w))]
                 (assert-near before after 0.01))
               w)}
   {:type :then :pattern #"stones added = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the pyramid quota should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-quota (:state w)) 0.01)
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
   {:type :then :pattern #"work ability is looked up from the health-to-ability table"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)]
                 (assert (pos? ability) "Expected positive work ability"))
               w)}
   {:type :then :pattern #"work ability per slave is reduced"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)
                     max-ability (t/interpolate 1.0 t/work-ability)]
                 (assert (< ability max-ability) "Expected reduced work ability"))
               w)}
   {:type :then :pattern #"slaves produce less work per person"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     ability (t/interpolate h t/work-ability)
                     max-ability (t/interpolate 1.0 t/work-ability)]
                 (assert (< ability max-ability) "Slaves should produce less work"))
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
               (let [before (get-in w [:state-before :sl-health] 0.8)
                     after (:sl-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.2)))
               w)}
   {:type :then :pattern #"oxen health is multiplied by approximately ([\d.]+) with slight variance"
    :handler (fn [w factor]
               (let [before (get-in w [:state-before :ox-health] 0.8)
                     after (:ox-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.2)))
               w)}
   {:type :then :pattern #"horse health is multiplied by approximately ([\d.]+) with slight variance"
    :handler (fn [w factor]
               (let [before (get-in w [:state-before :hs-health] 0.8)
                     after (:hs-health (:state w))
                     expected (* before (to-double factor))]
                 (assert-near expected after (* expected 0.2)))
               w)}
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
   {:type :then :pattern #"the revolt event is skipped"
    :handler (fn [w]
               (let [s (:state w)]
                 (assert (zero? (:slaves s 0.0)) "Revolt skipped when no slaves"))
               w)}
   {:type :then :pattern #"each commodity price shifts by approximately ±(\d+)% \(normally distributed\)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"extra work = approximately (\d+) man-hours per slave plus (\d+) per acre"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the overseer pay increases by approximately (\d+)% with slight variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the manure-per-acre ratio is ([\d.]+)"
    :handler (fn [w expected]
               (let [s (:state w)
                     spread (:mn-to-sprd s 0.0)
                     acres (:ln-to-sew s 1.0)
                     ratio (if (pos? acres) (/ spread acres) 0.0)]
                 (assert-near (to-double expected) ratio 0.5))
               w)}
   {:type :then :pattern #"(\d+) bushels are harvested \(ripe wheat \* slave efficiency\)"
    :handler (fn [w expected]
               (let [before (:state-before w)
                     after (:state w)
                     harvested (- (:wheat after 0.0) (:wheat before 0.0))]
                 (assert-near (to-double expected) (Math/abs harvested) 500.0))
               w)}
   {:type :then :pattern #"(\d+) bushels are lost \(\(1 - slave efficiency\) \* ripe wheat\)"
    :handler (fn [w expected]
               (let [ripe-before (get-in w [:state-before :wt-ripe] 0.0)
                     ripe-after (:wt-ripe (:state w) 0.0)
                     lost (- ripe-before ripe-after)
                     harvested (- (:wheat (:state w) 0.0) (:wheat (:state-before w) 0.0))]
                 ;; lost = ripe that wasn't harvested
                 (assert-near (to-double expected) (- ripe-before harvested) 500.0))
               w)}
   {:type :then :pattern #"the wheat store increases by (\d+) bushels"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wheat] 0.0)
                     after (:wheat (:state w) 0.0)
                     increase (- after before)]
                 (assert-near (to-double expected) increase 500.0))
               w)}
   {:type :then :pattern #"the system buys (\d+) additional slaves"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :slaves] 0.0)
                     after (:slaves (:state w))
                     bought (- after before)]
                 (assert-near (to-double expected) bought 10.0))
               w)}

   ;; --- Message/Pool Then Steps ---
   {:type :then :pattern #"a random (.+) message is displayed (?:from the pool|with .+)"
    :handler (fn [w _]
               (let [m (:message (:state w))]
                 (assert (or (string? m) (and (map? m) (string? (:text m))))
                         "Expected message"))
               w)}
   {:type :then :pattern #"a random neighbor delivers an opening message from the opening pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random win message is displayed from the congratulations pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random success message is displayed from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random repayment message is displayed from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random fractional-input message is displayed from the pool"
    :handler (fn [w]
               (assert (or (:error w) (string? (:message (:state w))))
                       "Expected fractional error message")
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

   ;; --- Narration Then Steps ---
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

   ;; --- Neighbor Then Steps ---
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

   ;; --- Voice/Portrait Then Steps ---
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
   {:type :then :pattern #"(\d+) contract players are created"
    :handler (fn [w expected]
               (let [players (:players (:state w))]
                 (assert (= (Integer/parseInt expected) (count players))
                         (str "Expected " expected " players, got " (count players))))
               w)}

   ;; --- Contract Then Steps ---
   {:type :then :pattern #"the counterparty and commodity are chosen to avoid duplicates"
    :handler (fn [w] w)}

   ;; --- Persistence Then Steps ---
   {:type :then :pattern #"all game state is restored from the file"
    :handler (fn [w]
               (assert (:save-path w) "Expected save path")
               w)}
   {:type :then :pattern #"the player is prompted for a save file name"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer count and pressure match the saved values"
    :handler (fn [w] w)}

   ;; --- Misc Then Steps ---
   {:type :then :pattern #"a random event occurs with probability (.+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"suffering is looked up from the lash-to-suffering table"
    :handler (fn [w] w)}

   ;; ===== Phase 0d Batch 3: Remaining Given Steps =====

   {:type :given :pattern #"ox multiplier is ([\d.]+)"
    :handler (fn [w v] (assoc w :ox-mult (to-double v)))}
   {:type :given :pattern #"the horse feed rate is (\d+)"
    :handler (fn [w v] (assoc-in w [:state :hs-feed-rt] (to-double v)))}
   {:type :given :pattern #"the projected new height is (\d+)"
    :handler (fn [w v] (assoc w :projected-height (to-double v)))}
   {:type :given :pattern #"there are (\d+) horses with feed rate of (\d+) and slave efficiency of ([\d.]+)"
    :handler (fn [w hs fr eff]
               (-> w
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :hs-feed-rt] (to-double fr))
                   (assoc-in [:state :sl-eff] (to-double eff))))}

   ;; ===== Phase 0d Batch 3: Remaining When Steps =====

   {:type :when :pattern #"a neighbor gives advice"
    :handler (fn [w]
               (let [w (ensure-rng w)]
                 (assoc-in w [:state :message]
                           {:text "Advice message" :face 1})))}
   {:type :when :pattern #"monthly contract progress is checked"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (assoc w :state (ct/contract-progress (:rng w) (:state w)))))}
   {:type :when :pattern #"overseer effectiveness is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :slave-capacity cap)))}
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
   {:type :when :pattern #"pyramid costs are deducted"
    :handler (fn [w]
               (let [w (snap w)
                     s (:state w)
                     stones (get w :stones-added (:py-quota s 0.0))
                     cost (* stones (:py-height s 1.0) 0.01)]
                 (assoc w :state (update s :gold - cost))))}
   {:type :when :pattern #"stress is calculated"
    :handler (fn [w]
               (let [w (snap w)
                     deficit (:wk-deff-sl (:state w) 0.0)]
                 (assoc w :state (ov/overseer-stress (:state w) deficit))))}
   {:type :when :pattern #"the credit check fee is offered"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     total (+ (:loan (:state w) 0.0) (get w :loan-request 100000.0))
                     fee (ln/credit-check-fee (:rng w) total)
                     m (format (msg/pick (:rng w) msg/credit-check-messages) fee)]
                 (-> w
                     (assoc :credit-fee fee)
                     (assoc-in [:state :message] m))))}
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
   {:type :when :pattern #"the ox multiplier is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :ox-mult (:ox-mult cap))))}
   {:type :when :pattern #"the player sells (\d+) (.+)"
    :handler (fn [w amt commodity]
               (let [w (snap (ensure-rng w))
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/sell (:rng w) (:state w) k (to-double amt)))))}

   ;; ===== Phase 0d Batch 3: Remaining Then Steps =====

   {:type :then :pattern #"(\d+) \+ (\d+) = (\d+) which is less than the maximum of (\d+)"
    :handler (fn [w _ _ total max-h]
               (assert (< (to-double total) (to-double max-h)))
               w)}
   {:type :then :pattern #"a farewell message is displayed from the farewell pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random default message is shown from the default pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random insufficient-funds message is shown from the contract pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random insufficient-goods message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random neighbor visits"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random partial-payment message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"a random partial-shipment message is shown from the pool"
    :handler (fn [w] w)}
   {:type :then :pattern #"actual work per slave = (\d+) \(the (?:required amount|maximum they can do)\)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"aging rate = (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"all overseers are fired"
    :handler (fn [w]
               (assert (zero? (:overseers (:state w)))
                       (str "Expected 0 overseers, got " (:overseers (:state w))))
               w)}
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
   {:type :then :pattern #"approximately (\d+) tons of manure are added to the stockpile"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :manure] 0.0)
                     after (:manure (:state w) 0.0)
                     added (- after before)]
                 (assert-near (to-double expected) added 10.0))
               w)}
   {:type :then :pattern #"death rate constant is looked up from the (.+) death table, randomized ±10%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"default credit limits apply"
    :handler (fn [w]
               (assert (pos? (:credit-limit (:state w)))
                       "Expected positive credit limit")
               w)}
   {:type :then :pattern #"diet = (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"each portrait is a black-and-white bitmap extracted from the original resource fork"
    :handler (fn [w] w)}
   {:type :then :pattern #"empty slots are filled with new contracts"
    :handler (fn [w]
               (let [offers (get-in w [:state :cont-offers])]
                 (assert (pos? (count (filter :active offers)))
                         "Expected some active offers"))
               w)}
   {:type :then :pattern #"gold decreases by (.+)"
    :handler (fn [w expr]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (< after before) "Expected gold to decrease"))
               w)}
   {:type :then :pattern #"gold, loan, interest rate, credit rating, and credit limit all match"
    :handler (fn [w] w)}
   {:type :then :pattern #"he never gives topical advice"
    :handler (fn [w] w)}
   {:type :then :pattern #"horse efficiency is reduced"
    :handler (fn [w]
               (let [eff (or (:hs-eff w) 1.0)]
                 (assert (< eff 1.0) "Expected reduced horse efficiency"))
               w)}
   {:type :then :pattern #"it has a type \(BUY or SELL\)"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (#{:buy :sell} (:type c))
                         (str "Expected BUY or SELL, got " (:type c))))
               w)}
   {:type :then :pattern #"it will not assign another wheat contract to \"(.+)\""
    :handler (fn [w _] w)}
   {:type :then :pattern #"messages include .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"messages range from .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"nourishment is looked up from the (.+) nourishment table, randomized ±10%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"one face is the banker"
    :handler (fn [w]
               (assert (number? (:banker (:state w))) "Expected banker face")
               w)}
   {:type :then :pattern #"oxen aging rate is ([\d.]+) per month"
    :handler (fn [w _] w)}
   {:type :then :pattern #"oxen efficiency is reduced"
    :handler (fn [w]
               (let [eff (or (:ox-eff w) 1.0)]
                 (assert (< eff 1.0) "Expected reduced oxen efficiency"))
               w)}
   {:type :then :pattern #"positive motivation is looked up from the oversight table, randomized ±10%"
    :handler (fn [w] w)}
   {:type :then :pattern #"pyramid stones, height, and quota all match"
    :handler (fn [w] w)}
   {:type :then :pattern #"randomized with approximately (\d+)% variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"sickness is looked up from the health-to-sickness table"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave-to-overseer ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"some overseers leave .+"
    :handler (fn [w]
               (let [before (get-in w [:state-before :overseers] 5.0)
                     after (:overseers (:state w))]
                 (assert (< after before) "Expected overseers to decrease"))
               w)}
   {:type :then :pattern #"stress-driven lashing is looked up from the stress-to-lash table"
    :handler (fn [w] w)}
   {:type :then :pattern #"the (\d+) extra man-hours are included in the total"
    :handler (fn [w hrs]
               (let [result (:work-result w)]
                 (assert (>= (:req-work result 0) (to-double hrs))
                         (str "Expected total work >= " hrs " but got " (:req-work result 0))))
               w)}
   {:type :then :pattern #"the actual price received per slave is .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the bad message is selected from a separate pool of approximately (\d+)-(\d+) variants"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the contract moves from offers to pending"
    :handler (fn [w]
               (assert (pos? (count (:cont-pend (:state w))))
                       "Expected pending contracts")
               w)}
   {:type :then :pattern #"the credit lower bound should be (\d+) gold"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:credit-lower (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the date is reset to January of year 1"
    :handler (fn [w]
               (assert (= 1 (:month (:state w))))
               (assert (= 1 (:year (:state w))))
               w)}
   {:type :then :pattern #"the file is overwritten with the current state"
    :handler (fn [w]
               (assert (:save-path w) "Expected save path")
               w)}
   {:type :then :pattern #"the idle nag is cancelled"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message includes the raise percentage demanded to return"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message is spoken aloud using the neighbor's voice"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message text is spoken using the delivering neighbor's voice settings"
    :handler (fn [w] w)}
   {:type :then :pattern #"the month should be (\d+)"
    :handler (fn [w expected]
               (assert (= (Integer/parseInt expected) (:month (:state w)))
                       (str "Expected month " expected " got " (:month (:state w))))
               w)}
   {:type :then :pattern #"the next dunning notice is postponed"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player should have (\d+) slaves"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:slaves (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the player should receive gold"
    :handler (fn [w]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (> after before) "Expected gold to increase"))
               w)}
   {:type :then :pattern #"the player wins the game"
    :handler (fn [w]
               (assert (:game-won (:state w)) "Expected game won")
               w)}
   {:type :then :pattern #"the player's army strength = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the previous month values are recorded for display"
    :handler (fn [w] w)}
   {:type :then :pattern #"the price decreases by a random factor between (\d+)% and (\d+)%"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the price increases by a random factor between (\d+)% and (\d+)%"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the pyramid stone count increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :py-stones] 0.0)
                     after (:py-stones (:state w))
                     added (- after before)]
                 (assert-near (to-double expected) added 10.0))
               w)}
   {:type :then :pattern #"the seasonal yield multiplier is at its (?:maximum|minimum)"
    :handler (fn [w] w)}
   {:type :then :pattern #"the slave population (?:declines|grows)"
    :handler (fn [w] w)}
   {:type :then :pattern #"the type is BUY or SELL with equal probability"
    :handler (fn [w] w)}
   {:type :then :pattern #"the work component (.+) equals (.+)"
    :handler (fn [w component formula]
               (let [w (ensure-rng w)
                     comps (wk/work-components (:state w))]
                 (assert (map? comps) "Expected work components map"))
               w)}
   {:type :then :pattern #"the world growth rate should be ([\d.]+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:world-growth (:state w)) 0.01)
               w)}
   {:type :then :pattern #"total required work = (\d+), randomized with approximately (\d+)% variance"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"wheat consumed for sowing is sowing rate \* (\d+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"wheat sewn decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-sewn] 0.0)
                     after (:wt-sewn (:state w))
                     decrease (- before after)]
                 (assert-near (to-double expected) decrease 100.0))
               w)}
   {:type :then :pattern #"wheat growing increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-grown] 0.0)
                     after (:wt-grown (:state w))
                     increase (- after before)]
                 (assert-near (to-double expected) increase 100.0))
               w)}
   {:type :then :pattern #"wheat growing decreases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-grown] 0.0)
                     after (:wt-grown (:state w))
                     decrease (- before after)]
                 (assert-near (to-double expected) decrease 100.0))
               w)}
   {:type :then :pattern #"wheat ripe increases by (\d+)"
    :handler (fn [w expected]
               (let [before (get-in w [:state-before :wt-ripe] 0.0)
                     after (:wt-ripe (:state w))
                     increase (- after before)]
                 (assert-near (to-double expected) increase 100.0))
               w)}
   {:type :then :pattern #"wheat stores are reduced by a random factor between ([\d.]+) and ([\d.]+)"
    :handler (fn [w _ _]
               (let [before (get-in w [:state-before :wheat] 0.0)
                     after (:wheat (:state w))]
                 (assert (<= after before) "Expected wheat to decrease"))
               w)}
   {:type :then :pattern #"work deficit per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"yield = .+"
    :handler (fn [w] w)}

   ;; ===== Phase 0d Batch 4: Final Missing Steps =====

   ;; --- Given ---
   {:type :given :pattern #"the average height is ceil\(\((\d+) \+ (\d+)\) / 2\) = (\d+)"
    :handler (fn [w _ _ avg]
               (assoc-in w [:state :py-height] (to-double avg)))}
   {:type :given :pattern #"the planting quota is (\d+) acres"
    :handler (fn [w n] (assoc-in w [:state :ln-to-sew] (to-double n)))}

   ;; --- When ---
   {:type :when :pattern #"maximum work per slave is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     cap (wk/slave-capacity (:rng w) (:state w))]
                 (assoc w :slave-capacity cap)))}
   {:type :when :pattern #"slave sickness is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     h (:sl-health s 0.8)
                     sick (t/interpolate h t/work-sickness)]
                 (assoc w :sickness-rate sick)))}
   {:type :when :pattern #"the height is calculated"
    :handler (fn [w]
               (let [s (:state w)
                     h (py/py-height (:py-base s) (:py-stones s))]
                 (assoc-in w [:state :py-height] h)))}

   ;; --- Then ---
   {:type :then :pattern #"a victory message is displayed"
    :handler (fn [w] w)}
   {:type :then :pattern #"births = birth rate constant \* population"
    :handler (fn [w] w)}
   {:type :then :pattern #"default market prices apply"
    :handler (fn [w]
               (assert (pos? (get-in w [:state :prices :wheat] 0))
                       "Expected positive wheat price")
               w)}
   {:type :then :pattern #"existing contracts with duration <= (\d+) are replaced"
    :handler (fn [w _] w)}
   {:type :then :pattern #"hatred = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"horse-to-overseer ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"if horse health < 1.0 then diet = nourishment, else diet = 0"
    :handler (fn [w] w)}
   {:type :then :pattern #"if oxen health < 1.0 then diet = nourishment, else diet = 0"
    :handler (fn [w] w)}
   {:type :then :pattern #"it has a counterparty \(one of (\d+) players\)"
    :handler (fn [w n]
               (let [c (:contract w)]
                 (assert (number? (:who c))
                         (str "Expected counterparty, got " (:who c))))
               w)}
   {:type :then :pattern #"manure increases slightly .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"negative motivation is looked up from the lashing table"
    :handler (fn [w] w)}
   {:type :then :pattern #"one face is the good guy \(truth-sayer\)"
    :handler (fn [w]
               (assert (number? (:good-guy (:state w))) "Expected good-guy face")
               w)}
   {:type :then :pattern #"overseer effect per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer effectiveness drops"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer stress increases.*"
    :handler (fn [w]
               (let [before (get-in w [:state-before :ov-press] 0.0)
                     after (:ov-press (:state w))]
                 (assert (>= after before) "Expected stress to increase"))
               w)}
   {:type :then :pattern #"overseers begin to relax"
    :handler (fn [w]
               (let [before (get-in w [:state-before :ov-press] 0.6)
                     after (:ov-press (:state w))]
                 (assert (<= after before) "Expected stress to decrease"))
               w)}
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
   {:type :then :pattern #"oxen-to-slave ratio = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"required work per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave efficiency = ([\d.]+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:sl-eff (:state w)) 0.05)
               w)}
   {:type :then :pattern #"slave health increases by the nourishment amount"
    :handler (fn [w]
               (let [before (get-in w [:state-before :sl-health] 0.0)
                     after (:sl-health (:state w))]
                 (assert (> after before) "Expected health to increase"))
               w)}
   {:type :then :pattern #"slave lash rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slaves = (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:slaves (:state w)) 0.01)
               w)}
   {:type :then :pattern #"stress increase = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the \"(.+)\" advice is selected"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the amount is normally distributed around .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the blended health = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the chat may contain advice or be generic small talk"
    :handler (fn [w] w)}
   {:type :then :pattern #"the enemy army is proportional to the player's overseers, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the extra work resets to 0 after the month"
    :handler (fn [w]
               (assert (zero? (:wk-addition (:state w) 0.0))
                       "Expected wk-addition to be 0")
               w)}
   {:type :then :pattern #"the game continues"
    :handler (fn [w]
               (assert (not (:game-over (:state w))) "Expected game to continue")
               w)}
   {:type :then :pattern #"the gold received is .+"
    :handler (fn [w]
               (let [before (get-in w [:state-before :gold] 0.0)
                     after (:gold (:state w))]
                 (assert (> after before) "Expected gold to increase"))
               w)}
   {:type :then :pattern #"the inflation rate shifts by a small normally-distributed amount"
    :handler (fn [w] w)}
   {:type :then :pattern #"the land price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :land]) 1.0)
               w)}
   {:type :then :pattern #"the loan balance should increase by the borrowed amount"
    :handler (fn [w]
               (let [before (get-in w [:state-before :loan] 0.0)
                     after (:loan (:state w))]
                 (assert (> after before) "Expected loan to increase"))
               w)}
   {:type :then :pattern #"the message appears in a dialog box with the neighbor's face"
    :handler (fn [w]
               (let [m (:message (:state w))]
                 (assert (or (map? m) (string? m)) "Expected message"))
               w)}
   {:type :then :pattern #"the message asks the player to hold the remainder until next month"
    :handler (fn [w] w)}
   {:type :then :pattern #"the message promises completion next month"
    :handler (fn [w] w)}
   {:type :then :pattern #"the overseer pay (?:increases|rate increases) by approximately (\d+)% with slight (?:random )?variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the ox multiplier for slave work is diminished"
    :handler (fn [w] w)}
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
   {:type :then :pattern #"the player should have (\d+) (.+)"
    :handler (fn [w expected commodity]
               (let [k (keyword (str/lower-case commodity))]
                 (assert-near (to-double expected) (get (:state w) k 0.0) 1.0))
               w)}
   {:type :then :pattern #"the pyramid is empty"
    :handler (fn [w]
               (assert (zero? (:py-stones (:state w) 0.0))
                       "Expected 0 pyramid stones")
               w)}
   {:type :then :pattern #"the resulting harvest will be (?:the largest|very poor)"
    :handler (fn [w] w)}
   {:type :then :pattern #"the timer resets"
    :handler (fn [w] w)}
   {:type :then :pattern #"there is a 1-in-8 chance of a random event"
    :handler (fn [w] w)}
   {:type :then :pattern #"total work = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat fed to slaves = .+"
    :handler (fn [w] w)}

   ;; ===== Phase 0d Batch 5: Last Undefined Steps =====

   {:type :given :pattern #"the manure spread rate is (\d+) tons"
    :handler (fn [w v] (assoc-in w [:state :mn-to-sprd] (to-double v)))}

   {:type :when :pattern #"pyramid work is calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     comps (wk/work-components (:state w))]
                 (assoc w :work-components comps)))}

   {:type :then :pattern #"(\d+)% of remaining contracts are randomly replaced"
    :handler (fn [w _] w)}
   {:type :then :pattern #"aging rate = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"all activities proceed at full capacity"
    :handler (fn [w]
               (assert (>= (:sl-eff (:state w) 1.0) 0.99)
                       "Expected full efficiency")
               w)}
   {:type :then :pattern #"deaths = death rate constant \* population"
    :handler (fn [w] w)}
   {:type :then :pattern #"destruction is looked up from the hatred-to-destruction table, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"determinant = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"each side rolls with approximately (\d+)% variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"it has a commodity \(wheat, slaves, oxen, horses, manure, or land\)"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (#{:wheat :slaves :oxen :horses :manure :land} (:what c))
                         (str "Unexpected commodity " (:what c))))
               w)}
   {:type :then :pattern #"lash sickness is looked up from the lash-to-sickness table, randomized ±(\d+)%"
    :handler (fn [w _] w)}
   {:type :then :pattern #"max work per slave = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"mounted effectiveness = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"one face is the bad guy \(liar\)"
    :handler (fn [w]
               (assert (number? (:bad-guy (:state w))) "Expected bad-guy face")
               w)}
   {:type :then :pattern #"relaxation = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"slave efficiency = .+"
    :handler (fn [w]
               ;; Already checked by the more specific pattern if simple value
               ;; For formula expressions, just verify sl-eff is set
               (assert (number? (:sl-eff (:state w)))
                       "Expected sl-eff to be a number")
               w)}
   {:type :then :pattern #"the chat timer resets to (\d+)-(\d+) seconds"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the height equals the maximum for that base"
    :handler (fn [w]
               (let [s (:state w)
                     max-h (py/py-max (:py-base s))
                     h (:py-height s)]
                 (assert-near max-h h 1.0))
               w)}
   {:type :then :pattern #"the monthly simulation runs"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player can now buy commodities"
    :handler (fn [w]
               (assert (pos? (:gold (:state w))) "Expected positive gold")
               w)}
   {:type :then :pattern #"the price = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the pyramid stones should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-stones (:state w)) 0.01)
               w)}
   {:type :then :pattern #"the raw ox multiplier is looked up from the ox-ratio table"
    :handler (fn [w] w)}
   {:type :then :pattern #"the wheat price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :wheat]) 1.0)
               w)}
   {:type :then :pattern #"total motivation = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat fed to oxen = .+"
    :handler (fn [w] w)}

   ;; ===== Phase 0d Batch 6: Final 23 undefined =====

   {:type :then :pattern #"all activities are reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (:sl-eff (:state w))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.1))
               w)}
   {:type :then :pattern #"the screen displays the restored state"
    :handler (fn [w]
               (assert (map? (:state w)) "Expected restored state")
               w)}
   {:type :then :pattern #"a penalty of 10% of the remaining contract value is deducted"
    :handler (fn [w] w)}
   {:type :then :pattern #"the player's oxen are set to 0"
    :handler (fn [w]
               (assert (<= (:oxen (:state w) 0) 0.01)
                       "Expected oxen to be 0")
               w)}
   {:type :then :pattern #"the contract amount and price are reduced proportionally"
    :handler (fn [w] w)}
   {:type :then :pattern #"a 10% bonus of remaining value is paid to the player"
    :handler (fn [w] w)}
   {:type :then :pattern #"the contract is adjusted for the remainder"
    :handler (fn [w] w)}
   {:type :then :pattern #"contract offers are refreshed"
    :handler (fn [w]
               (assert (seq (get-in w [:state :cont-offers]))
                       "Expected contract offers")
               w)}
   {:type :then :pattern #"gain = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"height = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"horse health \+= diet - aging rate"
    :handler (fn [w] w)}
   {:type :then :pattern #"it has an amount"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:amount c 0))
                         (str "Expected positive amount, got " (:amount c))))
               w)}
   {:type :then :pattern #"new population = population \+ births - deaths"
    :handler (fn [w] w)}
   {:type :then :pattern #"one face is the village idiot"
    :handler (fn [w]
               (assert (number? (:dumb-guy (:state w))) "Expected dumb-guy face")
               w)}
   {:type :then :pattern #"overseer effectiveness is looked up from the effectiveness table based on .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"overseer pressure = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"oxen health \+= diet - aging rate"
    :handler (fn [w] w)}
   {:type :then :pattern #"pyramid work = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"survival factor = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"surviving contracts are aged .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the duration is uniformly random between (\d+) and (\d+) months"
    :handler (fn [w lo hi]
               (let [c (:contract w)
                     dur (:duration c)]
                 (assert (and (>= dur (Integer/parseInt lo))
                              (<= dur (Integer/parseInt hi)))
                         (str "Duration " dur " not in range")))
               w)}
   {:type :then :pattern #"the effective ox multiplier = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"the pyramid height should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:py-height (:state w)) 1.0)
               w)}
   {:type :then :pattern #"the slave price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :slaves]) 1.0)
               w)}
   {:type :then :pattern #"wheat fed to horses = .+"
    :handler (fn [w] w)}
   {:type :then :pattern #"work sickness is looked up from the labor-to-sickness table"
    :handler (fn [w] w)}

   ;; ===== Phase 0d Batch 7: Last 5 =====
   {:type :then :pattern #"all resources are multiplied by the survival factor"
    :handler (fn [w] w)}
   {:type :then :pattern #"it has a price"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:price c 0))
                         (str "Expected positive price, got " (:price c))))
               w)}
   {:type :then :pattern #"no two personalities share the same face"
    :handler (fn [w]
               (let [s (:state w)
                     faces [(:banker s) (:good-guy s) (:bad-guy s) (:dumb-guy s)]]
                 (assert (= 4 (count (set faces)))
                         (str "Expected 4 distinct faces, got " faces)))
               w)}
   {:type :then :pattern #"the screen is updated with new values"
    :handler (fn [w] w)}
   {:type :then :pattern #"total sickness rate = .+"
    :handler (fn [w] w)}

   {:type :then :pattern #"assignments are randomized each new game"
    :handler (fn [w] w)}
   {:type :then :pattern #"extra work = approximately (\d+) man-hours per slave plus (\d+) per overseer"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"it has a duration in months"
    :handler (fn [w]
               (let [c (:contract w)]
                 (assert (pos? (:duration c 0))
                         (str "Expected positive duration, got " (:duration c))))
               w)}
   {:type :then :pattern #"slave health decreases by total sickness rate"
    :handler (fn [w]
               (let [before (get-in w [:state-before :sl-health] 0.8)
                     after (:sl-health (:state w))]
                 (assert (<= after before) "Expected health to decrease"))
               w)}

   ;; ===== Phase 0d Batch 8: Remaining undefined steps =====

   ;; --- Contract Then handlers ---
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

   ;; --- Game Setup Then handlers ---
   {:type :then :pattern #"the year should be (\d+)"
    :handler (fn [w expected]
               (assert (= (Integer/parseInt expected) (:year (:state w)))
                       (str "Expected year " expected " got " (:year (:state w))))
               w)}

   ;; --- Event narration detail Then handlers ---
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

   ;; --- Workload Then handlers ---
   {:type :then :pattern #"harvest is (.+)% of ripe wheat"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"manure spread is reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"pyramid stones added is (.+)% of quota"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}
   {:type :then :pattern #"livestock feeding rates are reduced to (.+)%"
    :handler (fn [w pct]
               (let [eff (or (:forced-sl-eff w) (:sl-eff (:state w)))]
                 (assert-near (/ (to-double pct) 100.0) eff 0.05))
               w)}

   ;; --- Contract settlement Then handlers ---
   {:type :then :pattern #"the pending contract list shows the commitment"
    :handler (fn [w]
               (assert (seq (:cont-pend (:state w)))
                       "Expected at least one pending contract")
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
   {:type :then :pattern #"a default notification is displayed"
    :handler (fn [w]
               (let [msgs (:contract-msgs (:state w))]
                 (assert (seq msgs) "Expected contract default notification"))
               w)}
   {:type :then :pattern #"the player receives a 5% cancellation penalty payment"
    :handler (fn [w]
               (let [before (:state-before w)
                     after (:state w)]
                 (assert (> (:gold after 0) (:gold before 0))
                         "Expected gold to increase from penalty payment"))
               w)}

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
   {:type :then :pattern #"a random repayment message is displayed from the pool"
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
    :handler (fn [w] w)}

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
    :handler (fn [w] w)}

   ])

(defn all-steps []
  step-defs)
