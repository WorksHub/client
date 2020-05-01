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
    [wh.components.auth-popup :as auth]
    [wh.components.tracking-popup :as tracking]
    [wh.routes :as routes]
    [wh.verticals :as verticals]))

(def config
  {:api-server "https://works-hub-api.herokuapp.com"
   :vertical "functional"
   :google-maps-key "AIzaSyD7Ko7E0XrtzUzf0CxLv3xQcAxlbwqmu1E"})

(defn inline-js
  [page]
  (cond-> []
          (= :company page)
          (conj "public/pswp/photoswipe.js"
                "public/pswp/photoswipe-ui-default.js")
          true
          (conj "body-scroll-lock/bodyScrollLock.js")))

(defn external-css
  []
  ["/pswp/photoswipe.css"
   "/pswp/default-skin/default-skin.css"])

(defn icons-to-load
  [page]
  (let [base ["symbols/common.svg" "symbols/tags.svg"]]
    (cond
      (or (= :company page)
          (= :companies page))
      (conj base "symbols/company_profile.svg")
      (= :issues page)
      (conj base "symbols/issues.svg")
      (= :job page)
      (conj base "symbols/job.svg")
      :else
      base)))

(defn ->html
  [d]
  (html/html
    (clojure.walk/prewalk
      (fn [x]
        (if (and (vector? x) (fn? (first x)))
          (let [component (apply (first x) (rest x))]
            (if (fn? component)
              (->html (apply component (rest x))) ; form-2
              (->html component)))                ; form-1
          x))
      d)))

(defn- load-public-js
  []
  (let [files (read-string (slurp (io/resource "publicjs.edn")))]
    (reduce #(str %1 (slurp (io/resource (.getName (clojure.java.io/file %2))))) "" files)))

(html/deftemplate wh-index-template "index.html"
                  [{:keys [app-db page] :as ctx}]
                  [:script#data-init] (html/content (pr-str app-db))
                  [:script#google-maps] (html/content (format "window.googleMapsURL = 'https://maps.googleapis.com/maps/api/js?key=%s&libraries=places';" (:google-maps-key config)))
                  [:script.inline-js]  (html/clone-for [filename (inline-js page)]
                                                       (html/do->
                                                         (html/content (slurp (io/resource filename)))
                                                         (html/set-attr :id filename)))
                  ;;TODO is this needed?
                  [:script.load-icons] (html/clone-for [filename (icons-to-load page)]
                                                       (html/do->
                                                         (html/content (format "loadSymbols(\"%s\");" filename))
                                                         (html/set-attr :id (str "load-icons-" filename))))
                  [:script#public] (html/content (load-public-js))
                  [:script#public] (html/transform-content (html/replace-vars {:prefix ""}))
                  ;;TODO is this needed?
                  [:script#load-symbols-file-fn] (html/transform-content (html/replace-vars {:prefix ""}))
                  [:div#app] (html/set-attr :class "")
                  [:div#app-ssr] (html/set-attr :class "app--hidden")
                  [:div#app-js] (html/content {:tag :script, :attrs {:type "text/javascript", :src "/js/wh.js"}})
                  [:div#tracking-popup-container] (html/content (->html (tracking/popup)))
                  [:div#auth-popup-container] (html/content (->html (auth/popup (verticals/config (:wh.db/vertical app-db) :platform-name))))
                  [:style#main-style] (constantly {:tag :link, :attrs {:rel "stylesheet", :type "text/css", :href "/wh.css"}})
                  [:link.external-css]  (html/clone-for [filename (external-css)]
                                                        (html/set-attr :href filename
                                                                       :rel "stylesheet"
                                                                       :type "text/css")))

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
   :wh.db/uri (:uri request)
   :wh.db/initial-load? true
   :wh.db/default-technologies (verticals/config vertical :default-technologies)})

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
