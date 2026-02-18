(ns pharaoh.gherkin.parser
  (:require [clojure.string :as str]))

(defn- comment-or-blank? [line]
  (let [t (str/trim line)]
    (or (str/blank? t) (str/starts-with? t "#"))))

(defn- keyword-line [line]
  (let [t (str/trim line)]
    (cond
      (str/starts-with? t "Feature:") [:feature (str/trim (subs t 8))]
      (str/starts-with? t "Background:") [:background nil]
      (str/starts-with? t "Scenario Outline:") [:outline (str/trim (subs t 17))]
      (str/starts-with? t "Scenario:") [:scenario (str/trim (subs t 9))]
      (str/starts-with? t "Examples:") [:examples nil]
      (str/starts-with? t "Given ") [:given (str/trim (subs t 6))]
      (str/starts-with? t "When ") [:when (str/trim (subs t 5))]
      (str/starts-with? t "Then ") [:then (str/trim (subs t 5))]
      (str/starts-with? t "And ") [:and (str/trim (subs t 4))]
      (str/starts-with? t "But ") [:but (str/trim (subs t 4))]
      (str/starts-with? t "|") [:table-row t]
      :else nil)))

(defn- parse-table-row [line]
  (let [cells (str/split (subs (str/trim line) 1) #"\|" -1)]
    (mapv str/trim (butlast cells))))

(defn- resolve-and-but [steps]
  (loop [result [] last-type :given remaining steps]
    (if (empty? remaining)
      result
      (let [{:keys [type text]} (first remaining)
            resolved-type (if (#{:and :but} type) last-type type)]
        (recur (conj result {:type resolved-type :text text})
               resolved-type (rest remaining))))))

(defn- expand-outline [name steps example-rows]
  (when (>= (count example-rows) 2)
    (let [headers (first example-rows)
          rows (rest example-rows)]
      (mapv (fn [row]
              (let [bindings (map vector headers row)
                    label (str/join ", " (map (fn [[k v]] (str k "=" v)) bindings))
                    expanded (mapv (fn [step]
                                     (update step :text
                                             (fn [t]
                                               (reduce (fn [s [k v]]
                                                         (str/replace s (str "<" k ">") v))
                                                       t bindings))))
                                   steps)]
                {:name (str name " [" label "]")
                 :kind :outline :steps expanded}))
            rows))))

(defn- finalize-current [scenarios current current-steps outline-steps example-rows]
  (cond
    (nil? current) scenarios
    (= :background (:kind current)) scenarios
    (= :outline (:kind current))
    (into scenarios (expand-outline (:name current)
                                    (resolve-and-but outline-steps)
                                    example-rows))
    :else
    (conj scenarios (assoc current :steps (resolve-and-but current-steps)))))

(defn parse-feature [text]
  (let [lines (remove comment-or-blank? (str/split-lines text))]
    (loop [lines (seq lines)
           feature-name nil
           background []
           scenarios []
           current nil
           current-steps []
           in-bg false
           in-examples false
           example-rows []
           outline-steps []]
      (if (nil? lines)
        {:name feature-name
         :background (resolve-and-but background)
         :scenarios (vec (finalize-current scenarios current current-steps
                                           outline-steps example-rows))}
        (let [[k v] (or (keyword-line (first lines)) [nil nil])]
          (case k
            nil
            (recur (next lines) feature-name background scenarios
                   current current-steps in-bg in-examples example-rows outline-steps)

            :feature
            (recur (next lines) v background scenarios
                   current current-steps false false example-rows outline-steps)

            :background
            (recur (next lines) feature-name background scenarios
                   {:kind :background} [] true false example-rows outline-steps)

            (:scenario :outline)
            (let [new-scenarios (finalize-current scenarios current current-steps
                                                  outline-steps example-rows)]
              (recur (next lines) feature-name background new-scenarios
                     {:name v :kind k} [] false false [] []))

            :examples
            (recur (next lines) feature-name background scenarios
                   current current-steps false true [] outline-steps)

            :table-row
            (if in-examples
              (recur (next lines) feature-name background scenarios
                     current current-steps false true
                     (conj example-rows (parse-table-row v)) outline-steps)
              (recur (next lines) feature-name background scenarios
                     current current-steps in-bg in-examples example-rows outline-steps))

            (:given :when :then :and :but)
            (let [step {:type k :text v}]
              (cond
                in-bg
                (recur (next lines) feature-name (conj background step) scenarios
                       current current-steps in-bg false example-rows outline-steps)
                (= :outline (:kind current))
                (recur (next lines) feature-name background scenarios
                       current current-steps false false example-rows
                       (conj outline-steps step))
                :else
                (recur (next lines) feature-name background scenarios
                       current (conj current-steps step) false false
                       example-rows outline-steps)))))))))

(defn parse-file [path]
  (parse-feature (slurp path)))
