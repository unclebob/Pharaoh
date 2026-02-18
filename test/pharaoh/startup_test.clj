(ns pharaoh.startup-test
  (:require [clojure.test :refer :all]
            [pharaoh.startup :as su]
            [pharaoh.contracts :as ct]
            [pharaoh.random :as r]
            [pharaoh.state :as st]))

;; --- difficulty-for-key ---

(deftest key-1-returns-easy
  (is (= "Easy" (su/difficulty-for-key \1))))

(deftest key-e-returns-easy
  (is (= "Easy" (su/difficulty-for-key \e))))

(deftest key-E-returns-easy
  (is (= "Easy" (su/difficulty-for-key \E))))

(deftest key-2-returns-normal
  (is (= "Normal" (su/difficulty-for-key \2))))

(deftest key-n-returns-normal
  (is (= "Normal" (su/difficulty-for-key \n))))

(deftest key-N-returns-normal
  (is (= "Normal" (su/difficulty-for-key \N))))

(deftest key-3-returns-hard
  (is (= "Hard" (su/difficulty-for-key \3))))

(deftest key-h-returns-hard
  (is (= "Hard" (su/difficulty-for-key \h))))

(deftest key-H-returns-hard
  (is (= "Hard" (su/difficulty-for-key \H))))

(deftest unrecognized-key-returns-nil
  (is (nil? (su/difficulty-for-key \x)))
  (is (nil? (su/difficulty-for-key \4)))
  (is (nil? (su/difficulty-for-key \space))))

;; --- difficulty-for-click ---

(deftest click-easy-button
  (let [{:keys [x y w h]} (su/button-rect 0)]
    (is (= "Easy" (su/difficulty-for-click (+ x 5) (+ y 5))))
    (is (= "Easy" (su/difficulty-for-click (+ x (/ w 2)) (+ y (/ h 2)))))))

(deftest click-normal-button
  (let [{:keys [x y w h]} (su/button-rect 1)]
    (is (= "Normal" (su/difficulty-for-click (+ x 5) (+ y 5))))
    (is (= "Normal" (su/difficulty-for-click (+ x (/ w 2)) (+ y (/ h 2)))))))

(deftest click-hard-button
  (let [{:keys [x y w h]} (su/button-rect 2)]
    (is (= "Hard" (su/difficulty-for-click (+ x 5) (+ y 5))))
    (is (= "Hard" (su/difficulty-for-click (+ x (/ w 2)) (+ y (/ h 2)))))))

(deftest click-outside-buttons-returns-nil
  (is (nil? (su/difficulty-for-click 0 0)))
  (is (nil? (su/difficulty-for-click 999 999))))

;; --- select-difficulty ---

(deftest select-difficulty-sets-screen-to-game
  (let [app {:state (st/initial-state) :screen :difficulty :rng (r/make-rng 1)}
        result (su/select-difficulty app "Normal")]
    (is (= :game (:screen result)))))

(deftest select-easy-applies-settings
  (let [app {:state (st/initial-state) :screen :difficulty :rng (r/make-rng 1)}
        result (su/select-difficulty app "Easy")
        s (:state result)]
    (is (== 115.47 (:py-base s)))
    (is (== 5e6 (:credit-limit s)))
    (is (== 0.15 (:world-growth s)))))

(deftest select-normal-applies-settings
  (let [app {:state (st/initial-state) :screen :difficulty :rng (r/make-rng 1)}
        result (su/select-difficulty app "Normal")
        s (:state result)]
    (is (== 346.41 (:py-base s)))
    (is (== 5e5 (:credit-limit s)))
    (is (== 0.10 (:world-growth s)))))

(deftest select-hard-applies-settings
  (let [app {:state (st/initial-state) :screen :difficulty :rng (r/make-rng 1)}
        result (su/select-difficulty app "Hard")
        s (:state result)]
    (is (== 1154.7 (:py-base s)))))

(deftest select-difficulty-nil-returns-app-unchanged
  (let [app {:state (st/initial-state) :screen :difficulty}
        result (su/select-difficulty app nil)]
    (is (= :difficulty (:screen result)))
    (is (= (:state app) (:state result)))))

;; --- player and offer initialization ---

(deftest select-difficulty-initializes-players
  (let [rng (r/make-rng 42)
        app {:state (st/initial-state) :screen :difficulty :rng rng}
        result (su/select-difficulty app "Normal")]
    (is (= 10 (count (get-in result [:state :players]))))))

(deftest select-difficulty-generates-initial-offers
  (let [rng (r/make-rng 42)
        app {:state (st/initial-state) :screen :difficulty :rng rng}
        result (su/select-difficulty app "Normal")]
    (is (= 15 (count (get-in result [:state :cont-offers]))))))
