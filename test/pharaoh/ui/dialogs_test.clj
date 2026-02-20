(ns pharaoh.ui.dialogs-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [pharaoh.persistence :as ps]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st])
  (:import [java.io File]))

;; ---- open-dialog ----

(deftest open-dialog-creates-dialog-with-defaults
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell)]
    (is (= {:type :buy-sell :input "" :mode nil}
           (:dialog state)))))

(deftest open-dialog-merges-opts
  (let [state (dlg/open-dialog (st/initial-state) :buy-sell {:commodity :wheat})]
    (is (= :buy-sell (get-in state [:dialog :type])))
    (is (= :wheat (get-in state [:dialog :commodity])))
    (is (= "" (get-in state [:dialog :input])))
    (is (nil? (get-in state [:dialog :mode])))))

(deftest open-dialog-preserves-rest-of-state
  (let [base (assoc (st/initial-state) :gold 5000.0)
        state (dlg/open-dialog base :loan)]
    (is (= 5000.0 (:gold state)))
    (is (= :loan (get-in state [:dialog :type])))))

(deftest open-dialog-replaces-existing-dialog
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (dlg/open-dialog :buy-sell {:commodity :wheat}))]
    (is (= :buy-sell (get-in state [:dialog :type])))
    (is (= :wheat (get-in state [:dialog :commodity])))))

;; ---- close-dialog ----

(deftest close-dialog-removes-dialog
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  dlg/close-dialog)]
    (is (nil? (:dialog state)))))

(deftest close-dialog-on-state-without-dialog
  (let [state (dlg/close-dialog (st/initial-state))]
    (is (nil? (:dialog state)))))

(deftest close-dialog-sets-dirty-flag
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  dlg/close-dialog)]
    (is (true? (:dirty state)))))

(deftest close-dialog-preserves-rest-of-state
  (let [base (assoc (st/initial-state) :gold 9999.0)
        state (-> base
                  (dlg/open-dialog :loan)
                  dlg/close-dialog)]
    (is (= 9999.0 (:gold state)))))

;; ---- update-dialog-input ----

(deftest update-dialog-input-appends-digit
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  (dlg/update-dialog-input \5))]
    (is (= "5" (get-in state [:dialog :input])))))

(deftest update-dialog-input-appends-multiple-digits
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  (dlg/update-dialog-input \1)
                  (dlg/update-dialog-input \2)
                  (dlg/update-dialog-input \3))]
    (is (= "123" (get-in state [:dialog :input])))))

(deftest update-dialog-input-appends-dot
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :feed)
                  (dlg/update-dialog-input \5)
                  (dlg/update-dialog-input \.)
                  (dlg/update-dialog-input \3))]
    (is (= "5.3" (get-in state [:dialog :input])))))

(deftest update-dialog-input-backspace-removes-last-char
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  (dlg/update-dialog-input \1)
                  (dlg/update-dialog-input \2)
                  (dlg/update-dialog-input \backspace))]
    (is (= "1" (get-in state [:dialog :input])))))

(deftest update-dialog-input-backspace-on-empty-stays-empty
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  (dlg/update-dialog-input \backspace))]
    (is (= "" (get-in state [:dialog :input])))))

(deftest update-dialog-input-ignores-letters
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell)
                  (dlg/update-dialog-input \5)
                  (dlg/update-dialog-input \a))]
    (is (= "5" (get-in state [:dialog :input])))))

(deftest update-dialog-input-no-dialog-returns-state
  (let [state (st/initial-state)
        result (dlg/update-dialog-input state \5)]
    (is (= state result))))

;; ---- set-dialog-mode ----

(deftest set-dialog-mode-sets-mode
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy))]
    (is (= :buy (get-in state [:dialog :mode])))))

(deftest set-dialog-mode-overwrites-previous-mode
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (dlg/set-dialog-mode :sell))]
    (is (= :sell (get-in state [:dialog :mode])))))

;; ---- execute-dialog ----

;; -- no dialog --

(deftest execute-dialog-no-dialog-returns-state
  (let [rng (r/make-rng 42)
        state (st/initial-state)]
    (is (= state (dlg/execute-dialog rng state)))))

;; -- invalid input --

(deftest execute-dialog-invalid-input-sets-message
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "abc"))]
    (is (string? (:message (dlg/execute-dialog rng state))))))

(deftest execute-dialog-empty-input-sets-message
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy))]
    (is (string? (:message (dlg/execute-dialog rng state))))))

;; -- buy-sell --

