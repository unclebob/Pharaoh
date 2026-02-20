(ns pharaoh.ui.file-actions-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.file-actions :as fa]
            [pharaoh.state :as st]))

(deftest save-action-with-save-path-saves-directly
  (let [path (str "/tmp/pharaoh-fa-" (System/currentTimeMillis) ".edn")
        state (assoc (st/initial-state) :gold 4444.0 :dirty true :save-path path)
        result (fa/do-save state)]
    (is (false? (:dirty result)))
    (is (nil? (:dialog result)))
    (is (.exists (java.io.File. path)))))

(deftest save-action-without-save-path-opens-dialog
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-save state)]
    (is (= :save-file (get-in result [:dialog :type])))))

(deftest save-as-always-opens-dialog
  (let [state (assoc (st/initial-state) :save-path "/tmp/existing.edn")
        result (fa/do-save-as state)]
    (is (= :save-file (get-in result [:dialog :type])))))

(deftest open-action-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-open state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :load (get-in result [:dialog :next-action])))))

(deftest open-action-when-clean-opens-load-dialog
  (let [state (assoc (st/initial-state) :dirty false)
        result (fa/do-open state)]
    (is (= :load-file (get-in result [:dialog :type])))))

(deftest new-game-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-new-game state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :new-game (get-in result [:dialog :next-action])))))

(deftest new-game-when-clean-resets-state
  (let [state (assoc (st/initial-state) :dirty false :gold 9999.0)
        result (fa/do-new-game state)]
    (is (= 0.0 (:gold result)))
    (is (= 1 (:month result)))
    (is (false? (:dirty result)))
    (is (nil? (:save-path result)))))

(deftest quit-when-dirty-prompts-save
  (let [state (assoc (st/initial-state) :dirty true)
        result (fa/do-quit state)]
    (is (= :confirm-save (get-in result [:dialog :type])))
    (is (= :quit (get-in result [:dialog :next-action])))))

(deftest quit-when-clean-sets-quit-flag
  (let [state (assoc (st/initial-state) :dirty false)
        result (fa/do-quit state)]
    (is (true? (:quit-clicked result)))))
