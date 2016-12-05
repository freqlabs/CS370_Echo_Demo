(ns com.wolfpack.voice.parsers
  (:use [com.wolfpack.voice.alexaskill]
        [com.wolfpack.voice.util])
  (:require [java-time :as time]))

;;
;; AMAZON.DATE slot type parser
;;

(defn time-interval [date num units]
  (time/interval date (time/duration num units)))

(defn decade-interval [date]
  (let [year (.replace date "X" "0")]
    (time-interval year 10 :years)))

(defn year-interval [date]
  (time-interval date 1 :year))

(defn month-interval [date]
  (time-interval date 1 :month))

(defn season-interval [date]
  ;; I don't even want to touch this.
  nil)

(defn week-interval [date]
  (let [[month week] (.split date "-W")
        monthstart (str month "-01")
        monthdate (time/zoned-date-time monthstart)
        weekdate (+ monthdate (time/duration 1 :week))]
    (time-interval weekdate 1 :week)))

(defn weekend-interval [date]
  (letfn [(incr-day [date] (+ (time/duration 1 :day) date))
          (weekday? [date] (not (time/weekend? date)))
          (first-weekend [days] (first (drop-while weekday? days)))]
    (let [[month week-we] (.split date "-W")
          [week _] (.split week-we "-WE")
          monthstart (str month "-01")
          monthdate (time/zoned-date-time monthstart)
          weekdate (+ monthdate (time/duration 1 :week))
          dayseq (iterate incr-day weekdate)
          weekend-start-date (first-weekend dayseq)]
      (time-interval weekend-start-date 2 :days))))

(defn day-interval [date]
  (time-interval date 1 :day))


;; https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interaction-model-reference#slot-types
(defn get-requested-interval [request]
  (if-let* [date (get-date-slot request)
            parts (.split date "-")]
    (case (count parts)
      ;; Decades and years have one part.
      1 (if (.endsWith date "X")
          ;; Decades end with "X".
          (decade-interval date)
          ;; Years don't.
          (year-interval date))
      ;; Weeks, months and seasons have two parts.
      2 (let [second-part (drop 1 parts)]
          (case (count second-part)
            ;; Dates and seasons have two characters in the second part.
            2 (let [lower-second-part (.toLowerCase (.clone second-part))]
                (if (.equals second-part lower-second-part)
                  ;; Months are all numbers.
                  (month-interval date)
                  ;; Seasons contain letters.
                  (season-interval date)))
            ;; The second part of the week format has three characters.
            3 (time-interval date 1 :week)))
      ;; Days and weekends have three parts.
      3 (if (.startsWith (drop 1 parts) "W")
          ;; The second part of weekends start with "W".
          (weekend-interval date)
          ;; Dates are all numbers.
          (day-interval date)))))