(deftest execute-dialog-buy-success
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (> (:wheat result) 0.0))
      (is (< (:gold result) 50000.0)))))

(deftest execute-dialog-sell-success
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :wheat 500.0 :gold 0.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :sell)
                  (assoc-in [:dialog :input] "100"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (= 400.0 (:wheat result)))
      (is (> (:gold result) 0.0)))))

(deftest execute-dialog-buy-sell-no-mode-sets-message
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (assoc-in [:dialog :input] "100"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (string? (:message result))))))

(deftest execute-dialog-buy-with-just-enough-gold-succeeds
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 15.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "1"))]
    ;; buy completes (dialog closes) with sufficient gold
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (> (:wheat result) 0.0)))))

;; -- buy validation --

(deftest execute-dialog-buy-insufficient-gold-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 500.0)
                  (assoc-in [:prices :wheat] 10.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))]
    (let [result (dlg/execute-dialog rng state)]
      ;; dialog stays open
      (is (some? (:dialog result)))
      ;; error message from insufficient-funds pool
      (is (string? (:message result)))
      ;; gold unchanged
      (is (== 500.0 (:gold result))))))

(deftest execute-dialog-buy-exactly-affordable-succeeds
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 1000.0)
                  (assoc-in [:prices :wheat] 10.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (>= (:gold result) 0.0)))))

;; -- buy validation: demand limit --

(deftest execute-dialog-buy-low-supply-shows-demand-limit-and-buys
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (assoc-in [:prices :wheat] 10.0)
                  (assoc-in [:supply :wheat] 3.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "5000"))
        result (dlg/execute-dialog rng state)]
    ;; Dialog should be closed (purchase completed)
    (is (nil? (:dialog result)))
    ;; Should show demand-limit message (string, not face map)
    (is (string? (:message result)))
    ;; Message should include the available supply amount
    (is (str/includes? (:message result) "3"))
    ;; Should have bought what was available (3, not 5000)
    (is (> (:wheat result) (:wheat (st/initial-state))))
    (is (< (:wheat result) 5000.0))))

(deftest execute-dialog-buy-sufficient-supply-no-demand-limit
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 50000.0)
                  (assoc-in [:prices :wheat] 10.0)
                  (assoc-in [:supply :wheat] 10000.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "100"))
        result (dlg/execute-dialog rng state)]
    ;; Dialog should be closed
    (is (nil? (:dialog result)))
    ;; No demand-limit message (message should be nil or not a string)
    (is (nil? (:message result)))))

;; -- sell validation --

(deftest execute-dialog-sell-more-than-owned-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :wheat 100.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :sell)
                  (assoc-in [:dialog :input] "200"))]
    (let [result (dlg/execute-dialog rng state)]
      ;; dialog stays open
      (is (some? (:dialog result)))
      ;; error message shown
      (is (string? (:message result)))
      ;; wheat unchanged
      (is (== 100.0 (:wheat result))))))

(deftest execute-dialog-sell-exceeds-market-capacity-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :wheat 5000.0)
                  (assoc-in [:demand :wheat] 1000.0)
                  (assoc-in [:supply :wheat] 0.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :sell)
                  (assoc-in [:dialog :input] "2000"))]
    (let [result (dlg/execute-dialog rng state)]
      ;; dialog stays open
      (is (some? (:dialog result)))
      ;; error message shown
      (is (string? (:message result)))
      ;; wheat unchanged
      (is (== 5000.0 (:wheat result))))))

;; -- loan --

(deftest execute-dialog-borrow-success
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (dlg/set-dialog-mode :borrow)
                  (assoc-in [:dialog :input] "1000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 1000.0 (:loan result)))
      (is (== 1000.0 (:gold result))))))

(deftest execute-dialog-borrow-exceeds-limit-enters-credit-check
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0)
                  (dlg/open-dialog :loan)
                  (dlg/set-dialog-mode :borrow)
                  (assoc-in [:dialog :input] "1000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (= :credit-check (get-in result [:dialog :mode])))
      (is (pos? (get-in result [:dialog :fee])))
      (is (== 1000.0 (get-in result [:dialog :borrow-amt])))
      (is (string? (get-in result [:dialog :message]))))))

