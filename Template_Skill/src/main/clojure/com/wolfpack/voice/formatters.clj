(ns com.wolfpack.voice.formatters
  (:use [com.wolfpack.voice.ssml])
  (:require [java-time :as time]))

;;
;; Date/time formatting helpers
;;

(def timezone "America/Los_Angeles")
(def zone-id (time/zone-id timezone))
(def day-formatter (time/formatter "EEEE"))
(def date-formatter (time/formatter "????MMdd"))
(def time-formatter (time/formatter "h:mm a"))

(defn zoned-format [when formatter]
  (time/format formatter (time/with-zone when zone-id)))

(defn format-day [when]
  (zoned-format when day-formatter))

(defn format-date [when]
  (zoned-format when date-formatter))

(defn format-time [when]
  (zoned-format when time-formatter))


;;
;; Field formatters
;;

(defn day-ssml [dtvalue]
  (format-day dtvalue))

(defn date-ssml [dtvalue]
  (say-as "date" (format-date dtvalue)))

(defn time-ssml [dtvalue]
  (say-as "time" (format-time dtvalue)))

(defn fee-ssml [fee]
  (case fee
    nil "unspecified"
    (replace fee "-" " to ")))

(defn location-ssml [location]
  (case location
    nil "Sonoma State University"
    location))


;;
;; Format a string using fields from an event
;;

(defn split-dtkey [field]
  (let [[key _] (.split field ":" 2)]
    (keyword key)))

(defn format-event-field [event field]
  (let [key (keyword field)]
    (case key
      ;; Date/time fields
      (:start:day :end:day :start:date :end:date :start:time :end:time)
      (let [dtkey (split-dtkey field)
            dtvalue (get event dtkey)]
        (case key
          (:start:day :end:day) (day-ssml dtvalue)
          (:start:date :end:date) (date-ssml dtvalue)
          (:start:time :end:time) (time-ssml dtvalue)))

      ;; Others
      (let [value (get event key)]
        (case key
          (:student_admission_fee :general_admission_fee) (fee-ssml value)
          :location (location-ssml value)
          value)))))


(defn format-event [fmt event]
  (letfn [(search [acc s]
            (if (= "" s) acc
                (let [[head tail] (.split s "{" 2)]
                  #(replace (str acc head) tail))))
          (replace [acc s]
            (if (= "" s) acc
                (let [[field tail] (.split s "}" 2)
                      value (format-event-field event field)]
                  #(search (str acc value) tail))))]

    (trampoline search "" fmt)))


(defn format-events-list-ssml [prefix events]
  (let [fmt (section "{summary} at {start:time}")]
    (apply str (map format-event fmt events))))


(defn format-day-date-ssml [event]
  (format-event "{start:day}, {start:date}" event))


(defn format-interval-ssml [interval]
  (
