(ns wh.pages.core
  (:require [bidi.bidi :as bidi]
            [cljs.loader :as loader]
            [clojure.string :as str]
            [pushy.core :as pushy]
            [re-frame.core :refer [reg-fx reg-sub reg-event-db reg-event-fx dispatch dispatch-sync]]
            [re-frame.db :refer [app-db]]
            [wh.common.url :as common-url]
            [wh.common.user :as user-common]
            [wh.db :as db]
            [wh.pages.modules :as modules]
            [wh.routes :as routes]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(def default-route {:handler :not-found})
(def default-page-size 24)

;; If you came here looking for 'scroll-to-top' functionality,
;; be aware that although we're setting `scrollTop` of `documentElement`,
;; it's the `onscroll` event of `js/window` that is triggered when a scroll happens

(defn force-scroll-to-x! [x]
  (set! (.-scrollTop (.getElementById js/document "app")) x)
  (set! (.-scrollTop (.-documentElement js/document)) x)
  (set! (.-scrollTop (.-body js/document)) x))

(defn set-page-title! [page-name vertical]
  (set!
    (.-title js/document)
    (str page-name " | " (get-in verticals/vertical-config [vertical :platform-name]))))

(defn force-scroll-to-top! []
  (force-scroll-to-x! 0))

(defmulti on-page-load
  "Returns a vector of events to dispatch when a page is shown."
  ::db/page)

;; NB: Other `on-page-load` methods are installed from their events ns,
;;     e.g `wh.blogs.learn.events/on-page-load`.
(defmethod on-page-load :default [_db] [])

(defmulti on-page-browser-nav
  "Returns an event to dispatch upon the on-page browser navigation."
  ::db/page)

;; NB: Other `on-page-browser-nav` methods are installed from their events ns,
;;     e.g `wh.search.events/on-page-browser-nav`.
(defmethod on-page-browser-nav :default [_db] nil)

(defn- module-for [handler]
  (modules/module-for handler (get-in @app-db [:wh.user.db/sub-db :wh.user.db/type])))

(defn resolve-handler [db handler]
  (let [mapping (get-in db [::db/page-mapping handler])]
    (if (map? mapping)
      (if-let [redirect-fn (:redirect-fn mapping)]
        (recur db (redirect-fn db))
        handler)
      handler)))

(defn redirect-when-not-permitted
  "Return the page to redirect to when user is not allowed to
  access the requested page."
  [db handler]
  (let [mapping     (get-in db [::db/page-mapping handler])
        can-access? (if (map? mapping)
                      (:can-access? mapping)
                      (constantly true))]

    (cond
      (can-access? db) nil
      (= can-access? db/logged-in?) :login
      (and (not (db/logged-in? db))
           (= can-access? user-common/company?)) :login
      :else :not-found)))

;; Set current page and page params, if any.
;; NOTE: this is an internal event, meant to be dispatched by
;; pushy only. To programmatically navigate through the app,
;; use the :navigate fx defined below.
(reg-event-fx ::set-page
              db/default-interceptors
              (fn [{db :db} [{:keys [handler params route-params query-params uri] :as _route} history-state]]
                (let [handler   (resolve-handler db handler)
                      page-info {:page-name (routes/handler->name handler)
                                 :vertical  (:wh.db/vertical db)}]
                  (case (redirect-when-not-permitted db handler)
                    :not-found
                    {:db         (assoc db
                                        ::db/page :not-found
                                        ::db/loading? false) ; it might've been set in pre-rendered app-db
                     :page-title page-info}

                    :login
                    (let [current-path (bidi/url-encode (routes/path handler :params params :query-params query-params))
                          login-step   (if (= :company (module-for handler)) :email :root)] ;; if company, show email login
                      {:navigate   [:login :params {:step login-step} :query-params {:redirect current-path}]
                       :page-title page-info})

                    ;; otherwise
                    (let [new-page?     (not= (::db/page db) handler)
                          initial-load? (::db/initial-load? db)
                          scroll        (if history-state (aget history-state "scroll-position") 0)
                          ;; TODO: Come up with a universal way of replacing '+'s in route params' values with `bidi`?
                          route-params' (util/update* route-params :query #(str/replace % "+" " "))
                          query-params' (or (common-url/concat-vector-values query-params) {})
                          new-db        (-> db
                                            (assoc ::db/page handler
                                                   ::db/page-params (merge params route-params' {})
                                                   ::db/query-params query-params'
                                                   ::db/uri uri
                                                   ::db/scroll scroll)
                                            (update ::db/page-moves inc))
                          on-page-load? (nil? history-state)
                          on-page-nav   (when-not (or initial-load?
                                                      ;; NB: This should not normally be a part of the condition,
                                                      ;;     but our custom Pushy history state tx implementation
                                                      ;;     will cause `on-page-load` event on the latest entry
                                                      ;;     in the chain of forward-button events. So I decided
                                                      ;;     to pretend this is a new page load just in order to
                                                      ;;     avoid duplicate events triggering (`on-page-load` +
                                                      ;;     `on-page-browser-nav`).
                                                      on-page-load?)
                                          (on-page-browser-nav new-db))]
                      (cond-> {:db                 new-db
                               :analytics/pageview true
                               :analytics/track    [(str (routes/handler->name handler) " Viewed")
                                                    (merge params query-params route-params)
                                                    true]
                               :dispatch-n         [[:error/close-global]
                                                    on-page-nav
                                                    [::disable-no-scroll]
                                                    [:wh.events/show-chat? true]]}
                              (not initial-load?)
                              (assoc :page-title page-info)
                              ;; This forces scrolling to top when moving forward to a new page,
                              ;; but does nothing when moving backwards. We leverage a built-in
                              ;; `scrollRestoration` to keep & restore the scroll position.
                              (and new-page? (zero? scroll))
                              (assoc :scroll-to-top true)
                              ;; We only fire `on-page-load` events when we didn't receive a back-button
                              ;; navigation (i.e. `history-state` is `nil`). See `pushy/core.cljs`.
                              on-page-load?
                              (update :dispatch-n
                                      #(let [events (cond-> (on-page-load new-db)
                                                            initial-load? (conj [:wh.events/set-initial-load false]))]
                                         (concat % events)))))))))

