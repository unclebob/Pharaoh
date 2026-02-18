(ns pharaoh.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [pharaoh.state :as st]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.ui.layout :as lay]
            [pharaoh.ui.screen :as scr]
            [pharaoh.ui.input :as inp])
  (:gen-class))

(defn- setup []
  (q/frame-rate 30)
  (q/text-font (q/create-font "Monospaced" 10))
  {:state (st/initial-state)
   :rng (r/make-rng (System/currentTimeMillis))})

(defn- dialog-help [d]
  (case (:type d)
    :buy-sell "b=buy s=sell  Enter=ok  Esc=cancel"
    :loan "b=borrow r=repay  Enter=ok  Esc=cancel"
    :overseer "h=hire f=fire  Enter=ok  Esc=cancel"
    :feed "Enter amount, Enter=ok  Esc=cancel"
    :plant "Enter acres, Enter=ok  Esc=cancel"
    :spread "Enter tons, Enter=ok  Esc=cancel"
    :pyramid "Enter stones, Enter=ok  Esc=cancel"
    "Enter=ok  Esc=cancel"))

(defn- draw-dialog [state]
  (when-let [d (:dialog state)]
    (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)]
      (q/fill 30 30 50)
      (q/stroke 200 180 100)
      (q/rect x y w h)
      (q/fill 255 255 200)
      (q/text-size 10)
      (q/text (str (name (:type d))
                   (when (:commodity d) (str " " (name (:commodity d))))
                   (when (:mode d) (str " [" (name (:mode d)) "]")))
              (+ x 4) (+ y 14))
      (q/fill 255)
      (q/text (str "Amount: " (:input d)) (+ x 4) (+ y 34))
      (q/text-size 8)
      (q/fill 180)
      (q/text (dialog-help d) (+ x 4) (+ y 54)))))

(defn- draw [{:keys [state]}]
  (scr/draw-screen state)
  (draw-dialog state))

(defn- key-pressed [{:keys [state rng] :as app} {:keys [raw-key]}]
  (assoc app :state (inp/handle-key rng state raw-key)))

(defn- mouse-clicked [{:keys [state rng] :as app} {:keys [x y]}]
  (let [new-state (inp/handle-mouse state x y)]
    (cond
      (:run-clicked new-state)
      (assoc app :state
             (sim/do-run rng (dissoc new-state :run-clicked)))

      (:quit-clicked new-state)
      (do (q/exit) app)

      :else
      (assoc app :state new-state))))

(defn -main [& _args]
  (q/defsketch pharaoh-game
    :title "Pharaoh"
    :size [lay/win-w lay/win-h]
    :setup setup
    :draw draw
    :key-pressed key-pressed
    :mouse-clicked mouse-clicked
    :middleware [m/fun-mode]))
