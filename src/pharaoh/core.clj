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

(def ^:private radio-labels
  {:buy-sell ["Buy (b)" "Sell (s)"]
   :loan     ["Borrow (b)" "Repay (r)"]
   :overseer ["Hire (h)" "Fire (f)"]})

(defn- draw-radio [x y label selected?]
  (let [r 6 cx (+ x r) cy (+ y (/ lay/title-size 2))]
    (if selected?
      (do (q/fill 80 80 160) (q/no-stroke))
      (do (q/no-fill) (q/stroke 160 160 160)))
    (q/ellipse cx cy (* r 2) (* r 2))
    (q/fill 0) (q/no-stroke)
    (q/text-size lay/label-size)
    (q/text label (+ x (* r 2) 4) (+ y lay/label-size -2))))

(defn- draw-button [x y w h label fill-r fill-g fill-b stroke-r stroke-g stroke-b]
  (q/fill fill-r fill-g fill-b)
  (q/stroke stroke-r stroke-g stroke-b)
  (q/rect x y w h 3)
  (q/fill 0) (q/no-stroke)
  (q/text-size lay/label-size)
  (q/text label (+ x 6) (+ y lay/label-size -2)))

(defn- draw-dialog-buttons [d]
  (let [bounds (inp/dialog-button-bounds (:type d))
        {:keys [ok cancel]} bounds]
    (when-let [labels (radio-labels (:type d))]
      (let [{r1 :radio1 r2 :radio2} bounds
            mode (:mode d)]
        (draw-radio (:x r1) (:y r1) (first labels)
                    (= mode (inp/radio-mode-for (:type d) :radio1)))
        (draw-radio (:x r2) (:y r2) (second labels)
                    (= mode (inp/radio-mode-for (:type d) :radio2)))))
    (draw-button (:x ok) (:y ok) (:w ok) (:h ok)
                 "OK (Enter)" 180 230 180 80 160 80)
    (draw-button (:x cancel) (:y cancel) (:w cancel) (:h cancel)
                 "Cancel (Esc)" 230 180 180 160 80 80)))

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
        (if (= :credit-check (:mode d))
          (let [text-w (- (+ x w) text-x 8)
                btn-y (+ y h -20 (- lay/title-size) -8)
                btn-h lay/title-size]
            (q/fill 0)
            (q/text-size lay/value-size)
            (q/text-leading (* lay/value-size 1.3))
            (q/text (str (:message d)) text-x (+ y (* lay/title-size 2) 4)
                    text-w (- btn-y y (- (* lay/title-size 2)) 8))
            (q/text-size lay/label-size)
            ;; Yes button — green tint
            (q/fill 180 230 180)
            (q/stroke 80 160 80)
            (q/rect (+ x 8) btn-y 100 btn-h 3)
            (q/fill 0) (q/no-stroke)
            (q/text "Yes (y)" (+ x 14) (+ btn-y lay/label-size -2))
            ;; No button — red tint
            (q/fill 230 180 180)
            (q/stroke 160 80 80)
            (q/rect (+ x 120) btn-y 100 btn-h 3)
            (q/fill 0) (q/no-stroke)
            (q/text "No (n)" (+ x 126) (+ btn-y lay/label-size -2)))
          (let [amount-y (+ y (* lay/value-size 3) 8)
                ib (inp/dialog-input-bounds)]
            (q/fill 0)
            (q/text-size lay/value-size)
            (q/text "Amount:" text-x amount-y)
            (q/fill 255 255 255)
            (q/stroke 150 150 150)
            (q/stroke-weight 1)
            (q/rect (:x ib) (:y ib) (:w ib) (:h ib) 3)
            (q/fill 0)
            (q/no-stroke)
            (q/text (str (:input d)) (+ (:x ib) 4) amount-y)
            (draw-dialog-buttons d)))))))

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
    (let [btn-y (+ y h -20 (- lay/title-size) -8)
          btn-h lay/title-size]
      (q/text-size lay/label-size)
      ;; Accept button — green tint
      (q/fill 180 230 180)
      (q/stroke 80 160 80)
      (q/rect (+ x 8) btn-y 100 btn-h 3)
      (q/fill 0) (q/no-stroke)
      (q/text "Accept (y)" (+ x 14) (+ btn-y lay/label-size -2))
      ;; Reject button — red tint
      (q/fill 230 180 180)
      (q/stroke 160 80 80)
      (q/rect (+ x 120) btn-y 100 btn-h 3)
      (q/fill 0) (q/no-stroke)
      (q/text "Reject (n)" (+ x 126) (+ btn-y lay/label-size -2))
      ;; Cancel button — gray tint
      (q/fill 220 220 220)
      (q/stroke 140 140 140)
      (q/rect (+ x 232) btn-y 100 btn-h 3)
      (q/fill 0) (q/no-stroke)
      (q/text "Cancel (Esc)" (+ x 236) (+ btn-y lay/label-size -2)))))

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

(defn- show-face-message? [state]
  (and (map? (:message state))
       (not= :contracts (get-in state [:dialog :type]))))

(defn- draw [{:keys [screen state faces icons logo]}]
  (if (= :difficulty screen)
    (draw-difficulty logo)
    (do
      (scr/draw-screen state)
      (draw-dialog state icons)
      (draw-contracts-dialog state)
      (when (show-face-message? state)
        (draw-face-message (:message state) faces)))))

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

(defn- mouse-moved [{:keys [screen state] :as app} {:keys [x y]}]
  (if (= :difficulty screen)
    app
    (assoc app :state (inp/handle-mouse-move state x y))))

(defn- mouse-clicked [{:keys [screen state rng] :as app} {:keys [x y]}]
  (if (= :difficulty screen)
    (su/select-difficulty app (su/difficulty-for-click x y))
    (let [new-state (inp/handle-mouse state x y rng)]
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
    :mouse-moved mouse-moved
    :on-close (fn [_] (System/exit 0))
    :middleware [m/fun-mode]))
