(ns wh.common.url
  (:require #?(:clj  [ring.util.codec :as codec])
            #?(:clj  [taoensso.timbre :refer [error]])
            #?(:cljs [goog.Uri :as uri])
            [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [#?(:clj  clojure.spec.gen.alpha
                :cljs cljs.spec.gen.alpha) :as gen]
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

(s/def ::string-or-uri
  (s/spec (s/or :str string?
                :uri (partial instance? #?(:clj  java.net.URI
                                           :cljs goog.Uri)))
          :gen (fn []
                 (gen/elements
                   (->> [""
                         "?"
                         "?hey"
                         "?hey="
                         "?hey=jude"
                         "?hey=jude&sergeant=pepper"
                         "?hey=jude&sergeant=pepper&color=yellow%20submarine"]
                        (mapcat (juxt identity (partial str "http://test-url.com")))
                        (mapcat (juxt identity #?(:clj  #(java.net.URI. %)
                                                  :cljs uri/parse)))
                        (concat [" " "   " "=" "? " "?  " "?="]))))))
(s/def ::query-params
  (s/map-of (s/or :keyword keyword?
                  :string string?)
            (s/or :string string?
                  :strings (s/coll-of string?))))
(s/def ::query-params-without-vector-values
  (s/map-of (s/or :keyword keyword?
                  :string string?)
            (s/or :string string?
                  :strings string?)))

(defn concat-vector-values
  "Concatenates vector values of the `query-params` map to ';'-separated
   strings while deduplicating their constituent parts.

     `{\"tags\" [\"clojure\" \"scala\"], \"remote\" \"true\"}` ->
     `{\"tags\" \"clojure;scala\", \"remote\" \"true\"}`

   Returns the same `query-params` in case there are no vectored values."
  [query-params]
  (if (some (comp vector? second) query-params)
    (reduce (fn [m [k v]]
              (assoc m k (if (vector? v)
                           (->> v (distinct) (str/join ";"))
                           v)))
            {}
            query-params)
    query-params))

(s/fdef concat-vector-values
  :args (s/cat :query-params ::query-params)
  :ret ::query-params-without-vector-values)

#?(:cljs (declare uri->query-params))

(defn parse-query-string
  "Parses supplied 'www-form-urlencoded' string using UTF-8. For `nil` or
   empty string returns an empty map, otherwise — a \"query params\" map."
  [query-string]
  #?(:clj
     (if-not (empty? query-string)
       (let [parsed (codec/form-decode query-string)]
         (if (string? parsed)
           {(if (str/blank? parsed) " " parsed) ""} ; matches `goog.Uri` output
           (util/map-vals #(or % "") parsed))) ; matches `goog.Uri` output
       {}))
  #?(:cljs
     (let [uri (uri/create nil nil nil nil nil query-string nil false)]
       (uri->query-params uri))))

(s/fdef parse-query-string
  :args (s/cat :query-string (s/nilable string?))
  :ret ::query-params)

(defn uri->query-params
  "Transforms query string of some URI into a \"query params\" map.
   Accepts strings and platform native URI objects as its argument."
  [uri]
  #?(:clj
     (if (string? uri)
       (cond
         (str/blank? uri) (parse-query-string "")
         (= (.charAt ^String uri 0) \?) (parse-query-string (subs uri 1))
         :else (-> (java.net.URI. uri) .getRawQuery parse-query-string))
       (parse-query-string (when uri (.getRawQuery ^java.net.URI uri)))))
  #?(:cljs
     (let [params (-> uri uri/parse .getQueryData)]
       (->> (interleave (.getKeys params) (.getValues params))
            (partition 2)
            (reduce (fn [a [k v]]
                      (if (contains? a k)
                        (update a k #(if (coll? %) (conj % v) [% v]))
                        (assoc a k v)))
                    {})))))

(s/fdef uri->query-params
  :args (s/cat :uri (s/nilable ::string-or-uri))
  :ret ::query-params)

(defn serialize-query-params
  "Serializes a \"query params\" map into URI's query string.
   Filters out query params with no value, i.e. `nil`.
   Results in `nil` for `nil` or empty map."
  [m]
  #?(:clj
     (if (map? m) (-> m (util/remove-nils) (codec/form-encode)) ""))
  #?(:cljs
     (let [usp (js/URLSearchParams.)]
       (run! (fn [[k v]]
               (if (coll? v)
                 (run! (fn [v'] (.append usp (name k) v')) v)
                 (.append usp (name k) v)))
             (js->clj m))
       (.toString usp))))

(s/fdef serialize-query-params
        :args (s/cat :m (s/nilable ::query-params))
        :ret string?)

(defn uri->domain [uri]
  (let [uri (str/trim uri)]
    #?(:cljs
       (try
         (text/not-blank (.getDomain (uri/parse uri)))
         (catch js/Error _)))
    #?(:clj
       (try
         (.getHost (java.net.URI. uri))
         (catch Exception _e
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
         (catch Exception _e
           (error "Failed to parse URI:" uri))))))

(defn has-domain? [uri]
  (not (nil? (uri->domain uri))))

(defn detect-page-type [url]
  (when url
    (let [url                      (sanitize-url url)
          [_ _ domain & remaining] (str/split url #"/")
          handle                   (last remaining)]
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
    "prod"  (str "https://" (name vertical) ".works-hub.com")
    "stage" (str "/?vertical=" (name vertical))
    ;;else
    (str "http://" (name vertical) ".localdomain:8080")))

(defn share-urls [args]
  (let [{:keys [text text-twitter text-linkedin url]} (util/map-vals bidi/url-encode args)]
    {:twitter  (str "https://twitter.com/intent/tweet?text="
                    (or text-twitter text) "&url=" url)
     :facebook (str "http://www.facebook.com/sharer/sharer.php?u=" url)
     :linkedin (str "https://www.linkedin.com/shareArticle?mini=true&url="
                    url "&title=" (or text-linkedin text) "&summary=&origin=")}))
