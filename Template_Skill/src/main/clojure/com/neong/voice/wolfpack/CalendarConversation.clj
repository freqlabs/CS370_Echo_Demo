(ns com.neong.voice.wolfpack.CalendarConversation
  (:gen-class :extends com.neong.voice.model.base.Conversation
              :post-init register-intents)
  (:import [java.sql Timestamp]
           [java.time ZoneId]
           [java.time.format DateTimeFormatter]
           [com.amazon.speech.slu Intent]
           [com.amazon.speech.speechlet IntentRequest Session SpeechletResponse]
           [com.neong.voice.model.base Conversation]
           [com.neong.voice.wolfpack CalendarConversation]
           [com.wolfpack.database DbConnection]))


(defn -register-intents
  [^CalendarConversation this]
  (.add (.supportedIntentNames this) "NextEventIntent"))


;;
;; Date/time formatting helpers
;;

(def zone-id (ZoneId/of "America/Los_Angeles"))
(def day-formatter (DateTimeFormatter/ofPattern "EEEE"))
(def date-formatter (DateTimeFormatter/ofPattern "????MMdd"))
(def time-formatter (DateTimeFormatter/ofPattern "h:mm a"))

(defn at-zone [when]
  (.atZone (.toInstant when) zone-id))

(defn format-day [when]
  (.format when day-formatter))

(defn format-date [when]
  (.format when date-formatter))

(defn format-time [when]
  (.format when time-formatter))


;;
;; SSML helpers
;;

(defn speak [content]
  (str "<speak>" content "</speak>"))

(defn say-as [interpret-as content]
  (str "<say-as interpret-as=\"" interpret-as "\">" content "</say-as>"))

;;(defn section [content]
;;  (str "<s>" content "</s>"))

(defn format-calendar-response
  [what when where]
  (let [zoned (at-zone when)
        day (format-day zoned)
        date (format-date zoned)
        time (format-time zoned)]
    (speak
     (str "Okay, the next event is " what
          " on " day " " (say-as "date" date)
          " at " (say-as "time" time)
          ", at " where "."))))


;;
;; Database helpers
;;

(def next-event-query "SELECT * FROM event_info WHERE start > now() LIMIT 1;")

(defn select-next-event [db]
  (if-let [result (.runQuery db next-event-query)]
    (let [what (.get result "summary")
          when (or (.get result "start") "Sonoma State University")
          where (.get result "name")]
      {:what  (cast String    (.get what  0))
       :when  (cast Timestamp (.get when  0))
       :where (cast String    (.get where 0))})))


;;
;; Response helper
;;

(def abort-response (Conversation/newTellResponse "Sorry, I'm on break." false))

(defn lookup-response [db]
  (if-let [{what :what, when :when, where :where} (select-next-event db)]
    (Conversation/newTellResponse (format-calendar-response what when where) true)))

;;
;; Request handler
;;

(defn -respondToIntentRequest
  [^CalendarConversation this ^IntentRequest request ^Session session]
  (or
   (let [db (DbConnection. "DbConnection.xml")]
     (if (.getRemoteConnection db)
       (lookup-response db)))
   abort-response))
