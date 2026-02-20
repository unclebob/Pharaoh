(ns pharaoh.ui.input-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.input :as inp]
            [pharaoh.ui.layout :as lay]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.persistence :as ps]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(def esc-char (char 27))

;; ---- dialog-mode-for (private) ----

(deftest dialog-mode-for-buy-sell
  (let [dmf #'pharaoh.ui.input/dialog-mode-for]
    (is (= :buy (dmf :buy-sell \b)))
    (is (= :sell (dmf :buy-sell \s)))
    (is (= :keep (dmf :buy-sell \k)))
    (is (nil? (dmf :buy-sell \r)))
    (is (nil? (dmf :buy-sell \h)))))

(deftest dialog-mode-for-loan
  (let [dmf #'pharaoh.ui.input/dialog-mode-for]
    (is (= :borrow (dmf :loan \b)))
    (is (= :repay (dmf :loan \r)))
    (is (nil? (dmf :loan \s)))))

(deftest dialog-mode-for-overseer
  (let [dmf #'pharaoh.ui.input/dialog-mode-for]
    (is (= :hire (dmf :overseer \h)))
    (is (= :fire (dmf :overseer \f)))
    (is (= :obtain (dmf :overseer \o)))
    (is (nil? (dmf :overseer \b)))))

(deftest dialog-mode-for-other-types-return-nil
  (let [dmf #'pharaoh.ui.input/dialog-mode-for]
    (is (nil? (dmf :feed \b)))
    (is (nil? (dmf :plant \s)))
    (is (nil? (dmf :pyramid \h)))))

;; ---- commodity-for-row (private) ----

(deftest commodity-for-row-mapping
  (let [cfr #'pharaoh.ui.input/commodity-for-row]
    (is (= :wheat (cfr 1)))
    (is (= :manure (cfr 2)))
    (is (= :slaves (cfr 3)))
    (is (= :horses (cfr 4)))
    (is (= :oxen (cfr 5)))
    (is (= :land (cfr 6)))
    (is (nil? (cfr 0)))
    (is (nil? (cfr 7)))))

;; ---- handle-key: dialog mode ----

(deftest handle-key-esc-closes-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-key rng state esc-char)]
    (is (nil? (:dialog result)))))

(deftest handle-key-digit-in-dialog-appends-input
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-key rng state \5)]
    (is (= "5" (get-in result [:dialog :input])))))

(deftest handle-key-mode-key-in-buy-sell-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-key rng state \b)]
    (is (= :buy (get-in result [:dialog :mode])))))

(deftest handle-key-mode-key-sell-in-buy-sell-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-key rng state \s)]
    (is (= :sell (get-in result [:dialog :mode])))))

(deftest handle-key-mode-key-in-loan-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :loan)
        borrow-result (inp/handle-key rng state \b)
        repay-result (inp/handle-key rng state \r)]
    (is (= :borrow (get-in borrow-result [:dialog :mode])))
    (is (= :repay (get-in repay-result [:dialog :mode])))))

(deftest handle-key-mode-key-in-overseer-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :overseer)
        hire-result (inp/handle-key rng state \h)
        fire-result (inp/handle-key rng state \f)]
    (is (= :hire (get-in hire-result [:dialog :mode])))
    (is (= :fire (get-in fire-result [:dialog :mode])))))

(deftest handle-key-enter-executes-dialog
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))
        result (inp/handle-key rng state \return)]
    (is (nil? (:dialog result)))
    (is (> (:wheat result) 0.0))))

(deftest handle-key-newline-executes-dialog
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))
        result (inp/handle-key rng state \newline)]
    (is (nil? (:dialog result)))))

(deftest handle-key-enter-clears-message-before-execute
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :plant)
                  (assoc-in [:dialog :input] "50")
                  (assoc :message "old error"))
        result (inp/handle-key rng state \return)]
    (is (nil? (:dialog result)))
    (is (== 50.0 (:ln-to-sew result)))))

;; ---- handle-key: credit-check mode in loan dialog ----

(deftest handle-key-y-in-credit-check-mode-accepts
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0 :loan 0.0
                         :gold 50000.0 :credit-rating 0.8
                         :credit-lower 500000.0
                         :wheat 1000.0 :slaves 50.0 :oxen 20.0 :horses 10.0
                         :sl-health 0.8 :ox-health 0.9 :hs-health 0.7
                         :ln-fallow 100.0 :manure 200.0
                         :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                                  :horses 500.0 :oxen 300.0 :land 5000.0})
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 1000.0))
        result (inp/handle-key rng state \y)]
    ;; Should close dialog (accept processes the credit check)
    (is (nil? (:dialog result)))
    ;; Should have a face message
    (is (map? (:message result)))))

(deftest handle-key-n-in-credit-check-mode-rejects
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 1000.0))
        result (inp/handle-key rng state \n)]
    ;; Dialog should still be open, mode nil
    (is (some? (:dialog result)))
    (is (nil? (get-in result [:dialog :mode])))))

