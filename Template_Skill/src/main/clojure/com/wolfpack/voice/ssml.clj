(ns com.wolfpack.voice.ssml)

;;
;; SSML helpers
;;

(defn speak [& content]
  (str "<speak>" (apply str content) "</speak>"))

(defn say-as [interpret-as & content]
  (str "<say-as interpret-as=\"" interpret-as "\">" (apply str content) "</say-as>"))

(defn section [& content]
  (str "<s>" (apply str content) "</s>"))
