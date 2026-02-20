(ns pharaoh.ui.layout)

;; Default window size
(def default-w 1024)
(def default-h 768)

;; Grid dimensions
(def x-cells 10)
(def y-cells 25)

;; Computed from window size
(def pad 16)
(def menu-bar-h 22)
(def top-pad (+ pad menu-bar-h))
(def win-w default-w)
(def win-h default-h)
(def cell-w (/ (- win-w (* 2 pad)) x-cells))
(def cell-h (/ (- win-h pad top-pad) y-cells))

(defn cell-rect [col row]
  {:x (+ (* col cell-w) pad)
   :y (+ (* row cell-h) top-pad)
   :w cell-w :h cell-h})

(defn cell-rect-span [col row cols rows]
  {:x (+ (* col cell-w) pad)
   :y (+ (* row cell-h) top-pad)
   :w (* cols cell-w) :h (* rows cell-h)})

;; Section positions matching spec wireframe [col row width-cols height-rows]
(def sections
  {:commodities  [0 0 4 8]
   :prices       [4 0 2 8]
   :feed-rates   [6 0 2 4]
   :date         [8 0 2 3]
   :overseers    [6 4 2 4]
   :loan         [8 3 2 5]
   :land         [0 8 5 3]
   :spread-plant [5 8 2 3]
   :gold         [7 8 3 3]
   :pyramid      [0 11 3 13]
   :contracts    [3 11 7 12]
   :controls     [0 23 10 2]})

;; Font sizes scaled to cell height
(def title-size (max 10 (int (* cell-h 0.55))))
(def label-size (max 9 (int (* cell-h 0.48))))
(def value-size (max 9 (int (* cell-h 0.48))))
(def small-size (max 8 (int (* cell-h 0.38))))

;; Keyboard shortcuts
(def key-commands
  {\w :wheat \s :slaves \o :oxen \h :horses \m :manure
   \l :land \f :spread \p :plant \q :pyramid-quota
   \L :loan \g :overseers \S :slave-feed \O :oxen-feed \H :horse-feed
   \r :run \R :run})