(deftest handle-key-esc-in-credit-check-mode-rejects
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 1000.0))
        result (inp/handle-key rng state esc-char)]
    ;; Dialog should still be open, mode nil (reject, not close)
    (is (some? (:dialog result)))
    (is (nil? (get-in result [:dialog :mode])))))

;; ---- handle-key: face message mode ----

(deftest handle-key-dismisses-face-message
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message {:text "Hello" :face :good-guy})
        result (inp/handle-key rng state \x)]
    (is (nil? (:message result)))))

(deftest handle-key-face-message-any-key-dismisses
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message {:text "Yo" :face :banker})]
    (doseq [ch [\a \b \1 \space]]
      (is (nil? (:message (inp/handle-key rng state ch)))))))

(deftest handle-key-dismiss-face-message-sets-reset-visit-timers
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message {:text "Hello" :face 0})
        result (inp/handle-key rng state \x)]
    (is (true? (:reset-visit-timers result)))))

;; ---- handle-key: main mode, key-actions ----

(deftest handle-key-w-opens-wheat-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \w)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :wheat (get-in result [:dialog :commodity])))))

(deftest handle-key-s-opens-slaves-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \s)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :slaves (get-in result [:dialog :commodity])))))

(deftest handle-key-o-opens-oxen-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \o)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :oxen (get-in result [:dialog :commodity])))))

(deftest handle-key-h-opens-horses-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \h)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :horses (get-in result [:dialog :commodity])))))

(deftest handle-key-m-opens-manure-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \m)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :manure (get-in result [:dialog :commodity])))))

(deftest handle-key-l-opens-land-trade
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \l)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :land (get-in result [:dialog :commodity])))))

(deftest handle-key-L-opens-loan-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \L)]
    (is (= :loan (get-in result [:dialog :type])))))

(deftest handle-key-g-opens-overseer-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \g)]
    (is (= :overseer (get-in result [:dialog :type])))))

(deftest handle-key-p-opens-plant-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \p)]
    (is (= :plant (get-in result [:dialog :type])))))

(deftest handle-key-f-opens-spread-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \f)]
    (is (= :spread (get-in result [:dialog :type])))))

(deftest handle-key-q-opens-pyramid-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \q)]
    (is (= :pyramid (get-in result [:dialog :type])))))

(deftest handle-key-S-opens-slave-feed-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \S)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :slaves (get-in result [:dialog :commodity])))))

(deftest handle-key-O-opens-oxen-feed-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \O)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :oxen (get-in result [:dialog :commodity])))))

(deftest handle-key-H-opens-horse-feed-dialog
  (let [rng (r/make-rng 42)
        result (inp/handle-key rng (st/initial-state) \H)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :horses (get-in result [:dialog :commodity])))))

;; ---- handle-key: main mode, run ----

(deftest handle-key-r-runs-month
  (let [rng (r/make-rng 42)
        state (st/initial-state)
        result (inp/handle-key rng state \r)]
    ;; after run, month or year should advance
    (is (or (> (:month result) (:month state))
            (> (:year result) (:year state))))))

(deftest handle-key-R-runs-month
  (let [rng (r/make-rng 42)
        state (st/initial-state)
        result (inp/handle-key rng state \R)]
    (is (or (> (:month result) (:month state))
            (> (:year result) (:year state))))))

;; ---- handle-key: string message alert mode ----

(deftest handle-key-string-message-any-key-dismisses
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message "Error occurred")]
    (doseq [ch [\a \w \s \1 \space \r]]
      (is (nil? (:message (inp/handle-key rng state ch)))
          (str "key " ch " should dismiss string message")))))

(deftest handle-key-string-message-blocks-key-actions
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message "Error occurred")
        result (inp/handle-key rng state \w)]
    ;; Should NOT open a dialog — message dismissal takes priority
    (is (nil? (:dialog result)))
    (is (nil? (:message result)))))

(deftest handle-key-string-message-blocks-run
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message "Error occurred")
        result (inp/handle-key rng state \r)]
    ;; Should NOT run — message dismissal takes priority
    (is (= 1 (:month result)))
    (is (nil? (:message result)))))

;; ---- handle-key: main mode, unknown key ----

(deftest handle-key-unknown-clears-message
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :message "some string")
        result (inp/handle-key rng state \z)]
    (is (nil? (:message result)))))

;; ---- handle-mouse ----

(defn click-coords
  "Compute pixel coords for center of grid cell (col, row)."
  [col row]
  [(+ (* col lay/cell-w) lay/pad (/ lay/cell-w 2))
   (+ (* row lay/cell-h) lay/top-pad (/ lay/cell-h 2))])

;; RUN button (cols 8-9, row 23)

(deftest handle-mouse-run-button-at-drawn-position
  (let [state (st/initial-state)
        {:keys [x y w h]} (lay/cell-rect-span 8 23 2 1)
        mx (+ x (/ w 2))
        my (+ y (/ h 2))
        result (inp/handle-mouse state mx my)]
    (is (:run-clicked result))))

(deftest handle-mouse-run-button
  (let [state (st/initial-state)
        [mx my] (click-coords 8 23)
        result (inp/handle-mouse state mx my)]
    (is (:run-clicked result))))

