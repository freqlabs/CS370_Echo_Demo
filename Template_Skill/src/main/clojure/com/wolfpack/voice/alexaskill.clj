(ns com.wolfpack.voice.alexaskill
  (:use [com.wolfpack.voice.properties]
        [com.wolfpack.voice.ssml])
  (:import [com.neong.voice.model.base Conversation]))

;;
;; Skill helpers
;;

;; Slots
(defn get-slot-value [request slot-name]
  (if-let [slot (.getSlot request slot-name)]
    (.getValue slot)))

(defn get-event-slot [request]
  (get-slot-value request :event))

(defn get-date-slot [request]
  (get-slot-value request :date))


;; Intents
(defn get-intent-name [request]
  (.. request (getIntent) (getName)))


;; Attributes
(defn get-attribute [session attrib-name]
  (let [attr (get attrib attrib-name)]
    (.getAttribute session attr)))

(defn get-state-attrib [session]
  (get-attribute session :state))

(defn get-interval-attrib [session]
  (get-attribute session :interval))

(defn get-events-attrib [session]
  (get-attribute session :events))

(defn set-attribute [session attrib-name value]
  (let [attr (get attrib attrib-name)]
    (.setAttribute session attr value)))

(defn set-state-attrib [session state-name]
  (let [state-value (get state state-name)]
    (set-attribute session :state state-value)))

(defn set-interval-attrib [session interval]
  (set-attribute session :interval interval))

(defn set-events-attrib [session events]
  (let [f (fn [e] [(:event_id e) (:summary e)])
        event-map (hash-map (map f events))]
    (set-attribute session :events event-map)))


;;
;; Response helpers
;;

(defn ssml-tell-response [response]
  (Conversation/newTellResponse (speak response) true))

(defn ssml-ask-response [response reprompt]
  (Conversation/newAskResponse (speak response) true (speak reprompt) true))
