(ns pharaoh.gherkin.runner-test
  (:require [clojure.test :refer :all]
            [pharaoh.gherkin.parser :as parser]
            [pharaoh.gherkin.runner :as runner]
            [pharaoh.gherkin.steps :as steps]))

(deftest runner-executes-simple-steps
  (let [step-defs [{:type :given :pattern #"a value of (\d+)"
                     :handler (fn [w v] (assoc w :val (Integer/parseInt v)))}
                    {:type :when :pattern #"it is doubled"
                     :handler (fn [w] (update w :val * 2))}
                    {:type :then :pattern #"the result is (\d+)"
                     :handler (fn [w expected]
                                (assert (= (:val w) (Integer/parseInt expected)))
                                w)}]
        feature {:name "Test"
                 :background []
                 :scenarios [{:name "Double"
                              :steps [{:type :given :text "a value of 5"}
                                      {:type :when :text "it is doubled"}
                                      {:type :then :text "the result is 10"}]}]}
        results (runner/run-feature step-defs feature)]
    (is (= 1 (count results)))
    (is (:pass (first results)))))

(deftest runner-reports-undefined-steps
  (let [step-defs []
        feature {:name "Test"
                 :background []
                 :scenarios [{:name "Missing"
                              :steps [{:type :given :text "something unknown"}]}]}
        results (runner/run-feature step-defs feature)]
    (is (= 1 (count results)))
    (is (not (:pass (first results))))))

(deftest runner-reports-failures
  (let [step-defs [{:type :given :pattern #"setup"
                     :handler (fn [w] w)}
                    {:type :then :pattern #"it fails"
                     :handler (fn [w] (throw (AssertionError. "boom")))}]
        feature {:name "Test"
                 :background []
                 :scenarios [{:name "Failing"
                              :steps [{:type :given :text "setup"}
                                      {:type :then :text "it fails"}]}]}
        results (runner/run-feature step-defs feature)]
    (is (not (:pass (first results))))))

(deftest runner-runs-background-before-each-scenario
  (let [step-defs [{:type :given :pattern #"counter is zero"
                     :handler (fn [w] (assoc w :counter 0))}
                    {:type :when :pattern #"incremented"
                     :handler (fn [w] (update w :counter inc))}
                    {:type :then :pattern #"counter is (\d+)"
                     :handler (fn [w expected]
                                (assert (= (:counter w) (Integer/parseInt expected)))
                                w)}]
        feature {:name "Test"
                 :background [{:type :given :text "counter is zero"}]
                 :scenarios [{:name "First"
                              :steps [{:type :when :text "incremented"}
                                      {:type :then :text "counter is 1"}]}
                             {:name "Second"
                              :steps [{:type :when :text "incremented"}
                                      {:type :then :text "counter is 1"}]}]}
        results (runner/run-feature step-defs feature)]
    (is (every? :pass results))))

(deftest feature-files-parse-without-errors
  (let [features ["features/game_setup.feature"
                   "features/pyramid.feature"
                   "features/market_economy.feature"
                   "features/trading.feature"
                   "features/feeding.feature"
                   "features/planting.feature"
                   "features/workload.feature"
                   "features/health.feature"
                   "features/overseers.feature"
                   "features/loans.feature"
                   "features/contracts.feature"
                   "features/random_events.feature"
                   "features/neighbors.feature"
                   "features/game_persistence.feature"]]
    (doseq [f features]
      (testing f
        (let [result (parser/parse-file f)]
          (is (string? (:name result)))
          (is (pos? (count (:scenarios result)))))))))

(deftest game-setup-feature-runs-with-steps
  (let [step-defs (steps/all-steps)
        result (runner/run-feature-file step-defs "features/game_setup.feature")]
    (is (= "Game Setup" (:feature result)))
    (is (pos? (count (:results result))))))

(deftest pyramid-feature-runs-with-steps
  (let [step-defs (steps/all-steps)
        result (runner/run-feature-file step-defs "features/pyramid.feature")]
    (is (pos? (count (:results result))))))

(deftest random-events-feature-runs-with-steps
  (let [step-defs (steps/all-steps)
        result (runner/run-feature-file step-defs "features/random_events.feature")]
    (is (pos? (count (:results result))))))
