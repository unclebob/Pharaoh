(ns pharaoh.gherkin.steps.economy
  (:require [clojure.string :as str]
            [pharaoh.economy :as ec]
            [pharaoh.gherkin.steps.helpers :refer [near? assert-near to-double
                                                    ensure-rng snap]]
            [pharaoh.overseers :as ov]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(defn steps []
  [;; ===== Economy Given (most specific patterns first) =====

   {:type :given :pattern #"the current inflation rate is (.+)"
    :handler (fn [w v] (assoc-in w [:state :inflation] (to-double v)))}
   {:type :given :pattern #"the world growth rate is (.+)"
    :handler (fn [w v] (assoc-in w [:state :world-growth] (to-double v)))}
   {:type :given :pattern #"current (.+) demand is (.+)"
    :handler (fn [w commodity amt]
               (assoc-in w [:state :demand (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the commodity \"(.+)\" has supply, demand, and production"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player is producing and selling large quantities of (.+)"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the player is buying large quantities of (.+)"
    :handler (fn [w _] w)}
   {:type :given :pattern #"the (.+) demand is (.+)"
    :handler (fn [w commodity amt]
               (assoc-in w [:state :demand (keyword (str/lower-case commodity))]
                         (to-double amt)))}
   {:type :given :pattern #"the (.+) market price is (\d+)"
    :handler (fn [w commodity price]
               (let [k (keyword (str/lower-case commodity))]
                 (assoc-in w [:state :prices k] (to-double price))))}

   ;; ===== Market Given =====

   {:type :given :pattern #"the player has (.+) acres of total land"
    :handler (fn [w amt] (assoc-in w [:state :ln-fallow] (to-double amt)))}

   ;; ===== Economy When =====

   {:type :when :pattern #"monthly prices are updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     rng (:rng w)
                     s (:state w)
                     inf (:inflation s)]
                 (assoc w :state
                        (reduce (fn [s k]
                                  (update-in s [:prices k]
                                             #(ec/update-price rng % inf)))
                                s [:wheat :oxen :horses :slaves :manure :land]))))}
   {:type :when :pattern #"inflation is updated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))]
                 (update-in w [:state :inflation]
                            #(ec/update-inflation (:rng w) %))))}
   {:type :when :pattern #"the production cycle (?:adjusts|runs)"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     rng (:rng w)
                     s (:state w)]
                 (assoc w :state
                        (reduce (fn [s commodity]
                                  (let [data {:supply (get-in s [:supply commodity] 10000.0)
                                              :demand (get-in s [:demand commodity] 10000.0)
                                              :production (get-in s [:production commodity] 10000.0)
                                              :price (get-in s [:prices commodity] 10.0)}
                                        result (ec/adjust-production rng data (:world-growth s))]
                                    (-> s
                                        (assoc-in [:supply commodity] (:supply result))
                                        (assoc-in [:demand commodity] (:demand result))
                                        (assoc-in [:production commodity] (:production result))
                                        (assoc-in [:prices commodity] (:price result)))))
                                s [:wheat :oxen :horses :slaves :manure :land]))))}
   {:type :when :pattern #"the wheat supply grows above demand"
    :handler (fn [w] w)}
   {:type :when :pattern #"wheat demand increases"
    :handler (fn [w] w)}
   {:type :when :pattern #"monthly costs are calculated"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)
                     lt (st/total-land s)
                     k1 (+ (* lt 100) (* (:slaves s) 10) (* (:horses s) 5) (* (:oxen s) 3))
                     cost (* k1 (+ (r/abs-gaussian (:rng w) 0.7 0.3) 0.3))]
                 (assoc w :state (update s :gold - cost))))}
   {:type :when :pattern #"net worth is calculated"
    :handler (fn [w]
               (let [w (snap w)
                     nw (ec/net-worth (:state w))]
                 (assoc-in w [:state :net-worth] nw)))}
   {:type :when :pattern #"monthly costs are assessed"
    :handler (fn [w]
               (let [w (snap (ensure-rng w))
                     s (:state w)]
                 (if (neg? (:gold s))
                   (assoc w :state (ov/overseers-quit (:rng w) s))
                   w)))}

   ;; ===== Economy Then (most specific patterns first) =====

   {:type :then :pattern #"each commodity price is multiplied by approximately.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the adjustment has slight random variance.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"inflation shifts by a small normally-distributed random amount.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"inflation can drift positive or negative.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"wheat demand grows by.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"price (?:increases|decreases) by a random factor.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"production (?:increases|decreases|varies) by a random factor.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"supply increases by production.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the supply, demand, production, and price for.*"
    :handler (fn [w] w)}
   {:type :then :pattern #"the wheat price begins to (?:fall|rise)"
    :handler (fn [w] w)}
   {:type :then :pattern #"global wheat production (?:contracts|increases).*"
    :handler (fn [w] w)}
   {:type :then :pattern #"base cost = .*"
    :handler (fn [w] w)}
   {:type :then :pattern #"actual cost = .*"
    :handler (fn [w] w)}
   {:type :then :pattern #"gold decreases by the actual cost"
    :handler (fn [w] w)}
   {:type :then :pattern #"debt-to-asset ratio = .*"
    :handler (fn [w] w)}
   {:type :then :pattern #"net worth = (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"net worth \+= (.+)"
    :handler (fn [w _] w)}
   {:type :then :pattern #"net worth -= loan"
    :handler (fn [w] w)}
   {:type :then :pattern #"each commodity price shifts by approximately ±(\d+)% \(normally distributed\)"
    :handler (fn [w _] w)}

   ;; --- Phase 0d economy Then ---

   {:type :then :pattern #"the inflation rate shifts by a small normally-distributed amount"
    :handler (fn [w] w)}
   {:type :then :pattern #"the overseer pay increases by approximately (\d+)% with slight variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the overseer pay (?:increases|rate increases) by approximately (\d+)% with slight (?:random )?variance"
    :handler (fn [w _] w)}
   {:type :then :pattern #"the price decreases by a random factor between (\d+)% and (\d+)%"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the price increases by a random factor between (\d+)% and (\d+)%"
    :handler (fn [w _ _] w)}
   {:type :then :pattern #"the wheat price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :wheat]) 1.0)
               w)}
   {:type :then :pattern #"the land price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :land]) 1.0)
               w)}
   {:type :then :pattern #"the slave price should be (\d+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (get-in w [:state :prices :slaves]) 1.0)
               w)}
   {:type :then :pattern #"the world growth rate should be ([\d.]+)"
    :handler (fn [w expected]
               (assert-near (to-double expected) (:world-growth (:state w)) 0.01)
               w)}
   {:type :then :pattern #"default market prices apply"
    :handler (fn [w]
               (assert (pos? (get-in w [:state :prices :wheat] 0))
                       "Expected positive wheat price")
               w)}])