(deftest accept-credit-check-grants-loan-when-limit-sufficient
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0 :loan 0.0
                         :gold 50000.0 :credit-rating 0.8
                         :credit-lower 500000.0
                         :wheat 1000.0 :slaves 50.0 :oxen 20.0 :horses 10.0
                         :sl-health 0.8 :ox-health 0.9 :hs-health 0.7
                         :ln-fallow 100.0 :manure 200.0
                         :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                                  :horses 500.0 :oxen 300.0 :land 5000.0}
                         :interest 5.0 :int-addition 0.0)
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 1000.0))
        result (dlg/accept-credit-check rng state)]
    ;; Dialog should be closed
    (is (nil? (:dialog result)))
    ;; Loan includes borrow + fee (C code: amt += cost; loan += amt)
    (is (== 1100.0 (:loan result)))
    ;; Gold increases by total-amt (C code: gold += amt, fee is financed)
    (is (== (+ 50000.0 1100.0) (:gold result)))
    ;; Face message shows approval
    (is (map? (:message result)))
    (is (string? (:text (:message result))))))

(deftest accept-credit-check-denies-loan-when-limit-insufficient
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0 :loan 0.0
                         :gold 50000.0 :credit-rating 0.01
                         :credit-lower 0.0
                         :wheat 0.0 :slaves 0.0 :oxen 0.0 :horses 0.0
                         :sl-health 0.0 :ox-health 0.0 :hs-health 0.0
                         :ln-fallow 0.0 :manure 0.0
                         :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                                  :horses 500.0 :oxen 300.0 :land 5000.0})
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 100000.0))
        result (dlg/accept-credit-check rng state)]
    ;; Dialog should be closed
    (is (nil? (:dialog result)))
    ;; Fee deducted, clamped to 0 (C code: gold = max(0, gold-cost))
    (is (== (max 0.0 (- 50000.0 100.0)) (:gold result)))
    ;; Loan should NOT include the borrow amount
    (is (== 0.0 (:loan result)))
    ;; Face message shows denial
    (is (map? (:message result)))
    (is (string? (:text (:message result))))))

(deftest accept-credit-check-denial-clamps-gold-to-zero
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0 :loan 0.0
                         :gold 50.0 :credit-rating 0.01
                         :credit-lower 0.0
                         :wheat 0.0 :slaves 0.0 :oxen 0.0 :horses 0.0
                         :sl-health 0.0 :ox-health 0.0 :hs-health 0.0
                         :ln-fallow 0.0 :manure 0.0
                         :prices {:wheat 10.0 :manure 5.0 :slaves 1000.0
                                  :horses 500.0 :oxen 300.0 :land 5000.0})
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 200.0)
                  (assoc-in [:dialog :borrow-amt] 100000.0))
        result (dlg/accept-credit-check rng state)]
    ;; Gold clamped to 0, not negative (C code: gold = max(0, gold-cost))
    (is (== 0.0 (:gold result)))))

(deftest reject-credit-check-returns-to-input
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :mode] :credit-check)
                  (assoc-in [:dialog :fee] 100.0)
                  (assoc-in [:dialog :borrow-amt] 1000.0)
                  (assoc-in [:dialog :message] "Pay up!"))
        result (dlg/reject-credit-check state)]
    ;; Mode goes back to nil (standard loan input)
    (is (nil? (get-in result [:dialog :mode])))
    ;; Fee/borrow-amt/message cleared from dialog
    (is (nil? (get-in result [:dialog :fee])))
    (is (nil? (get-in result [:dialog :borrow-amt])))
    (is (nil? (get-in result [:dialog :message])))
    ;; Input cleared
    (is (= "" (get-in result [:dialog :input])))))

(deftest execute-dialog-repay-success
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :loan 5000.0 :gold 10000.0)
                  (dlg/open-dialog :loan)
                  (dlg/set-dialog-mode :repay)
                  (assoc-in [:dialog :input] "2000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 3000.0 (:loan result)))
      (is (== 8000.0 (:gold result))))))

(deftest execute-dialog-repay-insufficient-gold
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :loan 5000.0 :gold 100.0)
                  (dlg/open-dialog :loan)
                  (dlg/set-dialog-mode :repay)
                  (assoc-in [:dialog :input] "2000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (string? (:message result))))))

(deftest execute-dialog-loan-no-mode-sets-message
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :loan)
                  (assoc-in [:dialog :input] "1000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (string? (:message result))))))

;; -- feed --

(deftest execute-dialog-feed-slaves
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :feed {:commodity :slaves})
                  (assoc-in [:dialog :input] "5.5"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 5.5 (:sl-feed-rt result))))))

(deftest execute-dialog-feed-oxen
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :feed {:commodity :oxen})
                  (assoc-in [:dialog :input] "12.0"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 12.0 (:ox-feed-rt result))))))

