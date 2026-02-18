(ns pharaoh.persistence
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn save-game [state path]
  (spit path (pr-str state)))

(defn load-game [path]
  (when (.exists (io/file path))
    (edn/read-string (slurp path))))
