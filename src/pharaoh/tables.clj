(ns pharaoh.tables)

(defn make-table [min-x max-x y-vector]
  {:min-x min-x :max-x max-x :y-vector (vec y-vector)})

(defn interpolate [x {:keys [min-x max-x y-vector]}]
  (cond
    (<= x min-x) (y-vector 0)
    (>= x max-x) (y-vector 10)
    :else
    (let [quantum (/ (- max-x min-x) 10.0)
          i (long (/ (- x min-x) quantum))
          i (min i 9)
          m (/ (- (y-vector (inc i)) (y-vector i)) quantum)
          dx (- x min-x (* i quantum))]
      (+ (y-vector i) (* m dx)))))

;; Seasonal yield: month 1-12, June/July peak, Jan minimum
(def seasonal-yield
  (make-table 1.0 12.0
    [0.2 0.3 0.5 0.65 0.8 0.95 1.0 0.95 0.8 0.6 0.4 0.25 0.2]))

;; Wheat yield from manure-per-acre (0-10 tons/acre)
(def wheat-yield
  (make-table 0.0 10.0
    [0.1 0.25 0.45 0.6 0.72 0.82 0.88 0.92 0.95 0.97 1.0]))

;; Positive motivation from overseer-effectiveness-per-slave
(def positive-motive
  (make-table 0.0 2.0
    [0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0 1.1 1.2 1.3]))

;; Negative motivation from lash rate (adds work output but hurts health)
(def negative-motive
  (make-table 0.0 1.0
    [0.0 0.05 0.1 0.15 0.2 0.25 0.3 0.35 0.4 0.45 0.5]))

;; Work ability from slave health (hours/day a slave can work)
(def work-ability
  (make-table 0.0 1.0
    [0.0 1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0]))

;; Ox work multiplier from oxen-per-slave ratio
(def ox-mult
  (make-table 0.0 3.0
    [1.0 1.15 1.3 1.5 1.7 1.9 2.1 2.3 2.5 2.7 2.8]))

;; Stress-to-lash rate from overseer pressure
(def stress-lash
  (make-table 0.0 1.0
    [0.0 0.02 0.05 0.1 0.18 0.28 0.4 0.55 0.7 0.85 1.0]))

;; Slave nourishment from feed rate (bushels/month)
(def slave-nourishment
  (make-table 0.0 20.0
    [0.0 0.01 0.02 0.04 0.06 0.08 0.1 0.12 0.13 0.14 0.15]))

;; Oxen nourishment from feed rate (bushels/month)
(def oxen-nourishment
  (make-table 0.0 120.0
    [0.0 0.02 0.04 0.07 0.1 0.13 0.15 0.17 0.18 0.19 0.20]))

;; Horse nourishment from feed rate (bushels/month)
(def horse-nourishment
  (make-table 0.0 100.0
    [0.0 0.03 0.06 0.1 0.14 0.18 0.21 0.23 0.24 0.245 0.25]))

;; Slave birth rate from health
(def slave-birth
  (make-table 0.0 1.0
    [0.0 0.001 0.003 0.006 0.01 0.015 0.02 0.025 0.03 0.04 0.05]))

;; Slave death rate from health (inverse - high health = low death)
(def slave-death
  (make-table 0.0 1.0
    [0.05 0.04 0.035 0.03 0.025 0.02 0.015 0.01 0.006 0.003 0.001]))

;; Oxen birth rate from health
(def oxen-birth
  (make-table 0.0 1.0
    [0.0 0.001 0.003 0.006 0.01 0.015 0.02 0.025 0.03 0.04 0.05]))

;; Oxen death rate from health
(def oxen-death
  (make-table 0.0 1.0
    [0.05 0.04 0.035 0.03 0.025 0.02 0.015 0.01 0.006 0.003 0.001]))

;; Horse birth rate from health
(def horse-birth
  (make-table 0.0 1.0
    [0.0 0.001 0.003 0.006 0.01 0.015 0.02 0.025 0.03 0.04 0.05]))

;; Horse death rate from health
(def horse-death
  (make-table 0.0 1.0
    [0.05 0.04 0.035 0.03 0.025 0.02 0.015 0.01 0.006 0.003 0.001]))

;; Lash sickness from lash rate
(def lash-sickness
  (make-table 0.0 1.0
    [0.0 0.005 0.015 0.03 0.05 0.08 0.12 0.17 0.23 0.3 0.4]))

;; Work sickness from labor (work_per_slave / ox_mult)
(def work-sickness
  (make-table 0.0 15.0
    [0.0 0.005 0.01 0.015 0.02 0.025 0.03 0.04 0.05 0.07 0.1]))

;; Overseer effectiveness from mounted effectiveness (hs_ov * hsEff)
(def overseer-effectiveness
  (make-table 0.0 5.0
    [0.5 0.7 0.9 1.1 1.3 1.5 1.7 1.9 2.2 2.5 3.0]))

;; Oxen efficiency from health
(def oxen-efficiency
  (make-table 0.0 1.0
    [0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0]))

;; Horse efficiency from health
(def horse-efficiency
  (make-table 0.0 1.0
    [0.0 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0]))

;; Credit repayment index (payment/loan -> credit multiplier)
;; From loan.c: {0.0, 0.1, 1.0,1.02,1.05,1.1,1.15,1.2,1.25,1.275,1.282,1.295,1.3}
(def repay-index
  (make-table 0.0 0.1
    [1.0 1.02 1.05 1.1 1.15 1.2 1.25 1.275 1.282 1.295 1.3]))

;; Debt support threshold from credit rating
(def debt-support
  (make-table 0.0 1.0
    [0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0 1.2 1.5 2.0]))

;; Revolt tables (from randomevent.c)
(def revolt-suffering
  (make-table 0.0 1.0
    [0.0 0.01 0.02 0.1 0.2 0.4 0.6 0.9 0.95 0.98 1.0]))

(def revolt-sickness
  (make-table 0.0 1.0
    [1.0 0.95 0.9 0.8 0.4 0.2 0.1 0.04 0.02 0.01 0.0]))

(def revolt-destruction
  (make-table 0.0 1.0
    [0.0 0.01 0.03 0.08 0.15 0.25 0.4 0.6 0.9 0.95 1.0]))

;; Dunning time interval from credit rating (seconds between notices)
(def dunning-time
  (make-table 0.0 1.0
    [5.0 6.0 8.0 12.0 20.0 30.0 45.0 60.0 90.0 200.0 300.0]))