(deftest execute-dialog-feed-horses
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :feed {:commodity :horses})
                  (assoc-in [:dialog :input] "8.0"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 8.0 (:hs-feed-rt result))))))

;; -- plant --

(deftest execute-dialog-plant
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :plant)
                  (assoc-in [:dialog :input] "250"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 250.0 (:ln-to-sew result))))))

;; -- spread --

(deftest execute-dialog-spread
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :spread)
                  (assoc-in [:dialog :input] "75"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 75.0 (:mn-to-sprd result))))))

;; -- pyramid --

(deftest execute-dialog-pyramid
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :pyramid)
                  (assoc-in [:dialog :input] "300"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 300.0 (:py-quota result))))))

;; -- overseer --

(deftest execute-dialog-hire-overseers
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :overseers 5.0)
                  (dlg/open-dialog :overseer)
                  (dlg/set-dialog-mode :hire)
                  (assoc-in [:dialog :input] "3"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 8.0 (:overseers result))))))

(deftest execute-dialog-fire-overseers
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :overseers 5.0)
                  (dlg/open-dialog :overseer)
                  (dlg/set-dialog-mode :fire)
                  (assoc-in [:dialog :input] "2"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (== 3.0 (:overseers result))))))

(deftest execute-dialog-fire-too-many-overseers
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :overseers 2.0)
                  (dlg/open-dialog :overseer)
                  (dlg/set-dialog-mode :fire)
                  (assoc-in [:dialog :input] "5"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (string? (:message result))))))

(deftest execute-dialog-overseer-no-mode-sets-message
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :overseer)
                  (assoc-in [:dialog :input] "3"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (string? (:message result))))))

;; -- contracts dialog --

(deftest open-contracts-dialog-sets-browsing-mode
  (let [offers (vec (repeat 5 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers)
                  (dlg/open-contracts-dialog))]
    (is (= :contracts (get-in state [:dialog :type])))
    (is (= :browsing (get-in state [:dialog :mode])))
    (is (= 0 (get-in state [:dialog :selected])))))

(deftest open-contracts-dialog-filters-active-offers
  (let [offers [{:type :buy :who 0 :what :wheat :amount 100.0
                 :price 1000.0 :duration 24 :active true :pct 0.0}
                {:active false}
                {:type :sell :who 1 :what :slaves :amount 50.0
                 :price 5000.0 :duration 12 :active true :pct 0.0}]
        state (-> (st/initial-state)
                  (assoc :cont-offers offers)
                  (dlg/open-contracts-dialog))]
    (is (= 2 (count (get-in state [:dialog :active-offers]))))))

(deftest contracts-dialog-navigate-down
  (let [offers (vec (repeat 5 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers)
                  (dlg/open-contracts-dialog))]
    (is (= 1 (get-in (dlg/navigate-contracts state :down) [:dialog :selected])))))

(deftest contracts-dialog-navigate-up-at-zero-wraps-to-last
  (let [offers (vec (repeat 3 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers)
                  (dlg/open-contracts-dialog))]
    (is (= 2 (get-in (dlg/navigate-contracts state :up) [:dialog :selected])))))

(deftest contracts-dialog-navigate-down-wraps-to-first
  (let [offers (vec (repeat 3 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers)
                  (dlg/open-contracts-dialog))
        at-last (assoc-in state [:dialog :selected] 2)]
    (is (= 0 (get-in (dlg/navigate-contracts at-last :down) [:dialog :selected])))))

(deftest contracts-dialog-navigate-with-no-offers
  (let [state (-> (st/initial-state)
                  (assoc :cont-offers [])
                  (dlg/open-contracts-dialog))]
    (is (= 0 (get-in (dlg/navigate-contracts state :down) [:dialog :selected])))
    (is (= 0 (get-in (dlg/navigate-contracts state :up) [:dialog :selected])))))

;; -- contracts confirm/accept/reject --

(deftest contracts-dialog-enter-confirming-mode
  (let [offers (vec (repeat 5 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers :players [{:name "King HamuNam"}])
                  (dlg/open-contracts-dialog))
        result (dlg/confirm-selected state)]
    (is (= :confirming (get-in result [:dialog :mode])))))

(deftest contracts-dialog-accept-moves-to-pending
  (let [offers (vec (repeat 5 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers
                         :players [{:name "King HamuNam"}]
                         :cont-pend [])
                  (dlg/open-contracts-dialog))
        result (dlg/accept-selected state)]
    (is (= 1 (count (:cont-pend result))))
    (is (nil? (:dialog result)))))

