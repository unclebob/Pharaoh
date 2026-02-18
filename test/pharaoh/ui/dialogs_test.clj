(ns pharaoh.ui.dialogs-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

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

(deftest execute-dialog-buy-with-low-gold-still-executes
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :gold 5.0)
                  (dlg/open-dialog :buy-sell {:commodity :wheat})
                  (dlg/set-dialog-mode :buy)
                  (assoc-in [:dialog :input] "1"))]
    ;; buy completes (dialog closes) even with minimal gold
    (let [result (dlg/execute-dialog rng state)]
      (is (nil? (:dialog result)))
      (is (> (:wheat result) 0.0)))))

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

(deftest execute-dialog-borrow-exceeds-credit-limit
  (let [rng (r/make-rng 42)
        state (-> (st/initial-state)
                  (assoc :credit-limit 100.0)
                  (dlg/open-dialog :loan)
                  (dlg/set-dialog-mode :borrow)
                  (assoc-in [:dialog :input] "1000"))]
    (let [result (dlg/execute-dialog rng state)]
      (is (= "Exceeds credit limit" (:message result))))))

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
