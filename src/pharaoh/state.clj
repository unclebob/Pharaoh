(ns pharaoh.state)

(def default-prices
  {:wheat 10 :manure 5 :slaves 1000
   :horses 500 :oxen 300 :land 5000})

(def default-supply
  {:wheat 10000 :manure 5000 :slaves 5000
   :horses 3000 :oxen 4000 :land 10000})

(defn initial-state []
  {:month 1 :year 1

   ;; Commodities
   :gold 0.0 :old-gold 0.0
   :wheat 0.0 :old-wheat 0.0
   :manure 0.0 :old-manure 0.0
   :slaves 0.0 :old-slaves 0.0
   :horses 0.0 :old-horses 0.0
   :oxen 0.0 :old-oxen 0.0

   ;; Land stages
   :ln-fallow 0.0 :ln-sewn 0.0 :ln-grown 0.0 :ln-ripe 0.0

   ;; Wheat growth stages
   :wt-sewn 0.0 :wt-grown 0.0 :wt-ripe 0.0

   ;; Health (0-1)
   :sl-health 0.8 :ox-health 0.8 :hs-health 0.8

   ;; Feed rates
   :sl-feed-rt 0.0 :ox-feed-rt 0.0 :hs-feed-rt 0.0

   ;; Planting
   :ln-to-sew 0.0 :mn-to-sprd 0.0
   :wt-sewn-ln 20.0  ;; bushels of seed per acre
   :wt-rot-rt 0.02   ;; 2% rot per month

   ;; Overseers
   :overseers 0.0
   :ov-pay 100.0
   :ov-press 0.0

   ;; Pyramid
   :py-stones 0.0 :py-height 0.0
   :py-base 346.41 :py-quota 0.0

   ;; Loan
   :loan 0.0
   :interest 5.0
   :int-addition 0.0
   :credit-rating 1.0
   :credit-limit 500000.0
   :credit-lower 500000.0

   ;; Market
   :prices (into {} (map (fn [[k v]] [k (double v)]) default-prices))
   :inflation 0.0
   :supply (into {} (map (fn [[k v]] [k (double v)]) default-supply))
   :demand (into {} (map (fn [[k v]] [k (double v)]) default-supply))
   :production (into {} (map (fn [[k v]] [k (double v)]) default-supply))
   :world-growth 0.10

   ;; Contracts
   :cont-offers [] :cont-pend [] :players []

   ;; Neighbors
   :banker 0 :good-guy 1 :bad-guy 2 :dumb-guy 3

   ;; Temporary per-month
   :wk-addition 0.0
   :message nil
   :game-over false :game-won false
   :dirty false :save-path nil

   ;; Computed values (filled during simulation)
   :sl-eff 1.0 :wt-eff 1.0
   :sl-fed 0.0 :ox-fed 0.0 :hs-fed 0.0
   :net-worth 0.0 :debt-asset 0.0})

(defn set-difficulty [state difficulty]
  (case difficulty
    "Easy"
    (assoc state
      :py-base 115.47
      :credit-limit 5e6 :credit-lower 5e6
      :world-growth 0.15
      :prices (assoc (:prices state) :land 1000.0 :wheat 10.0 :slaves 1000.0))
    "Normal"
    (assoc state
      :py-base 346.41
      :credit-limit 5e5 :credit-lower 5e5
      :world-growth 0.10
      :prices (assoc (:prices state) :land 5000.0 :wheat 8.0 :slaves 800.0))
    "Hard"
    (assoc state :py-base 1154.7
      :credit-limit 5e4 :credit-lower 5e4)
    state))

(defn total-land [state]
  (+ (:ln-fallow state) (:ln-sewn state)
     (:ln-grown state) (:ln-ripe state)))
