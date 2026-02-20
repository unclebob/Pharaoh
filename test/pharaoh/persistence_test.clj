(ns pharaoh.persistence-test
  (:require [clojure.test :refer :all]
            [pharaoh.persistence :as p]
            [pharaoh.state :as st]
            [pharaoh.ui.file-actions :as fa]
            [pharaoh.ui.dialogs :as dlg]
            [pharaoh.random :as r]
            [clojure.java.io :as io]))

(defn- tmp-file []
  (let [f (java.io.File/createTempFile "pharaoh-test" ".edn")]
    (.deleteOnExit f)
    (.getAbsolutePath f)))

(deftest save-and-load-round-trip
  (let [path (tmp-file)
        state (assoc (st/initial-state) :gold 12345.0 :wheat 999.0)]
    (p/save-game state path)
    (let [loaded (p/load-game path)]
      (is (= 12345.0 (:gold loaded)))
      (is (= 999.0 (:wheat loaded)))
      (is (= (:py-base state) (:py-base loaded))))))

(deftest save-creates-file
  (let [path (tmp-file)]
    (io/delete-file path true)
    (is (not (.exists (io/file path))))
    (p/save-game (st/initial-state) path)
    (is (.exists (io/file path)))))

(deftest load-nonexistent-returns-nil
  (is (nil? (p/load-game "/tmp/pharaoh-nonexistent-abc123.edn"))))

(deftest round-trip-preserves-all-keys
  (let [path (tmp-file)
        state (st/initial-state)]
    (p/save-game state path)
    (let [loaded (p/load-game path)]
      (is (= (set (keys state)) (set (keys loaded)))))))

(deftest round-trip-with-modified-state
  (let [path (tmp-file)
        state (-> (st/initial-state)
                  (assoc :month 7 :year 15
                         :slaves 200.0 :overseers 5.0
                         :py-stones 50000.0 :loan 100000.0
                         :game-over false :game-won false))]
    (p/save-game state path)
    (let [loaded (p/load-game path)]
      (is (= 7 (:month loaded)))
      (is (= 15 (:year loaded)))
      (is (= 200.0 (:slaves loaded)))
      (is (= 50000.0 (:py-stones loaded))))))

(deftest full-save-load-via-file-actions
  (let [state (-> (st/initial-state)
                  (assoc :gold 12345.0 :month 7 :year 5 :dirty true))
        path (tmp-file)
        ;; Save via file-actions
        saved (fa/do-save (assoc state :save-path path))]
    (is (false? (:dirty saved)))
    (is (.exists (java.io.File. path)))
    ;; Load into fresh state via dialog
    (let [fresh (assoc (st/initial-state) :dirty false)
          with-dialog (fa/do-open fresh)]
      (is (= :load-file (get-in with-dialog [:dialog :type])))
      ;; Simulate typing filename and pressing enter
      (let [typed (assoc-in with-dialog [:dialog :input] path)
            rng (r/make-rng 42)
            loaded (dlg/execute-dialog rng typed)]
        (is (nil? (:dialog loaded)))
        (is (= 12345.0 (:gold (:loaded-state loaded))))
        (is (= 7 (:month (:loaded-state loaded))))
        (is (= 5 (:year (:loaded-state loaded))))))))
