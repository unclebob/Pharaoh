(ns pharaoh.neighbors-test
  (:require [clojure.test :refer :all]
            [pharaoh.neighbors :as nb]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

(deftest set-men-assigns-unique-faces
  (let [rng (r/make-rng 42)
        result (nb/set-men rng)]
    (is (== 4 (count (set (vals result)))))
    (doseq [[_ v] result]
      (is (<= 0 v 3)))))

(deftest choose-man-in-range
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (is (<= 0 (nb/choose-man rng) 3)))))

(deftest voice-settings-by-face
  (is (= {:rate 100 :pitch 200} (nb/voice-settings 0)))
  (is (= {:rate 150 :pitch 66} (nb/voice-settings 1)))
  (is (= {:rate 200 :pitch 100} (nb/voice-settings 2)))
  (is (= {:rate 250 :pitch 150} (nb/voice-settings 3)))
  (is (= {:rate 190 :pitch 310} (nb/voice-settings 99))))

(deftest banker-only-chats
  (let [rng (r/make-rng 42)
        state (assoc (st/initial-state) :banker 0)]
    (dotimes [_ 50]
      (is (= :chat (nb/choose-chat rng state 0))))))

(deftest good-guy-gives-accurate-advice
  (let [state (assoc (st/initial-state)
                :good-guy 1 :bad-guy 2 :dumb-guy 3 :banker 0
                :sl-health 0.4 :slaves 10.0)
        results (for [seed (range 1000)]
                  (nb/choose-chat (r/make-rng seed) state 1))
        sl-advice (filter #{:bad-sl-health :good-sl-health} results)]
    (when (seq sl-advice)
      (let [bad-count (count (filter #(= :bad-sl-health %) sl-advice))]
        (is (> bad-count (* (count sl-advice) 0.8)))))))

(deftest bad-guy-inverts-advice
  (let [state (assoc (st/initial-state)
                :good-guy 1 :bad-guy 2 :dumb-guy 3 :banker 0
                :sl-health 0.4 :slaves 10.0)
        results (for [seed (range 1000)]
                  (nb/choose-chat (r/make-rng seed) state 2))
        sl-advice (filter #{:bad-sl-health :good-sl-health} results)]
    (when (seq sl-advice)
      (let [good-count (count (filter #(= :good-sl-health %) sl-advice))]
        (is (> good-count (* (count sl-advice) 0.8)))))))

(deftest advice-topics-cover-all-areas
  (is (== 10 (count nb/advice-topics))))

(deftest dunning-interval-depends-on-credit
  (let [low-credit (nb/dunning-interval 0.1)
        high-credit (nb/dunning-interval 0.9)]
    (is (< low-credit high-credit))))

(deftest idle-timer-range
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (let [t (nb/idle-interval rng)]
        (is (<= 60 t 90))))))

(deftest chat-timer-range
  (let [rng (r/make-rng 42)]
    (dotimes [_ 100]
      (let [t (nb/chat-interval rng)]
        (is (<= 90 t 200))))))
