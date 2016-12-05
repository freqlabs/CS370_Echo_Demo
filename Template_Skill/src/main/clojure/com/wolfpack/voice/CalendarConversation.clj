(ns com.wolfpack.voice.CalendarConversation
  (:gen-class :extends com.neong.voice.model.base.Conversation
              :post-init register-intents)
  (:use [com.wolfpack.voice.alexaskill]
        [com.wolfpack.voice.database]
        [com.wolfpack.voice.formatters]
        [com.wolfpack.voice.parsers]
        [com.wolfpack.voice.properties]
        [com.wolfpack.voice.ssml]
        [com.wolfpack.voice.util])
  (:import [com.amazon.speech.speechlet IntentRequest Session SpeechletResponse]
           [com.wolfpack.voice CalendarConversation]))


;; Class initialization
(defn -register-intents [this]
  (let [supported (.supportedNames this)]
    (doseq [ntnt (vals intent)]
      (.add supported ntnt))))


;;
;; Response builders
;;

;; Note to self: hire a writer
(def more-info-prompt-ssml " Would you like to know more about one of these events?")
(def more-info-reprompt-ssml (str " I can tell you about details such as admission fee, "
                                  "location, or end time."))


(def max-list-count 5)


(defn update-session [sesson events]
  (do
    (set-events-attrib session events)
    (if (> max-list-count (count events))
      (set-state-attrib session :long-list)
      (set-events-attrib session :heard-list))))

(defn too-many-events-response [interval events]
  (let [numevents (count events)
        interval-ssml (format-interval-ssml interval)
        response-ssml (str "I was able to find " numevents
                           " events for " interval-ssml
                           "What category of events are you interested in?")
        reprompt-ssml (reprompt-categories-ssml events)]
    (ssml-ask-response response-ssml reprompt-ssml)))

(defn events-list-response [interval events]
  (let [fmt (str "The events on " day-date-ssml " are: ")
        prefix (format-event fmt event)
        events-ssml (format-events-list-ssml prefix events)
        response-ssml (str events-ssml more-info-prompt-ssml)
        reprompt-ssml more-info-reprompt-ssml]
    (ssml-ask-response response-ssml reprompt-ssml)))

(defn build-interval-events-response [interval events]
  (if (> max-list-count (count events))
    (too-many-events-response interval events)
    (events-list-response interval events)))

interval-ssml (format-interval-ssml interval)]


;;
;; Intent handlers
;;

(defn handle-category-intent [request session]
  (if-let* [intent (get-intent-name request)
            event-category (get category intent)
            [begin, end] (get-interval-attrib session)
            events (query-interval-given-category begin end event-category)
            prefix (str "The events in " category " are: ")
            events-list (format-events-list prefix events)
            response-ssml (str events-list more-info-prompt-ssml)]
    (ssml-ask-response response-ssml more-info-reprompt-ssml)))


(defn detail-intent-handler [handler]
  (fn [request session]
    (if-let* [spoken-event (get-event-slot request)
              saved-events (get-events-attrib session)
              event-id (get saved-events spoken-event)
              response-ssml (handler request session event-id)]
      (ssml-tell-response response-ssml))))

(def handle-fee-detail-intent
  (detail-intent-handler
   (fn [request session event-id]
     (if-let* [event (query-event-fee event-id)
               fmt "The admission fee for {summary} is {general_admission_fee}."]
       (format-event fmt event)))))

(def handle-location-detail-intent
  (detail-intent-handler
   (fn [request session event-id]
     (if-let* [event (query-event-location event-id)
              fmt "{summary} is at {location}."]
       (format-event fmt event)))))

(def handle-end-detail-intent
  (detail-intent-handler
   (fn [request session event-id]
     (if-let* [event (query-event-end event-id)
               fmt "{summary} ends at {end:time}"]
       (format-event fmt event)))))


(defn handle-find-events-intent [request session]
  (if-let* [interval (get-requested-interval request)
            events (query-interval-events interval)
            event-count (count events)]
    (if (> 0 event-count)
      (do
        (update-session session events)
        (format-interval-events-response interval events))
      (let [response-ssml (str "Sorry, I couldn't find any events for " interval-ssml)]
        (ssml-tell-response response-ssml)))))


(defn handle-find-events-intent-next [request session]
  (if-let* [fmt "The next event is {summary}, on {start:day}, {start:date}, {start:time}."
        event (get-next-event)
        ssml (format-event fmt event)]
    (ssml-tell-response ssml)))


;;
;; Request routing
;;

;; Detail handler lookup table
(let [detail-handler {(:fee-detail intent) handle-fee-detail-intent
                      (:location-detail intent) handle-location-detail-intent
                      (:end-detail intent) handle-end-detail-intent}]

  ;; Route intents for event details
  (defn route-detail-request [request session]
    (let [intent (get-intent-name request)
          handler (get detail-handler intent)]
      (handler request session))))


(defn route-find-events-intent [request session]
  (if-let [date (get-date-slot request)]
    (handle-find-events-intent request session)
    (handle-find-events-intent-next request session)))


;; Route intents that depend on previously prepared state
(defn route-state-sensitive-request [request session]
  (let [long-list (:long-list state)
        heard-list (:heard-list state)]
    (case (get-state-attrib session)
      long-list (handle-category-intent request session)
      heard-list (route-detail-request request session))))


;; Route entry intents (state insensitive) vs re-entry intents (state sensitive)
(defn route-intent-request [request session]
  (case (get-intent-name request)
    ;; State insensitive requests
    (:find-events intent) (route-find-events-request request session)

    ;; State sensitive intents
    (route-state-sensitive-request request session)))


;;
;; Entry point
;;

(defn -respondToIntentRequest
  [^CalendarConversation this ^IntentRequest request ^Session session]
  (route-intent-request request session))
