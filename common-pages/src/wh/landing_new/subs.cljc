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


(defn display-month [date]
  (tf/unparse (tf/formatter "MMM d yyyy")
              date))

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
        :display-date (display-month (time/str->time (:creation-date blog) :date-time)))))

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
          feed-company {:object      feed-company
                        :object-type "company"}
          :else        {}))
      (update :actor :id)))

(reg-sub
  ::recent-activities
  (fn [{:keys [wh.db/query-params] :as db} _]
    (let [tags (tags/param->tags (get query-params "tags"))]
      (map normalize-activity
           (get-in (gqlc/result db :recent-activities {:activities_tags tags})
                   [:query-activities :activities])))))