(deftest handle-mouse-run-button-col9
  (let [state (st/initial-state)
        [mx my] (click-coords 9 23)
        result (inp/handle-mouse state mx my)]
    (is (:run-clicked result))))

;; QUIT button (cols 0-1, row 23)

(deftest handle-mouse-quit-button
  (let [state (st/initial-state)
        [mx my] (click-coords 0 23)
        result (inp/handle-mouse state mx my)]
    (is (:quit-clicked result))))

(deftest handle-mouse-quit-button-col1
  (let [state (st/initial-state)
        [mx my] (click-coords 1 23)
        result (inp/handle-mouse state mx my)]
    (is (:quit-clicked result))))

(deftest quit-button-when-dirty-prompts-save
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :dirty true)
        [mx my] (click-coords 0 23)
        result (inp/handle-mouse state mx my rng)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :quit (get-in result [:dialog :next-action])))))

(deftest quit-button-when-clean-quits
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :dirty false)
        [mx my] (click-coords 0 23)
        result (inp/handle-mouse state mx my rng)]
    (is (true? (:quit-clicked result)))))

;; Commodities section click opens buy/sell

(deftest handle-mouse-commodities-wheat
  (let [state (st/initial-state)
        [mx my] (click-coords 1 1)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :wheat (get-in result [:dialog :commodity])))))

(deftest handle-mouse-commodities-slaves
  (let [state (st/initial-state)
        [mx my] (click-coords 1 3)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :slaves (get-in result [:dialog :commodity])))))

(deftest handle-mouse-commodities-land
  (let [state (st/initial-state)
        [mx my] (click-coords 1 6)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :land (get-in result [:dialog :commodity])))))

;; Prices section click opens buy/sell

(deftest handle-mouse-prices-wheat
  (let [state (st/initial-state)
        [mx my] (click-coords 4 1)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :wheat (get-in result [:dialog :commodity])))))

(deftest handle-mouse-prices-horses
  (let [state (st/initial-state)
        [mx my] (click-coords 5 4)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :horses (get-in result [:dialog :commodity])))))

;; Feed rates section (cols 6-7, rows 1-3)

(deftest handle-mouse-feed-rates-slaves
  (let [state (st/initial-state)
        [mx my] (click-coords 6 1)
        result (inp/handle-mouse state mx my)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :slaves (get-in result [:dialog :commodity])))))

(deftest handle-mouse-feed-rates-oxen
  (let [state (st/initial-state)
        [mx my] (click-coords 7 2)
        result (inp/handle-mouse state mx my)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :oxen (get-in result [:dialog :commodity])))))

(deftest handle-mouse-feed-rates-horses
  (let [state (st/initial-state)
        [mx my] (click-coords 7 3)
        result (inp/handle-mouse state mx my)]
    (is (= :feed (get-in result [:dialog :type])))
    (is (= :horses (get-in result [:dialog :commodity])))))

;; Overseers section

(deftest handle-mouse-overseers-section
  (let [state (st/initial-state)
        [mx my] (click-coords 6 5)
        result (inp/handle-mouse state mx my)]
    (is (= :overseer (get-in result [:dialog :type])))))

;; Loan section

(deftest handle-mouse-loan-section
  (let [state (st/initial-state)
        [mx my] (click-coords 8 4)
        result (inp/handle-mouse state mx my)]
    (is (= :loan (get-in result [:dialog :type])))))

;; Land section

(deftest handle-mouse-land-section
  (let [state (st/initial-state)
        [mx my] (click-coords 2 9)
        result (inp/handle-mouse state mx my)]
    (is (= :buy-sell (get-in result [:dialog :type])))
    (is (= :land (get-in result [:dialog :commodity])))))

;; Spread & Plant section

(deftest handle-mouse-spread-section
  (let [state (st/initial-state)
        [mx my] (click-coords 5 9)
        result (inp/handle-mouse state mx my)]
    (is (= :spread (get-in result [:dialog :type])))))

(deftest handle-mouse-plant-section
  (let [state (st/initial-state)
        [mx my] (click-coords 6 9)
        result (inp/handle-mouse state mx my)]
    (is (= :plant (get-in result [:dialog :type])))))

;; Gold section

(deftest handle-mouse-gold-section
  (let [state (st/initial-state)
        [mx my] (click-coords 8 9)
        result (inp/handle-mouse state mx my)]
    (is (= :loan (get-in result [:dialog :type])))))

;; Pyramid section

(deftest handle-mouse-pyramid-section
  (let [state (st/initial-state)
        [mx my] (click-coords 1 12)
        result (inp/handle-mouse state mx my)]
    (is (= :pyramid (get-in result [:dialog :type])))))

;; Click outside all sections

(deftest handle-mouse-outside-sections-returns-state
  (let [state (st/initial-state)
        ;; row 24 col 5 is in controls, but let's click way outside
        mx (+ (* 5 lay/cell-w) lay/pad (/ lay/cell-w 2))
        my (+ (* 24 lay/cell-h) lay/pad (/ lay/cell-h 2))
        result (inp/handle-mouse state mx my)]
    ;; Should return state unchanged (no dialog, no special flags)
    (is (nil? (:dialog result)))
    (is (nil? (:run-clicked result)))
    (is (nil? (:quit-clicked result)))))

