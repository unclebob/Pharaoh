(ns pharaoh.ui.menu
  (:require [quil.core :as q]
            [pharaoh.ui.layout :as lay]))

(defn toggle-menu [app]
  (update-in app [:menu :open?] not))

(defn close-menu [app]
  (assoc-in app [:menu :open?] false))

(def menu-bar-h 22)

(defn menu-bar-bounds []
  {:x 0 :y 0 :w 60 :h menu-bar-h})

(def ^:private items
  [{:label "Save         (Ctrl-S)" :action :save}
   {:label "Save As..."            :action :save-as}
   {:label "Open...      (Ctrl-O)" :action :open}
   {:label "New Game     (Ctrl-N)" :action :new-game}
   {:label "Quit"                  :action :quit}])

(defn menu-items [] items)

(def ^:private item-h 22)
(def ^:private dropdown-w 200)

(defn menu-item-hit [mx my]
  (when (and (<= 0 mx dropdown-w)
             (> my menu-bar-h)
             (<= my (+ menu-bar-h (* (count items) item-h))))
    (let [idx (int (/ (- my menu-bar-h) item-h))]
      (when (< idx (count items))
        (:action (nth items idx))))))

(defn draw-menu-bar [app]
  (let [open? (get-in app [:menu :open?])]
    ;; Menu bar background
    (q/fill 240 240 240)
    (q/stroke 200)
    (q/rect 0 0 lay/win-w menu-bar-h)
    ;; "File" label highlight
    (when open?
      (q/fill 210 220 240)
      (q/no-stroke)
      (q/rect 0 0 60 menu-bar-h))
    (q/fill 0)
    (q/no-stroke)
    (q/text-size 14)
    (q/text "File" 8 16)
    ;; Dropdown when open
    (when open?
      (let [n (count items)]
        (q/fill 250 250 250)
        (q/stroke 180)
        (q/rect 0 menu-bar-h dropdown-w (* n item-h) 2)
        (doseq [i (range n)]
          (let [y (+ menu-bar-h (* i item-h))]
            (q/stroke 230)
            (q/line 0 (+ y item-h) dropdown-w (+ y item-h))
            (q/fill 0)
            (q/no-stroke)
            (q/text-size 13)
            (q/text (:label (nth items i)) 12 (+ y 16))))))))
