(ns wh.company.profile.events
  (:require
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    [re-frame.core :refer [path]]
    [wh.company.profile.db :as profile]
    [wh.db :as db]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.company :refer [update-company-mutation-with-fields]]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [clojure.set :as set])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(def profile-interceptors (into db/default-interceptors
                                [(path ::profile/sub-db)]))

(defquery fetch-company-query
  {:venia/operation {:operation/type :query
                     :operation/name "company"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:company {:id :$id}
                    [:id :name :logo :profileEnabled
                     [:videos [:youtubeId :thumbnail :description]]
                     [:images [:url :width :height]]]]]})

(reg-query :company fetch-company-query)

(defn initial-query [db]
  [:company {:id (get-in db [:wh.db/page-params :id])}])

(defn cached-company
  [db]
  (:company
   (cache/result db :company {:id (get-in db [:wh.db/page-params :id])})))

#?(:cljs
   (defmethod on-page-load :company [db]
     [(into [:graphql/query] (initial-query db))
      [::load-photoswipe]]))

#?(:cljs
   (reg-event-fx
     ::load-photoswipe
     db/default-interceptors
     (fn [_ _]
       (when-not (exists? js/PhotoSwipe)
         (js/loadJavaScript "pswp/photoswipe.min.js")
         (js/loadJavaScript "pswp/photoswipe-ui-default.min.js")))))

#?(:cljs
   (reg-event-fx
     ::photo-upload
     db/default-interceptors
     upload/image-upload-fn))

(reg-event-db
  ::photo-upload-start
  profile-interceptors
  (fn [db _]
    (assoc db ::profile/photo-uploading? true)))

(reg-event-fx
  ::photo-upload-success
  profile-interceptors
  (fn [{db :db} [_ resp]]
    {:dispatch [::add-photo resp]}))

(reg-event-fx
  ::photo-upload-failure
  profile-interceptors
  (fn [{db :db} _]
    {:db (assoc db ::profile/photo-uploading? false)
     :dispatch [:error/set-global "There was an error adding the photo"]}))

(reg-event-fx
  ::add-photo
  db/default-interceptors
  (fn [{db :db} [image]]
    (let [company (cached-company db)]
      {:db (assoc-in db [::profile/sub-db ::profile/photo-uploading?] false)
       :graphql {:query (update-company-mutation-with-fields [[:images [:url :width :height]] :id])
                 :variables {:update_company
                             {:id (:id company)
                              :images (conj (:images company) (select-keys image [:url :width :height]))}}
                 :on-success [::add-photo-success]
                 :on-failure [::photo-upload-failure]}})))

(reg-event-fx
  ::add-photo-success
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [company (:update_company (:data resp))]
      {:dispatch [:graphql/update-entry :company {:id (:id company)} :merge {:company company}]})))

(reg-event-fx
  ::delete-photo
  db/default-interceptors
  (fn [{db :db} [image]]
    (let [company (cached-company db)
          updated-company (update company :images #(remove #{image} %))]
      {:dispatch [:graphql/update-entry :company {:id (:id company)} :overwrite {:company updated-company}]
       :graphql {:query (update-company-mutation-with-fields [[:images [:url :width :height]] :id])
                 :variables {:update_company
                             {:id (:id company)
                              :images (:images updated-company)}}
                 :on-failure [:error/set-global "There was an error deleting the photo"]}})))
