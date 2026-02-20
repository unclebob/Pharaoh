(ns pharaoh.ui.input
  (:require [pharaoh.ui.layout :as lay]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.ui.file-actions :as fa]
            [pharaoh.simulation :as sim]
            [pharaoh.random :as r]))

(defn dialog-input-bounds []
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
        icon-size (int (* h 0.4))
        text-x (+ x icon-size 16)
        amount-y (+ y (* lay/value-size 3) 8)
        box-x (+ text-x 68)
        box-y (- amount-y lay/value-size 2)
        box-w (- (+ x w) box-x 12)
        box-h (+ lay/value-size 8)]
    {:x box-x :y box-y :w box-w :h box-h}))

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

(defn- credit-check-mode? [state]
  (and (= :loan (get-in state [:dialog :type]))
       (= :credit-check (get-in state [:dialog :mode]))))

(defn- handle-confirm-key [state key-char handler-yes handler-no]
  (case key-char
    \y (handler-yes state)
    \n (handler-no state)
    (if (= key-char esc-char) (dlg/close-dialog state) state)))

(defn- handle-file-dialog-key [rng state key-char]
  (cond
    (= key-char esc-char) (dlg/close-dialog state)
    (or (= key-char \return) (= key-char \newline))
    (dlg/execute-dialog rng state)
    :else (dlg/update-dialog-input state key-char)))

(defn- handle-overwrite-yes [rng state]
  (let [path (get-in state [:dialog :path])]
    (dlg/execute-dialog rng (-> state
                                (dissoc :dialog)
                                (dlg/open-dialog :save-file {:input path})))))

(defn- handle-generic-dialog-key [rng state key-char]
  (if (credit-check-mode? state)
    (case key-char
      \y (dlg/accept-credit-check rng state)
      \n (dlg/reject-credit-check state)
      (if (= key-char esc-char) (dlg/reject-credit-check state) state))
    (cond
      (= key-char esc-char) (dlg/close-dialog state)
      (or (= key-char \return) (= key-char \newline))
      (dlg/execute-dialog rng (dissoc state :message))
      :else
      (if-let [mode (dialog-mode-for (get-in state [:dialog :type]) key-char)]
        (dlg/set-dialog-mode state mode)
        (dlg/update-dialog-input state key-char)))))

(defn- handle-dialog-key [rng state key-char key-kw]
  (let [dtype (get-in state [:dialog :type])]
    (case dtype
      :contracts (handle-contracts-key state key-char key-kw)
      :confirm-save (handle-confirm-key state key-char
                                        dlg/handle-confirm-save-yes
                                        dlg/handle-confirm-save-no)
      :confirm-overwrite (handle-confirm-key state key-char
                                             (partial handle-overwrite-yes rng)
                                             dlg/close-dialog)
      (:save-file :load-file) (handle-file-dialog-key rng state key-char)
      (handle-generic-dialog-key rng state key-char))))

(defn handle-ctrl-key [state key-char]
  (case key-char
    \s (fa/do-save state)
    \o (fa/do-open state)
    \n (fa/do-new-game state)
    nil))

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
      (cond-> (assoc state :reset-visit-timers true)
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

(defn- confirm-button-bounds []
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 5 7 14)
        btn-y (+ y h -20 (- lay/title-size) -8)]
    {:accept {:x (+ x 8) :y btn-y :w 100 :h lay/title-size}
     :reject {:x (+ x 120) :y btn-y :w 100 :h lay/title-size}
     :cancel {:x (+ x 232) :y btn-y :w 100 :h lay/title-size}}))

(defn- in-rect? [mx my {:keys [x y w h]}]
  (and (<= x mx (+ x w)) (<= y my (+ y h))))

(defn- handle-confirm-click [state mx my]
  (let [{:keys [accept reject cancel]} (confirm-button-bounds)]
    (cond
      (in-rect? mx my accept) (dlg/accept-selected state)
      (in-rect? mx my reject) (dlg/reject-selected state)
      (in-rect? mx my cancel) (dlg/close-dialog state)
      :else state)))

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

