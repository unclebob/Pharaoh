(ns pharaoh.ui.dialogs
  (:require [pharaoh.loans :as ln]
            [pharaoh.messages :as msg]
            [pharaoh.overseers :as ov]
            [pharaoh.trading :as tr]
            [pharaoh.random :as r]))

;; Dialog state is stored in the app state atom
;; :dialog - nil or {:type :buy-sell/:loan/:feed/:plant/:overseer/:pyramid
;;                   :commodity :wheat/:slaves/etc.
;;                   :mode :buy/:sell/:keep/:acquire
;;                   :input ""}

(defn open-dialog [state dialog-type & [opts]]
  (assoc state :dialog (merge {:type dialog-type :input "" :mode nil} opts)))

(defn close-dialog [state]
  (dissoc state :dialog))

(defn update-dialog-input [state ch]
  (if-let [d (:dialog state)]
    (cond
      (= ch \backspace)
      (assoc state :dialog
             (update d :input #(if (seq %) (subs % 0 (dec (count %))) "")))
      (or (Character/isDigit ch) (= ch \.))
      (assoc state :dialog (update d :input str ch))
      :else state)
    state))

(defn set-dialog-mode [state mode]
  (assoc-in state [:dialog :mode] mode))

(defn- parse-input [input]
  (try (Double/parseDouble input) (catch Exception _ nil)))

(defn- error-category [dialog-type]
  (case dialog-type
    :buy-sell :buysell-invalid :loan :loan-invalid
    :feed :feed-invalid :plant :planting-invalid
    :spread :manure-invalid :pyramid :pyramid-invalid
    :overseer :generic-numeric :generic-numeric))

(defn- pick-error [rng cat]
  (msg/pick rng (get msg/input-error-messages cat)))

(defn execute-dialog [rng state]
  (if-let [d (:dialog state)]
    (let [amt (parse-input (:input d))]
      (if (nil? amt)
        (assoc state :message (pick-error rng (error-category (:type d))))
        (case (:type d)
          :buy-sell
          (let [commodity (:commodity d)]
            (case (:mode d)
              :buy (let [result (tr/buy rng state commodity amt)]
                     (if (:error result)
                       (assoc state :message (:error result))
                       (close-dialog result)))
              :sell (let [result (tr/sell rng state commodity amt)]
                      (if (:error result)
                        (assoc state :message (:error result))
                        (close-dialog result)))
              (assoc state :message
                     (pick-error rng :buysell-no-function))))

          :loan
          (case (:mode d)
            :borrow (let [result (ln/borrow state amt)]
                      (if (:needs-credit-check result)
                        (assoc state :message "Exceeds credit limit")
                        (close-dialog result)))
            :repay (let [result (ln/repay state amt)]
                     (if (:error result)
                       (assoc state :message (:error result))
                       (close-dialog result)))
            (assoc state :message
                   (pick-error rng :loan-no-function)))

          :feed
          (let [k (case (:commodity d)
                    :slaves :sl-feed-rt
                    :oxen :ox-feed-rt
                    :horses :hs-feed-rt)]
            (close-dialog (assoc state k amt)))

          :plant
          (close-dialog (assoc state :ln-to-sew amt))

          :spread
          (close-dialog (assoc state :mn-to-sprd amt))

          :pyramid
          (close-dialog (assoc state :py-quota amt))

          :overseer
          (case (:mode d)
            :hire (close-dialog (ov/hire state (long amt)))
            :fire (let [result (ov/fire state (long amt))]
                    (if (:error result)
                      (assoc state :message (:error result))
                      (close-dialog result)))
            (assoc state :message
                   (pick-error rng :overseer-no-function)))

          (close-dialog state))))
    state))
