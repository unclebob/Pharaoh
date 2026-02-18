(ns pharaoh.messages-completeness-test
  (:require [clojure.test :refer :all]
            [pharaoh.messages :as msg]))

(deftest opening-messages-count
  (is (= 29 (count msg/opening-messages))))

(deftest win-messages-count
  (is (= 5 (count msg/win-messages))))

(deftest farewell-messages-count
  (is (= 5 (count msg/farewell-messages))))

(deftest idle-messages-count
  (is (= 53 (count msg/idle-messages))))

(deftest chat-messages-count
  (is (= 32 (count msg/chat-messages))))

(deftest dunning-messages-count
  (is (= 37 (count msg/dunning-messages))))

(deftest foreclosure-warning-messages-count
  (is (= 20 (count msg/foreclosure-warning-messages))))

(deftest foreclosure-messages-count
  (is (= 15 (count msg/foreclosure-messages))))

(deftest game-over-messages-count
  (is (= 5 (count msg/game-over-messages))))

(deftest cash-shortage-messages-count
  (is (= 10 (count msg/cash-shortage-messages))))

(deftest bankruptcy-messages-count
  (is (= 10 (count msg/bankruptcy-messages))))

(deftest credit-check-messages-count
  (is (= 15 (count msg/credit-check-messages))))

(deftest loan-approval-messages-count
  (is (= 15 (count msg/loan-approval-messages))))

(deftest loan-denial-messages-count
  (is (= 15 (count msg/loan-denial-messages))))

(deftest loan-repayment-messages-count
  (is (= 7 (count msg/loan-repayment-messages))))

(deftest repayment-signoff-messages-count
  (is (= 8 (count msg/repayment-signoff-messages))))

(deftest supply-limit-messages-count
  (is (= 9 (count msg/supply-limit-messages))))

(deftest demand-limit-messages-count
  (is (= 8 (count msg/demand-limit-messages))))

(deftest transaction-success-messages-count
  (is (= 9 (count msg/transaction-success-messages))))

(deftest insufficient-funds-messages-count
  (is (= 9 (count msg/insufficient-funds-messages))))

(deftest selling-more-messages-count
  (is (= 5 (count msg/selling-more-messages))))

(deftest contract-default-messages-count
  (is (= 8 (count msg/contract-default-messages))))

(deftest contract-partial-pay-messages-count
  (is (= 8 (count msg/contract-partial-pay-messages))))

(deftest contract-partial-ship-messages-count
  (is (= 8 (count msg/contract-partial-ship-messages))))

(deftest contract-insufficient-goods-messages-count
  (is (= 10 (count msg/contract-insufficient-goods-messages))))

(deftest contract-insufficient-funds-messages-count
  (is (= 9 (count msg/contract-insufficient-funds-messages))))

(deftest buy-complete-messages-count
  (is (= 14 (count msg/buy-complete-messages))))

(deftest advice-messages-pool-counts
  (doseq [[k expected] {:good-ox-feed 10 :bad-ox-feed 10
                         :good-sl-feed 10 :bad-sl-feed 10
                         :good-hs-feed 10 :bad-hs-feed 10
                         :good-overseers 10 :bad-overseers 5
                         :good-stress 10 :bad-stress 10
                         :good-fertilizer 10 :bad-fertilizer 10
                         :good-sl-health 15 :bad-sl-health 15
                         :good-ox-health 15 :bad-ox-health 15
                         :good-hs-health 15 :bad-hs-health 15
                         :good-credit 15 :bad-credit 15}]
    (is (= expected (count (get msg/advice-messages k)))
        (str "Pool " k " expected " expected))))

(deftest aog-adjectives-count
  (is (= 6 (count msg/aog-adjectives))))

(deftest aog-disasters-count
  (is (= 11 (count msg/aog-disasters))))

(deftest aog-consequences-count
  (is (= 10 (count msg/aog-consequences))))

(deftest aog-templates-count
  (is (= 6 (count msg/aog-templates))))

