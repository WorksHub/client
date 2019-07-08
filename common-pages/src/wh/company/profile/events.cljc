(ns wh.company.profile.events
  (:require
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])
    #?(:cljs [wh.user.db :as user])
    [re-frame.core :refer [path]]
    [wh.company.profile.db :as profile]
    [wh.db :as db]
    [wh.graphql-cache :as cache :refer [reg-query]]
    [wh.graphql.company :refer [update-company-mutation update-company-mutation-with-fields]]
    [wh.graphql.tag :as tag-gql]
    [wh.re-frame.events :refer [reg-event-db reg-event-fx]]
    [clojure.set :as set]
    [wh.common.cases :as cases]
    [wh.util :as util])
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
                    [:id :name :logo :profileEnabled :descriptionHtml
                     [:devSetup [:hardware :software :sourcecontrol :ci :infrastructure]]
                     [:tags [:id :type :label :slug]]
                     [:videos [:youtubeId :thumbnail :description]]
                     [:images [:url :width :height]]]]]})

(reg-query :company fetch-company-query)

(defquery fetch-tags
  {:venia/operation {:operation/type :query
                     :operation/name "list_tags"}
   :venia/variables [{:variable/name "type"
                      :variable/type :tag_type}]
   :venia/queries [[:list_tags {:type :$type}
                    [[:tags [:id :label :type :slug]]]]]})

(reg-query :tags fetch-tags)

(defn initial-query [db]
  [:company {:id (get-in db [:wh.db/page-params :id])}])

(defn tag-query [type-filter]
  (if type-filter
    [:tags {:type type-filter}]
    [:tags {}]))

(defn company-id
  [db]
  (get-in db [:wh.db/page-params :id]))

(defn cached-company
  [db]
  (->> (cache/result db :company {:id (company-id db)})
       :company
       (profile/->company)))

#?(:cljs
   (defmethod on-page-load :company
     [db]
     (list (into [:graphql/query] (initial-query db))
           [::load-photoswipe]
           (when (or (user/admin? db)
                     (user/owner? db (company-id db)))
             [::fetch-all-tags])))) ;; TODO for now we get all tags and filter client-side

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

(defn company-changes->cache
  [db changes]
  (cond-> changes
          (:tag-ids changes)
          (-> (assoc :tags (->> (cache/result db :tags {})
                                :list-tags
                                :tags
                                (filter #(contains? (set (:tag-ids changes)) (:id %)))
                                (map profile/->tag)))
              (dissoc :tag-ids))))

(reg-event-fx
  ::update-company
  db/default-interceptors
  (fn [{db :db} [changes]]
    (let [company (cached-company db)]
      {:db (assoc-in db [::profile/sub-db ::profile/updating?] true)
       :dispatch [:graphql/update-entry :company {:id (:id company)}
                  :merge {:company (company-changes->cache db changes)}]
       :graphql {:query (update-company-mutation-with-fields [:id
                                                              [:tags [:id :label :slug :type]]])
                 :variables {:update_company
                             (merge {:id (:id company)}
                                    (cases/->camel-case changes))}
                 :on-success [::update-company-success]
                 :on-failure [::update-company-failure changes company]}})))

(reg-event-fx
  ::update-company-failure
  profile-interceptors
  (fn [{db :db} [changes old-company]]
    {:db (assoc db ::profile/updating? false)
     :dispatch-n [[:graphql/update-entry :company {:id (:id old-company)} :overwrite {:company old-company}]
                  [:error/set-global "There was an error updating the company"
                   [::update-company changes]]]}))

(reg-event-fx
  ::update-company-success
  profile-interceptors
  (fn [{db :db} [resp]]
    (let [company (get-in resp [:data :update_company])]
      {:dispatch [:graphql/update-entry :company {:id (:id company)}
                  :merge {:company company}]
       :db (assoc db ::profile/updating? false)})))

(reg-event-db
  ::set-tag-search
  profile-interceptors
  (fn [db [tag-type tag-search]]
    (assoc-in db [::profile/tag-search tag-type] tag-search)))

(reg-event-fx
  ::fetch-all-tags
  (fn [{db :db} _]
    {:dispatch (into [:graphql/query] (tag-query nil))}))

(reg-event-db
  ::reset-selected-tag-ids
  db/default-interceptors
  (fn [db [tag-type]]
    (assoc-in db [::profile/sub-db ::profile/selected-tag-ids tag-type]
              (->> (cached-company db)
                   :tags
                   (filter (comp (partial = tag-type) :type))
                   (map :id)
                   (set)))))

(reg-event-db
  ::toggle-selected-tag-id
  db/default-interceptors
  (fn [db [tag-type id]]
    (update-in db [::profile/sub-db ::profile/selected-tag-ids tag-type] (fnil util/toggle #{}) id)))

(reg-event-fx
  ::create-new-tag
  profile-interceptors
  (fn [{db :db} [label tag-type]]
    {:db (assoc db ::profile/creating-tag? true)
     :graphql {:query tag-gql/create-tag-mutation
               :variables {:label label :type tag-type}
               :on-success [::create-new-tag-success tag-type]
               :on-failure [::create-new-tag-failure]}}))

(reg-event-fx
  ::create-new-tag-success
  db/default-interceptors
  (fn [{db :db} [tag-type resp]]
    (let [new-tag (get-in resp [:data :create_tag])
          all-tags (cache/result db :tags {})]
      (merge {:db (-> db
                      (assoc-in  [::profile/sub-db ::profile/creating-tag?] false)
                      (assoc-in  [::profile/sub-db ::profile/tag-search tag-type] nil)
                      (update-in [::profile/sub-db ::profile/selected-tag-ids tag-type] (fnil conj #{}) (:id new-tag)))}
             (when (not (some #(= (:id new-tag) (:id %)) (get-in all-tags [:list-tags :tags]))) ;; check the tag isn't an existing tag
               {:dispatch [:graphql/update-entry :tags {}
                           :overwrite (update-in all-tags [:list-tags :tags] conj new-tag)]})))))

(reg-event-fx
  ::create-new-tag-failure
  profile-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::profile/creating-tag? false)
     :dispatch [:error/set-global "There was an error adding the tag"]}))
