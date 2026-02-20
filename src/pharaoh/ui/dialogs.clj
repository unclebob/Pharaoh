(ns pharaoh.ui.dialogs
  (:require [pharaoh.contracts :as ct]
            [pharaoh.loans :as ln]
            [pharaoh.messages :as msg]
            [pharaoh.overseers :as ov]
            [pharaoh.trading :as tr]
            [pharaoh.random :as r]))

;; Dialog state is stored in the app state atom
;; :dialog - nil or {:type :buy-sell/:loan/:feed/:plant/:overseer/:pyramid
;;                   :commodity :wheat/:slaves/etc.
;;                   :mode :buy/:sell/:keep/:acquire
;;                   :input ""}

(defn open-contracts-dialog [state]
  (let [active (filterv :active (:cont-offers state))]
    (assoc state :dialog {:type :contracts :mode :browsing
                          :selected 0 :input ""
                          :active-offers active})))

(defn navigate-contracts [state direction]
  (let [d (:dialog state)
        n (count (:active-offers d))
        sel (:selected d)]
    (if (zero? n)
      state
      (let [new-sel (case direction
                      :down (mod (inc sel) n)
                      :up (mod (+ sel (dec n)) n))]
        (assoc-in state [:dialog :selected] new-sel)))))

(defn confirm-selected [state]
  (assoc-in state [:dialog :mode] :confirming))

(defn reject-selected [state]
  (assoc-in state [:dialog :mode] :browsing))

(defn close-dialog [state]
  (dissoc state :dialog))

(defn accept-selected [state]
  (let [d (:dialog state)
        offer (nth (:active-offers d) (:selected d))
        idx (.indexOf (:cont-offers state) offer)
        result (ct/accept-contract state idx)]
    (if (:error result)
      (assoc state :message (:error result))
      (close-dialog result))))

(defn open-dialog [state dialog-type & [opts]]
  (assoc state :dialog (merge {:type dialog-type :input "" :mode nil} opts)))

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

(defn accept-credit-check [rng state]
  (let [d (:dialog state)
        fee (:fee d)
        amt (:borrow-amt d)
        total-amt (+ amt fee)
        old-limit (:credit-limit state)
        state (ln/credit-check rng state)
        new-limit (:credit-limit state)]
    (if (<= (+ (:loan state) total-amt) new-limit)
      ;; C code: loan += amt; gold += amt (fee financed into loan)
      (let [int-rate (+ (:interest state) (:int-addition state))
            m (format (msg/pick rng msg/loan-approval-messages) total-amt int-rate)]
        (-> state
            (update :loan + total-amt)
            (update :gold + total-amt)
            (dissoc :dialog)
            (assoc :message {:text m :face (:banker state)})))
      ;; C code: creditLimit = oldCreditLimit; gold = max(0, gold-cost)
      (let [m (msg/pick rng msg/loan-denial-messages)]
        (-> state
            (assoc :credit-limit old-limit)
            (update :gold #(max 0.0 (- % fee)))
            (dissoc :dialog)
            (assoc :message {:text m :face (:banker state)}))))))

(defn reject-credit-check [state]
  (assoc state :dialog
         (-> (:dialog state)
             (assoc :mode nil :input "")
             (dissoc :fee :borrow-amt :message))))

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
              :buy (let [v (tr/validate-buy state commodity amt)]
                     (if (= :error (:status v))
                       (assoc state :message
                              (format (msg/pick rng msg/insufficient-funds-messages)
                                      (:max-amount v)))
                       (let [supply (get-in state [:supply commodity] 0.0)
                             actual (min amt (max supply 1.0))
                             result (tr/buy rng state commodity amt)]
                         (if (< actual amt)
                           (assoc (close-dialog result) :message
                                  (format (msg/pick rng msg/demand-limit-messages)
                                          actual))
                           (close-dialog result)))))
              :sell (let [v (tr/validate-sell state commodity amt)]
                      (case (:status v)
                        :error
                        (assoc state :message
                               (format (msg/pick rng msg/selling-more-messages)
                                       (:max-amount v)))
                        :capped
                        (assoc state :message
                               (format (msg/pick rng msg/supply-limit-messages)
                                       (:max-amount v)))
                        (close-dialog (tr/sell rng state commodity amt))))
              (assoc state :message
                     (pick-error rng :buysell-no-function))))

          :loan
          (case (:mode d)
            :borrow (let [result (ln/borrow rng state amt)]
                      (cond
                        (:needs-credit-check result)
                        (let [fee (:fee result)
                              m (format (msg/pick rng msg/credit-check-messages) fee)]
                          (assoc state :dialog
                                 (assoc d :mode :credit-check
                                          :fee fee :borrow-amt amt
                                          :message m)))
                        (:error result)
                        (assoc state :message (:error result))
                        :else
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
