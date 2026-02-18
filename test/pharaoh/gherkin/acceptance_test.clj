(ns pharaoh.gherkin.acceptance-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pharaoh.gherkin.runner :as runner]
            [pharaoh.gherkin.steps :as steps]))

(defn- feature-files []
  (->> (file-seq (io/file "features"))
       (filter #(.endsWith (.getName %) ".feature"))
       (sort-by #(.getName %))
       (mapv #(.getPath %))))

(defn- run-all-features []
  (let [step-defs (steps/all-steps)]
    (mapv #(runner/run-feature-file step-defs %) (feature-files))))

(deftest all-feature-files-discovered
  (let [files (feature-files)]
    (is (pos? (count files))
        "Should find at least one .feature file")))

(deftest all-acceptance-tests-pass
  (let [results (run-all-features)]
    (doseq [{:keys [feature path results]} results]
      (testing (str feature " (" path ")")
        (doseq [{:keys [scenario pass error]} results]
          (testing scenario
            (is pass (or error "scenario failed"))))))))
