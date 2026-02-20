(ns pharaoh.easy-mode-sim-test
  (:require [clojure.test :refer :all]
            [pharaoh.contracts :as ct]
            [pharaoh.loans :as ln]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.state :as st]
            [pharaoh.trading :as tr]))

(defn- setup-easy-mode [seed]
  (let [rng (r/make-rng seed)
        state (-> (st/initial-state)
                  (st/set-difficulty "Easy")
                  (assoc :players (ct/make-players rng)))]
    {:rng rng :state state}))

(defn- initial-setup [rng state]
  (ln/borrow rng state 200000.0))

(defn- pyramid-quota [state]
  (let [sl-eff (get state :sl-eff 1.0)]
    (cond
      (< (:slaves state) 200) 0.0
      (< (:wheat state) 3000) 0.0
      (< sl-eff 0.5) 0.0
      (< sl-eff 0.7) 5.0
      :else 15.0)))

(defn- adjust-between-months [rng state month-n]
  (let [slaves (:slaves state)
        ;; === OVERSEERS: 1 per 15 slaves ===
        target-ov (max 7.0 (Math/ceil (/ slaves 15.0)))
        state (if (> target-ov (:overseers state))
                (assoc state :overseers target-ov) state)
        ;; === SELL excess horses (cap at 2x overseers) ===
        hs-max (max 7.0 (* (:overseers state) 2.0))
        hs-excess (long (- (:horses state) hs-max))
        state (if (> hs-excess 2)
                (tr/sell rng state :horses hs-excess) state)
        ;; === SELL excess oxen (cap at 0.3x slaves) ===
        ox-max (max 15.0 (* (:slaves state) 0.3))
        ox-excess (long (- (:oxen state) ox-max))
        state (if (> ox-excess 3)
                (tr/sell rng state :oxen ox-excess) state)
        ;; === CAP slaves (ramp with pyramid height) ===
        max-slaves (min 600.0 (max 300.0 (+ 200.0 (* (:py-height state) 5.0))))
        sl-excess (long (- (:slaves state) max-slaves))
        state (if (> sl-excess 0)
                (tr/sell rng state :slaves sl-excess) state)
        ;; === Wheat consumption for emergency/planting calcs ===
        wt-consumption (+ (* (:slaves state) (:sl-feed-rt state))
                          (* (:oxen state) 70.0)
                          (* (:horses state) 55.0))
        ;; === EMERGENCY BUYS (wheat first, then livestock) ===
        ;; Wheat: prevent starvation (buy before restocking population)
        wt-need (* (max (:slaves state) 100) (:sl-feed-rt state) 3)
        state (if (and (< (:wheat state) wt-need) (> (:gold state) 50000))
                (let [wt-price (get-in state [:prices :wheat] 10)
                      budget (* (:gold state) 0.05)
                      amount (max 1 (min (long (* wt-need 2))
                                         (long (/ budget wt-price))))]
                  (tr/buy rng state :wheat amount))
                state)
        ;; Manure: keep farming alive
        state (if (and (< (:manure state) 30) (> (:gold state) 30000))
                (tr/buy rng state :manure
                  (min 100 (long (/ (* (:gold state) 0.05)
                                    (get-in state [:prices :manure] 20)))))
                state)
        ;; Oxen: restore work capacity
        state (if (and (< (:oxen state) 10) (> (:gold state) 50000))
                (tr/buy rng state :oxen
                  (min 20 (long (/ (* (:gold state) 0.1)
                                   (get-in state [:prices :oxen] 90)))))
                state)
        ;; Horses: restore motivation
        state (if (and (< (:horses state) 5) (> (:gold state) 20000))
                (tr/buy rng state :horses
                  (min 15 (long (/ (* (:gold state) 0.05)
                                   (get-in state [:prices :horses] 100)))))
                state)
        ;; Slaves: only if wheat is sufficient for feeding
        state (if (and (< (:slaves state) 50)
                       (> (:wheat state) 1500)
                       (> (:gold state) 50000))
                (tr/buy rng state :slaves
                  (min 100 (long (/ (* (:gold state) 0.1)
                                    (get-in state [:prices :slaves] 1000)))))
                state)
        ;; === REPAY LOAN when flush ===
        repay-amt (if (and (> month-n 24) (> (:gold state) 500000)
                           (> (:loan state) 0))
                    (min (- (:gold state) 300000) (:loan state)) 0)
        state (if (pos? repay-amt) (ln/repay state repay-amt) state)
        ;; === PLANTING: 5 mn/acre for high yield ===
        fallow (:ln-fallow state)
        manure (:manure state)
        plant? (and (> fallow 0) (> manure 10))
        need-acres (Math/ceil (/ wt-consumption 700.0))
        safe-acres (max 3.0 (Math/floor (/ slaves 18.0)))
        acres (if plant? (min fallow need-acres safe-acres) 0.0)
        mn-spread (if plant? (min (* acres 5.0) manure) 0.0)
        ;; === PYRAMID (no month gate) ===
        quota (pyramid-quota state)]
    (assoc state :ln-to-sew acres :mn-to-sprd mn-spread :py-quota quota)))

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

(defn- run-simulation
  ([seed] (run-simulation seed false))
  ([seed verbose?]
   (let [{:keys [rng state]} (setup-easy-mode seed)
         state (initial-setup rng state)
         max-months (* 40 12)]
     (when verbose?
       (println (str "\n=== Easy Mode (seed " seed "): Build Pyramid in 40 Years ==="))
       (println (str "Target: pyramid height "
                     (format "%.1f" (* 0.866 (:py-base state))) "\n"))
       (print-header)
       (println "--- Initial ---")
       (print-month state)
       (println))
     (loop [s state month-n 0]
       (if (or (>= month-n max-months) (:game-won s) (:game-over s))
         (do
           (when verbose?
             (when (not= 1 (:month s)) (print-month s))
             (println)
             (println (str "Months played:   " month-n
                           " (" (quot month-n 12) " years)"))
             (println (str "Final net worth: " (fmt (or (:net-worth s) 0))))
             (println (str "Pyramid:         "
                           (format "%.1f" (double (:py-height s)))
                           " / "
                           (format "%.1f" (* 0.866 (:py-base s)))))
             (println (str "Game won?        " (:game-won s))))
           {:won (:game-won s) :game-over (:game-over s)
            :months month-n :py-height (:py-height s)
            :net-worth (:net-worth s)})
         (let [s (adjust-between-months rng s month-n)
               s (sim/do-run rng s)]
           (when (and verbose? (or (= 1 (:month s)) (:game-won s)))
             (print-month s))
           (recur s (inc month-n))))))))

(deftest easy-mode-seed-72-wins
  (let [result (run-simulation 72 true)]
    (is (:won result) "Seed 72 should win")))

(deftest easy-mode-seed-116-wins
  (let [result (run-simulation 116 true)]
    (is (:won result) "Seed 116 should win")))
