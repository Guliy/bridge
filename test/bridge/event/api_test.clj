(ns bridge.event.api-test
  (:require [bridge.data.slug :as slug]
            [bridge.event.api :as event.api]
            [bridge.event.data :as event.data]
            [bridge.event.schema :as event.schema]
            [bridge.test.fixtures :as fixtures :refer [TEST-CHAPTER-ID TEST-PERSON-ID]]
            [bridge.test.util :refer [conn db test-setup with-database]]
            [bridge.web.api.base :as api.base]
            [clojure.test :refer [deftest is join-fixtures use-fixtures]]))

(def db-name (str *ns*))

(use-fixtures :once test-setup)
(use-fixtures :each (join-fixtures [(with-database db-name event.schema/schema)
                                    (fixtures/person-fixtures db-name)
                                    (fixtures/chapter-fixtures db-name)]))

(def TEST-EVENT-TITLE "April Event")
(def TEST-EVENT-SLUG (slug/->slug TEST-EVENT-TITLE))

(def TEST-NEW-EVENT
  #:event{:title      TEST-EVENT-TITLE
          :start-date #inst "2018-04-06"
          :end-date   #inst "2018-04-07"})

(defn TEST-NEW-EVENT-TX
  "A function, so that spec instrumentation has a chance to work"
  []
  (event.data/new-event-tx TEST-CHAPTER-ID TEST-PERSON-ID TEST-NEW-EVENT))

(defn TEST-PAYLOAD []
  {:datomic/db       (db db-name)
   :datomic/conn     (conn db-name)
   :active-person-id TEST-PERSON-ID})

(deftest event-details

  (event.data/save-new-event! (conn db-name) (TEST-NEW-EVENT-TX))

  (let [result (api.base/api (merge (TEST-PAYLOAD)
                                    {:action   ::event.api/event-details
                                     :event-id [:event/slug "april-event"]}))]

    (is (= (get-in result [:event/chapter :chapter/slug])
           "clojurebridge-hermanus"))
    (is (= (->  result :event/organisers first :person/name)
           "Test Name"))))

(deftest save-new-event!

  (let [result (api.base/api (merge (TEST-PAYLOAD)
                                    {:action     ::event.api/save-new-event!
                                     :chapter-id TEST-CHAPTER-ID
                                     :new-event  TEST-NEW-EVENT}))]

    (is (= (get-in result [:event/chapter :chapter/slug])
           "clojurebridge-hermanus"))
    (is (= (->  result :event/organisers first :person/name)
           "Test Name"))))

(deftest update-field-value!

  (event.data/save-new-event! (conn db-name) (TEST-NEW-EVENT-TX))

  (let [result (api.base/api (merge (TEST-PAYLOAD)
                                    {:action   ::event.api/update-field-value!
                                     :event-id [:event/slug "april-event"]
                                     :field-update
                                     #:field{:attr  :event/notes-markdown
                                             :value "# notes"}}))]

    (is (= (:event/notes-markdown result)
           "# notes"))))
