;; Shamelessly stolen from https://github.com/Day8/re-frame/pull/249
;; That PR was closed for re-frame because they decided they don't want this in core.
;; It hasn't been factored out to a library yet, so I'm including it here.

(ns wh.common.fx.debounce
  (:require [re-frame.core :as rf]))

(def debounced-events (atom {}))

(defn cancel-timeout [id]
  (js/clearTimeout (:timeout (@debounced-events id)))
  (swap! debounced-events dissoc id))

(rf/reg-fx
  :dispatch-debounce
  (fn [dispatches]
    (let [dispatches (if (sequential? dispatches) dispatches [dispatches])]
      (doseq [{:keys [id action dispatch timeout]
               :or   {action :dispatch}}
              dispatches]
        (case action
          :dispatch (do
                      (cancel-timeout id)
                      (swap! debounced-events assoc id
                             {:timeout  (js/setTimeout (fn []
                                                         (swap! debounced-events dissoc id)
                                                         (rf/dispatch dispatch))
                                                       timeout)
                              :dispatch dispatch}))
          :cancel (cancel-timeout id)
          :flush (let [ev (get-in @debounced-events [id :dispatch])]
                   (cancel-timeout id)
                   (rf/dispatch ev))
          (js/console.warn "re-frame: ignoring bad :dispatch-debounce action:" action "id:" id))))))
