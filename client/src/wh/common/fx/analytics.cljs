(ns wh.common.fx.analytics
  (:require [re-frame.core :refer [reg-fx]]
            [wh.common.cases :as cases]
            [wh.common.keywords :as keywords]
            [wh.common.user :as user]))

(reg-fx
  :analytics/reset
  (fn []
    (js/resetAnalytics)))

(defn alias [db]
  (js/sendServerAnalytics (clj->js {:type :alias}))
  (js/submitAnalyticsAlias (get-in db [:wh.user.db/sub-db :wh.user.db/id])))

(defn identify [db]
  (let [{:keys [id] :as user} (-> (:wh.user.db/sub-db db)
                                  keywords/strip-ns-from-map-keys)]
    (when id
      (js/submitAnalyticsIdentify
        id
        (clj->js (-> user
                     user/user->segment-traits))
        (clj->js {:integrations {"Sentry" false}})))))

(reg-fx :analytics/identify identify)

(reg-fx
  :analytics/pageview
  (fn [_]
    (when (.. js/window -wh_analytics -init)
      (js/trackPage))))

(defn add-noninteractive [data non-interactive?]
  (if non-interactive?
    (assoc data :nonInteraction 1)
    data))

(defn track [[event-name data non-interactive?]]
  (let [properties (-> data
                       cases/->snake-case
                       (add-noninteractive non-interactive?))]
    (js/sendServerAnalytics (clj->js {:type    :track
                                      :payload (clj->js {:event-name event-name
                                                         :properties properties})}))
    (js/submitAnalyticsTrack event-name (clj->js properties))))

(reg-fx :analytics/track track)

(defn track-reddit []
  (-> (js/Image.)
      (aset "src" "https://alb.reddit.com/snoo.gif?q=CAAHAAABAAoACQAAAAbiHMscAA==&s=Mdk07HNJof2V4iYvOG7Xjze3WPvvGToIlqHu8uuIhLY=")))

(defn track-linkedin [registration-type]
  (let [img (if (= "company" registration-type)
              "https://px.ads.linkedin.com/collect/?pid=1684137&conversionId=1620346&fmt=gif"
              "https://px.ads.linkedin.com/collect/?pid=1684137&conversionId=1587249&fmt=gif")]
    (-> (js/Image.)
        (aset "src" img))))

(reg-fx
  :analytics/account-created
  (fn [[data db]]
    (track ["Account Created" data])
    (track-reddit)
    (track-linkedin (:type data))))

(reg-fx
  :analytics/agree-to-tracking
  (fn [agreed?]
    (when agreed?
      (js/agreeToTracking))))
