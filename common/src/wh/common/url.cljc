(ns wh.common.url
  (:require #?(:clj [taoensso.timbre :refer [error]])
            #?(:cljs [goog.Uri :as uri])
            [bidi.bidi :as bidi]
            [clojure.string :as str]
            [wh.common.text :as text]
            [wh.util :as util]))

(def wh-cookie-names {:auth             "auth_token"
                      :tracking-consent "wh_tracking_consent"
                      :tracking-id      "wh_aid"})

(defn sanitize-url [url]
  (if (str/includes? url "://")
    url
    (str "http://" url)))

(defn websites-domain? [website domain]
  (when domain
    (str/includes? domain website)))

(defn strip-query-params
  [uri]
  (if-let [?-index (str/last-index-of uri "?")]
    (subs uri 0 ?-index)
    uri))

(defn uri->domain [uri]
  (let [uri (str/trim uri)]
    #?(:cljs
       (try
         (text/not-blank (.getDomain (uri/parse uri)))
         (catch js/Error _)))
    #?(:clj
       (try
         (.getHost (java.net.URI. uri))
         (catch Exception e
           (error "Failed to parse URI:" uri))))))

(defn strip-path [uri]
  (let [uri (str/trim uri)]
    #?(:cljs
       (try
         (let [u    (uri/parse uri)
               port (.getPort u)]
           (str (.getScheme u) "://" (.getDomain u) (when (and port (pos-int? port)) (str ":" port))))
         (catch js/Error _)))
    #?(:clj
       (try
         (let [u    (java.net.URI. uri)
               port (.getPort u)]
           (str (.getScheme u) "://" (.getHost u) (when (and port (pos-int? port)) (str ":" port))))
         (catch Exception e
           (error "Failed to parse URI:" uri))))))

(defn has-domain? [uri]
  (not (nil? (uri->domain uri))))

(defn detect-page-type [url]
  (when url
    (let [url (sanitize-url url)
          [_ _ domain & remaining] (str/split url #"/")
          handle (last remaining)]
      (merge {:url url}
             (condp websites-domain? domain
               "github.com" {:type :github, :display handle}
               "twitter.com" {:type :twitter, :display handle}
               "facebook.com" {:type :facebook, :display handle}
               "linkedin.com" {:type :linkedin, :display handle}
               "stackoverflow.com" {:type :stackoverflow, :display handle}
               {:type :web, :display (uri->domain url)})))))

(defn detect-urls-type [urls]
  (mapv (comp detect-page-type :url) urls))

(defn vertical-homepage-href
  "Server-side callers should consider wh.url/base-url as a more
  robust - but less portable - version of ths fn"
  [env vertical]
  (case (name env)
    "prod" (str "https://" (name vertical) ".works-hub.com")
    "stage" (str "/?vertical=" (name vertical))
    "dev" (str "http://" (name vertical) ".localdomain:3449")))

(defn share-urls [args]
  (let [{:keys [text text-twitter text-linkedin url]} (util/fmap args bidi/url-encode)]
    {:twitter  (str "https://twitter.com/intent/tweet?text="
                    (or text-twitter text) "&url=" url)
     :facebook (str "http://www.facebook.com/sharer/sharer.php?u=" url)
     :linkedin (str "https://www.linkedin.com/shareArticle?mini=true&url="
                    url "&title=" (or text-linkedin text) "&summary=&origin=")}))