;; ---- contracts dialog key handling ----

(def test-offers
  (vec (repeat 5 {:type :buy :who 0 :what :wheat
                   :amount 100.0 :price 1000.0
                   :duration 24 :active true :pct 0.0})))

(deftest c-key-opens-contracts-dialog
  (let [state (assoc (st/initial-state) :cont-offers test-offers)
        rng (r/make-rng 42)
        result (inp/handle-key rng state \c)]
    (is (= :contracts (get-in result [:dialog :type])))))

(deftest arrow-down-navigates-contracts-dialog
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        rng (r/make-rng 42)
        result (inp/handle-key rng state \uFFFF :down)]
    (is (= 1 (get-in result [:dialog :selected])))))

(deftest arrow-up-navigates-contracts-dialog
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        rng (r/make-rng 42)
        result (inp/handle-key rng state \uFFFF :up)]
    (is (= 4 (get-in result [:dialog :selected])))))

(deftest enter-in-browsing-mode-confirms
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers
                         :players [{:name "Test"}])
                  (dlg/open-contracts-dialog))
        rng (r/make-rng 42)
        result (inp/handle-key rng state \return)]
    (is (= :confirming (get-in result [:dialog :mode])))))

(deftest y-in-confirming-mode-accepts
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers
                         :players [{:name "Test"}]
                         :cont-pend [])
                  (dlg/open-contracts-dialog)
                  (dlg/confirm-selected))
        rng (r/make-rng 42)
        result (inp/handle-key rng state \y)]
    (is (= 1 (count (:cont-pend result))))
    (is (nil? (:dialog result)))))

(deftest n-in-confirming-mode-rejects
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers
                         :players [{:name "Test"}]
                         :cont-pend [])
                  (dlg/open-contracts-dialog)
                  (dlg/confirm-selected))
        rng (r/make-rng 42)
        result (inp/handle-key rng state \n)]
    (is (= :browsing (get-in result [:dialog :mode])))
    (is (empty? (:cont-pend result)))))

(deftest esc-from-confirming-returns-to-browsing
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers
                         :players [{:name "Test"}])
                  (dlg/open-contracts-dialog)
                  (dlg/confirm-selected))
        rng (r/make-rng 42)
        result (inp/handle-key rng state esc-char)]
    (is (= :browsing (get-in result [:dialog :mode])))))

(deftest esc-from-browsing-closes-dialog
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        rng (r/make-rng 42)
        result (inp/handle-key rng state esc-char)]
    (is (nil? (:dialog result)))))

;; ---- contracts mouse click ----

(deftest clicking-contracts-section-opens-dialog
  (let [state (assoc (st/initial-state) :cont-offers test-offers)
        [mx my] (click-coords 5 11)
        result (inp/handle-mouse state mx my)]
    (is (= :contracts (get-in result [:dialog :type])))))

;; ---- contract dialog click-to-select ----

(defn- offer-row-y
  "Compute pixel y for the center of offer row idx in the contracts dialog."
  [idx]
  (let [{:keys [y]} (lay/cell-rect-span 2 5 7 14)
        y0 (+ y (* lay/title-size 2) lay/small-size 8)
        row-h (+ lay/label-size 4)]
    (+ y0 (* idx row-h) (/ row-h 2))))

(defn- dialog-center-x []
  (let [{:keys [x w]} (lay/cell-rect-span 2 5 7 14)]
    (+ x (/ w 2))))

(deftest click-offer-row-selects-it
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        mx (dialog-center-x)
        my (offer-row-y 2)
        result (inp/handle-mouse state mx my)]
    (is (= 2 (get-in result [:dialog :selected])))))

(deftest click-selected-offer-confirms-it
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers
                         :players [{:name "Test"}])
                  (dlg/open-contracts-dialog))
        mx (dialog-center-x)
        my (offer-row-y 0)
        result (inp/handle-mouse state mx my)]
    (is (= :confirming (get-in result [:dialog :mode])))))

(deftest click-outside-offer-area-does-nothing
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        mx (dialog-center-x)
        ;; Click well below all offer rows
        my (+ (offer-row-y 4) 200)
        result (inp/handle-mouse state mx my)]
    (is (= :browsing (get-in result [:dialog :mode])))
    (is (= 0 (get-in result [:dialog :selected])))))

;; ---- contract message queue ----

(deftest dismiss-message-pops-next-contract-msg
  (let [rng (r/make-rng 42)
        msg1 {:text "First message" :face 0}
        msg2 {:text "Second message" :face 1}
        state (assoc (st/initial-state)
                :message msg1
                :contract-msgs [msg2])
        result (inp/handle-key rng state \space)]
    ;; Dismissing msg1 should pop msg2 to :message
    (is (= msg2 (:message result)))
    (is (empty? (:contract-msgs result)))))

;; ---- handle-mouse-move (hover changes selection) ----

