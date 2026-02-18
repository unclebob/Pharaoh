(ns pharaoh.gherkin.parser-test
  (:require [clojure.test :refer :all]
            [pharaoh.gherkin.parser :as p]))

(def sample-feature
  "Feature: Sample
  Description line.

  Background:
    Given the game is running

  Scenario: Simple test
    Given a value of 5
    When the player does something
    Then the result should be 10

  Scenario Outline: Parameterized
    Given the input is <x>
    Then the output is <y>

    Examples:
      | x  | y  |
      | 1  | 2  |
      | 3  | 6  |")

(deftest parse-feature-name
  (let [result (p/parse-feature sample-feature)]
    (is (= "Sample" (:name result)))))

(deftest parse-background
  (let [result (p/parse-feature sample-feature)]
    (is (= 1 (count (:background result))))
    (is (= :given (:type (first (:background result)))))
    (is (= "the game is running" (:text (first (:background result)))))))

(deftest parse-scenarios
  (let [result (p/parse-feature sample-feature)]
    (is (= 3 (count (:scenarios result))))))

(deftest parse-simple-scenario
  (let [result (p/parse-feature sample-feature)
        s (first (:scenarios result))]
    (is (= "Simple test" (:name s)))
    (is (= 3 (count (:steps s))))
    (is (= :given (:type (first (:steps s)))))
    (is (= :when (:type (second (:steps s)))))
    (is (= :then (:type (nth (:steps s) 2))))))

(deftest parse-scenario-outline-expands
  (let [result (p/parse-feature sample-feature)
        outlines (filter #(= :outline (:kind %)) (:scenarios result))]
    (is (= 2 (count outlines)))
    (let [s1 (first outlines)]
      (is (= "Parameterized [x=1, y=2]" (:name s1)))
      (is (= "the input is 1" (:text (first (:steps s1))))))))

(deftest parse-comments-ignored
  (let [text "Feature: Test
  # This is a comment
  Scenario: Basic
    Given something
    # Another comment
    Then result"]
    (let [result (p/parse-feature text)
          s (first (:scenarios result))]
      (is (= 2 (count (:steps s)))))))

(deftest parse-and-but-inherit-type
  (let [text "Feature: Test
  Scenario: With And
    Given a setup
    And another setup
    When an action
    But with a twist
    Then a result
    And another result"]
    (let [result (p/parse-feature text)
          steps (:steps (first (:scenarios result)))]
      (is (= :given (:type (nth steps 0))))
      (is (= :given (:type (nth steps 1))))
      (is (= :when (:type (nth steps 2))))
      (is (= :when (:type (nth steps 3))))
      (is (= :then (:type (nth steps 4))))
      (is (= :then (:type (nth steps 5)))))))

(deftest parse-feature-from-file
  (let [result (p/parse-file "features/game_setup.feature")]
    (is (string? (:name result)))
    (is (vector? (:scenarios result)))
    (is (pos? (count (:scenarios result))))))