(deftest contracts-dialog-accept-deactivates-offer
  (let [offers (vec (repeat 3 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers
                         :players [{:name "King HamuNam"}]
                         :cont-pend [])
                  (dlg/open-contracts-dialog))
        result (dlg/accept-selected state)]
    ;; dialog should be closed after accept
    (is (nil? (:dialog result)))))

(deftest contracts-dialog-reject-returns-to-browsing
  (let [offers (vec (repeat 5 {:type :buy :who 0 :what :wheat
                                :amount 100.0 :price 1000.0
                                :duration 24 :active true :pct 0.0}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers
                         :players [{:name "King HamuNam"}]
                         :cont-pend [])
                  (dlg/open-contracts-dialog)
                  (dlg/confirm-selected))
        result (dlg/reject-selected state)]
    (is (= :browsing (get-in result [:dialog :mode])))
    (is (empty? (:cont-pend result)))))

(deftest contracts-dialog-accept-at-max-pending-shows-error
  (let [offers [{:type :buy :who 0 :what :wheat :amount 100.0
                 :price 1000.0 :duration 24 :active true :pct 0.0}]
        pend (vec (repeat 10 {:active true}))
        state (-> (st/initial-state)
                  (assoc :cont-offers offers
                         :players [{:name "King HamuNam"}]
                         :cont-pend pend)
                  (dlg/open-contracts-dialog))
        result (dlg/accept-selected state)]
    (is (string? (:message result)))))

;; -- save-file dialog --

(deftest open-save-file-dialog
  (let [state (dlg/open-dialog (st/initial-state) :save-file)]
    (is (= :save-file (get-in state [:dialog :type])))
    (is (= "" (get-in state [:dialog :input])))))

(deftest update-file-input-accepts-path-chars
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :save-file)
                  (dlg/update-dialog-input \m)
                  (dlg/update-dialog-input \y)
                  (dlg/update-dialog-input \-)
                  (dlg/update-dialog-input \g))]
    (is (= "my-g" (get-in state [:dialog :input])))))

(deftest update-file-input-accepts-full-path
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :load-file))
        state (reduce dlg/update-dialog-input state (seq "/tmp/save_1.edn"))]
    (is (= "/tmp/save_1.edn" (get-in state [:dialog :input])))))

(deftest update-file-input-backspace-works
  (let [state (-> (st/initial-state)
                  (dlg/open-dialog :save-file))
        state (reduce dlg/update-dialog-input state (seq "abc"))
        state (dlg/update-dialog-input state \backspace)]
    (is (= "ab" (get-in state [:dialog :input])))))

(deftest execute-save-file-dialog-saves-game
  (let [path (str "/tmp/pharaoh-dlg-test-" (System/currentTimeMillis) ".edn")
        rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 7777.0 :dirty true)
                  (dlg/open-dialog :save-file {:input path}))
        result (dlg/execute-dialog rng state)]
    (is (nil? (:dialog result)))
    (is (false? (:dirty result)))
    (is (= path (:save-path result)))
    (is (.exists (File. path)))))

(deftest execute-save-file-empty-input-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :save-file))
        result (dlg/execute-dialog rng state)]
    (is (= "No filename entered." (:message result)))))

(deftest execute-load-file-dialog-loads-game
  (let [path (str "/tmp/pharaoh-load-test-" (System/currentTimeMillis) ".edn")
        rng (r/make-rng 42)
        saved-state (assoc (st/initial-state) :gold 3333.0)]
    (ps/save-game saved-state path)
    (let [state (-> (st/initial-state)
                    (dlg/open-dialog :load-file {:input path}))
          result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (= 3333.0 (:gold (:loaded-state result))))
      (is (false? (:dirty result))))))

(deftest execute-load-file-missing-shows-error
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (dlg/open-dialog :load-file {:input "/tmp/no-such-file-xyz.edn"}))
        result (dlg/execute-dialog rng state)]
    (is (nil? (:dialog result)))
    (is (string? (:message result)))))

;; -- error categories --

(deftest execute-dialog-error-categories
  (let [rng (r/make-rng 42)]
    (doseq [dtype [:buy-sell :loan :feed :plant :spread :pyramid :overseer]]
      (let [state (-> (st/initial-state)
                      (dlg/open-dialog dtype)
                      (assoc-in [:dialog :input] "xyz"))
            result (dlg/execute-dialog (r/make-rng 99) state)]
        (is (string? (:message result))
            (str "Expected error message for dialog type " dtype))))))
