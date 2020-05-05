(ns wh.admin.tags.events
  (:require
    [re-frame.core :refer [path]]
    [wh.admin.tags.db :as tags]
    [wh.db :as db]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.tag :refer [update-tag-mutation]]
    [wh.pages.core :refer [on-page-load]]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]])
  (:require-macros
    [wh.graphql-macros :refer [defquery]]))

(def tags-interceptors (into db/default-interceptors
                             [(path ::tags/sub-db)]))

(defmethod on-page-load :tags-edit [db]
  [[::fetch-tags]])

(reg-event-fx
  ::fetch-tags
  (fn [_ _]
    {:dispatch (into [:graphql/query] [:tags {}])}))

(reg-event-db
  ::set-type-filter
  tags-interceptors
  (fn [db [t]]
    (assoc db ::tags/type-filter t)))

(reg-event-db
  ::set-search-term
  tags-interceptors
  (fn [db [t]]
    (assoc db ::tags/search-term t)))

(reg-event-fx
  ::delete-tag
  tags-interceptors
  (fn [_ _]
    (js/alert "Not implemented")))

(defn submit-updated-tag!
  [db k old-tag updated-tag]
  (let [all-tags (get-in (cache/result db :tags {}) [:list-tags :tags])
        all-updated-tags (map (fn [t] (if (= (:id old-tag) (:id t))
                                        updated-tag
                                        t)) all-tags)]
    {:dispatch [:graphql/update-entry :tags {}
                :overwrite {:list-tags {:tags all-updated-tags}}]
     :graphql {:query update-tag-mutation
               :variables (select-keys updated-tag [:id k])}}))

(reg-event-fx
  ::set-tag-type
  (fn [{db :db} [_ old-tag tag-type]]
    (let [updated-tag (assoc old-tag :type tag-type)]
      (submit-updated-tag! db :type old-tag updated-tag))))

(reg-event-fx
  ::set-tag-subtype
  (fn [{db :db} [_ old-tag tag-subtype]]
    (let [updated-tag (assoc old-tag :subtype tag-subtype)]
      (submit-updated-tag! db :subtype old-tag updated-tag))))

(reg-event-fx
  ::set-tag-weight
  (fn [{db :db} [_ old-tag tag-weight]]
    (let [updated-tag (assoc old-tag :weight tag-weight)]
      (submit-updated-tag! db :weight old-tag updated-tag))))

(reg-event-fx
  ::set-tag-label
  (fn [{db :db} [_ old-tag label]]
    (let [updated-tag (assoc old-tag :label label)]
      (submit-updated-tag! db :label old-tag updated-tag))))
