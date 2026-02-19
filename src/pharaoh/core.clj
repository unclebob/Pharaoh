(ns pharaoh.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [pharaoh.state :as st]
            [pharaoh.random :as r]
            [pharaoh.simulation :as sim]
            [pharaoh.neighbors :as nb]
            [pharaoh.visits :as vis]
            [pharaoh.speech :as speech]
            [pharaoh.startup :as su]
            [pharaoh.ui.layout :as lay]
            [pharaoh.ui.screen :as scr]
            [pharaoh.ui.input :as inp])
  (:gen-class))

(defn- load-faces []
  (mapv #(q/load-image (str "resources/faces/man" (inc %) ".png"))
        (range 4)))

(defn- load-icons []
  {:buy-sell  (q/load-image "resources/images/icon_buysell.png")
   :feed      (q/load-image "resources/images/icon_feed.png")
   :overseer  (q/load-image "resources/images/icon_overseers.png")
   :plant     (q/load-image "resources/images/icon_plant.png")
   :spread    (q/load-image "resources/images/icon_manure.png")
   :loan      (q/load-image "resources/images/icon_loan.png")
   :pyramid   (q/load-image "resources/images/icon_pyramid.png")
   :event     (q/load-image "resources/images/icon_event.png")})

(defn- init-timers [rng]
  (vis/init-timers rng (System/currentTimeMillis)))

(defn- setup []
  (q/frame-rate 30)
  (q/text-font (q/create-font "Monospaced" lay/value-size))
  (let [rng (r/make-rng (System/currentTimeMillis))
        men (nb/set-men rng)]
    (merge
      {:state (merge (st/initial-state) men)
       :screen :difficulty
       :rng rng
       :logo (q/load-image "resources/images/logo.png")
       :faces (load-faces)
       :icons (load-icons)}
      (init-timers rng))))

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

(defn- draw-dialog [state icons]
  (when-let [d (:dialog state)]
    (when (not= :contracts (:type d))
      (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
            key-hint (dialog-shortcut d)
            title (str (name (:type d))
                       (when (:commodity d) (str " " (name (:commodity d))))
                       (when key-hint (str " (" key-hint ")"))
                       (when (:mode d) (str " [" (name (:mode d)) "]")))
            icon (get icons (:type d))
            icon-size (int (* h 0.4))
            text-x (if icon (+ x icon-size 16) (+ x 8))]
        (q/fill 245 245 255)
        (q/stroke 100)
        (q/stroke-weight 2)
        (q/rect x y w h 5)
        (q/stroke-weight 1)
        (when icon
          (q/image icon (+ x 8) (+ y 8) icon-size icon-size))
        (q/fill 0)
        (q/text-size lay/title-size)
        (q/text title text-x (+ y lay/title-size 8))
        (q/fill 0)
        (q/text-size lay/value-size)
        (q/text (str "Amount: " (:input d)) text-x (+ y (* lay/value-size 3) 8))
        (q/text-size lay/small-size)
        (q/fill 100)
        (q/text (dialog-help d) text-x (+ y (* lay/value-size 5) 8))))))

(defn- fmt-offer [offer players]
  (let [name (get-in players [(:who offer) :name] "?")
        verb (if (= :buy (:type offer)) "BUY" "SELL")]
    (format "%s: %s %.0f %s @ %.0f gold %dmo"
            name verb (:amount offer)
            (clojure.core/name (:what offer))
            (:price offer) (:duration offer))))

(defn- fmt-confirm [offer players]
  (let [name (get-in players [(:who offer) :name] "?")
        verb (if (= :buy (:type offer)) "sell" "buy")
        dir (if (= :buy (:type offer)) "to" "from")]
    (format "Will you %s %.0f %s %s %s for %.0f gold in %d months?"
            verb (:amount offer)
            (clojure.core/name (:what offer))
            dir name (:price offer) (:duration offer))))

(defn- draw-offer-list [d x y w players]
  (let [offers (:active-offers d)
        selected (:selected d)
        row-h (+ lay/label-size 4)
        y0 (+ y (* lay/title-size 2) lay/small-size 8)]
    (q/text-size lay/small-size)
    (q/fill 100)
    (q/text "Up/Down=navigate  Enter=select  Esc=close"
            (+ x 8) (+ y lay/title-size lay/small-size 12))
    (doseq [i (range (count offers))]
      (let [oy (+ y0 (* i row-h))
            text (fmt-offer (nth offers i) players)]
        (when (= i selected)
          (q/fill 200 210 255)
          (q/no-stroke)
          (q/rect (+ x 4) (- oy 2) (- w 8) row-h 2)
          (q/stroke 100))
        (q/fill 0)
        (q/text-size lay/label-size)
        (q/text text (+ x 8) (+ oy lay/label-size))))))

