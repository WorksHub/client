(ns wh.common.fx.graphql
  (:require
    [ajax.json :as json]
    [ajax.simple :as ajax-simple]
    [clojure.string :as str]
    [goog.net.ErrorCode :as errors]
    [re-frame.core :refer [reg-fx dispatch]]))

(def compile-query-fn
  (atom
    (fn [query]
      (throw (js/Error. "Venia not loaded" query)))))

(defn mutation? [query]
  (if (string? query)
    (str/starts-with? query "mutation")
    (= (get-in query [:venia/operation :operation/type]) :mutation)))

(defn compile-if-needed [query]
  (if (string? query)
    query
    (@compile-query-fn query)))

(defn graphql-endpoint []
  (str (:wh.db/api-server @re-frame.db/app-db)
       "/api/graphql"))

(defn app-version []
  (let [app-js-url (.getAttribute (js/document.querySelector "#app-js script") "src")
        prefix (str/replace app-js-url #"/js/wh.js$" "")]
    (when (seq prefix)
      (str/replace prefix #"^/" ""))))

(defn make-graphql-request-map
  [{:keys [query variables] :as request}]
  (let [post? (or (mutation? query)
                  (= (:method request) :post))]
    (merge
      {:method          (if post? :post :get)
       :uri             (graphql-endpoint)
       :params          {:query     (compile-if-needed query)
                         :variables (if post? variables (json/write-json-native variables))}
       :format          (json/json-request-format)
       :response-format (json/json-response-format {:keywords? true})
       :with-credentials true
       :timeout         15000 ;; 15s
       :on-success      [:graphql/success]
       :on-failure      [:graphql/failure]}
      (dissoc request :query :variables))))

(defn graphql-success?
  [response]
  (not (contains? response :errors)))

;; For GraphQL, we need to dispatch failure events even when we got a
;; 200 response.  http-fx's implementation isn't flexible enough to
;; support this, so we need to cherry-pick and tweak the relevant
;; function here.  Luckily, it's short.

(defn ajax-xhrio-handler
  "ajax-request only provides a single handler for success and errors"
  [on-success on-failure xhrio [success? response]]
  ; see http://docs.closure-library.googlecode.com/git/class_goog_net_XhrIo.html
  (let [version (app-version)
        header-version (.getResponseHeader xhrio "x-workshub-version")]
    (cond
      #_(and version (not (str/blank? header-version)) (not= version header-version)) #_(dispatch [:graphql/version-mismatch])
      success? (on-success response xhrio)
      :otherwise (let [details (merge
                                 {:uri             (.getLastUri xhrio)
                                  :last-method     (.-lastMethod_ xhrio)
                                  :last-error      (.getLastError xhrio)
                                  :last-error-code (.getLastErrorCode xhrio)
                                  :debug-message   (-> xhrio .getLastErrorCode (errors/getDebugMessage))}
                                 response)]
                   (on-failure details xhrio)))))

(defn graphql-request->xhrio-options
  [{:as   request
    :keys [on-success on-failure]
    :or   {on-success      [:http-no-on-success]
           on-failure      [:http-no-on-failure]}}]
  ;; wrap events in cljs-ajax callback
  (let [api (new js/goog.net.XhrIo)]
    (-> request
        (assoc
          :api     api
          :handler (partial ajax-xhrio-handler
                            #(dispatch (conj (if (graphql-success? %1)
                                               on-success
                                               on-failure) %1))
                            #(dispatch (conj on-failure %1))
                            api))
        (dissoc :on-success :on-failure))))

(defn graphql-effect
  [request]
  (-> request make-graphql-request-map graphql-request->xhrio-options ajax-simple/ajax-request))

(reg-fx :graphql graphql-effect)
