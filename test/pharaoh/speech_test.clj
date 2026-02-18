(ns pharaoh.speech-test
  (:require [clojure.test :refer :all]
            [pharaoh.speech :as sp]))

(deftest voice-settings-returns-map
  (doseq [face (range 4)]
    (let [v (sp/voice-settings face)]
      (is (number? (:rate v)))
      (is (number? (:pitch v))))))

(deftest voice-settings-default-for-unknown
  (let [v (sp/voice-settings 99)]
    (is (= 190 (:rate v)))
    (is (= 310 (:pitch v)))))

(deftest speak-async-returns-future
  (let [f (sp/speak-async "test" {:rate 190 :pitch 310})]
    (is (future? f))
    (future-cancel f)))

(deftest format-message-substitutes
  (is (= "Hello World" (sp/format-message "Hello {0}" ["World"])))
  (is (= "A and B" (sp/format-message "{0} and {1}" ["A" "B"]))))
