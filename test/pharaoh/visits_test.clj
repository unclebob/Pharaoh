(ns pharaoh.visits-test
  (:require [clojure.test :refer :all]
            [pharaoh.visits :as v]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(defn- make-app [& {:as overrides}]
  (let [rng (r/make-rng 42)
        base {:state (merge (st/initial-state)
                            {:banker 0 :good-guy 1 :bad-guy 2 :dumb-guy 3})
              :rng rng
              :next-idle 1000
              :next-chat 2000
              :next-dunning 3000}]
    (merge base overrides)))

;; --- init-timers ---

(deftest init-timers-sets-future-timestamps
  (let [rng (r/make-rng 42)
        now 10000
        timers (v/init-timers rng now)]
    (is (> (:next-idle timers) now))
    (is (> (:next-chat timers) now))
    (is (> (:next-dunning timers) now))))

;; --- check-idle ---

(deftest check-idle-fires-when-past-due
  (let [app (make-app :next-idle 500)
        now 1000
        result (v/check-idle app now)]
    (is (some? (get-in result [:state :message])))
    (is (> (:next-idle result) now))))

(deftest check-idle-skips-when-not-due
  (let [app (make-app :next-idle 5000)
        now 1000
        result (v/check-idle app now)]
    (is (nil? (get-in result [:state :message])))
    (is (= 5000 (:next-idle result)))))

;; --- check-chat ---

(deftest check-chat-fires-with-face-message
  (let [app (make-app :next-chat 500)
        now 1000
        result (v/check-chat app now)]
    (is (map? (get-in result [:state :message])))
    (is (contains? (get-in result [:state :message]) :text))
    (is (contains? (get-in result [:state :message]) :face))
    (is (> (:next-chat result) now))))

(deftest check-chat-skips-when-not-due
  (let [app (make-app :next-chat 5000)
        now 1000
        result (v/check-chat app now)]
    (is (nil? (get-in result [:state :message])))
    (is (= 5000 (:next-chat result)))))

(deftest check-chat-advice-uses-neighbor-personality
  (let [state (merge (st/initial-state)
                     {:banker 0 :good-guy 1 :bad-guy 2 :dumb-guy 3
                      :slaves 10.0 :sl-health 0.4 :oxen 10.0 :ox-health 0.4})
        results (for [seed (range 200)]
                  (let [rng (r/make-rng seed)
                        app {:state state :rng rng
                             :next-idle 99999 :next-chat 0 :next-dunning 99999}
                        result (v/check-chat app 1000)
                        msg (get-in result [:state :message])]
                    msg))
        face-msgs (filter map? results)]
    (is (pos? (count face-msgs)))
    (is (every? #(contains? % :face) face-msgs))
    (is (every? #(string? (:text %)) face-msgs))))

;; --- check-dunning ---

(deftest check-dunning-fires-when-loan-positive
  (let [app (make-app :next-dunning 500)
        app (assoc-in app [:state :loan] 50000.0)
        now 1000
        result (v/check-dunning app now)
        m (get-in result [:state :message])]
    (is (some? m))
    (is (= (:banker (:state app)) (:face m)))
    (is (some #(= (:text m) %) msg/dunning-messages))
    (is (> (:next-dunning result) now))))

(deftest check-dunning-skips-when-no-loan
  (let [app (make-app :next-dunning 500)
        app (assoc-in app [:state :loan] 0.0)
        now 1000
        result (v/check-dunning app now)]
    (is (nil? (get-in result [:state :message])))))

(deftest check-dunning-skips-when-not-due
  (let [app (make-app :next-dunning 5000)
        app (assoc-in app [:state :loan] 50000.0)
        now 1000
        result (v/check-dunning app now)]
    (is (nil? (get-in result [:state :message])))))

;; --- check-visits ---

(deftest check-visits-skips-when-message-showing
  (let [app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        app (assoc-in app [:state :message] {:text "hi" :face 0})
        now 5000
        result (v/check-visits app now)]
    (is (= {:text "hi" :face 0} (get-in result [:state :message])))))

(deftest check-visits-skips-when-dialog-open
  (let [app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        app (assoc-in app [:state :dialog] {:type :buy-sell})
        now 5000
        result (v/check-visits app now)]
    (is (nil? (get-in result [:state :message])))))

(deftest check-visits-skips-when-menu-open
  (let [app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        app (assoc app :menu {:open? true})
        now 5000
        result (v/check-visits app now)]
    (is (nil? (get-in result [:state :message])))))

;; --- reset-timers ---

(deftest reset-timers-sets-all-future-timestamps
  (let [rng (r/make-rng 42)
        app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        now 5000
        result (v/reset-timers app now)]
    (is (> (:next-idle result) now))
    (is (> (:next-chat result) now))
    (is (> (:next-dunning result) now))))

(deftest reset-timers-preserves-rest-of-app
  (let [app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        now 5000
        result (v/reset-timers app now)]
    (is (= (:state app) (:state result)))
    (is (= (:rng app) (:rng result)))))

;; --- check-visits ---

(deftest check-visits-only-one-fires
  (let [app (make-app :next-idle 0 :next-chat 0 :next-dunning 0)
        app (assoc-in app [:state :loan] 50000.0)
        now 5000
        result (v/check-visits app now)
        m (get-in result [:state :message])]
    (is (some? m))
    (is (map? m))
    (is (string? (:text m)))))
