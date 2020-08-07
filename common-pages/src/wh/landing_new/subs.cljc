(ns wh.landing-new.subs
  (:require #?(:clj [clj-time.coerce :as tc]
               :cljs [cljs-time.coerce :as tc])
            #?(:clj [clj-time.format :as tf]
               :cljs [cljs-time.format :as tf])
            [clojure.string :as str]
            [clojure.walk :as walk]
            [re-frame.core :refer [reg-sub]]
            [wh.common.job :as jobc]
            [wh.common.time :as time]
            [wh.common.user :as user-common]
            [wh.components.tag-selector.tag-selector :as tag-selector]
            [wh.graphql-cache :as gqlc]
            [wh.landing-new.events :as events]
            [wh.landing-new.tags :as tags]
            [wh.util :as util]))

(defn cache-results
  [query-fn db ks]
  (get-in (apply gqlc/result db (query-fn db)) ks))

(defn cache-loading?
  [query-fn db]
  (= "executing" (apply gqlc/state db (query-fn db))))

(reg-sub
  ::top-blogs
  (fn [db _]
    (cache-results events/top-blogs db [:top-blogs :results])))

(reg-sub
  ::top-blogs-loading?
  (fn [db _]
    (cache-loading? events/top-blogs db)))

(reg-sub
  ::top-companies
  (fn [db _]
    (cache-results events/top-companies db [:top-companies :results])))

(reg-sub
  ::top-companies-loading?
  (fn [db _]
    (cache-loading? events/top-companies db)))

(reg-sub
  ::top-tags
  (fn [db _]
    (cache-results events/top-tags db [:top-tags :results])))

(reg-sub
  ::top-tags-loading?
  (fn [db _]
    (cache-loading? events/top-tags db)))

(reg-sub
  ::top-users
  (fn [db _]
    (cache-results events/top-users db [:top-users :results])))

(reg-sub
  ::top-users-loading?
  (fn [db _]
    (cache-loading? events/top-users db)))

(reg-sub
  ::recent-jobs
  (fn [db _]
    (cache-results events/recent-jobs db [:recent-jobs :results])))

(reg-sub
  ::recent-jobs-loading?
  (fn [db _]
    (cache-loading? events/recent-jobs db)))

(reg-sub
  ::recent-issues
  (fn [db _]
    (cache-results events/recent-issues db [:recent-issues :results])))

(reg-sub
  ::recent-issues-loading?
  (fn [db _]
    (cache-loading? events/recent-issues db)))

(reg-sub
  ::recommended-jobs
  (fn [db _]
    (some->> (cache-results events/recommended-jobs db [:jobs])
             (map #(assoc % :company-info (:company %))))))

(reg-sub
  ::recommended-jobs-loading?
  (fn [db _]
    (cache-loading? events/recommended-jobs db)))

(defn translate-job [job]
  (-> job
      (util/update-in*
        [:remuneration :currency] #(when % (name %)))
      (assoc
        :display-date (time/human-time (time/str->time (:first-published job) :date-time)))
      (util/update*
        :role-type #(-> % name (str/replace "_" " ")))
      jobc/translate-job))

(defn translate-blog [blog]
  (-> blog
      (assoc
        :display-date (time/human-time (time/str->time (:creation-date blog) :date-time)))))

(defn translate-company [c]
  (-> c
      (assoc
        :creation-date (time/human-time (time/str->time (:creation-date c) :date-time)))))

(defn normalize-activity
  [{:keys [feed-company feed-issue feed-job feed-blog] :as activity}]
  (-> activity
      (merge
        (cond
          feed-job     {:object      (translate-job feed-job)
                        :object-type "job"}
          feed-blog    {:object      (translate-blog feed-blog)
                        :object-type "article"}
          feed-issue   {:object      feed-issue
                        :object-type "issue"}
          feed-company {:object      (translate-company feed-company)
                        :object-type "company"}
          :else        {}))
      (update :actor :id)
      (update :verb  keyword)))

(reg-sub
  ::recent-activities
  (fn [db _]
    (map normalize-activity
         (get-in (apply gqlc/result db (events/recent-activities db))
                 [:query-activities :activities]))))

(reg-sub
  ::recent-activities-last-page
  (fn [db _]
    (cache-results events/recent-activities db [:query-activities :last-page])))

(reg-sub
  ::recent-activities-first-page
  (fn [db _]
    (cache-results events/recent-activities db [:query-activities :first-page])))

(reg-sub
  ::recent-activities-next-page
  :<- [::recent-activities-last-page]
  (fn [last-page _]
    (not last-page)))

(reg-sub
  ::recent-activities-prev-page
  :<- [::recent-activities-first-page]
  (fn [first-page _]
    (not first-page)))

(reg-sub
  ::recent-activities-loading?
  (fn [db _]
    (let [state (apply gqlc/state db (events/recent-activities db))]
      (or (nil? state)
          (= :executing (keyword state))))))

(reg-sub
  ::not-enough-activities?
  :<- [::user-skills]
  :<- [::recent-activities-first-page]
  :<- [::recent-activities]
  (fn [[skills first-page activities] _]
    (boolean
      (and (seq skills) first-page (< (count activities) 10)))))

(reg-sub
  ::selected-tags
  :<- [:wh/query-params]
  (fn [query-params _]
    (->> query-params
         tag-selector/query-params->query-params-tags-slugs
         (map tag-selector/query-params-tag-slug->partial-tag))))

(reg-sub
  ::user-image
  :<- [:user/sub-db]
  (fn [user _]
    (:wh.user.db/image-url user)))

(reg-sub
  ::candidate?
  (fn [db _]
    (user-common/candidate? db)))

(reg-sub
  ::user-details
  (fn [db _]
    (cache-results events/user-stats db [:query-user-stats])))

(reg-sub
  ::user-skills
  :<- [:user/sub-db]
  (fn [user _]
    (:wh.user.db/skills user)))
