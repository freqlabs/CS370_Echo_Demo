(ns com.wolfpack.voice.database
  (:use [com.wolfpack.voice.util])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]))

;;
;; Database helpers
;;

(let [config-filename "DbConfig.clj"
      config-url (io/resource config-filename)
      config (edn/read-string (slurp config-url))

      hostname (:hostname config)
      port (:port config)
      database (:database config)
      username (:username config)
      password (:password config)
      schema (:schema config)
      rootcert-filename (:rootcert config)
      rootcert-url (io/resource rootcert-filename)
      rootcert (io/as-file rootcert-url)]

  (def db {:classname "org.postgresql.Driver"
           :subprotocol "postgresql"
           :subname (str "//" hostname ":" port "/" database)
           :user username
           :password password
           :currentSchema schema
           :sslmode "verify-full"
           :sslrootcert rootcert}))


;;
;; Queries
;;

(defn query-interval-given-category [begin end event-category]
  (if-let [query ["SELECT * FROM given_category(?, ?, ?)" event-category begin end]]
    (jdbc/query query)))

(defn query-interval-events [begin end]
  (if-let [query [(str "SELECT event_id, summary, start, 'end', l.name AS location "
                       "    FROM events, locations AS l "
                       "    WHERE start >= ? AND start < ? "
                       "    ORDER BY start ASC") begin end]]
    (jdbc/query query)))


(defn query-event-location [event-id]
  (if-let* [query ["SELECT summary, location FROM event_info WHERE event_id = ?" event-id]
            result (jdbc/query query)]
    (first result)))

(defn query-event-fee [event-id]
  (if-let* [query ["SELECT summary, general_admission_fee FROM events WHERE event_id = ?" event-id]
            result (jdbc/query query)]
    (first result)))

(defn query-event-end [event-id]
  (if-let* [query ["SELECT summary, 'end' FROM events WHERE event_id = ?" event-id]
            result (jdbc/query query)]
    (first result)))