;; Internal event meant to be triggered by :navigate only.

(reg-sub ::page
         (fn [db _]
           (::db/page db)))

(defn- parse-url [url]
  (let [query-params (common-url/uri->query-params url)]
    (assoc
      (or (bidi/match-route routes/routes url)
          default-route)
      ;; TODO: Shouldn't it also check `url` URI 'path' for a `query`?
      ;;       If true, it makes sense to re-write this via delegating
      ;;       to the shared `wh.common.search/->search-term` fn.
      :params (when (contains? query-params "search")
                {:query (get query-params "search")})
      :query-params query-params
      :uri url)))

(defn load-and-dispatch [[module event]]
  (let [db           @re-frame.db/app-db
        skip-loader? (or (= (first event) :github/call) ;; this is to avoid loader flickering when logging in via GitHub
                         (= (:wh.db/page db) :github-callback) ;; hacky, but we're not in an event
                         (= (:wh.db/page db) :stackoverflow-callback)
                         (= (:wh.db/page db) :twitter-callback)
                         (and (zero? (:wh.db/page-moves db))
                              (:wh.db/server-side-rendered? db)))]
    (when (and (not skip-loader?)
               (not (loader/loaded? module)))
      (dispatch [::set-loader]))
    ;; If we don't wrap the loader/load call in setTimeout and happen to
    ;; invoke this from an /admin/... URL, cljs-base is not yet fully loaded
    ;; by the time we're here.
    ;; See David Nolen's comment in https://dev.clojure.org/jira/browse/CLJS-2264.
    (js/window.setTimeout
      (fn [] (loader/load module
                          #(do
                             (when event
                               (dispatch-sync event))
                             (when-not skip-loader?
                               (dispatch [::unset-loader]))))))))

(reg-fx :load-and-dispatch load-and-dispatch)

(defn get-app-ssr-scroll-value []
  (if-let [app-ssr (js/document.getElementById "app-ssr")]
    (max (.-scrollY js/window) (.-scrollTop app-ssr))
    (max (.-scrollY js/window) 0)))

(defn show-app! []
  (when-let [app-ssr (js/document.getElementById "app-ssr")]
    (let [scroll-value (get-app-ssr-scroll-value)
          app          (js/document.getElementById "app")]
      (.remove (.-classList app) "app--hidden")
      (force-scroll-to-x! scroll-value)
      (js/document.body.removeChild app-ssr))))

(reg-fx :show-app? show-app!)

(defn- set-page [route history-state]
  (let [set-page-event [::set-page route history-state]]
    (if-let [module (module-for (:handler route))]
      (load-and-dispatch [module set-page-event])
      (dispatch set-page-event))))

(reg-fx :load-module-and-set-page #(set-page % nil))

(reg-event-fx
  ::load-module-and-set-page
  db/default-interceptors
  (fn [_ [route]]
    {:load-module-and-set-page route}))

(reg-event-db
  ::disable-no-scroll
  db/default-interceptors
  (fn [db _]
    (js/disableNoScroll)
    db))

(reg-fx
  :page-title
  (fn [{:keys [page-name vertical]}]
    (set-page-title! page-name vertical)))

(reg-event-db
  ::enable-no-scroll
  db/default-interceptors
  (fn [db _]
    (js/setNoScroll "app" true)
    db))

(defn processable-url? [uri]
  (and (not (contains? routes/server-side-only-paths (.getPath uri))) ;; we tell pushy to leave alone server side rendered pages
       ;; while preserving the default behavior
       (pushy/processable-url? uri)))

(defn dispatch-for-route-data? [{:keys [handler] :as route-data}]
  (and route-data (not (contains? routes/nextjs-pages handler))))

(def pushy-instance (pushy/pushy set-page parse-url
                                 :processable-url? processable-url?
                                 :state-fn (fn []
                                             (clj->js {:scroll-position (.-scrollTop (.-documentElement js/document))}))
                                 :dispatch-for-route-data? dispatch-for-route-data?))

(defn navigate
  "Construct a URI from `handler` & `opts` and set it as current via Pushy.
   This will trigger setting ::db/page and ::db/page-params as appropriate."
  [[handler & {:keys [params query-params anchor] :as _opts}]]
  (when-let [token (routes/path handler
                                :params params
                                :query-params query-params
                                :anchor anchor)]
    (pushy/set-token! pushy-instance token "")))

(reg-fx :navigate navigate)

(defn hook-browser-navigation! []
  (pushy/start! pushy-instance))

(reg-event-db
  ::set-loader
  db/default-interceptors
  (fn [db _]
    (assoc db ::db/loading? true)))

(reg-event-fx
  ::unset-loader
  db/default-interceptors
  (fn [{db :db} _]
    {:db        (assoc db ::db/loading? false)
     :show-app? true}))
