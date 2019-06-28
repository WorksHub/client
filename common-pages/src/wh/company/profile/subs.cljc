(ns wh.company.profile.subs
  (:require
    [re-frame.core :refer [reg-sub reg-sub-raw]]
    [wh.company.profile.db :as profile]
    [wh.graphql-cache :as gql-cache]
    [wh.re-frame.subs :refer [<sub]])
  (#?(:clj :require :cljs :require-macros)
    [wh.re-frame.subs :refer [reaction]]))

(reg-sub
  ::sub-db
  (fn [db _]
    (::profile/sub-db db)))

(reg-sub-raw
  ::company
  (fn [_ _]
    (reaction
     (let [id     (<sub [:wh/page-param :id])
           result (<sub [:graphql/result :company {:id id}])]
       (:company result)))))

(reg-sub
  ::profile-enabled?
  :<- [::company]
  (fn [company _]
    (:profile-enabled company)))

(reg-sub
  ::name
  :<- [::company]
  (fn [company _]
    (:name company)))

(reg-sub
  ::logo
  :<- [::company]
  (fn [company _]
    (:logo company)))

(reg-sub
  ::id
  :<- [::company]
  (fn [company _]
    (:id company)))

(reg-sub
  ::images
  :<- [::company]
  (fn [company _]
    (map-indexed (fn [idx img] (assoc img :index idx)) (:images company))))

(reg-sub
  ::photo-uploading?
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/photo-uploading? sub-db)))

(reg-sub
  ::videos
  :<- [::company]
  (fn [company _]
    (:videos company)))

(reg-sub
  ::video-error
  :<- [::sub-db]
  (fn [sub-db _]
    (::profile/video-error sub-db)))
