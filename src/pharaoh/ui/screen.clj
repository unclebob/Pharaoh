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
    (q/stroke 120)
    (q/no-fill)
    (q/rect x y w h)
    (q/fill 220 200 160)
    (q/text-size 10)
    (q/text title (+ x 4) (+ y 12))))

(defn- draw-label [col row label]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (q/fill 180 170 140)
    (q/text-size 9)
    (q/text (str label) (+ x 2) (+ y 12))))

(defn- draw-val [col row value]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (q/fill 255)
    (q/text-size 9)
    (q/text (str value) (+ x 2) (+ y 12))))

(defn- draw-delta [col row value]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (when (seq (str value))
      (q/fill 200 200 100)
      (q/text-size 8)
      (q/text (str value) (+ x 2) (+ y 12)))))

(defn- draw-compact [col row label value]
  (let [{:keys [x y]} (lay/cell-rect col row)]
    (q/fill 180 170 140)
    (q/text-size 9)
    (q/text (str label) (+ x 2) (+ y 12))
    (q/fill 255)
    (q/text (str value) (+ x 30) (+ y 12))))

(defn draw-screen [state]
  (q/background 40 30 20)

  ;; Section frames
  (doseq [[section [c r w h]] lay/sections]
    (draw-section-frame c r w h (name section)))

  (let [s state]
    ;; Commodities: label col0, value col1, delta col2, old col3
    (draw-label 0 1 "Wheat")   (draw-val 1 1 (fmt (:wheat s)))
    (draw-delta 2 1 (delta-pct (:wheat s) (:old-wheat s)))
    (draw-val 3 1 (fmt (:old-wheat s)))

    (draw-label 0 2 "Manure")  (draw-val 1 2 (fmt (:manure s)))

    (draw-label 0 3 "Slaves")  (draw-val 1 3 (fmt (:slaves s)))
    (draw-delta 2 3 (delta-pct (:slaves s) (:old-slaves s)))
    (draw-val 3 3 (fmt (:old-slaves s)))

    (draw-label 0 4 "Horses")  (draw-val 1 4 (fmt (:horses s)))
    (draw-label 0 5 "Oxen")    (draw-val 1 5 (fmt (:oxen s)))
    (draw-label 0 6 "Land")    (draw-val 1 6 (fmt (st/total-land s)))

    ;; Prices: label col6, value col7
    (draw-label 6 1 "Wt$")  (draw-val 7 1 (fmt1 (get-in s [:prices :wheat])))
    (draw-label 6 2 "Mn$")  (draw-val 7 2 (fmt1 (get-in s [:prices :manure])))
    (draw-label 6 3 "Sl$")  (draw-val 7 3 (fmt (get-in s [:prices :slaves])))
    (draw-label 6 4 "Hs$")  (draw-val 7 4 (fmt (get-in s [:prices :horses])))
    (draw-label 6 5 "Ox$")  (draw-val 7 5 (fmt (get-in s [:prices :oxen])))
    (draw-label 6 6 "Ln$")  (draw-val 7 6 (fmt (get-in s [:prices :land])))

    ;; Feed rates (compact — short labels fit in one cell)
    (draw-compact 0 8 "SlFd" (fmt1 (:sl-feed-rt s)))
    (draw-compact 1 8 "HsFd" (fmt1 (:hs-feed-rt s)))
    (draw-compact 0 9 "OxFd" (fmt1 (:ox-feed-rt s)))

    ;; Overseers
    (draw-compact 3 8 "Ovrs" (fmt (:overseers s)))
    (draw-compact 3 9 "Pay"  (fmt (:ov-pay s)))

    ;; Loan
    (draw-compact 6 8 "Loan" (fmt (:loan s)))
    (draw-compact 6 9 "Int%" (fmt1 (+ (:interest s) (:int-addition s))))

    ;; Land stages
    (draw-label 0 11 "Fallow") (draw-val 1 11 (fmt (:ln-fallow s)))
    (draw-label 0 12 "Sewn")   (draw-val 1 12 (fmt (:ln-sewn s)))
    (draw-label 2 11 "Grown")  (draw-val 3 11 (fmt (:ln-grown s)))
    (draw-label 2 12 "Ripe")   (draw-val 3 12 (fmt (:ln-ripe s)))

    ;; Date
    (draw-label 5 11 "Date") (draw-val 6 11 (str (:month s) "/" (:year s)))

    ;; Pyramid — data in row 13 (row 12 is section title)
    (draw-label 5 13 "Stones") (draw-val 6 13 (fmt (:py-stones s)))
    (draw-label 7 13 "Ht")     (draw-val 8 13 (fmt1 (:py-height s)))
    (let [{:keys [x y w h]} (lay/cell-rect-span 5 14 5 2)]
      (pyr/draw-pyramid x y w h (:py-base s) (:py-stones s)))

    ;; Gold — data in row 15 (row 14 is section title)
    (draw-label 0 15 "Gold")   (draw-val 1 15 (fmt (:gold s)))
    (draw-delta 2 15 (delta-pct (:gold s) (:old-gold s)))
    (draw-val 3 15 (fmt (:old-gold s)))
    (draw-label 0 16 "NetWth") (draw-val 1 16 (fmt (get s :net-worth 0)))

    ;; Controls
    (let [{:keys [x y w h]} (lay/cell-rect-span 7 17 3 1)]
      (q/fill 60 120 60)
      (q/rect x y w h 5)
      (q/fill 255)
      (q/text-size 10)
      (q/text "RUN (r)" (+ x 25) (+ y 12)))
    (let [{:keys [x y w h]} (lay/cell-rect-span 7 18 3 1)]
      (q/fill 120 60 60)
      (q/rect x y w h 5)
      (q/fill 255)
      (q/text-size 10)
      (q/text "QUIT" (+ x 30) (+ y 12)))

    ;; Message bar
    (when-let [msg (:message s)]
      (let [{:keys [x y w h]} (lay/cell-rect-span 0 23 10 2)]
        (q/fill 255 255 200)
        (q/text-size 10)
        (q/text (str msg) (+ x 4) (+ y 14))))))