(deftest mouse-move-over-row-sets-selected
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog))
        mx (dialog-center-x)
        my (offer-row-y 2)
        result (inp/handle-mouse-move state mx my)]
    (is (= 2 (get-in result [:dialog :selected])))))

(deftest mouse-move-outside-offer-area-keeps-selected
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers test-offers)
                  (dlg/open-contracts-dialog)
                  (assoc-in [:dialog :selected] 3))
        mx (dialog-center-x)
        my (+ (offer-row-y 4) 200)
        result (inp/handle-mouse-move state mx my)]
    (is (= 3 (get-in result [:dialog :selected])))))

(deftest mouse-move-no-dialog-returns-state-unchanged
  (let [state (st/initial-state)
        result (inp/handle-mouse-move state 100 100)]
    (is (nil? (:dialog result)))))

;; ---- hover tracking for Run/Quit buttons ----

(deftest mouse-move-over-run-button-sets-hover
  (let [state (st/initial-state)
        [mx my] (click-coords 8 23)
        result (inp/handle-mouse-move state mx my)]
    (is (= :run (:hover-btn result)))))

(deftest mouse-move-over-quit-button-sets-hover
  (let [state (st/initial-state)
        [mx my] (click-coords 0 23)
        result (inp/handle-mouse-move state mx my)]
    (is (= :quit (:hover-btn result)))))

(deftest mouse-move-elsewhere-clears-hover
  (let [state (assoc (st/initial-state) :hover-btn :run)
        [mx my] (click-coords 5 10)
        result (inp/handle-mouse-move state mx my)]
    (is (nil? (:hover-btn result)))))

;; ---- confirm dialog button clicks ----

(defn- confirm-button-y
  "Compute pixel y for the center of the confirm buttons."
  []
  (let [{:keys [y h]} (lay/cell-rect-span 2 5 7 14)
        btn-y (+ y h -20 (- lay/title-size) -8)]
    (+ btn-y (/ lay/title-size 2))))

(defn- accept-button-x
  "Compute pixel x for the center of the Accept button."
  []
  (let [{:keys [x]} (lay/cell-rect-span 2 5 7 14)]
    (+ x 8 50)))

(defn- reject-button-x
  "Compute pixel x for the center of the Reject button."
  []
  (let [{:keys [x]} (lay/cell-rect-span 2 5 7 14)]
    (+ x 120 50)))

(defn- cancel-button-x
  "Compute pixel x for the center of the Cancel button."
  []
  (let [{:keys [x]} (lay/cell-rect-span 2 5 7 14)]
    (+ x 232 50)))

(defn- confirming-state []
  (-> (st/initial-state)
      (assoc :cont-offers test-offers
             :players [{:name "Test"}]
             :cont-pend [])
      (dlg/open-contracts-dialog)
      (dlg/confirm-selected)))

(deftest click-accept-button-accepts-contract
  (let [state (confirming-state)
        result (inp/handle-mouse state (accept-button-x) (confirm-button-y))]
    (is (= 1 (count (:cont-pend result))))
    (is (nil? (:dialog result)))))

(deftest click-reject-button-rejects-contract
  (let [state (confirming-state)
        result (inp/handle-mouse state (reject-button-x) (confirm-button-y))]
    (is (= :browsing (get-in result [:dialog :mode])))
    (is (empty? (:cont-pend result)))))

(deftest click-cancel-button-closes-dialog
  (let [state (confirming-state)
        result (inp/handle-mouse state (cancel-button-x) (confirm-button-y))]
    (is (nil? (:dialog result)))))

(deftest click-outside-confirm-buttons-stays-confirming
  (let [state (confirming-state)
        ;; Click well to the right of all buttons
        mx (+ (cancel-button-x) 300)
        my (confirm-button-y)
        result (inp/handle-mouse state mx my)]
    (is (= :confirming (get-in result [:dialog :mode])))))

;; ---- credit-check button clicks ----

(defn- credit-check-button-y []
  (let [{:keys [y h]} (lay/cell-rect-span 2 8 6 5)
        btn-y (+ y h -20 (- lay/title-size) -8)]
    (+ btn-y (/ lay/title-size 2))))

(defn- credit-check-yes-x []
  (let [{:keys [x]} (lay/cell-rect-span 2 8 6 5)]
    (+ x 8 50)))

(defn- credit-check-no-x []
  (let [{:keys [x]} (lay/cell-rect-span 2 8 6 5)]
    (+ x 120 50)))

(defn- credit-check-state []
  (-> (st/initial-state)
      (assoc :credit-limit 100.0 :loan 0.0
             :gold 50000.0 :credit-rating 0.8
             :credit-lower 500000.0
             :wheat 1000.0 :slaves 50.0 :oxen 20.0 :horses 10.0
             :sl-health 0.8 :ox-health 0.9 :hs-health 0.7
             :ln-fallow 100.0 :manure 200.0
             :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                      :horses 500.0 :oxen 300.0 :land 5000.0})
      (dlg/open-dialog :loan)
      (assoc-in [:dialog :mode] :credit-check)
      (assoc-in [:dialog :fee] 100.0)
      (assoc-in [:dialog :borrow-amt] 1000.0)))

