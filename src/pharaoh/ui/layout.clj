(ns pharaoh.ui.layout)

;; Grid dimensions matching original (10x25 cells)
(def x-cells 10)
(def y-cells 25)
(def cell-w 60)
(def cell-h 16)
(def frame-x 8)
(def frame-y 8)

(def win-w (+ (* x-cells cell-w) (* 2 frame-x)))
(def win-h (+ (* y-cells cell-h) (* 2 frame-y) 30))

(defn cell-rect [col row]
  {:x (+ (* col cell-w) frame-x)
   :y (+ (* row cell-h) frame-y)
   :w cell-w :h cell-h})

(defn cell-rect-span [col row cols rows]
  {:x (+ (* col cell-w) frame-x)
   :y (+ (* row cell-h) frame-y)
   :w (* cols cell-w) :h (* rows cell-h)})

;; Section positions (col, row, width-cols, height-rows)
(def sections
  {:commodities [0 0 6 7]
   :prices      [6 0 4 7]
   :feed-rates  [0 7 3 3]
   :overseers   [3 7 3 3]
   :loan        [6 7 4 3]
   :land        [0 10 5 4]
   :date        [5 10 2 2]
   :pyramid     [5 12 5 4]
   :gold        [0 14 5 3]
   :contracts   [0 17 7 6]
   :controls    [7 17 3 2]
   :status      [7 19 3 1]
   :message     [0 23 10 2]})

;; Keyboard shortcuts
(def key-commands
  {\w :wheat \s :slaves \o :oxen \h :horses \m :manure
   \l :land \f :feed \p :plant \q :pyramid-quota
   \L :loan \O :overseers \S :spread \H :harvest
   \r :run \R :run})
