(ns wh.landing-new.subs
  (:require #?(:clj [clj-time.coerce :as c])
            #?(:clj [clj-time.format :as cf])
            [re-frame.core :refer [reg-sub]]
            [wh.common.job :as jobc]
            [wh.util :as util]
            [clojure.string :as str]))

(reg-sub
  ::sub-db
  (fn [db _]
    (:wh.homepage-new.db/sub-db db)))

(reg-sub
  ::top-tags
  :<- [::sub-db]
  (fn [sub-db _]
    (:top-tags sub-db)))

(reg-sub
  ::top-blogs
  (fn [db _]
      (get-in (wh.graphql-cache/result db :top-blogs nil) [:top-blogs :results])))

(reg-sub
  ::top-companies
  (fn [db _]
    (get-in (wh.graphql-cache/result db :top-companies nil) [:top-companies :results])))

(reg-sub
  ::top-users
  (fn [db _]
    (get-in (wh.graphql-cache/result db :top-users nil) [:top-users :results])))

(reg-sub
  ::recent-jobs
  (fn [db _]
    (get-in (wh.graphql-cache/result db :recent-jobs nil) [:recent-jobs :results])))

(reg-sub
  ::recent-issues
  (fn [db _]
    (get-in (wh.graphql-cache/result db :recent-issues nil) [:recent-issues :results])))

(defn display-date [date]
  #?(:clj
     (when (pos? date)
       (cf/unparse (cf/formatter "yyyy-MM-dd")
                   (c/from-long (long date))))

     ;; TODO: implement cljs version based on moment.js
     ;; CH4363
     :cljs date))

(defn translate-job [job]
  (-> job
      (util/update-in*
        [:remuneration :currency] #(when % (name %)))
      (assoc
        :display-date (display-date (:first-published job)))
      (util/update*
        :role-type #(-> % name (str/replace "_" " ")))
      jobc/translate-job))

(defn normalize-activity
  [{:keys [feed-company feed-issue feed-job feed-blog] :as activity}]
  (-> activity
      (merge
        (cond
          feed-job     {:object      (translate-job feed-job)
                        :object-type "job"}
          feed-blog    {:object      feed-blog
                        :object-type "article"}
          feed-issue   {:object      feed-issue
                        :object-type "issue"}
          feed-company {:object      feed-company
                        :object-type "company"}
          :else        {}))
      (update :actor :id)))

(reg-sub
  ::all-activities
  (fn [db _]
    (map normalize-activity
         (get-in (wh.graphql-cache/result db :all-activities nil)
                 [:query-activities :activities]))))
