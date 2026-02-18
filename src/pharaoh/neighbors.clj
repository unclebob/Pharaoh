(ns pharaoh.neighbors
  (:require [pharaoh.random :as r]
            [pharaoh.state :as st]
            [pharaoh.tables :as t]))

(defn choose-man [rng]
  (long (r/uniform rng 0 3.999)))

(defn set-men [rng]
  (let [banker (choose-man rng)]
    (loop [good-guy (choose-man rng)]
      (if (not= good-guy banker)
        (loop [bad-guy (choose-man rng)]
          (if (and (not= bad-guy banker) (not= bad-guy good-guy))
            (loop [dumb-guy (choose-man rng)]
              (if (and (not= dumb-guy banker) (not= dumb-guy bad-guy)
                       (not= dumb-guy good-guy))
                {:banker banker :good-guy good-guy
                 :bad-guy bad-guy :dumb-guy dumb-guy}
                (recur (choose-man rng))))
            (recur (choose-man rng))))
        (recur (choose-man rng))))))

(defn voice-settings [face]
  (case (int face)
    0 {:rate 100 :pitch 200}
    1 {:rate 150 :pitch 66}
    2 {:rate 200 :pitch 100}
    3 {:rate 250 :pitch 150}
    {:rate 190 :pitch 310}))

(def advice-topics
  [{:id :ox-feed
    :check (fn [s] (when (>= (:oxen s) 1)
                     (cond (< (:ox-feed-rt s) 50) :bad
                           (> (:ox-feed-rt s) 80) :good)))
    :good :good-ox-feed :bad :bad-ox-feed}
   {:id :hs-feed
    :check (fn [s] (when (>= (:horses s) 1)
                     (cond (< (:hs-feed-rt s) 40) :bad
                           (> (:hs-feed-rt s) 65) :good)))
    :good :good-hs-feed :bad :bad-hs-feed}
   {:id :sl-feed
    :check (fn [s] (when (>= (:slaves s) 1)
                     (cond (and (< (:sl-feed-rt s) 5) (< (:sl-health s) 0.7)) :bad
                           (and (> (:sl-feed-rt s) 8) (> (:sl-health s) 0.8)) :good)))
    :good :good-sl-feed :bad :bad-sl-feed}
   {:id :overseers
    :check (fn [s] (when (>= (:overseers s) 1)
                     (let [sl-ov (if (pos? (:overseers s))
                                   (/ (:slaves s) (:overseers s)) 0)]
                       (cond (> sl-ov 30) :bad
                             (< sl-ov 15) :good))))
    :good :good-overseers :bad :bad-overseers}
   {:id :stress
    :check (fn [s] (when (>= (:overseers s) 1)
                     (cond (> (:ov-press s) 0.5) :bad
                           (< (:ov-press s) 0.2) :good)))
    :good :good-stress :bad :bad-stress}
   {:id :fertilizer
    :check (fn [s] (let [lt (st/total-land s)]
                     (when (>= lt 1)
                       (let [mn-ln (/ (:manure s) lt)]
                         (cond (< mn-ln 2) :bad
                               (and (> mn-ln 3.5) (< mn-ln 7)) :good)))))
    :good :good-fertilizer :bad :bad-fertilizer}
   {:id :sl-health
    :check (fn [s] (when (>= (:slaves s) 1)
                     (cond (< (:sl-health s) 0.6) :bad
                           (> (:sl-health s) 0.9) :good)))
    :good :good-sl-health :bad :bad-sl-health}
   {:id :ox-health
    :check (fn [s] (when (>= (:oxen s) 1)
                     (cond (< (:ox-health s) 0.5) :bad
                           (> (:ox-health s) 0.85) :good)))
    :good :good-ox-health :bad :bad-ox-health}
   {:id :hs-health
    :check (fn [s] (when (>= (:horses s) 1)
                     (cond (< (:hs-health s) 0.5) :bad
                           (> (:hs-health s) 0.85) :good)))
    :good :good-hs-health :bad :bad-hs-health}
   {:id :credit
    :check (fn [s] (when (>= (:loan s) 1)
                     (cond (< (:credit-rating s) 0.4) :bad
                           (> (:credit-rating s) 0.8) :good)))
    :good :good-credit :bad :bad-credit}])

(defn- flip-advice [advice topic]
  (cond
    (= advice (:good topic)) (:bad topic)
    (= advice (:bad topic)) (:good topic)
    :else advice))

(defn choose-chat [rng state man]
  (if (== man (:banker state))
    :chat
    (if (< (r/uniform rng 0 100) 20)
      :chat
      (let [topic-idx (long (r/uniform rng 0 9.999))
            topic (nth advice-topics topic-idx)
            quality ((:check topic) state)]
        (if (nil? quality)
          :chat
          (let [advice (if (= quality :good) (:good topic) (:bad topic))
                advice (if (== man (:bad-guy state))
                         (flip-advice advice topic) advice)
                advice (if (== man (:dumb-guy state))
                         (if (< (r/uniform rng 0 1) 0.5)
                           (flip-advice advice topic) advice)
                         advice)
                advice (if (> (r/uniform rng 0 100) 95)
                         (flip-advice advice topic) advice)]
            advice))))))

(defn dunning-interval [credit-rating]
  (t/interpolate credit-rating t/dunning-time))

(defn idle-interval [rng]
  (r/uniform rng 60 90))

(defn chat-interval [rng]
  (r/uniform rng 90 200))
