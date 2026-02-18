(ns pharaoh.ui.screen
  (:require [quil.core :as q]
            [pharaoh.ui.layout :as lay]
            [pharaoh.ui.pyramid-render :as pyr]
            [pharaoh.state :as st]))

(defn- fmt [x] (format "%.0f" (double x)))
(defn- fmt1 [x] (format "%.1f" (double x)))
(defn- delta-pct [cur old]
  (if (and (pos? old) (not= cur old))
    (format "%+.0f%%" (* 100 (/ (- cur old) old)))
    ""))

(defn- draw-section-frame [col row cols rows title]
  (let [{:keys [x y w h]} (lay/cell-rect-span col row cols rows)]
    (q/stroke 160)
    (q/stroke-weight 1)
    (q/no-fill)
    (q/rect x y w h)
    ;; Gray lines between rows (skip title row)
    (q/stroke 210)
    (doseq [r (range 1 rows)]
      (let [ly (+ y (* r lay/cell-h))]
        (q/line x ly (+ x w) ly)))
    (q/fill 60)
    (q/text-size lay/title-size)
    (q/text title (+ x 4) (+ y lay/title-size 2))))

(defn- draw-label [col row label]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (q/fill 80)
    (q/text-size lay/label-size)
    (q/text (str label) (+ x 3) (+ y lay/label-size 2))))

(defn- draw-val [col row value]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (q/fill 0)
    (q/text-size lay/value-size)
    (q/text (str value) (+ x 3) (+ y lay/value-size 2))))

(defn- draw-delta [col row value]
  (let [s (str value)]
    (when (seq s)
      (let [{:keys [x y]} (lay/cell-rect col row)]
        (if (.startsWith s "+")
          (q/fill 0 120 0)
          (q/fill 180 0 0))
        (q/text-size lay/small-size)
        (q/text s (+ x 3) (+ y lay/small-size 2))))))

(defn- draw-compact [col row label value]
  (let [{:keys [x y w]} (lay/cell-rect col row)
        mid (/ w 2)]
    (q/fill 80)
    (q/text-size lay/label-size)
    (q/text (str label) (+ x 3) (+ y lay/label-size 2))
    (q/fill 0)
    (q/text (str value) (+ x mid) (+ y lay/label-size 2))))

