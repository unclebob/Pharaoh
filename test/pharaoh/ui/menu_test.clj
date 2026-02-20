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

(deftest handle-menu-key-esc-closes-menu
  (let [app {:menu {:open? true}}
        result (menu/handle-menu-key app (char 27))]
    (is (false? (get-in result [:menu :open?])))))

(deftest handle-menu-key-esc-returns-nil-when-closed
  (let [app {:menu {:open? false}}]
    (is (nil? (menu/handle-menu-key app (char 27))))))

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

(deftest menu-hover-index-first-item
  (is (= 0 (menu/menu-hover-index 30 (+ 22 5)))))

(deftest menu-hover-index-last-item
  (is (= 4 (menu/menu-hover-index 30 (+ 22 (* 4 22) 5)))))

(deftest menu-hover-index-nil-outside
  (is (nil? (menu/menu-hover-index -100 -100)))
  (is (nil? (menu/menu-hover-index 30 10)))
  (is (nil? (menu/menu-hover-index 250 30))))

(deftest update-hover-sets-index
  (let [app {:menu {:open? true :hover nil}}
        result (menu/update-hover app 30 (+ 22 5))]
    (is (= 0 (get-in result [:menu :hover])))))

(deftest update-hover-clears-when-outside
  (let [app {:menu {:open? true :hover 2}}
        result (menu/update-hover app 30 10)]
    (is (nil? (get-in result [:menu :hover])))))

;; ---- submenu ----

(deftest update-hover-opens-submenu-on-open-item
  (let [app {:menu {:open? true :hover nil}}
        ;; Open... is item index 2, y = 22 + 2*22 + 5 = 71
        result (menu/update-hover app 30 71)]
    (is (= 2 (get-in result [:menu :hover])))
    (is (true? (get-in result [:menu :submenu :open?])))))

(deftest update-hover-populates-submenu-items
  (with-redefs [menu/get-save-files (fn [] ["a.edn" "b.edn"])]
    (let [app {:menu {:open? true :hover nil}}
          result (menu/update-hover app 30 71)]
      (is (= ["a.edn" "b.edn"] (get-in result [:menu :submenu :items]))))))

(deftest update-hover-closes-submenu-when-leaving-open-item
  (let [app {:menu {:open? true :hover 2
                    :submenu {:open? true :items ["a.edn"] :hover nil}}}
        ;; hover on item 0 (Save) y = 22 + 5 = 27
        result (menu/update-hover app 30 27)]
    (is (= 0 (get-in result [:menu :hover])))
    (is (not (get-in result [:menu :submenu :open?])))))

(deftest update-hover-keeps-submenu-open-when-in-submenu-area
  (with-redefs [menu/get-save-files (fn [] ["a.edn"])]
    (let [app {:menu {:open? true :hover 2
                      :submenu {:open? true :items ["a.edn"] :hover nil}}}
          ;; submenu area: x=200..400, y = 22 + 2*22 = 66, first item at y=66..88
          result (menu/update-hover app 250 75)]
      (is (true? (get-in result [:menu :submenu :open?]))))))

(deftest submenu-hover-index-returns-file-index
  (is (= 0 (menu/submenu-hover-index ["a.edn" "b.edn"] 250 (+ 66 5))))
  (is (= 1 (menu/submenu-hover-index ["a.edn" "b.edn"] 250 (+ 66 22 5)))))

(deftest submenu-hover-index-returns-browse-index
  ;; browse is at the end, after 2 files
  (is (= 2 (menu/submenu-hover-index ["a.edn" "b.edn"] 250 (+ 66 44 5)))))

(deftest submenu-hover-index-nil-outside
  (is (nil? (menu/submenu-hover-index ["a.edn"] 150 75)))
  (is (nil? (menu/submenu-hover-index ["a.edn"] 450 75))))

(deftest submenu-item-hit-returns-filename
  (is (= "a.edn" (menu/submenu-item-hit ["a.edn" "b.edn"] 250 (+ 66 5)))))

(deftest submenu-item-hit-returns-browse-keyword
  (is (= :browse (menu/submenu-item-hit ["a.edn" "b.edn"] 250 (+ 66 44 5)))))

(deftest submenu-item-hit-nil-outside
  (is (nil? (menu/submenu-item-hit ["a.edn"] 150 75))))
