(ns pharaoh.core-test
  (:require [clojure.test :refer :all]
            [pharaoh.core]))

;; Access private functions via var references
(def fmt-offer #'pharaoh.core/fmt-offer)
(def fmt-confirm #'pharaoh.core/fmt-confirm)

;; ---- fmt-offer ----

(deftest fmt-offer-buy-contract
  (let [offer {:type :buy :who 0 :what :wheat
               :amount 100.0 :price 10.0 :duration 24}
        players [{:name "King HamuNam"}]]
    (is (= "King HamuNam: BUY 100 wheat @ 10.0g 24mo"
           (fmt-offer offer players)))))

(deftest fmt-offer-sell-contract
  (let [offer {:type :sell :who 1 :what :slaves
               :amount 50.0 :price 5000.0 :duration 12}
        players [{:name "P1"} {:name "Queen Nefi"}]]
    (is (= "Queen Nefi: SELL 50 slaves @ 5000.0g 12mo"
           (fmt-offer offer players)))))

(deftest fmt-offer-unknown-player
  (let [offer {:type :buy :who 5 :what :oxen
               :amount 200.0 :price 300.0 :duration 36}
        players [{:name "Only One"}]]
    (is (= "?: BUY 200 oxen @ 300.0g 36mo"
           (fmt-offer offer players)))))

;; ---- fmt-confirm ----

(deftest fmt-confirm-buy-contract-inverts-to-sell
  (let [offer {:type :buy :who 0 :what :wheat
               :amount 100.0 :price 1000.0 :duration 24}
        players [{:name "King HamuNam"}]]
    (is (= "Will you sell 100 wheat to King HamuNam for 1000 gold in 24 months?"
           (fmt-confirm offer players)))))

(deftest fmt-confirm-sell-contract-inverts-to-buy
  (let [offer {:type :sell :who 1 :what :slaves
               :amount 50.0 :price 5000.0 :duration 12}
        players [{:name "P1"} {:name "Queen Nefi"}]]
    (is (= "Will you buy 50 slaves from Queen Nefi for 5000 gold in 12 months?"
           (fmt-confirm offer players)))))

(deftest fmt-confirm-unknown-player
  (let [offer {:type :buy :who 9 :what :horses
               :amount 25.0 :price 500.0 :duration 18}
        players [{:name "Only One"}]]
    (is (= "Will you sell 25 horses to ? for 500 gold in 18 months?"
           (fmt-confirm offer players)))))
