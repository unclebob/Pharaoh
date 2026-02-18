(ns pharaoh.gherkin.runner
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [pharaoh.gherkin.parser :as parser]))

(def ^:dynamic *step-defs* (atom {}))

(defn register-step [type pattern handler]
  (swap! *step-defs* conj {:type type :pattern pattern :handler handler}))

(defn- match-step [step-defs step]
  (some (fn [{:keys [type pattern handler]}]
          (when (or (= type :any) (= type (:type step)))
            (let [m (re-matches pattern (:text step))]
              (when m
                {:handler handler
                 :args (if (string? m) [] (vec (rest m)))}))))
        step-defs))

(defn- exec-step [step-defs world step]
  (let [match (match-step step-defs step)]
    (if (nil? match)
      {:status :undefined
       :error (str "Undefined step: " (name (:type step)) " " (:text step))}
      (try
        (let [new-world (apply (:handler match) world (:args match))]
          {:status :pass :world (or new-world world)})
        (catch Throwable e
          {:status :fail :error (.getMessage e)})))))

(defn run-scenario [step-defs bg-steps scenario]
  (let [all-steps (concat bg-steps (:steps scenario))]
    (loop [steps (seq all-steps)
           world {}
           results []]
      (if (nil? steps)
        {:pass true :results results}
        (let [step (first steps)
              result (exec-step step-defs world step)]
          (if (= :pass (:status result))
            (recur (next steps)
                   (:world result)
                   (conj results {:step step :status :pass}))
            {:pass false
             :results (conj results {:step step :status (:status result)
                                     :error (:error result)})
             :error (:error result)}))))))

(defn run-feature [step-defs feature]
  (let [bg (:background feature)]
    (mapv (fn [scenario]
            (let [result (run-scenario step-defs bg scenario)]
              (assoc result :scenario (:name scenario))))
          (:scenarios feature))))

(defn run-feature-file [step-defs path]
  (let [feature (parser/parse-file path)]
    {:feature (:name feature)
     :path path
     :results (run-feature step-defs feature)}))

(defn summarize [results]
  (let [all-scenarios (mapcat :results results)
        pass-count (count (filter :pass all-scenarios))
        fail-count (count (remove :pass all-scenarios))
        undefined (filter (fn [r] (some #(= :undefined (:status %)) (:results r)))
                          all-scenarios)]
    {:total (count all-scenarios)
     :pass pass-count
     :fail fail-count
     :undefined (count undefined)}))
