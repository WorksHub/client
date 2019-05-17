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

(reg-event-fx
  ::tracking-success
  (fn [_ [_ response]]
    nil))

(reg-event-fx
  ::tracking-failure
  (fn [_ [_ response]]
    (js/console.error "Analytics failure:" response)))

(defn rename-utm-tags [[k v]]
  (let [tag-name (second (str/split k #"_"))]
    [(if (= "campaign" tag-name) "name" tag-name) v]))

(defn attach-source-info [body]
  (merge body
         {:sourcing {:campaign (into {} (map rename-utm-tags (:wh.db/initial-utm-tags @app-db)))
                     :referrer (:wh.db/initial-referrer @app-db)}}))

(defn server-tracking [body]
  (http-fx/http-effect {:method          :post
                        :uri             (routes/path :analytics)
                        :params          (-> body attach-source-info util/remove-nils)
                        :format          (ajax-json/json-request-format)
                        :response-format (ajax-json/json-response-format {:keywords? true})
                        :timeout         10000
                        :on-success      [::tracking-success]
                        :on-failure      [::tracking-failure]}))

(reg-fx
  :analytics/init-tracking
  (fn []
    (server-tracking {:type :init})))

(reg-fx
  :analytics/load
  (fn []
    (when (exists? js/analytics)
      (js/analytics.load))))

(reg-fx
  :analytics/reset
  (fn []
    (when (exists? js/analytics)
      (js/analytics.reset))))

(reg-fx
  :analytics/alias
  (fn [{:keys [id]}]
    (when (exists? js/analytics)
      (js/analytics.alias id))))

(reg-fx
  :analytics/identify
  (fn [{:keys [id] :as identify-user}]
    (when (exists? js/analytics)
      (js/analytics.identify id (clj->js identify-user) (clj->js {:integrations {"Sentry" false}})))))

(reg-fx
  :analytics/pageview
  (fn [[event-name data]]
    (server-tracking {:type    :page
                      :payload {:page-name  event-name
                                :properties data}})
    (when (exists? js/analytics)
      (js/analytics.page))))

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
      (server-tracking {:type    :track
                        :payload {:event-name event-name
                                  :properties properties}})
      (when (exists? js/analytics)
        (js/analytics.track event-name (clj->js properties))))))

(reg-fx
  :reddit/conversion-pixel
  (fn []
    (-> (js/Image.)
        (aset "src" "https://alb.reddit.com/snoo.gif?q=CAAHAAABAAoACQAAAAbiHMscAA==&s=Mdk07HNJof2V4iYvOG7Xjze3WPvvGToIlqHu8uuIhLY="))))
