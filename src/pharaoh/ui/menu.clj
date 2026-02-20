(ns pharaoh.ui.menu)

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
