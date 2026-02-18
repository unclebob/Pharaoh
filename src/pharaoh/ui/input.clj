(ns pharaoh.ui.input
  (:require [pharaoh.ui.layout :as lay]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.simulation :as sim]
            [pharaoh.random :as r]))

(defn- open-trade [state commodity]
  (dlg/open-dialog state :buy-sell {:commodity commodity}))

(defn- open-feed [state commodity]
  (dlg/open-dialog state :feed {:commodity commodity}))

(def key-actions
  {\w #(open-trade % :wheat)
   \s #(open-trade % :slaves)
   \o #(open-trade % :oxen)
   \h #(open-trade % :horses)
   \m #(open-trade % :manure)
   \l #(open-trade % :land)
   \L #(dlg/open-dialog % :loan)
   \g #(dlg/open-dialog % :overseer)
   \p #(dlg/open-dialog % :plant)
   \f #(dlg/open-dialog % :spread)
   \q #(dlg/open-dialog % :pyramid)
   \S #(open-feed % :slaves)
   \O #(open-feed % :oxen)
   \H #(open-feed % :horses)})

(def esc-char (char 27))

(defn- dialog-mode-for [dtype key-char]
  (case key-char
    \b (case dtype :buy-sell :buy :loan :borrow nil)
    \s (case dtype :buy-sell :sell nil)
    \r (case dtype :loan :repay nil)
    \h (case dtype :overseer :hire nil)
    \f (case dtype :overseer :fire nil)
    nil))

(defn- handle-dialog-key [rng state key-char]
  (cond
    (= key-char esc-char) (dlg/close-dialog state)
    (or (= key-char \return) (= key-char \newline))
    (dlg/execute-dialog rng (dissoc state :message))
    :else
    (if-let [mode (dialog-mode-for (get-in state [:dialog :type]) key-char)]
      (dlg/set-dialog-mode state mode)
      (dlg/update-dialog-input state key-char))))

(defn handle-key [rng state key-char]
  (cond
    (:dialog state)
    (handle-dialog-key rng state key-char)

    (map? (:message state))
    (dissoc state :message)

    :else
    (if-let [action (get key-actions key-char)]
      (action state)
      (case key-char
        \r (sim/do-run rng state)
        \R (sim/do-run rng state)
        (dissoc state :message)))))

(defn- in-section? [col row sec-key]
  (let [[sc sr sw sh] (get lay/sections sec-key)]
    (and (<= sc col (+ sc sw -1))
         (<= sr row (+ sr sh -1)))))

(defn- commodity-for-row [row]
  (case row 1 :wheat 2 :manure 3 :slaves 4 :horses 5 :oxen 6 :land nil))

(defn handle-mouse [state mx my]
  (let [col (int (/ (- mx lay/pad) lay/cell-w))
        row (int (/ (- my lay/pad) lay/cell-h))]
    (cond
      ;; RUN button (cols 8-9, row 23)
      (and (<= 8 col 9) (= row 23))
      (assoc state :run-clicked true)

      ;; QUIT button (cols 0-1, row 23)
      (and (<= 0 col 1) (= row 23))
      (assoc state :quit-clicked true)

      ;; Commodities section — open buy/sell for clicked row
      (in-section? col row :commodities)
      (if-let [c (commodity-for-row row)]
        (open-trade state c)
        state)

      ;; Prices section — open buy/sell for clicked row
      (in-section? col row :prices)
      (if-let [c (commodity-for-row row)]
        (open-trade state c)
        state)

      ;; Feed rates section (cols 6-7, rows 1-3)
      (in-section? col row :feed-rates)
      (case row
        1 (open-feed state :slaves)
        2 (open-feed state :oxen)
        3 (open-feed state :horses)
        state)

      ;; Overseers section
      (in-section? col row :overseers)
      (dlg/open-dialog state :overseer)

      ;; Loan section
      (in-section? col row :loan)
      (dlg/open-dialog state :loan)

      ;; Land section
      (in-section? col row :land)
      (open-trade state :land)

      ;; Spread & Plant section
      (in-section? col row :spread-plant)
      (if (= col 5)
        (dlg/open-dialog state :spread)
        (dlg/open-dialog state :plant))

      ;; Gold section
      (in-section? col row :gold)
      (dlg/open-dialog state :loan)

      ;; Pyramid section
      (in-section? col row :pyramid)
      (dlg/open-dialog state :pyramid)

      :else state)))
