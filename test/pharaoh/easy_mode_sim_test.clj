(ns pharaoh.easy-mode-sim-test
  (:require [clojure.test :refer :all]
            [pharaoh.contracts :as ct]
            [pharaoh.loans :as ln]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.state :as st]
            [pharaoh.trading :as tr]))

(defn- setup-easy-mode []
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (st/set-difficulty "Easy")
                  (assoc :players (ct/make-players rng)))]
    {:rng rng :state state}))

(defn- initial-setup [rng state]
  ;; Easy mode provides: 100 slaves, 50 oxen, 7 horses,
  ;; 20000 wheat, 400 manure, 80 land, loan=393200, gold=0.
  (ln/borrow rng state 200000.0))

(defn- total-land [state]
  (+ (:ln-fallow state) (:ln-sewn state)
     (:ln-grown state) (:ln-ripe state)))

(defn- pyramid-quota [state]
  (let [sl-eff (get state :sl-eff 1.0)]
    (cond
      (< (:slaves state) 200) 0.0
      (< (:wheat state) 3000) 0.0
      (< sl-eff 0.5) 0.0
      (< sl-eff 0.7) 5.0
      :else 15.0)))

(defn- adjust-between-months [rng state month-n]
  (let [stable? (> month-n 12)
        slaves (:slaves state)
        ;; Overseers: 1 per 15 slaves
        target-ov (max 7.0 (Math/ceil (/ slaves 15.0)))
        state (if (> target-ov (:overseers state))
                (assoc state :overseers target-ov) state)
        ;; Sell wheat when abundant and cash is low
        state (if (and (> (:wheat state) 15000) (< (:gold state) 50000))
                (tr/sell rng state :wheat
                  (min (- (:wheat state) 10000) 20000)) state)
        ;; Slave population management (only after stable)
        state (cond
                ;; Emergency: buy slaves if population crashed
                (and stable? (< slaves 200) (> (:gold state) 300000))
                (tr/buy rng state :slaves
                  (min 100 (long (/ (- (:gold state) 200000) 2000))))
                ;; Sell excess above 800 for income
                (and stable? (> slaves 800))
                (tr/sell rng state :slaves (min (- slaves 800) 200))
                :else state)
        ;; Oxen: target ratio 0.3 (only after stable)
        ox-target (long (* (:slaves state) 0.3))
        state (if (and stable? (> (- ox-target (:oxen state)) 10)
                       (> (:gold state) 200000))
                (tr/buy rng state :oxen
                  (min (- ox-target (:oxen state)) 20)) state)
        ;; Horses: 2 per overseer (only after stable)
        hs-target (* (:overseers state) 2.0)
        hs-need (- hs-target (:horses state))
        state (if (and stable? (> hs-need 5) (> (:gold state) 200000))
                (tr/buy rng state :horses (min (long hs-need) 10)) state)
        ;; Buy wheat: need at least (slaves * feed-rate) per month
        wt-need (long (* (:slaves state) (:sl-feed-rt state)))
        state (if (and (< (:wheat state) (* wt-need 2))
                       (> (:gold state) 50000))
                (tr/buy rng state :wheat
                  (min (* wt-need 3)
                       (long (/ (- (:gold state) 30000)
                                (get-in state [:prices :wheat] 10))))) state)
        ;; Buy manure if running low
        state (if (and (< (:manure state) 100) (> (:gold state) 20000))
                (tr/buy rng state :manure 200) state)
        ;; Repay loan when flush
        repay-amt (if (and (> (:gold state) 200000) (> (:loan state) 0))
                    (min (- (:gold state) 100000) (:loan state)) 0)
        state (if (pos? repay-amt) (ln/repay state repay-amt) state)
        ;; Plant conservatively: 10 acres
        fallow (:ln-fallow state)
        manure (:manure state)
        plant? (and (> fallow 0) (> manure 20) (pos? month-n))
        acres (if plant? (min fallow 10.0) 0.0)
        mn-spread (if plant? (min (* acres 5.0) manure) 0.0)
        ;; Pyramid quota (only after stable)
        quota (if stable? (pyramid-quota state) 0.0)]
    (if (zero? month-n)
      (assoc state :py-quota 0.0)
      (assoc state :ln-to-sew acres :mn-to-sprd mn-spread
                   :py-quota quota))))

(defn- fmt [v] (format "%,.0f" (double v)))

(defn- print-header []
  (println (format "%-6s %10s %10s %6s %6s %6s %6s %6s %8s %8s %6s"
                   "Month" "Gold" "Wheat" "Slave" "Oxen" "Horse"
                   "SlEff" "OxRt" "PyStone" "PyHt" "Quota")))

(defn- print-month [state]
  (let [slaves (:slaves state)
        oxen (:oxen state)]
    (println (format "%-6s %10s %10s %6s %6s %6s %6.2f %6.2f %8s %8.1f %6s"
                     (str "Y" (:year state) "M" (:month state))
                     (fmt (:gold state))
                     (fmt (:wheat state))
                     (fmt slaves) (fmt oxen) (fmt (:horses state))
                     (double (get state :sl-eff 1.0))
                     (if (pos? slaves) (double (/ oxen slaves)) 0.0)
                     (fmt (:py-stones state))
                     (double (:py-height state))
                     (fmt (:py-quota state))))))

(deftest easy-mode-build-pyramid-in-40-years
  (let [{:keys [rng state]} (setup-easy-mode)
        state (initial-setup rng state)
        max-months (* 40 12)]
    (println "\n=== Easy Mode: Build Pyramid in 40 Years ===")
    (println (str "Target: pyramid height "
                  (format "%.1f" (* 0.866 (:py-base state))) "\n"))
    (print-header)
    (println "--- Initial ---")
    (print-month state)
    (println)
    (loop [s state month-n 0]
      (if (or (>= month-n max-months) (:game-won s) (:game-over s))
        (do
          (when (not= 1 (:month s)) (print-month s))
          (println)
          (println (str "Months played:   " month-n
                        " (" (quot month-n 12) " years)"))
          (println (str "Final net worth: " (fmt (:net-worth s))))
          (println (str "Pyramid:         "
                        (format "%.1f" (double (:py-height s)))
                        " / "
                        (format "%.1f" (* 0.866 (:py-base s)))))
          (println (str "Game won?        " (:game-won s)))
          (is (:game-won s) "Should win by building the pyramid"))
        (let [s (adjust-between-months rng s month-n)
              s (sim/do-run rng s)]
          (when (or (= 1 (:month s)) (:game-won s))
            (print-month s))
          (recur s (inc month-n)))))))
