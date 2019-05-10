(ns wh.common.http
  (:require
    [clojure.string :as str]
    [goog.Uri :as uri]
    [goog.uri.utils :as uri-utils])
  (:require-macros
    [clojure.core.strint :refer [<<]]))


(defn staging? [domain]
  (str/ends-with? domain ".herokuapp.com"))

(defn subdomain-suffix [domain]
  (cond
    (= domain "localhost") "localdomain"
    (staging? domain)      domain
    :otherwise             (str/replace domain #"^[^.]+\." "")))

(defn vertical-url [vertical]
  (let [current js/window.location.href
        scheme (uri-utils/getScheme current)
        domain (uri-utils/getDomain current)
        prefix (when-not (staging? domain)
                 (str vertical "."))
        suffix (subdomain-suffix domain)
        colon-port (when-let [port (uri-utils/getPort current)]
                     (str ":" port))
        query-params (when (staging? domain)
                       (str "?vertical=" vertical))]
    (<< "~{scheme}://~{prefix}~{suffix}~{colon-port}~{query-params}")))

(defn url-encode [url]
  (some-> url str (js/encodeURIComponent) (.replace "+" "%20")))
