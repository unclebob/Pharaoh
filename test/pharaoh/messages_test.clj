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
  (is (pos? (count msg/game-over-messages)))
  (is (pos? (count msg/opening-messages)))
  (is (pos? (count msg/cash-shortage-messages)))
  (is (pos? (count msg/foreclosure-messages))))

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
  (doseq [k [:locusts :plagues :health :workload :labor :wheat :gold :economy]]
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
