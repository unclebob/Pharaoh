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
   \H #(open-feed % :horses)
   \c #(dlg/open-contracts-dialog %)})

(def esc-char (char 27))

(defn- dialog-mode-for [dtype key-char]
  (case key-char
    \b (case dtype :buy-sell :buy :loan :borrow nil)
    \s (case dtype :buy-sell :sell nil)
    \r (case dtype :loan :repay nil)
    \h (case dtype :overseer :hire nil)
    \f (case dtype :overseer :fire nil)
    nil))

(defn- handle-contracts-key [state key-char key-kw]
  (let [mode (get-in state [:dialog :mode])]
    (cond
      (= key-char esc-char)
      (if (= mode :confirming)
        (dlg/reject-selected state)
        (dlg/close-dialog state))

      (and (= mode :browsing) (or (= key-char \return) (= key-char \newline)))
      (dlg/confirm-selected state)

      (and (= mode :confirming) (= key-char \y))
      (dlg/accept-selected state)

      (and (= mode :confirming) (= key-char \n))
      (dlg/reject-selected state)

      (and (= mode :browsing) (= key-kw :down))
      (dlg/navigate-contracts state :down)

      (and (= mode :browsing) (= key-kw :up))
      (dlg/navigate-contracts state :up)

      :else state)))

(defn- handle-dialog-key [rng state key-char key-kw]
  (if (= :contracts (get-in state [:dialog :type]))
    (handle-contracts-key state key-char key-kw)
    (cond
      (= key-char esc-char) (dlg/close-dialog state)
      (or (= key-char \return) (= key-char \newline))
      (dlg/execute-dialog rng (dissoc state :message))
      :else
      (if-let [mode (dialog-mode-for (get-in state [:dialog :type]) key-char)]
        (dlg/set-dialog-mode state mode)
        (dlg/update-dialog-input state key-char)))))

(defn handle-key [rng state key-char & [key-kw]]
  (cond
    (:dialog state)
    (handle-dialog-key rng state key-char key-kw)

    (map? (:message state))
    (let [state (dissoc state :message)
          state (if (seq (:contract-msgs state))
                  (let [[msg & rest] (:contract-msgs state)]
                    (assoc state :message msg :contract-msgs (vec rest)))
                  state)]
      (cond-> state
        (:game-over state) (assoc :quit-clicked true)))

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

(defn- handle-contract-click [state my]
  (let [{:keys [y]} (lay/cell-rect-span 2 5 7 14)
        y0 (+ y (* lay/title-size 2) lay/small-size 8)
        row-h (+ lay/label-size 4)
        idx (int (/ (- my y0) row-h))
        n (count (get-in state [:dialog :active-offers]))
        sel (get-in state [:dialog :selected])]
    (if (and (>= idx 0) (< idx n))
      (if (= idx sel)
        (dlg/confirm-selected state)
        (assoc-in state [:dialog :selected] idx))
      state)))

(defn handle-mouse [state mx my]
  (let [col (int (/ (- mx lay/pad) lay/cell-w))
        row (int (/ (- my lay/pad) lay/cell-h))]
    (cond
      (and (= :contracts (get-in state [:dialog :type]))
           (= :browsing (get-in state [:dialog :mode])))
      (handle-contract-click state my)

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

      ;; Contracts section
      (in-section? col row :contracts)
      (dlg/open-contracts-dialog state)

      :else state)))