(deftest click-credit-check-yes-accepts
  (let [rng (r/make-rng 42)
        state (credit-check-state)
        result (inp/handle-mouse state (credit-check-yes-x) (credit-check-button-y) rng)]
    (is (nil? (:dialog result)))
    (is (map? (:message result)))))

(deftest click-credit-check-no-rejects
  (let [state (credit-check-state)
        result (inp/handle-mouse state (credit-check-no-x) (credit-check-button-y))]
    (is (some? (:dialog result)))
    (is (nil? (get-in result [:dialog :mode])))))

(deftest click-outside-credit-check-buttons-does-nothing
  (let [state (credit-check-state)
        result (inp/handle-mouse state (+ (credit-check-no-x) 300) (credit-check-button-y))]
    (is (= :credit-check (get-in result [:dialog :mode])))))

(deftest dismiss-message-no-queue
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state)
                :message {:text "Only message" :face 0}
                :contract-msgs [])
        result (inp/handle-key rng state \space)]
    ;; No more queued messages, message should be nil
    (is (nil? (:message result)))))

;; ---- dialog-button-bounds ----

(defn- dialog-rect []
  (lay/cell-rect-span 2 8 6 5))

(defn- expected-btn-y []
  (let [{:keys [y h]} (dialog-rect)]
    (+ y h -20 (- lay/title-size) -8)))

(deftest dialog-button-bounds-mode-dialog-has-radios-and-buttons
  (let [bounds (inp/dialog-button-bounds :buy-sell)]
    (is (contains? bounds :radio1))
    (is (contains? bounds :radio2))
    (is (contains? bounds :radio3))
    (is (contains? bounds :ok))
    (is (contains? bounds :cancel))
    (let [{:keys [x]} (dialog-rect)
          btn-y (expected-btn-y)]
      (is (= {:x (+ x 8) :y btn-y :w 110 :h lay/title-size} (:radio1 bounds)))
      (is (= {:x (+ x 126) :y btn-y :w 110 :h lay/title-size} (:radio2 bounds)))
      (is (= {:x (+ x 244) :y btn-y :w 130 :h lay/title-size} (:radio3 bounds)))
      (is (= {:x (+ x 382) :y btn-y :w 60 :h lay/title-size} (:ok bounds)))
      (is (= {:x (+ x 450) :y btn-y :w 60 :h lay/title-size} (:cancel bounds))))))

(deftest dialog-button-bounds-simple-dialog-has-only-buttons
  (let [bounds (inp/dialog-button-bounds :feed)]
    (is (nil? (:radio1 bounds)))
    (is (nil? (:radio2 bounds)))
    (is (contains? bounds :ok))
    (is (contains? bounds :cancel))
    (let [{:keys [x]} (dialog-rect)
          btn-y (expected-btn-y)]
      (is (= {:x (+ x 8) :y btn-y :w 120 :h lay/title-size} (:ok bounds)))
      (is (= {:x (+ x 136) :y btn-y :w 120 :h lay/title-size} (:cancel bounds))))))

(deftest dialog-button-bounds-loan-has-radios
  (let [bounds (inp/dialog-button-bounds :loan)]
    (is (contains? bounds :radio1))
    (is (contains? bounds :radio2))))

(deftest dialog-button-bounds-overseer-has-radios
  (let [bounds (inp/dialog-button-bounds :overseer)]
    (is (contains? bounds :radio1))
    (is (contains? bounds :radio2))
    (is (contains? bounds :radio3))))

;; ---- handle-dialog-click ----

(defn- btn-center [{:keys [x y w h]}]
  [(+ x (/ w 2)) (+ y (/ h 2))])

(deftest click-radio1-in-buy-sell-sets-buy-mode
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        bounds (inp/dialog-button-bounds :buy-sell)
        [mx my] (btn-center (:radio1 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :buy (get-in result [:dialog :mode])))))

(deftest click-radio2-in-buy-sell-sets-sell-mode
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        bounds (inp/dialog-button-bounds :buy-sell)
        [mx my] (btn-center (:radio2 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :sell (get-in result [:dialog :mode])))))

