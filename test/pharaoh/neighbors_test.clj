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

(defn- check-topic [id state]
  (let [topic (first (filter #(= id (:id %)) nb/advice-topics))]
    ((:check topic) state)))

(deftest ox-feed-advice
  (let [s (assoc (st/initial-state) :oxen 5.0)]
    (is (= :bad (check-topic :ox-feed (assoc s :ox-feed-rt 30))))
    (is (= :good (check-topic :ox-feed (assoc s :ox-feed-rt 90))))
    (is (nil? (check-topic :ox-feed (assoc s :ox-feed-rt 65))))))

(deftest ox-feed-skipped-when-no-oxen
  (is (nil? (check-topic :ox-feed (st/initial-state)))))

(deftest hs-feed-advice
  (let [s (assoc (st/initial-state) :horses 5.0)]
    (is (= :bad (check-topic :hs-feed (assoc s :hs-feed-rt 20))))
    (is (= :good (check-topic :hs-feed (assoc s :hs-feed-rt 80))))
    (is (nil? (check-topic :hs-feed (assoc s :hs-feed-rt 50))))))

(deftest sl-feed-advice
  (let [s (assoc (st/initial-state) :slaves 10.0)]
    (is (= :bad (check-topic :sl-feed (assoc s :sl-feed-rt 3 :sl-health 0.5))))
    (is (= :good (check-topic :sl-feed (assoc s :sl-feed-rt 10 :sl-health 0.9))))
    (is (nil? (check-topic :sl-feed (assoc s :sl-feed-rt 6 :sl-health 0.75))))))

(deftest overseers-advice
  (let [s (assoc (st/initial-state) :overseers 2.0)]
    (is (= :bad (check-topic :overseers (assoc s :slaves 70.0))))
    (is (= :good (check-topic :overseers (assoc s :slaves 20.0))))
    (is (nil? (check-topic :overseers (assoc s :slaves 40.0))))))

(deftest stress-advice
  (let [s (assoc (st/initial-state) :overseers 2.0)]
    (is (= :bad (check-topic :stress (assoc s :ov-press 0.7))))
    (is (= :good (check-topic :stress (assoc s :ov-press 0.1))))
    (is (nil? (check-topic :stress (assoc s :ov-press 0.3))))))

(deftest fertilizer-advice
  (let [s (assoc (st/initial-state) :ln-fallow 10.0)]
    (is (= :bad (check-topic :fertilizer (assoc s :manure 10.0))))
    (is (= :good (check-topic :fertilizer (assoc s :manure 50.0))))
    (is (nil? (check-topic :fertilizer (assoc s :manure 80.0))))))

(deftest sl-health-advice
  (let [s (assoc (st/initial-state) :slaves 10.0)]
    (is (= :bad (check-topic :sl-health (assoc s :sl-health 0.4))))
    (is (= :good (check-topic :sl-health (assoc s :sl-health 0.95))))
    (is (nil? (check-topic :sl-health (assoc s :sl-health 0.75))))))

(deftest ox-health-advice
  (let [s (assoc (st/initial-state) :oxen 5.0)]
    (is (= :bad (check-topic :ox-health (assoc s :ox-health 0.3))))
    (is (= :good (check-topic :ox-health (assoc s :ox-health 0.9))))
    (is (nil? (check-topic :ox-health (assoc s :ox-health 0.7))))))

(deftest hs-health-advice
  (let [s (assoc (st/initial-state) :horses 5.0)]
    (is (= :bad (check-topic :hs-health (assoc s :hs-health 0.3))))
    (is (= :good (check-topic :hs-health (assoc s :hs-health 0.9))))
    (is (nil? (check-topic :hs-health (assoc s :hs-health 0.7))))))

(deftest credit-advice
  (let [s (assoc (st/initial-state) :loan 100.0)]
    (is (= :bad (check-topic :credit (assoc s :credit-rating 0.2))))
    (is (= :good (check-topic :credit (assoc s :credit-rating 0.9))))
    (is (nil? (check-topic :credit (assoc s :credit-rating 0.6))))))

(deftest dumb-guy-randomly-flips-advice
  (let [state (assoc (st/initial-state)
                :good-guy 1 :bad-guy 2 :dumb-guy 3 :banker 0
                :sl-health 0.4 :slaves 10.0)
        results (for [seed (range 2000)]
                  (nb/choose-chat (r/make-rng seed) state 3))
        sl-advice (filter #{:bad-sl-health :good-sl-health} results)]
    (when (seq sl-advice)
      (let [bad-count (count (filter #(= :bad-sl-health %) sl-advice))
            good-count (count (filter #(= :good-sl-health %) sl-advice))]
        (is (pos? bad-count) "dumb guy should sometimes give correct advice")
        (is (pos? good-count) "dumb guy should sometimes flip advice")))))

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
