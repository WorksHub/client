(ns wh.server
  (:require
    [bidi.ring :as bidi-ring]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [net.cgrand.enlive-html :as html]
    [ring.adapter.jetty :refer [run-jetty]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.util.response :as resp]
    [wh.routes :as routes]
    [wh.verticals :refer [vertical-config]]))

(def config
  {:api-server "https://works-hub-api.herokuapp.com"
   :vertical "functional"
   :google-maps-key "AIzaSyD7Ko7E0XrtzUzf0CxLv3xQcAxlbwqmu1E"})

(html/deftemplate wh-index-template "index.html"
                  [{:keys [app-db] :as ctx}]
                  [:script#data-init] (html/content (pr-str app-db))
                  [:script#google-maps] (html/content (format "window.googleMapsURL = 'https://maps.googleapis.com/maps/api/js?key=%s&libraries=places';"
                                                              (:google-maps-key config)))
                  [:script#load-symbols-file-fn] (html/transform-content (html/replace-vars {:prefix ""}))
                  [:div#app-js] (html/content {:tag :script, :attrs {:type "text/javascript", :src "/js/wh.js"}})
                  [:style#main-style] (constantly {:tag :link, :attrs {:rel "stylesheet", :type "text/css", :href "/wh.css"}}))

(defn html-response
  [^String s]
  (-> s
      resp/response
      (resp/content-type "text/html; charset=utf-8")))

(defn initial-app-db
  [{:keys [vertical api-server] :as config} request]
  {:wh.settings/environment :dev
   :wh.db/api-server api-server
   :wh.db/vertical vertical
   :wh.db/query-params (:query-params request)
   :wh.db/page-params {}
   :wh.db/initial-load? true
   :wh.db/default-technologies (get-in vertical-config [vertical :default-technologies])})

(defn index-handler [request]
  (-> {:app-db (initial-app-db config request)}
      wh-index-template
      html-response))

(def handler
  (-> (bidi-ring/make-handler routes/routes (constantly index-handler))
      (wrap-resource "public")
      (wrap-content-type)))

(defonce server
  (atom nil))

(defn start-server
  "Start a Jetty server to handle requests. Provide a port to dynamically override the config file."
  ([]
   (start-server 3449))
  ([port]
   (swap! server (fn [running]
                   (when running (.stop running))
                   (run-jetty (var handler) {:port port :join? false})))))
