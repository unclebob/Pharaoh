(ns pharaoh.startup
  (:require [pharaoh.state :as st]
            [pharaoh.ui.layout :as lay]))

(def ^:private btn-w 500)
(def ^:private btn-h 50)
(def ^:private btn-gap 20)
(def ^:private btn-x (/ (- lay/win-w btn-w) 2))
(def ^:private btn-y0 350)

(def ^:private difficulties ["Easy" "Normal" "Hard"])

(defn button-rect [i]
  {:x btn-x
   :y (+ btn-y0 (* i (+ btn-h btn-gap)))
   :w btn-w
   :h btn-h})

(defn difficulty-for-key [ch]
  (case ch
    (\1 \e \E) "Easy"
    (\2 \n \N) "Normal"
    (\3 \h \H) "Hard"
    nil))

(defn- in-rect? [px py {:keys [x y w h]}]
  (and (>= px x) (<= px (+ x w))
       (>= py y) (<= py (+ y h))))

(defn difficulty-for-click [px py]
  (some (fn [i]
          (when (in-rect? px py (button-rect i))
            (nth difficulties i)))
        (range 3)))

(defn select-difficulty [app difficulty]
  (if difficulty
    (-> app
        (update :state st/set-difficulty difficulty)
        (assoc :screen :game))
    app))
