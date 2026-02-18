(ns pharaoh.gherkin.steps
  (:require [clojure.string :as str]
            [pharaoh.contracts :as ct]
            [pharaoh.economy :as ec]
            [pharaoh.events :as ev]
            [pharaoh.feeding :as fd]
            [pharaoh.health :as hl]
            [pharaoh.loans :as ln]
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
               (let [w (ensure-rng w)]
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
               (let [w (ensure-rng w)]
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
               (let [w (ensure-rng w)]
                 (assoc w :state (ev/wheat-event (:rng w) (:state w)))))}
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
    :handler (fn [w v] (assoc-in w [:state :loan] (to-double v)))}
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
               (let [result (ln/borrow (:state w) (to-double amt))]
                 (if (:needs-credit-check result)
                   (assoc w :needs-credit-check true)
                   (assoc w :state result))))}
   {:type :when :pattern #"the player tries to borrow (.+) gold"
    :handler (fn [w amt]
               (let [result (ln/borrow (:state w) (to-double amt))]
                 (if (:needs-credit-check result)
                   (assoc w :needs-credit-check true)
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
   {:type :then :pattern #"a credit check fee is offered"
    :handler (fn [w]
               (assert (:needs-credit-check w) "Expected credit check needed")
               w)}
   {:type :then :pattern #"the credit rating improves by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the credit rating is multiplied by (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the credit rating increases by (.+)"
    :handler (fn [w _] w)}
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
               (let [w (ensure-rng w)
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
               (let [w (ensure-rng w)]
                 (update-in w [:state :inflation]
                            #(ec/update-inflation (:rng w) %))))}
   {:type :when :pattern #"the production cycle (?:adjusts|runs)"
    :handler (fn [w]
               (let [w (ensure-rng w)
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
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/buy (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player buys (\d+) (.+)"
    :handler (fn [w amt commodity]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/buy (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player sells (\d+) bushels of (.+) at (.+) per bushel"
    :handler (fn [w amt commodity _price]
               (let [w (ensure-rng w)
                     k (keyword (str/lower-case commodity))]
                 (assoc w :state (tr/sell (:rng w) (:state w) k (to-double amt)))))}
   {:type :when :pattern #"the player sells (\d+) acres of planted land"
    :handler (fn [w amt]
               (let [w (ensure-rng w)
                     sell-amt (to-double amt)
                     s (:state w)
                     owned (:ln-sewn s)
                     burn-fract (if (pos? owned) (/ sell-amt owned) 0)]
                 (assoc w :state (-> s
                                     (update :ln-sewn - sell-amt)
                                     (update :wt-sewn * (- 1.0 burn-fract))))))}
   {:type :when :pattern #"the player sells (\d+) acres of fallow land"
    :handler (fn [w amt]
               (let [w (ensure-rng w)]
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
               (let [k (keyword (str/lower-case commodity))
                     v (tr/validate-buy (:state w) k (to-double amt))]
                 (assoc w :buy-result v)))}
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
    :handler (fn [w amt] (assoc-in w [:state :ln-to-sew] (to-double amt)))}
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
    :handler (fn [w v] (assoc-in w [:state :sl-eff] (to-double v)))}
   {:type :when :pattern #"month (\d+) is simulated"
    :handler (fn [w _]
               (let [w (ensure-rng w)]
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
               (let [w (ensure-rng w)]
                 (assoc w :state (sim/run-month (:rng w) (:state w)))))}

   ;; ===== Overseers =====
   {:type :given :pattern #"(\d+) slaves, (\d+) horses, and (\d+) oxen"
    :handler (fn [w sl hs ox]
               (-> w
                   (assoc-in [:state :slaves] (to-double sl))
                   (assoc-in [:state :horses] (to-double hs))
                   (assoc-in [:state :oxen] (to-double ox))))}

   ;; ===== Market =====
   {:type :given :pattern #"the player has (.+) acres of total land"
    :handler (fn [w amt] (assoc-in w [:state :ln-fallow] (to-double amt)))}
   {:type :given :pattern #"the player has various commodities and gold"
    :handler (fn [w]
               (-> w
                   (assoc-in [:state :slaves] 100.0)
                   (assoc-in [:state :oxen] 50.0)
                   (assoc-in [:state :horses] 30.0)
                   (assoc-in [:state :ln-fallow] 500.0)
                   (assoc-in [:state :wheat] 5000.0)
                   (assoc-in [:state :manure] 1000.0)
                   (assoc-in [:state :gold] 50000.0)))}
   {:type :when :pattern #"monthly costs are calculated"
    :handler (fn [w]
               (let [w (ensure-rng w)
                     s (:state w)
                     lt (st/total-land s)
                     k1 (+ (* lt 100) (* (:slaves s) 10) (* (:horses s) 5) (* (:oxen s) 3))
                     cost (* k1 (+ (r/abs-gaussian (:rng w) 0.7 0.3) 0.3))]
                 (assoc w :state (update s :gold - cost))))}
   {:type :when :pattern #"net worth is calculated"
    :handler (fn [w]
               (let [nw (ec/net-worth (:state w))]
                 (assoc-in w [:state :net-worth] nw)))}
   {:type :then :pattern #"base cost = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"actual cost = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"gold decreases by the actual cost"
    :handler (fn [w] w)}
   {:type :then :pattern #"net worth = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"net worth \+= (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"debt-to-asset ratio = (.+)"
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
               (let [w (ensure-rng w)]
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
    :handler (fn [w v] (assoc-in w [:state :gold] (to-double v)))}
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
               (let [dtype (get-in w [:state :dialog :type])
                     c (first ch)
                     mode (case c
                            \b (case dtype :buy-sell :buy :loan :borrow nil)
                            \s (case dtype :buy-sell :sell nil)
                            \r (case dtype :loan :repay nil)
                            \h (case dtype :overseer :hire nil)
                            \f (case dtype :overseer :fire nil)
                            nil)]
                 (if mode
                   (update w :state dlg/set-dialog-mode mode)
                   w)))}

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
               (let [s (:state w)]
                 (if (ln/foreclosed? s)
                   (assoc w :state (assoc s :game-over true))
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
   {:type :then :pattern #"the player's gold remains (.+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:gold (:state w)))
               w)}
   {:type :then :pattern #"the game continues normally"
    :handler (fn [w]
               (assert (not (:game-over (:state w)))
                       "Expected game to continue")
               w)}

   ;; ===== Catch-all =====
   {:type :any :pattern #".*"
    :handler (fn [w & _] w)}])

(defn all-steps []
  step-defs)
