(ns wh.common.fx.algolia
  (:require
    [ajax.json :as ajax-json]
    [ajax.simple :as ajax-simple]
    [re-frame.core :refer [reg-fx]]
    [re-frame.db :refer [app-db]]
    [wh.algolia :as algolia]
    [wh.common.fx.http :as http-fx]))

;; NOTE: The helper functions below access app-db even though it's not
;; explicitly passed to them. This is a workaround of reg-fx handlers not
;; having access to app-db. We could pass the configurations explicitly,
;; but that would mean a lot of repetition, and stuff under :wh.settings
;; never changes anyway.

(defn- algolia-application-id
  [index]
  (if (= index :places)
    "plLKWJEULLEO"
    (:wh.settings/algolia-application-id @app-db)))

(defn- algolia-application-name
  [index]
  (if (= index :places)
    "places"
    (:wh.settings/algolia-application-id @app-db)))

(defn- algolia-api-key
  [index]
  (if (= index :places)
    "871a27f2d9c3de7f7a53d6de6e6740de"
    (:wh.settings/algolia-search-key @app-db)))

(defn- algolia-index
  [index]
  (if (= index :candidates)
    (:wh.settings/candidates-index @app-db)
    (name index)))

(defn- algolia-url
  [index retry-num]
  (let [index-part (if (= index :places)
                     "places"
                     (str "indexes/" (algolia-index index)))]
    (str (algolia/get-search-url (algolia-application-name index) retry-num) "/1/" index-part "/query")))

(defn make-algolia-request-map
  [{:keys [index params retry-num on-success on-failure] :as request}]
  {:method          :post
   :uri             (algolia-url index retry-num)
   :headers         {"X-Algolia-Application-Id" (algolia-application-id index)
                     "X-Algolia-API-Key"        (algolia-api-key index)}
   :params          params
   :timeout         10000
   :format          (ajax-json/json-request-format)
   :response-format (ajax-json/json-response-format {:keywords? true})
   :on-success      on-success
   :on-failure      (conj on-failure retry-num)})

(defn algolia-effect
  [request]
  (-> request make-algolia-request-map http-fx/request->xhrio-options ajax-simple/ajax-request))

(reg-fx :algolia algolia-effect)
