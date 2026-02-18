(ns pharaoh.messages-test
  (:require [clojure.test :refer :all]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]))

(deftest pick-returns-element-from-pool
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (let [m (msg/pick rng msg/idle-messages)]
        (is (some #(= m %) msg/idle-messages))))))

(deftest pick-covers-pool
  (let [rng (r/make-rng 42)
        pool ["a" "b" "c" "d" "e"]
        results (set (repeatedly 200 #(msg/pick rng pool)))]
    (is (= 5 (count results)))))

(deftest all-message-pools-non-empty
  (is (pos? (count msg/idle-messages)))
  (is (pos? (count msg/chat-messages)))
  (is (pos? (count msg/dunning-messages)))
  (is (pos? (count msg/win-messages)))
  (is (pos? (count msg/farewell-messages)))
  (is (pos? (count msg/game-over-messages)))
  (is (pos? (count msg/opening-messages)))
  (is (pos? (count msg/cash-shortage-messages)))
  (is (pos? (count msg/foreclosure-messages)))
  (is (pos? (count msg/foreclosure-warning-messages)))
  (is (pos? (count msg/bankruptcy-messages)))
  (is (pos? (count msg/repayment-signoff-messages)))
  (is (pos? (count msg/demand-limit-messages)))
  (is (pos? (count msg/transaction-success-messages)))
  (is (pos? (count msg/selling-more-messages)))
  (is (pos? (count msg/contract-insufficient-goods-messages)))
  (is (pos? (count msg/contract-insufficient-funds-messages)))
  (is (pos? (count msg/buy-complete-messages)))
  (is (pos? (count msg/missed-payroll-messages)))
  (is (pos? (count msg/player-names)))
  (is (pos? (count msg/quit-save-messages)))
  (is (pos? (count msg/plague-diseases)))
  (is (pos? (count msg/plague-templates)))
  (is (pos? (count msg/aog-templates)))
  (is (pos? (count msg/aom-templates))))

(deftest advice-messages-cover-all-topics
  (let [expected-keys #{:good-ox-feed :bad-ox-feed :good-hs-feed :bad-hs-feed
                         :good-sl-feed :bad-sl-feed :good-overseers :bad-overseers
                         :good-stress :bad-stress :good-fertilizer :bad-fertilizer
                         :good-sl-health :bad-sl-health :good-ox-health :bad-ox-health
                         :good-hs-health :bad-hs-health :good-credit :bad-credit}]
    (is (= expected-keys (set (keys msg/advice-messages))))
    (doseq [[_ pool] msg/advice-messages]
      (is (pos? (count pool))))))

(deftest event-messages-cover-all-types
  (doseq [k [:locusts :health :workload :labor :wheat :gold :economy]]
    (is (pos? (count (get msg/event-messages k))))))

(deftest word-pools-non-empty
  (is (pos? (count msg/aog-adjectives)))
  (is (pos? (count msg/aog-disasters)))
  (is (pos? (count msg/aog-consequences)))
  (is (pos? (count msg/aom-crowds)))
  (is (pos? (count msg/aom-motivations)))
  (is (pos? (count msg/aom-actions)))
  (is (pos? (count msg/war-attackers)))
  (is (pos? (count msg/revolt-messages))))

(deftest input-error-messages-cover-all-categories
  (doseq [k [:buysell-invalid :buysell-no-function :buysell-negative
              :loan-invalid :loan-no-function :loan-insufficient
              :overseer-no-function :overseer-fractional :overseer-too-many
              :planting-invalid :planting-negative
              :pyramid-invalid :pyramid-negative :negative-stones
              :manure-invalid :manure-negative :feed-invalid :generic-numeric]]
    (is (pos? (count (get msg/input-error-messages k)))
        (str "Missing category: " k))))
