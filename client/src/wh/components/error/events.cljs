(ns wh.components.error.events
  (:require [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.components.error.db :as error]
            [wh.db :as db]))

(def error-interceptors (into db/default-interceptors
                              [(path ::error/sub-db)]))
(reg-event-fx
  :error/set-global
  error-interceptors
  (fn [{db :db} [message retry-event]]
    (let [new-fields (merge {::error/message message
                             ::error/type :error}
                            (when retry-event
                              {::error/retry-event retry-event}))]
      {:db (merge db new-fields)})))

(reg-event-fx
  :success/set-global
  error-interceptors
  (fn [{db :db} [message]]
    (let [new-fields (merge {::error/message     message
                             ::error/type        :success
                             ::error/retry-event nil})]
      {:db (merge db new-fields)})))

(reg-event-db
  :error/close-global
  error-interceptors
  (fn [_db []]
    error/default-db))

(reg-event-fx
  :error/retry-failed-action
  error-interceptors
  (fn [{_db :db} [event]]
    {:dispatch-n [event
                  [:error/close-global]]}))
