(ns com.wolfpack.voice.properties)

;; Intent names
(def ^:const intent {:find-events "FindEventsIntent"
                     :fee-detail "FeeDetailIntent"
                     :location-detail "LocationDetailIntent"
                     :end-detail "EndDetailIntent"
                     :all-category "AllCategoryIntent"
                     :sports-category "SportsCategoryIntent"
                     :arts-category "ArtsCategoryIntent"
                     :lectures-category "LecturesCategoryIntent"})


;; Category mappings
(def ^:const category {:all "all"
                       :sports "Athletics"
                       :arts "Arts and Entertainment"
                       :lectures "Lectures"})


;; Slot names
(def ^:const slot {:event-name "eventName"
                   :date-range "dateRange"})


;; Session states
(def ^:const state {:long-list "LONG_LIST"
                    :heard-list "HEARD_LIST"})


;; Session attributes
(def ^:const attrib {:state "savedState"
                     :interval "savedInterval"
                     :events "savedEvents"})
