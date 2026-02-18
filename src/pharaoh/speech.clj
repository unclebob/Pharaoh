(ns pharaoh.speech
  (:require [clojure.string :as str]))

(def voice-table
  {0 {:rate 180 :pitch 280 :voice "Alex"}
   1 {:rate 200 :pitch 340 :voice "Daniel"}
   2 {:rate 160 :pitch 260 :voice "Fred"}
   3 {:rate 220 :pitch 380 :voice "Samantha"}})

(def default-voice {:rate 190 :pitch 310 :voice "Alex"})

(defn voice-settings [face]
  (get voice-table face default-voice))

(defn format-message [template args]
  (reduce-kv
    (fn [s i v] (str/replace s (str "{" i "}") (str v)))
    template
    (vec args)))

(defn speak-async
  "Speaks text asynchronously using macOS say command. Returns a future."
  [text {:keys [rate voice] :or {rate 190 voice "Alex"}}]
  (future
    (try
      (let [pb (ProcessBuilder.
                 ["say" "-r" (str rate) "-v" voice (str text)])]
        (.waitFor (.start pb)))
      (catch Exception _))))

(defn speak [text face]
  (speak-async text (voice-settings face)))
