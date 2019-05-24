(ns wh.common.fx.analytics
  (:require
    [ajax.json :as ajax-json]
    [clojure.string :as str]
    [re-frame.core :refer [reg-fx reg-event-fx]]
    [re-frame.db :refer [app-db]]
    [wh.common.cases :as cases]
    [wh.common.fx.http :as http-fx]
    [wh.routes :as routes]
    [wh.util :as util])
  (:require-macros
    [cljs.core :refer [exists?]]))

(defn rename-utm-tags [[k v]]
  (let [tag-name (second (str/split k #"_"))]
    [(if (= "campaign" tag-name) "name" tag-name) v]))

(defn attach-source-info [body]
  (merge body
         {:sourcing {:campaign (into {} (map rename-utm-tags (:wh.db/initial-utm-tags @app-db)))
                     :referrer (:wh.db/initial-referrer @app-db)}}))

(reg-fx
 :analytics/reset
 (fn []
   (js/resetAnalytics)))

(reg-fx
 :analytics/alias
 (fn [{:keys [id]}]
   (js/submitAnalyticsAlias id)))

(reg-fx
 :analytics/identify
 (fn [{:keys [id] :as identify-user}]
   (js/submitAnalyticsIdentify id (clj->js identify-user) (clj->js {:integrations {"Sentry" false}}))))

(reg-fx
 :analytics/pageview
 (fn [[event-name data]]
   (js/sendServerAnalytics (clj->js {:type    :page
                                     :payload (clj->js {:page-name  event-name
                                                        :properties data})}))
   (js/submitAnalyticsPage)))

(defn add-noninteractive [data non-interactive?]
  (if non-interactive?
    (assoc data :nonInteraction 1)
    data))

(reg-fx
 :analytics/track
 (fn [[event-name data non-interactive?]]
   (let [properties (-> data
                        cases/->snake-case
                        (add-noninteractive non-interactive?))]
     (js/sendServerAnalytics (clj->js {:type    :track
                                       :payload (clj->js {:event-name event-name
                                                          :properties properties})}))
     (js/submitAnalyticsTrack event-name (clj->js properties)))))

(reg-fx
  :reddit/conversion-pixel
  (fn []
    (-> (js/Image.)
        (aset "src" "https://alb.reddit.com/snoo.gif?q=CAAHAAABAAoACQAAAAbiHMscAA==&s=Mdk07HNJof2V4iYvOG7Xjze3WPvvGToIlqHu8uuIhLY="))))
