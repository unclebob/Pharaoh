(ns pharaoh.persistence
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def saves-dir "saves/")

(defn save-path [name]
  (str saves-dir name))

(defn ensure-saves-dir []
  (.mkdirs (io/file saves-dir)))

(defn list-saves []
  (let [d (io/file saves-dir)]
    (if (.isDirectory d)
      (->> (.listFiles d)
           (filter #(.isFile %))
           (map #(.getName %))
           sort
           vec)
      [])))

(defn save-game [state path]
  (spit path (pr-str state)))

(defn load-game [path]
  (when (.exists (io/file path))
    (edn/read-string (slurp path))))
