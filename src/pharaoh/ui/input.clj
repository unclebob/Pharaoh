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
   \O #(dlg/open-dialog % :overseer)
   \p #(dlg/open-dialog % :plant)
   \S #(dlg/open-dialog % :spread)
   \q #(dlg/open-dialog % :pyramid)
   \f #(open-feed % :slaves)})

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
    (= key-char \return) (dlg/execute-dialog rng (dissoc state :message))
    (= key-char \newline) (dlg/close-dialog state)
    :else
    (if-let [mode (dialog-mode-for (get-in state [:dialog :type]) key-char)]
      (dlg/set-dialog-mode state mode)
      (dlg/update-dialog-input state key-char))))

(defn handle-key [rng state key-char]
  (cond
    (:dialog state)
    (handle-dialog-key rng state key-char)

    :else
    (if-let [action (get key-actions key-char)]
      (action state)
      (case key-char
        \r (sim/do-run rng state)
        \R (sim/do-run rng state)
        (dissoc state :message)))))

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

      :else state)))
