(ns pharaoh.visits
  (:require [pharaoh.neighbors :as nb]
            [pharaoh.messages :as msg]
            [pharaoh.random :as r]))

(defn init-timers [rng now]
  {:next-idle (+ now (long (* 1000 (nb/idle-interval rng))))
   :next-chat (+ now (long (* 1000 (nb/chat-interval rng))))
   :next-dunning (+ now (long (* 1000 (nb/dunning-interval 1.0))))})

(defn reset-timers [app now]
  (let [rng (:rng app)]
    (merge app (init-timers rng now))))

(defn check-idle [app now]
  (if (< now (:next-idle app))
    app
    (let [{:keys [rng]} app
          face (nb/choose-man rng)
          text (msg/pick rng msg/idle-messages)]
      (-> app
          (assoc-in [:state :message] {:text text :face face})
          (assoc :next-idle (+ now (long (* 1000 (nb/idle-interval rng)))))))))

(defn check-chat [app now]
  (if (< now (:next-chat app))
    app
    (let [{:keys [rng state]} app
          face (nb/choose-man rng)
          topic (nb/choose-chat rng state face)
          text (if (= topic :chat)
                 (msg/pick rng msg/chat-messages)
                 (msg/pick rng (get msg/advice-messages topic)))]
      (-> app
          (assoc-in [:state :message] {:text text :face face})
          (assoc :next-chat (+ now (long (* 1000 (nb/chat-interval rng)))))))))

(defn check-dunning [app now]
  (let [state (:state app)]
    (if (or (<= (:loan state) 0) (< now (:next-dunning app)))
      app
      (let [{:keys [rng]} app
            face (:banker state)
            text (msg/pick rng msg/dunning-messages)]
        (-> app
            (assoc-in [:state :message] {:text text :face face})
            (assoc :next-dunning
                   (+ now (long (* 1000 (nb/dunning-interval
                                          (:credit-rating state)))))))))))

(defn check-visits [{:keys [state] :as app} now]
  (if (or (:message state) (:dialog state))
    app
    (let [app (check-idle app now)]
      (if (get-in app [:state :message])
        app
        (let [app (check-chat app now)]
          (if (get-in app [:state :message])
            app
            (check-dunning app now)))))))
