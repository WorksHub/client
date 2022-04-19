;; This namespace serves as a re-frame counterpart to routes.cljc. It
;; requires all the namespaces containing individual pages, defines an
;; initial route-to-page mapping, and a current-page component that
;; renders the current page. A Y-junction of sorts.

(ns wh.pages.router
  (:require
    [re-frame.core :refer [dispatch-sync reg-event-db reg-sub]]
    [reagent.core :as r]
    [wh.common.tracking-pixels :as tracking-pixels]
    [wh.components.not-found :as not-found]
    [wh.db :as db]
    [wh.how-it-works.views :as how-it-works]
    [wh.landing-new.views :as home]
    [wh.pages.core :as pages]
    [wh.subs :refer [<sub run-sub]]))

(defn homepage-redirect [db]
  (cond (not (db/logged-in? db))      :homepage-not-logged-in
        (run-sub db [:user/company?]) :company-dashboard
        (run-sub db [:user/admin?])   :admin-applications
        :otherwise                    :homepage-dashboard))

(def initial-page-mapping
  {:homepage               {:redirect-fn homepage-redirect}
   :test-cache             {:redirect-fn homepage-redirect}
   :homepage-not-logged-in home/page
   :how-it-works           how-it-works/page
   :not-found              not-found/not-found-page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge initial-page-mapping)))

(reg-sub
  ::current-page
  (fn [{:keys [::db/page-mapping ::db/page] :as db} _]
    (let [page (pages/resolve-handler db page)
          mapping (get page-mapping page)]
      (if (map? mapping)
        (:page mapping)
        mapping))))

(defn- add-tracking-pixels-for-logged-in-user []
  (let [logged-in? (<sub [:user/logged-in?])]
    (when logged-in?
      (tracking-pixels/add-registration-tracking-pixels))))

(defn current-page []
  (r/create-class
    {:component-did-mount
     add-tracking-pixels-for-logged-in-user

     :component-did-update
     add-tracking-pixels-for-logged-in-user

     :reagent-render
     (fn []
       (let [_    (<sub [:wh/page-params]) ;; this sub causes re-render when `:wh.db/page-params` change
             page (<sub [::pages/page])]
         (when-let [page-handler (<sub [::current-page])]
           (if (contains? #{:job :register} (<sub [::pages/page])) ;; TODO :see_no_evil:
             [page-handler]
             [:div.main-wrapper
              {:class (str "main-wrapper--" (name page))}
              [page-handler]]))))}))