(defn draw-screen [state]
  (q/background 255)

  ;; Section frames
  (doseq [[section [c r w h]] lay/sections]
    (let [title (case section
                  :spread-plant "Spread & Plant"
                  :feed-rates "Feed Rates"
                  (clojure.string/capitalize (name section)))]
      (draw-section-frame c r w h title)))

  (let [s state]
    ;; === Commodities (cols 0-3, rows 1-6) ===
    (draw-label 0 1 "Wheat")   (draw-val 1 1 (fmt (:wheat s)))
    (draw-delta 2 1 (delta-pct (:wheat s) (:old-wheat s)))
    (draw-val 3 1 (fmt (:old-wheat s)))

    (draw-label 0 2 "Manure")  (draw-val 1 2 (fmt (:manure s)))
    (draw-delta 2 2 (delta-pct (:manure s) (:old-manure s)))
    (draw-val 3 2 (fmt (:old-manure s)))

    (draw-label 0 3 "Slaves")  (draw-val 1 3 (fmt (:slaves s)))
    (draw-delta 2 3 (delta-pct (:slaves s) (:old-slaves s)))
    (draw-val 3 3 (fmt (:old-slaves s)))

    (draw-label 0 4 "Horses")  (draw-val 1 4 (fmt (:horses s)))
    (draw-delta 2 4 (delta-pct (:horses s) (:old-horses s)))
    (draw-val 3 4 (fmt (:old-horses s)))

    (draw-label 0 5 "Oxen")    (draw-val 1 5 (fmt (:oxen s)))
    (draw-delta 2 5 (delta-pct (:oxen s) (:old-oxen s)))
    (draw-val 3 5 (fmt (:old-oxen s)))

    (draw-label 0 6 "Land")    (draw-val 1 6 (fmt (st/total-land s)))

    ;; === Prices (cols 4-5, rows 1-6) ===
    (draw-label 4 1 "Wheat")  (draw-val 5 1 (fmt1 (get-in s [:prices :wheat])))
    (draw-label 4 2 "Manure") (draw-val 5 2 (fmt1 (get-in s [:prices :manure])))
    (draw-label 4 3 "Slaves") (draw-val 5 3 (fmt (get-in s [:prices :slaves])))
    (draw-label 4 4 "Horses") (draw-val 5 4 (fmt (get-in s [:prices :horses])))
    (draw-label 4 5 "Oxen")   (draw-val 5 5 (fmt (get-in s [:prices :oxen])))
    (draw-label 4 6 "Land")   (draw-val 5 6 (fmt (get-in s [:prices :land])))

    ;; === Feed Rates (cols 6-7, rows 1-3) ===
    (draw-label 6 1 "Slaves") (draw-val 7 1 (fmt1 (:sl-feed-rt s)))
    (draw-label 6 2 "Oxen")   (draw-val 7 2 (fmt1 (:ox-feed-rt s)))
    (draw-label 6 3 "Horses") (draw-val 7 3 (fmt1 (:hs-feed-rt s)))

    ;; === Date (cols 8-9, rows 1-2) ===
    (draw-label 8 1 "Year")  (draw-val 9 1 (str (:year s)))
    (draw-label 8 2 "Month") (draw-val 9 2 (str (:month s)))

    ;; === Overseers (cols 6-7, rows 5-7) ===
    (draw-label 6 5 "O'seers") (draw-val 7 5 (fmt (:overseers s)))
    (draw-label 6 6 "Salary")  (draw-val 7 6 (fmt (:ov-pay s)))
    (draw-label 6 7 "Press")   (draw-val 7 7 (fmt1 (:ov-press s)))

    ;; === Loan (cols 8-9, rows 4-7) ===
    (draw-label 8 4 "Loan")  (draw-val 9 4 (fmt (:loan s)))
    (draw-label 8 5 "Int%")  (draw-val 9 5 (fmt1 (+ (:interest s) (:int-addition s))))
    (draw-label 8 6 "Credit") (draw-val 9 6 (fmt (:credit-limit s)))
    (draw-label 8 7 "Rating") (draw-val 9 7 (fmt1 (:credit-rating s)))

    ;; === Land (cols 0-4, rows 9-10) ===
    (draw-label 0 9 "Fallow")  (draw-val 0 10 (fmt (:ln-fallow s)))
    (draw-label 1 9 "Planted") (draw-val 1 10 (fmt (:ln-sewn s)))
    (draw-label 2 9 "Growing") (draw-val 2 10 (fmt (:ln-grown s)))
    (draw-label 3 9 "Ripe")    (draw-val 3 10 (fmt (:ln-ripe s)))
    (draw-label 4 9 "Total")   (draw-val 4 10 (fmt (st/total-land s)))

    ;; === Spread & Plant (cols 5-6, rows 9-10) ===
    (draw-label 5 9 "Manure") (draw-val 5 10 (fmt (:mn-to-sprd s)))
    (draw-label 6 9 "Land")   (draw-val 6 10 (fmt (:ln-to-sew s)))

    ;; === Gold (cols 7-9, rows 9-10) ===
    (draw-label 7 9 "Gold")    (draw-val 8 9 (fmt (:gold s)))
    (draw-delta 9 9 (delta-pct (:gold s) (:old-gold s)))
    (draw-label 7 10 "NetWth") (draw-val 8 10 (fmt (get s :net-worth 0)))

    ;; === Pyramid (cols 0-2, rows 12-23) ===
    (draw-label 0 12 "Quota")  (draw-label 1 12 "Stones")
    (draw-val   0 13 (fmt (:py-quota s)))
    (draw-val   1 13 (fmt (:py-stones s)))
    (draw-label 1 14 "Height") (draw-val 2 14 (fmt1 (:py-height s)))
    (let [{:keys [x y w h]} (lay/cell-rect-span 0 15 3 8)]
      (pyr/draw-pyramid x y w h (:py-base s) (:py-stones s)))

    ;; === Contracts (cols 3-9, rows 12-22) ===
    (let [pend (:cont-pend s)]
      (doseq [i (range (min 10 (count pend)))]
        (let [c (nth pend i)
              text (str (name (:type c)) " "
                        (fmt (:amount c)) " " (name (:what c))
                        " @ " (fmt (:price c)) "g")]
          (draw-label 3 (+ 12 i) text))))

    ;; === Controls (row 23-24) ===
    ;; Quit button (cols 0-1)
    (let [{:keys [x y w h]} (lay/cell-rect-span 0 23 2 1)]
      (q/fill 200)
      (q/stroke 160)
      (q/rect x y w h 5)
      (q/fill 0)
      (q/text-size lay/title-size)
      (q/text "QUIT" (+ x (/ w 4)) (+ y lay/title-size 4)))

    ;; Run button (cols 8-9)
    (let [{:keys [x y w h]} (lay/cell-rect-span 8 23 2 1)]
      (q/fill 100 180 100)
      (q/stroke 80 140 80)
      (q/rect x y w h 5)
      (q/fill 0)
      (q/text-size lay/title-size)
      (q/text "RUN (r)" (+ x (/ w 6)) (+ y lay/title-size 4)))

    ;; Status
    (let [{:keys [x y w]} (lay/cell-rect-span 3 23 4 1)]
      (q/fill 100)
      (q/text-size lay/small-size)
      (q/text (str "Yr " (:year s) " Mo " (:month s))
              (+ x 4) (+ y lay/small-size 4)))

    ;; Message bar (row 24) — plain string messages only
    (when-let [msg (:message s)]
      (when (string? msg)
        (let [{:keys [x y w h]} (lay/cell-rect-span 0 24 10 1)]
          (q/fill 255 255 220)
          (q/stroke 200)
          (q/rect x y w h)
          (q/fill 0)
          (q/text-size lay/label-size)
          (q/text msg (+ x 4) (+ y lay/label-size 4)))))))
