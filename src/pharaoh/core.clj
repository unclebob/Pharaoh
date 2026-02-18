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

(defn- load-faces []
  (mapv #(q/load-image (str "resources/faces/man" (inc %) ".png"))
        (range 4)))

(defn- setup []
  (q/frame-rate 30)
  (q/text-font (q/create-font "Monospaced" lay/value-size))
  {:state (st/initial-state)
   :rng (r/make-rng (System/currentTimeMillis))
   :faces (load-faces)})

(defn- dialog-shortcut [d]
  (case (:type d)
    :buy-sell (case (:commodity d)
               :wheat "w" :slaves "s" :oxen "o"
               :horses "h" :manure "m" :land "l" nil)
    :feed (case (:commodity d)
            :slaves "S" :oxen "O" :horses "H" nil)
    :loan "L" :overseer "g"
    :plant "p" :spread "f" :pyramid "q" nil))

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
    (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
          key-hint (dialog-shortcut d)
          title (str (name (:type d))
                     (when (:commodity d) (str " " (name (:commodity d))))
                     (when key-hint (str " (" key-hint ")"))
                     (when (:mode d) (str " [" (name (:mode d)) "]")))]
      (q/fill 245 245 255)
      (q/stroke 100)
      (q/stroke-weight 2)
      (q/rect x y w h 5)
      (q/stroke-weight 1)
      (q/fill 0)
      (q/text-size lay/title-size)
      (q/text title (+ x 8) (+ y lay/title-size 8))
      (q/fill 0)
      (q/text-size lay/value-size)
      (q/text (str "Amount: " (:input d)) (+ x 8) (+ y (* lay/value-size 3) 8))
      (q/text-size lay/small-size)
      (q/fill 100)
      (q/text (dialog-help d) (+ x 8) (+ y (* lay/value-size 5) 8)))))

(defn- draw-face-message [msg faces]
  (let [text (:text msg)
        face (:face msg)
        img (when (and face (< face (count faces))) (nth faces face))
        {:keys [x y w h]} (lay/cell-rect-span 1 8 8 4)
        img-size (int (* h 0.7))
        img-x (+ x 10)
        img-y (+ y (/ (- h img-size) 2))
        text-x (+ img-x img-size 12)
        text-w (- (+ x w) text-x 8)]
    (q/fill 245 245 255)
    (q/stroke 100)
    (q/stroke-weight 2)
    (q/rect x y w h 5)
    (q/stroke-weight 1)
    (when img (q/image img img-x img-y img-size img-size))
    (q/fill 0)
    (q/text-size lay/value-size)
    (q/text-leading (* lay/value-size 1.3))
    (q/text (str text) text-x (+ y lay/value-size 12)
            text-w (- h 16))
    (q/fill 140)
    (q/text-size lay/small-size)
    (q/text "[press any key]" text-x (+ y h -16))))

(defn- draw [{:keys [state faces]}]
  (scr/draw-screen state)
  (draw-dialog state)
  (when-let [msg (:message state)]
    (when (map? msg) (draw-face-message msg faces))))

(defn- key-pressed [{:keys [state rng] :as app} {:keys [raw-key]}]
  (set! (.key (quil.applet/current-applet)) (char 0))
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
    :mouse-pressed mouse-clicked
    :middleware [m/fun-mode]))
