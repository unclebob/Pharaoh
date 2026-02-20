(ns pharaoh.ui.menu
  (:require [quil.core :as q]
            [pharaoh.persistence :as ps]
            [pharaoh.ui.layout :as lay]))

(defn toggle-menu [app]
  (update-in app [:menu :open?] not))

(defn close-menu [app]
  (-> app
      (assoc-in [:menu :open?] false)
      (assoc-in [:menu :submenu :open?] false)))

(defn handle-menu-key [app key-char]
  (when (get-in app [:menu :open?])
    (when (= (int key-char) 27)
      (close-menu app))))

(def menu-bar-h lay/menu-bar-h)

(defn menu-bar-bounds []
  {:x 0 :y 0 :w 60 :h menu-bar-h})

(def ^:private items
  [{:label "Save"     :action :save}
   {:label "Save As..." :action :save-as}
   {:label "Open..."  :action :open}
   {:label "New Game" :action :new-game}
   {:label "Quit"     :action :quit}])

(defn menu-items [] items)

(def ^:private item-h 22)
(def ^:private dropdown-w 200)

(defn- item-index-at [mx my]
  (when (and (<= 0 mx dropdown-w)
             (> my menu-bar-h)
             (<= my (+ menu-bar-h (* (count items) item-h))))
    (let [idx (int (/ (- my menu-bar-h) item-h))]
      (when (< idx (count items)) idx))))

(defn menu-item-hit [mx my]
  (when-let [idx (item-index-at mx my)]
    (:action (nth items idx))))

(defn menu-hover-index [mx my]
  (item-index-at mx my))

(def ^:private open-item-idx 2)
(def ^:private submenu-x dropdown-w)
(def ^:private submenu-w 200)

(defn get-save-files []
  (ps/list-saves))

(defn- submenu-top []
  (+ menu-bar-h (* open-item-idx item-h)))

(defn- submenu-total-items [items]
  (inc (count items)))  ;; files + "Browse..."

(defn submenu-hover-index [items mx my]
  (let [top (submenu-top)
        n (submenu-total-items items)]
    (when (and (<= submenu-x mx (+ submenu-x submenu-w))
               (> my top)
               (<= my (+ top (* n item-h))))
      (int (/ (- my top) item-h)))))

(defn submenu-item-hit [items mx my]
  (when-let [idx (submenu-hover-index items mx my)]
    (if (< idx (count items))
      (nth items idx)
      :browse)))

(defn- in-submenu-area? [items mx my]
  (some? (submenu-hover-index items mx my)))

(defn update-hover [app mx my]
  (let [idx (menu-hover-index mx my)
        submenu (get-in app [:menu :submenu])
        sub-items (:items submenu)]
    (cond
      ;; Hovering over Open... item -> open submenu
      (= idx open-item-idx)
      (let [files (get-save-files)]
        (-> app
            (assoc-in [:menu :hover] idx)
            (assoc-in [:menu :submenu]
                      {:open? true :items files
                       :hover (submenu-hover-index files mx my)})))

      ;; Mouse in submenu area -> keep submenu open, update submenu hover
      (and (:open? submenu) (in-submenu-area? sub-items mx my))
      (-> app
          (assoc-in [:menu :hover] open-item-idx)
          (assoc-in [:menu :submenu :hover]
                    (submenu-hover-index sub-items mx my)))

      ;; Otherwise -> close submenu, normal hover
      :else
      (-> app
          (assoc-in [:menu :hover] idx)
          (assoc-in [:menu :submenu :open?] false)))))

(defn- draw-submenu [submenu]
  (when (:open? submenu)
    (let [files (:items submenu)
          n (submenu-total-items files)
          top (submenu-top)
          hover (:hover submenu)]
      (q/fill 250 250 250)
      (q/stroke 180)
      (q/rect submenu-x top submenu-w (* n item-h) 2)
      (doseq [i (range (count files))]
        (let [y (+ top (* i item-h))]
          (when (= i hover)
            (q/fill 200 215 245)
            (q/no-stroke)
            (q/rect submenu-x y submenu-w item-h))
          (q/stroke 230)
          (q/line submenu-x (+ y item-h)
                  (+ submenu-x submenu-w) (+ y item-h))
          (q/fill 0) (q/no-stroke)
          (q/text-size 13)
          (q/text (nth files i) (+ submenu-x 12) (+ y 16))))
      ;; "Browse..." at the end
      (let [y (+ top (* (count files) item-h))]
        (when (= (count files) hover)
          (q/fill 200 215 245)
          (q/no-stroke)
          (q/rect submenu-x y submenu-w item-h))
        (q/stroke 230)
        (q/line submenu-x (+ y item-h)
                (+ submenu-x submenu-w) (+ y item-h))
        (q/fill 0) (q/no-stroke)
        (q/text-size 13)
        (q/text "Browse..." (+ submenu-x 12) (+ y 16))))))

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
      (let [n (count items)
            hover (get-in app [:menu :hover])]
        (q/fill 250 250 250)
        (q/stroke 180)
        (q/rect 0 menu-bar-h dropdown-w (* n item-h) 2)
        (doseq [i (range n)]
          (let [y (+ menu-bar-h (* i item-h))]
            (when (= i hover)
              (q/fill 200 215 245)
              (q/no-stroke)
              (q/rect 0 y dropdown-w item-h))
            (q/stroke 230)
            (q/line 0 (+ y item-h) dropdown-w (+ y item-h))
            (q/fill 0)
            (q/no-stroke)
            (q/text-size 13)
            (q/text (:label (nth items i)) 12 (+ y 16))))
        (draw-submenu (get-in app [:menu :submenu]))))))
