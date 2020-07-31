(ns wh.components.navbar
  (:require #?(:cljs [wh.pages.core :as pages])
            [clojure.string :as str]
            [wh.common.data :as data]
            [wh.common.url :as url]
            [wh.common.user :as user-common]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.components.navbar.navbar :as navbar]
            [wh.components.navbar.tasks :as navbar-tasks]
            [wh.interop :as interop]
            [wh.re-frame :as r]
            [wh.routes :as routes]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn logo-title
  ([vertical]
   (case vertical
     "www" [:div.logo-title [:span.heavy-logo "WORKS"] "HUB"]
     "ai" [:div.logo-title [:span.heavy-logo (str/capitalize vertical)] "WORKS"]
     [:div.logo-title [:span.heavy-logo (str/upper-case vertical)] "WORKS"]))
  ([vertical env]
   [:a.logo-title-container
    {:href (url/vertical-homepage-href env vertical)}
    (logo-title vertical)]))

(defn promo-banner [{:keys [page logged-in?]}]
  (when (and (contains? #{:pricing :how-it-works :issues :homepage :homepage-not-logged-in} page)
             (not logged-in?))
    [:div.navbar__promo-banner.is-open
     {:id "promo-banner"}
     [link "Hiring? Sign up and post your first job for FREE!" :register-company]]))

(defn top-bar
  [_args]
  (let [tasks-open? (r/atom false)]
    (fn [{:keys [env vertical logged-in? query-params page user-type] :as args}]
      (let [candidate? (user-common/candidate-type? user-type)
            company?   (user-common/company-type? user-type)
            admin?     (user-common/admin-type? user-type)
            args       (assoc args
                              :tasks-open? tasks-open?
                              :show-tasks? (= user-type "company"))
            content?   (not (contains? routes/no-nav-link-pages page))]
        [:nav {:class      (util/merge-classes "navbar")
               :id         "wh-navbar"
               :role       "navigation"
               :aria-label "main navigation"}

         [navbar/navbar {:vertical     vertical
                         :page         page
                         :content?     content?
                         :env          env
                         :candidate?   candidate?
                         :company?     company?
                         :admin?       admin?
                         :logged-in?   logged-in?
                         :query-params query-params}]]))))