(deftest click-radio1-in-loan-sets-borrow-mode
  (let [state (dlg/open-dialog (st/initial-state) :loan)
        bounds (inp/dialog-button-bounds :loan)
        [mx my] (btn-center (:radio1 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :borrow (get-in result [:dialog :mode])))))

(deftest click-radio2-in-loan-sets-repay-mode
  (let [state (dlg/open-dialog (st/initial-state) :loan)
        bounds (inp/dialog-button-bounds :loan)
        [mx my] (btn-center (:radio2 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :repay (get-in result [:dialog :mode])))))

(deftest click-radio1-in-overseer-sets-hire-mode
  (let [state (dlg/open-dialog (st/initial-state) :overseer)
        bounds (inp/dialog-button-bounds :overseer)
        [mx my] (btn-center (:radio1 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :hire (get-in result [:dialog :mode])))))

(deftest click-radio2-in-overseer-sets-fire-mode
  (let [state (dlg/open-dialog (st/initial-state) :overseer)
        bounds (inp/dialog-button-bounds :overseer)
        [mx my] (btn-center (:radio2 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :fire (get-in result [:dialog :mode])))))

(deftest radio-mode-for-radio3-buy-sell-is-keep
  (is (= :keep (inp/radio-mode-for :buy-sell :radio3))))

(deftest radio-mode-for-radio3-overseer-is-obtain
  (is (= :obtain (inp/radio-mode-for :overseer :radio3))))

(deftest click-radio3-in-buy-sell-sets-keep-mode
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        bounds (inp/dialog-button-bounds :buy-sell)
        [mx my] (btn-center (:radio3 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :keep (get-in result [:dialog :mode])))))

(deftest click-radio3-in-overseer-sets-obtain-mode
  (let [state (dlg/open-dialog (st/initial-state) :overseer)
        bounds (inp/dialog-button-bounds :overseer)
        [mx my] (btn-center (:radio3 bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (= :obtain (get-in result [:dialog :mode])))))

(deftest handle-key-k-sets-keep-mode-in-buy-sell-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-key rng state \k)]
    (is (= :keep (get-in result [:dialog :mode])))))

(deftest handle-key-o-sets-obtain-mode-in-overseer-dialog
  (let [rng (r/make-rng 42)
        state (dlg/open-dialog (st/initial-state) :overseer)
        result (inp/handle-key rng state \o)]
    (is (= :obtain (get-in result [:dialog :mode])))))

(deftest click-ok-in-buy-sell-executes-dialog
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))
        bounds (inp/dialog-button-bounds :buy-sell)
        [mx my] (btn-center (:ok bounds))
        result (inp/handle-dialog-click state mx my rng)]
    (is (nil? (:dialog result)))
    (is (> (:wheat result) 0.0))))

(deftest click-cancel-closes-dialog
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        bounds (inp/dialog-button-bounds :buy-sell)
        [mx my] (btn-center (:cancel bounds))
        result (inp/handle-dialog-click state mx my nil)]
    (is (nil? (:dialog result)))))

(deftest click-ok-in-feed-executes-dialog
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :feed {:commodity :slaves})
                  (assoc-in [:dialog :input] "3.5"))
        bounds (inp/dialog-button-bounds :feed)
        [mx my] (btn-center (:ok bounds))
        result (inp/handle-dialog-click state mx my rng)]
    (is (nil? (:dialog result)))
    (is (== 3.5 (:sl-feed-rt result)))))

(deftest click-outside-dialog-buttons-returns-state-unchanged
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})
        result (inp/handle-dialog-click state 0 0 nil)]
    (is (= state result))))

;; ---- handle-mouse routes to handle-dialog-click ----

(deftest mouse-click-on-ok-in-open-dialog-executes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :plant)
                  (assoc-in [:dialog :input] "50"))
        bounds (inp/dialog-button-bounds :plant)
        [mx my] (btn-center (:ok bounds))
        result (inp/handle-mouse state mx my rng)]
    (is (nil? (:dialog result)))
    (is (== 50.0 (:ln-to-sew result)))))

(deftest mouse-click-cancel-in-open-dialog-closes
  (let [state (dlg/open-dialog (st/initial-state) :spread)
        bounds (inp/dialog-button-bounds :spread)
        [mx my] (btn-center (:cancel bounds))
        result (inp/handle-mouse state mx my)]
    (is (nil? (:dialog result)))))

;; ---- dialog-input-bounds ----

(deftest dialog-input-bounds-returns-expected-rect
  (let [{:keys [x y w h]} (lay/cell-rect-span 2 8 6 5)
        icon-size (int (* h 0.4))
        text-x (+ x icon-size 16)
        amount-y (+ y (* lay/value-size 3) 8)
        expected-x (+ text-x 68)
        expected-y (- amount-y lay/value-size 2)
        expected-w (- (+ x w) expected-x 12)
        expected-h (+ lay/value-size 8)
        bounds (inp/dialog-input-bounds)]
    (is (= expected-x (:x bounds)))
    (is (= expected-y (:y bounds)))
    (is (= expected-w (:w bounds)))
    (is (= expected-h (:h bounds)))))

(deftest dialog-input-bounds-box-starts-after-label
  (let [{:keys [x h]} (lay/cell-rect-span 2 8 6 5)
        icon-size (int (* h 0.4))
        text-x (+ x icon-size 16)
        bounds (inp/dialog-input-bounds)]
    (is (> (:x bounds) text-x))))

(deftest dialog-input-bounds-box-extends-to-near-right-edge
  (let [{:keys [x w]} (lay/cell-rect-span 2 8 6 5)
        right-edge (+ x w)
        bounds (inp/dialog-input-bounds)]
    (is (= 12 (- right-edge (+ (:x bounds) (:w bounds)))))))

;; ---- handle-key: confirm-save dialog ----

(deftest handle-key-confirm-save-yes-with-path
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-hk-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :dirty true :save-path path)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))
        result (inp/handle-key rng state \y)]
    (is (= :quit (:pending-action result)))
    (is (false? (:dirty result)))))