(defn- hover-row-index [my]
  (let [{:keys [y]} (lay/cell-rect-span 2 5 7 14)
        y0 (+ y (* lay/title-size 2) lay/small-size 8)
        row-h (+ lay/label-size 4)
        idx (int (/ (- my y0) row-h))]
    idx))

(defn handle-mouse-move [state _mx my]
  (if (and (= :contracts (get-in state [:dialog :type]))
           (= :browsing (get-in state [:dialog :mode])))
    (let [idx (hover-row-index my)
          n (count (get-in state [:dialog :active-offers]))]
      (if (and (>= idx 0) (< idx n))
        (assoc-in state [:dialog :selected] idx)
        state))
    state))

(def ^:private mode-dialog-types #{:buy-sell :loan :overseer})

(defn dialog-button-bounds [dtype]
  (let [{:keys [x y h]} (lay/cell-rect-span 2 8 6 5)
        btn-y (+ y h -20 (- lay/title-size) -8)]
    (if (mode-dialog-types dtype)
      {:radio1 {:x (+ x 8) :y btn-y :w 110 :h lay/title-size}
       :radio2 {:x (+ x 126) :y btn-y :w 110 :h lay/title-size}
       :ok     {:x (+ x 264) :y btn-y :w 110 :h lay/title-size}
       :cancel {:x (+ x 382) :y btn-y :w 120 :h lay/title-size}}
      {:ok     {:x (+ x 8) :y btn-y :w 120 :h lay/title-size}
       :cancel {:x (+ x 136) :y btn-y :w 120 :h lay/title-size}})))

(defn radio-mode-for [dtype which]
  (case [dtype which]
    [:buy-sell :radio1] :buy    [:buy-sell :radio2] :sell
    [:loan :radio1]     :borrow [:loan :radio2]     :repay
    [:overseer :radio1] :hire   [:overseer :radio2]  :fire
    nil))

(defn handle-dialog-click [state mx my rng]
  (let [dtype (get-in state [:dialog :type])
        bounds (dialog-button-bounds dtype)
        hit (some (fn [[k rect]] (when (in-rect? mx my rect) k))
                  bounds)]
    (case hit
      :radio1 (dlg/set-dialog-mode state (radio-mode-for dtype :radio1))
      :radio2 (dlg/set-dialog-mode state (radio-mode-for dtype :radio2))
      :ok     (dlg/execute-dialog rng (dissoc state :message))
      :cancel (dlg/close-dialog state)
      state)))

(defn- credit-check-button-bounds []
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
        btn-y (+ y h -20 (- lay/title-size) -8)]
    {:yes {:x (+ x 8) :y btn-y :w 100 :h lay/title-size}
     :no  {:x (+ x 120) :y btn-y :w 100 :h lay/title-size}}))

(defn- handle-credit-check-click [rng state mx my]
  (let [{:keys [yes no]} (credit-check-button-bounds)]
    (cond
      (in-rect? mx my yes) (dlg/accept-credit-check rng state)
      (in-rect? mx my no)  (dlg/reject-credit-check state)
      :else state)))

(defn- standard-dialog-open? [state]
  (and (:dialog state)
       (not= :contracts (get-in state [:dialog :type]))
       (not (credit-check-mode? state))))

(defn handle-mouse [state mx my & [rng]]
  (let [col (int (/ (- mx lay/pad) lay/cell-w))
        row (int (/ (- my lay/pad) lay/cell-h))]
    (cond
      (credit-check-mode? state)
      (handle-credit-check-click rng state mx my)

      (and (= :contracts (get-in state [:dialog :type]))
           (= :confirming (get-in state [:dialog :mode])))
      (handle-confirm-click state mx my)

      (and (= :contracts (get-in state [:dialog :type]))
           (= :browsing (get-in state [:dialog :mode])))
      (handle-contract-click state my)

      (standard-dialog-open? state)
      (handle-dialog-click state mx my rng)

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
