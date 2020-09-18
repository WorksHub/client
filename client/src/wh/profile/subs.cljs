(ns wh.profile.subs
  (:require [re-frame.core :refer [reg-sub]]
            [wh.common.specs.primitives]
            [wh.common.url :as url]
            [wh.graphql-cache :as gql-cache]
            [wh.subs :refer [with-unspecified-option]]
            [wh.profile.events :as profile-events]
            [wh.common.issue :refer [gql-issue->issue]])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::profile-query-result
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (gql-cache/result db query params))))

(reg-sub
  ::profile
  :<- [::profile-query-result]
  (fn [result _]
    (:user result)))

(reg-sub
  ::issues
  :<- [::profile-query-result]
  (fn [result _]
    (->> [:query-issues :issues]
         (get-in result)
         (map gql-issue->issue))))

(reg-sub
  ::blogs
  :<- [::profile-query-result]
  (fn [result _]
    (get-in result [:blogs :blogs])))

(reg-sub
  ::skills
  :<- [::profile]
  (fn [profile _]
    (->> profile
         :skills
         (sort-by #(or (:rating %) 0) >))))

(reg-sub
  ::social
  :<- [::profile]
  (fn [profile [_ type]]
    (->> profile
        :other-urls
        url/detect-urls-type
        (some #(when (= type (:type %)) %)))))

(reg-sub
  ::error?
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (= :failure (gql-cache/state db query params)))))

(reg-sub
  ::loader?
  (fn [db _]
    (let [[query params] (profile-events/profile-query-description db)]
      (boolean (#{:initial :executing} (gql-cache/state db query params))))))

