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
            [wh.graphql-cache :as gqlc]
            [wh.landing-new.events :as events]
            [wh.landing-new.tags :as tags]
            [wh.util :as util]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.homepage-new.db/sub-db db)))

(reg-sub
  ::top-blogs
  (fn [db _]
    (get-in (apply gqlc/result db (events/top-blogs db)) [:top-blogs :results])))

(reg-sub
  ::top-companies
  (fn [db _]
    (get-in (apply gqlc/result db (events/top-companies db)) [:top-companies :results])))

(reg-sub
  ::top-tags
  (fn [db _]
    (get-in (apply gqlc/result db (events/top-tags db)) [:top-tags :results])))

(reg-sub
  ::top-users
  (fn [db _]
    (get-in (apply gqlc/result db (events/top-users db)) [:top-users :results])))

(reg-sub
  ::recent-jobs
  (fn [db _]
    (get-in (apply gqlc/result db (events/recent-jobs db)) [:recent-jobs :results])))

(reg-sub
  ::recent-issues
  (fn [db _]
    (get-in (apply gqlc/result db (events/recent-issues db)) [:recent-issues :results])))

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
  ::recent-activities-loading?
  (fn [db _]
    (= :executing (keyword (apply gqlc/state db (events/recent-activities db))))))
