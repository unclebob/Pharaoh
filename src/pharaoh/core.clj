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
  (q/text-font (q/create-font "Monospaced" lay/value-size))
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
      (q/fill 245 245 255)
      (q/stroke 100)
      (q/stroke-weight 2)
      (q/rect x y w h 5)
      (q/stroke-weight 1)
      (q/fill 0)
      (q/text-size lay/title-size)
      (q/text (str (name (:type d))
                   (when (:commodity d) (str " " (name (:commodity d))))
                   (when (:mode d) (str " [" (name (:mode d)) "]")))
              (+ x 8) (+ y lay/title-size 8))
      (q/fill 0)
      (q/text-size lay/value-size)
      (q/text (str "Amount: " (:input d)) (+ x 8) (+ y (* lay/value-size 3) 8))
      (q/text-size lay/small-size)
      (q/fill 100)
      (q/text (dialog-help d) (+ x 8) (+ y (* lay/value-size 5) 8)))))

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
