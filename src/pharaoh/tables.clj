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

;; tSeasonYeild: month 1-12 (vars.c)
(def seasonal-yield
  (make-table 1.0 12.0
    [0.2 0.35 0.5 0.8 1.0 1.5 1.0 0.8 0.55 0.4 0.25]))

;; tWtYeild: bushels per bushel of seed from manure/acre (vars.c)
(def wheat-yield
  (make-table 0.0 10.0
    [20.0 35.0 70.0 100.0 150.0 200.0 180.0 140.0 100.0 50.0 0.0]))

;; tPosMotive: motivation from overseer effectiveness per slave (vars.c)
(def positive-motive
  (make-table 0.0 0.1
    [0.0 0.1 0.2 0.3 0.4 0.45 0.52 0.6 0.63 0.66 0.7]))

;; tNegMotive: motivation from lash rate (vars.c)
(def negative-motive
  (make-table 0.0 100.0
    [0.0 0.1 0.2 0.3 0.35 0.38 0.42 0.45 0.47 0.48 0.5]))

;; tWkAble_sl: man-hours/day from slave health (vars.c)
(def work-ability
  (make-table 0.0 1.0
    [0.0 1.0 5.0 10.0 14.0 15.0 17.0 18.0 19.0 19.5 20.0]))

;; tOxMultK: ox work multiplier from oxen/slave ratio (vars.c)
(def ox-mult
  (make-table 0.0 1.0
    [1.0 1.44 1.89 2.27 2.65 3.0 3.27 3.5 3.72 3.88 4.0]))

;; tStressLash: lashes from overseer pressure (vars.c)
(def stress-lash
  (make-table 0.0 10.0
    [0.0 20.0 80.0 150.0 300.0 500.0 600.0 700.0 800.0 900.0 1000.0]))

;; tSlNourish: slave health increment from feed rate (vars.c)
(def slave-nourishment
  (make-table 0.0 10.0
    [-1.0 -0.5 -0.185 0.036 0.0565 0.074 0.0865 0.098 0.12 0.25 0.18]))

;; tOxNourish: ox health increment from feed rate (vars.c)
(def oxen-nourishment
  (make-table 0.0 100.0
    [-1.0 -0.1 -0.0055 0.0 0.044 0.068 0.0825 0.0915 0.096 0.098 0.1]))

;; tHsNourish: horse health increment from feed rate (vars.c)
(def horse-nourishment
  (make-table 0.0 75.0
    [-1.0 -0.1 -0.046 0.0 0.0695 0.079 0.0865 0.092 0.0965 0.099 0.1]))

;; tSlBrthK: slave birth fraction from health (vars.c)
(def slave-birth
  (make-table 0.0 1.0
    [0.0 0.0021 0.007 0.0161 0.0364 0.0644 0.098 0.121 0.134 0.139 0.14]))

;; tSlDthK: slave death fraction from health (vars.c)
(def slave-death
  (make-table 0.0 1.0
    [1.0 0.485 0.235 0.135 0.0855 0.0605 0.0405 0.0255 0.0155 0.0105 0.002]))

;; tOxBrthK: oxen birth fraction from health (vars.c)
(def oxen-birth
  (make-table 0.0 1.0
    [0.0 0.0009 0.00285 0.00795 0.0159 0.028 0.038 0.05 0.06 0.065 0.07]))

;; tOxDthK: oxen death fraction from health (vars.c)
(def oxen-death
  (make-table 0.0 1.0
    [1.0 0.5 0.216 0.0959 0.0559 0.031 0.021 0.01 0.009 0.005 0.004]))

;; tHsBrthK: horse birth fraction from health (vars.c)
(def horse-birth
  (make-table 0.0 1.0
    [0.0 0.0012 0.0027 0.0045 0.001 0.02 0.04 0.05 0.06 0.065 0.07]))

;; tHsDthK: horse death fraction from health (vars.c)
(def horse-death
  (make-table 0.0 1.0
    [1.0 0.5 0.245 0.065 0.03 0.02 0.01 0.01 0.008 0.007 0.005]))

;; tLashSick: health loss from lash rate (vars.c)
(def lash-sickness
  (make-table 0.0 100.0
    [0.0 0.01 0.03 0.05 0.1 0.15 0.2 0.25 0.3 0.6 1.0]))

;; tWkSick: health loss from overwork (vars.c)
(def work-sickness
  (make-table 0.0 24.0
    [0.0 0.0005 0.0015 0.002 0.005 0.015 0.03 0.1 0.25 0.5 1.0]))

;; tOvEff: overseer effectiveness from mounted efficiency (vars.c)
(def overseer-effectiveness
  (make-table 0.0 1.0
    [0.3 0.44 0.58 0.681 0.762 0.825 0.884 0.93 0.965 0.983 0.997]))

;; tOxEff: oxen efficiency from health (vars.c)
(def oxen-efficiency
  (make-table 0.0 1.0
    [0.0 0.2 0.1 0.23 0.4 0.7 0.87 0.94 0.965 0.985 1.0]))

;; tHsEff: horse efficiency from health (vars.c)
(def horse-efficiency
  (make-table 0.0 1.0
    [0.0 0.0 0.015 0.065 0.19 0.66 0.835 0.93 0.99 1.0 1.0]))

;; tDebtSupport: debt support ratio from credit rating (vars.c)
(def debt-support
  (make-table 0.0 1.0
    [0.0 0.5 0.7 0.75 0.8 0.9 1.0 1.3 1.7 2.3 3.0]))

;; Repay index from loan.c
(def repay-index
  (make-table 0.0 0.1
    [1.0 1.02 1.05 1.1 1.15 1.2 1.25 1.275 1.282 1.295 1.3]))

;; Revolt tables (from randomevent.c) - verified as matching
(def revolt-suffering
  (make-table 0.0 1.0
    [0.0 0.01 0.02 0.1 0.2 0.4 0.6 0.9 0.95 0.98 1.0]))

(def revolt-sickness
  (make-table 0.0 1.0
    [1.0 0.95 0.9 0.8 0.4 0.2 0.1 0.04 0.02 0.01 0.0]))

(def revolt-destruction
  (make-table 0.0 1.0
    [0.0 0.01 0.03 0.08 0.15 0.25 0.4 0.6 0.9 0.95 1.0]))

;; Dunning time from loan.c - verified as matching
(def dunning-time
  (make-table 0.0 1.0
    [5.0 6.0 8.0 12.0 20.0 30.0 45.0 60.0 90.0 200.0 300.0]))
