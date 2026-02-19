(ns pharaoh.ui.input-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.input :as inp]
            [pharaoh.ui.layout :as lay]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(def esc-char (char 27))

;; ---- dialog-mode-for (private) ----

(deftest dialog-mode-for-buy-sell
  (let [dmf #'pharaoh.ui.input/dialog-mode-for]
    (is (= :buy (dmf :buy-sell \b)))
    (is (= :sell (dmf :buy-sell \s)))
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
   (+ (* row lay/cell-h) lay/pad (/ lay/cell-h 2))])

;; RUN button (cols 8-9, row 23)

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
    (is (= :browsing (get-in result [:dialog :mode])))))

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

(deftest dismiss-message-no-queue
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state)
                :message {:text "Only message" :face 0}
                :contract-msgs [])
        result (inp/handle-key rng state \space)]
    ;; No more queued messages, message should be nil
    (is (nil? (:message result)))))
