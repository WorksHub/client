(ns wh.components.auth-popup.events
  (:require
    [re-frame.core :refer [path reg-event-db reg-event-fx]]
    [wh.components.auth-popup.db :as popup]
    [wh.db :as db]
    [wh.interop :as interop]))

(def auth-popup-interceptors (into db/default-interceptors
                                   [(path ::popup/sub-db)]))

(reg-event-fx
  :auth/show-popup
  db/default-interceptors
  (fn [{db :db} [context]]
    (let [redirect (condp contains? (:type context)
                     #{:homepage-jobcard-apply :jobcard-apply} [:job :params {:id (get-in context [:job :id])} :query-params {"apply" "true"}]
                     #{:jobpage-see-more :homepage-jobcard-more-info} [:job :params {:id (get-in context [:job :id])}]
                     #{:jobpage-apply} [:job :params {:id (get-in context [:job :id])} :query-params {:apply "true"}]
                     #{:homepage-contribute} [:contribute]
                     #{:issue} [:issue :params {:id (:id context)}]
                     #{:upvote} [:blog :params {:id (get-in context [:blog :id])}])
          reset-job? (contains? #{:jobpage-apply :jobpage-see-more} (:type context))]
      (js/setNoScroll true)
      {:db (cond-> (-> db
                       (assoc-in [::popup/sub-db ::popup/visible?] true)
                       (assoc-in [::popup/sub-db ::popup/context] context))
                   reset-job? (assoc-in [:wh.jobs.job.db/sub-db :wh.jobs.job.db/id] ""))
       :dispatch (into [:login/set-redirect] redirect)})))

(reg-event-db
  :auth/hide-popup
  auth-popup-interceptors
  (fn [db _]
    (js/setNoScroll false)
    (assoc db ::popup/visible? false)))
