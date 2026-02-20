(ns pharaoh.ui.menu-test
  (:require [clojure.test :refer :all]
            [pharaoh.ui.menu :as menu]))

(deftest toggle-menu-opens-closed-menu
  (let [state (menu/toggle-menu {:menu {:open? false}})]
    (is (true? (get-in state [:menu :open?])))))

(deftest toggle-menu-closes-open-menu
  (let [state (menu/toggle-menu {:menu {:open? true}})]
    (is (false? (get-in state [:menu :open?])))))

(deftest close-menu-closes
  (let [state (menu/close-menu {:menu {:open? true}})]
    (is (false? (get-in state [:menu :open?])))))

(deftest menu-bar-bounds-returns-rect
  (let [b (menu/menu-bar-bounds)]
    (is (number? (:x b)))
    (is (number? (:y b)))
    (is (pos? (:w b)))
    (is (pos? (:h b)))))

(deftest menu-items-returns-five-items
  (is (= 5 (count (menu/menu-items)))))

(deftest menu-item-hit-returns-nil-outside
  (is (nil? (menu/menu-item-hit -100 -100))))

(deftest menu-item-hit-returns-save-for-first-item
  (is (= :save (menu/menu-item-hit 30 (+ 22 5)))))

(deftest menu-item-hit-returns-quit-for-last-item
  (is (= :quit (menu/menu-item-hit 30 (+ 22 (* 4 22) 5)))))

(deftest menu-item-hit-returns-nil-below-items
  (is (nil? (menu/menu-item-hit 30 (+ 22 (* 5 22) 5)))))

(deftest menu-item-hit-returns-nil-right-of-dropdown
  (is (nil? (menu/menu-item-hit 250 30))))

(deftest menu-item-hit-returns-nil-at-menu-bar-y
  (is (nil? (menu/menu-item-hit 30 22))))

(deftest menu-item-hit-returns-save-as-for-second-item
  (is (= :save-as (menu/menu-item-hit 100 (+ 22 22 5)))))

(deftest menu-item-hit-returns-open-for-third-item
  (is (= :open (menu/menu-item-hit 50 (+ 22 (* 2 22) 5)))))

(deftest menu-item-hit-returns-new-game-for-fourth-item
  (is (= :new-game (menu/menu-item-hit 50 (+ 22 (* 3 22) 5)))))

(deftest menu-items-have-labels-and-actions
  (doseq [item (menu/menu-items)]
    (is (string? (:label item)))
    (is (keyword? (:action item)))))