(defn- draw-confirm-prompt [d x y w h players]
  (let [offer (nth (:active-offers d) (:selected d))
        text (fmt-confirm offer players)]
    (q/fill 0)
    (q/text-size lay/value-size)
    (q/text-leading (* lay/value-size 1.3))
    (q/text text (+ x 8) (+ y (* lay/title-size 3)) (- w 16) (- h 60))
    (q/fill 100)
    (q/text-size lay/small-size)
    (q/text "y=accept  n=reject  Esc=back" (+ x 8) (+ y h -20))))

(defn- draw-contracts-dialog [state]
  (when-let [d (:dialog state)]
    (when (= :contracts (:type d))
      (let [{:keys [x y w h]} (lay/cell-rect-span 2 5 7 14)
            players (:players state)]
        (q/fill 245 245 255)
        (q/stroke 100)
        (q/stroke-weight 2)
        (q/rect x y w h 5)
        (q/stroke-weight 1)
        (q/fill 0)
        (q/text-size lay/title-size)
        (q/text "Contract Offers (c)" (+ x 8) (+ y lay/title-size 8))
        (if (= :confirming (:mode d))
          (draw-confirm-prompt d x y w h players)
          (draw-offer-list d x y w players))))))

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

(defn- update-app [app]
  (if (= :game (:screen app))
    (let [old-msg (get-in app [:state :message])
          app (vis/check-visits app (System/currentTimeMillis))
          new-msg (get-in app [:state :message])]
      (when (and new-msg (not= old-msg new-msg))
        (speech/speak (:text new-msg) (:face new-msg)))
      app)
    app))

(def ^:private btn-labels
  ["[1] Easy  — small pyramid, generous credit"
   "[2] Normal — medium pyramid, standard credit"
   "[3] Hard  — massive pyramid, tight credit"])

(defn- draw-difficulty [logo]
  (q/background 30 30 60)
  (let [logo-w (.width logo)
        logo-h (.height logo)
        scale (/ 200.0 logo-w)
        draw-w (int (* logo-w scale))
        draw-h (int (* logo-h scale))]
    (q/image logo 20 20 draw-w draw-h))
  (q/fill 255 215 0)
  (q/text-size 48)
  (q/text-align :center :center)
  (q/text "PHARAOH" (/ lay/win-w 2) 150)
  (q/fill 220)
  (q/text-size 22)
  (q/text "Choose your difficulty:" (/ lay/win-w 2) 280)
  (doseq [i (range 3)]
    (let [{:keys [x y w h]} (su/button-rect i)]
      (q/fill 60 60 100)
      (q/stroke 180 180 255)
      (q/stroke-weight 2)
      (q/rect x y w h 8)
      (q/stroke-weight 1)
      (q/fill 255)
      (q/text-size 18)
      (q/text-align :center :center)
      (q/text (nth btn-labels i) (+ x (/ w 2)) (+ y (/ h 2)))))
  (q/text-align :left :baseline))

(defn- draw [{:keys [screen state faces icons logo]}]
  (if (= :difficulty screen)
    (draw-difficulty logo)
    (do
      (scr/draw-screen state)
      (draw-dialog state icons)
      (draw-contracts-dialog state)
      (when-let [msg (:message state)]
        (when (map? msg) (draw-face-message msg faces))))))

(defn- quit! []
  (q/exit)
  (future (Thread/sleep 500) (System/exit 0)))

(defn- key-pressed [{:keys [screen] :as app} {:keys [raw-key key]}]
  (set! (.key (quil.applet/current-applet)) (char 0))
  (if (= :difficulty screen)
    (if (= (int raw-key) 27)
      (do (quit!) app)
      (su/select-difficulty app (su/difficulty-for-key raw-key)))
    (let [new-state (inp/handle-key (:rng app) (:state app) raw-key key)]
      (if (:quit-clicked new-state)
        (do (quit!) app)
        (assoc app :state new-state)))))

(defn- mouse-clicked [{:keys [screen state rng] :as app} {:keys [x y]}]
  (if (= :difficulty screen)
    (su/select-difficulty app (su/difficulty-for-click x y))
    (let [new-state (inp/handle-mouse state x y)]
      (cond
        (:run-clicked new-state)
        (assoc app :state
               (sim/do-run rng (dissoc new-state :run-clicked)))

        (:quit-clicked new-state)
        (do (quit!) app)

        :else
        (assoc app :state new-state)))))

(defn -main [& _args]
  (q/defsketch pharaoh-game
    :title "Pharaoh"
    :size [lay/win-w lay/win-h]
    :setup setup
    :update update-app
    :draw draw
    :key-pressed key-pressed
    :mouse-pressed mouse-clicked
    :middleware [m/fun-mode]))
