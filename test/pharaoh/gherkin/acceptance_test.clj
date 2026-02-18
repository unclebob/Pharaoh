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
  (let [all-results (run-all-features)
        total-pass (atom 0)
        total-fail (atom 0)]
    (doseq [{:keys [feature path results]} all-results]
      (let [passed (count (filter :pass results))
            failed (count (remove :pass results))]
        (swap! total-pass + passed)
        (swap! total-fail + failed)
        (println (format "  %-30s %3d passed, %d failed" feature passed failed))
        (testing (str feature " (" path ")")
          (doseq [{:keys [scenario pass error]} results]
            (when-not pass
              (println (format "    FAIL: %s — %s" scenario (or error "?"))))
            (testing scenario
              (is pass (or error "scenario failed")))))))
    (println (format "\n  Total: %d passed, %d failed" @total-pass @total-fail))))