(deftest aom-crowds-count
  (is (= 10 (count msg/aom-crowds))))

(deftest aom-populations-count
  (is (= 10 (count msg/aom-populations))))

(deftest aom-motivations-count
  (is (= 19 (count msg/aom-motivations))))

(deftest aom-actions-count
  (is (= 16 (count msg/aom-actions))))

(deftest aom-templates-count
  (is (= 7 (count msg/aom-templates))))

(deftest war-attackers-count
  (is (= 13 (count msg/war-attackers))))

(deftest war-win-messages-count
  (is (= 4 (count msg/war-win-messages))))

(deftest war-lose-messages-count
  (is (= 4 (count msg/war-lose-messages))))

(deftest revolt-messages-count
  (is (= 5 (count msg/revolt-messages))))

(deftest health-event-messages-count
  (is (= 10 (count (get msg/event-messages :health)))))

(deftest plague-diseases-count
  (is (= 12 (count msg/plague-diseases))))

(deftest plague-templates-count
  (is (= 5 (count msg/plague-templates))))

(deftest locust-messages-count
  (is (= 6 (count (get msg/event-messages :locusts)))))

(deftest wheat-event-messages-count
  (is (= 10 (count (get msg/event-messages :wheat)))))

(deftest gold-event-messages-count
  (is (= 11 (count (get msg/event-messages :gold)))))

(deftest economy-event-messages-count
  (is (= 10 (count (get msg/event-messages :economy)))))

(deftest labor-event-messages-count
  (is (= 9 (count (get msg/event-messages :labor)))))

(deftest workload-event-messages-count
  (is (= 10 (count (get msg/event-messages :workload)))))

(deftest missed-payroll-messages-count
  (is (= 9 (count msg/missed-payroll-messages))))

(deftest input-error-messages-count
  (doseq [[k expected] {:buysell-invalid 5 :buysell-no-function 5
                         :buysell-negative 5 :loan-invalid 5
                         :loan-no-function 5 :loan-insufficient 5
                         :overseer-no-function 5 :overseer-fractional 5
                         :overseer-too-many 5 :planting-invalid 5
                         :planting-negative 5 :pyramid-invalid 5
                         :pyramid-negative 5 :negative-stones 4
                         :manure-invalid 5 :manure-negative 5
                         :feed-invalid 5 :generic-numeric 6}]
    (is (= expected (count (get msg/input-error-messages k)))
        (str "Input error pool " k " expected " expected))))

(deftest player-names-count
  (is (= 10 (count msg/player-names))))

(deftest quit-save-messages-count
  (is (= 22 (count msg/quit-save-messages))))

(deftest all-pools-are-vectors-of-strings
  (doseq [pool [msg/opening-messages msg/win-messages msg/farewell-messages
                msg/idle-messages msg/chat-messages msg/dunning-messages
                msg/foreclosure-warning-messages msg/foreclosure-messages
                msg/game-over-messages msg/cash-shortage-messages
                msg/bankruptcy-messages msg/credit-check-messages
                msg/loan-approval-messages msg/loan-denial-messages
                msg/loan-repayment-messages msg/repayment-signoff-messages
                msg/supply-limit-messages msg/demand-limit-messages
                msg/transaction-success-messages msg/insufficient-funds-messages
                msg/selling-more-messages msg/contract-default-messages
                msg/contract-partial-pay-messages msg/contract-partial-ship-messages
                msg/contract-insufficient-goods-messages
                msg/contract-insufficient-funds-messages
                msg/buy-complete-messages msg/revolt-messages
                msg/missed-payroll-messages msg/player-names
                msg/quit-save-messages msg/aog-adjectives msg/aog-disasters
                msg/aog-consequences msg/aog-templates msg/aom-crowds
                msg/aom-populations msg/aom-motivations msg/aom-actions
                msg/aom-templates
                msg/war-attackers msg/war-win-messages msg/war-lose-messages
                msg/plague-diseases msg/plague-templates]]
    (is (vector? pool))
    (is (every? string? pool))))