(deftest handle-key-confirm-save-no
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-save {:next-action :new-game}))
        result (inp/handle-key rng state \n)]
    (is (= :new-game (:pending-action result)))
    (is (nil? (:dialog result)))))

(deftest handle-key-confirm-save-esc-closes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))
        result (inp/handle-key rng state (char 27))]
    (is (nil? (:dialog result)))))

;; ---- handle-mouse: confirm-save dialog buttons ----

(defn- confirm-save-btn-center [btn-key]
  (let [{:keys [x y h]} (lay/cell-rect-span 2 8 6 5)
        btn-y (+ y h -20 (- lay/title-size) -8)
        btn-x (case btn-key
                :yes (+ x 8)
                :no (+ x 120)
                :cancel (+ x 232))]
    [(+ btn-x 50) (+ btn-y (/ lay/title-size 2))]))

(deftest click-confirm-save-no-sets-pending-action
  (let [state (-> (st/initial-state)
                  (assoc :dirty true)
                  (dlg/open-dialog :confirm-save {:next-action :new-game}))
        [mx my] (confirm-save-btn-center :no)
        result (inp/handle-mouse state mx my)]
    (is (= :new-game (:pending-action result)))
    (is (nil? (:dialog result)))))

(deftest click-confirm-save-yes-saves-and-sets-pending
  (let [path (str "/tmp/pharaoh-cs-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :dirty true :save-path path)
                  (dlg/open-dialog :confirm-save {:next-action :quit}))
        [mx my] (confirm-save-btn-center :yes)
        result (inp/handle-mouse state mx my)]
    (is (= :quit (:pending-action result)))
    (is (false? (:dirty result)))))

(deftest click-confirm-save-cancel-closes-dialog
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-save {:next-action :new-game}))
        [mx my] (confirm-save-btn-center :cancel)
        result (inp/handle-mouse state mx my)]
    (is (nil? (:dialog result)))
    (is (nil? (:pending-action result)))))

;; ---- handle-key: confirm-overwrite dialog ----

(deftest handle-key-confirm-overwrite-yes-saves-file
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-ow-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :gold 4321.0)
                  (dlg/open-dialog :confirm-overwrite {:path path}))
        result (inp/handle-key rng state \y)]
    (is (nil? (:dialog result)))
    (is (= path (:save-path result)))
    (is (.exists (java.io.File. path)))))

(deftest handle-key-confirm-overwrite-no-closes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-overwrite {:path "/tmp/x.edn"}))
        result (inp/handle-key rng state \n)]
    (is (nil? (:dialog result)))))

(deftest handle-key-confirm-overwrite-esc-closes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :confirm-overwrite {:path "/tmp/x.edn"}))
        result (inp/handle-key rng state (char 27))]
    (is (nil? (:dialog result)))))

;; ---- handle-key: save-file dialog ----

(deftest handle-key-save-file-enter-saves
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-sf-" (System/currentTimeMillis) ".edn")
        state (-> (st/initial-state)
                  (assoc :gold 1234.0)
                  (dlg/open-dialog :save-file {:input path}))
        result (inp/handle-key rng state \return)]
    (is (nil? (:dialog result)))
    (is (= path (:save-path result)))
    (is (.exists (java.io.File. path)))))

(deftest handle-key-save-file-typing
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :save-file))
        result (inp/handle-key rng state \t)]
    (is (= "t" (get-in result [:dialog :input])))))

(deftest handle-key-save-file-esc-closes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :save-file))
        result (inp/handle-key rng state (char 27))]
    (is (nil? (:dialog result)))))

;; ---- handle-key: load-file dialog ----

(deftest handle-key-load-file-enter-loads
  (let [rng (r/make-rng 42)
        path (str "/tmp/pharaoh-lf-" (System/currentTimeMillis) ".edn")]
    (ps/save-game (assoc (st/initial-state) :gold 9876.0) path)
    (let [state (-> (st/initial-state) (dlg/open-dialog :load-file {:input path}))
          result (inp/handle-key rng state \return)]
      (is (nil? (:dialog result)))
      (is (= 9876.0 (:gold (:loaded-state result)))))))

(deftest handle-key-load-file-typing
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :load-file))
        result (inp/handle-key rng state \/)]
    (is (= "/" (get-in result [:dialog :input])))))

(deftest handle-key-load-file-esc-closes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :load-file))
        result (inp/handle-key rng state (char 27))]
    (is (nil? (:dialog result)))))

;; ---- handle-mouse: string message alert click ----

(deftest handle-mouse-string-message-click-dismisses
  (let [state (assoc (st/initial-state) :message "Error occurred")
        [mx my] (click-coords 5 10)
        result (inp/handle-mouse state mx my)]
    (is (nil? (:message result)))
    (is (nil? (:dialog result)))))

(deftest handle-mouse-string-message-blocks-section-click
  (let [state (assoc (st/initial-state) :message "Error occurred")
        [mx my] (click-coords 1 1)
        result (inp/handle-mouse state mx my)]
    (is (nil? (:message result)))
    (is (nil? (:dialog result)))))
