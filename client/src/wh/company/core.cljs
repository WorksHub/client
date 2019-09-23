(ns wh.company.core
  (:require
    [cljs.loader :as loader]
    [re-frame.core :refer [dispatch dispatch-sync reg-event-db]]
    [reagent.core :as reagent]
    [wh.company.applications.views :as applications]
    [wh.company.candidate.db :as candidate-db]
    [wh.company.candidate.events :as candidate-events]
    [wh.company.candidate.views :as candidate]
    [wh.company.create-job.events :as create-job-events]
    [wh.company.create-job.subs]
    [wh.company.create-job.views :as create-job]
    [wh.company.dashboard.events :as dashboard-events]
    [wh.company.dashboard.views :as dashboard]
    [wh.company.edit.db :as edit-db]
    [wh.company.edit.events :as edit-events]
    [wh.company.edit.views :as edit]
    [wh.company.events :as events]
    [wh.company.payment.db :as payment-db]
    [wh.company.payment.views :as payment]
    [wh.company.register.db :as register-db]
    [wh.company.register.views :as register]
    [wh.components.stats.impl]
    [wh.db :as db]
    [wh.user.db :as user]))

(def page-mapping
  {:register-company           register/page
   :create-company             {:page edit/create-page :can-access? user/admin?}
   :edit-company               {:page edit/edit-page :can-access? user/company?}
   :admin-edit-company         {:page edit/edit-page :can-access? user/admin?}
   :company-applications       {:page applications/page :can-access? user/company?}
   :admin-applications         {:page applications/page :can-access? user/admin?}
   :admin-company-applications {:page applications/page :can-access? user/admin?}
   :company-dashboard          {:page dashboard/page :can-access? (some-fn user/admin? user/company?)}
   :create-job                 {:page create-job/page :can-access? (some-fn user/admin? user/company?)}
   :edit-job                   {:page create-job/page :can-access? (some-fn user/admin? user/company?)}
   :payment-setup              {:page payment/page :can-access? db/logged-in?}
   :candidate                  candidate/page})

(reg-event-db
  ::initialize-page-mapping
  (fn [db _]
    (update db ::db/page-mapping merge page-mapping)))

(swap! db/sub-dbs conj
       ::register-db/sub-db
       ::candidate-db/sub-db
       ::edit-db/sub-db
       ::payment-db/sub-db)

(dispatch-sync [::initialize-page-mapping])
(dispatch-sync [::edit-events/initialize-db])
(dispatch-sync [::dashboard-events/initialize-db])
(dispatch-sync [::create-job-events/initialize-db])
(dispatch-sync [::candidate-events/initialize-db])

(db/redefine-app-db-spec!)

;; load stripe js
(let [head   (aget (.getElementsByTagName js/document "head") 0)
      script (.createElement js/document "script")]
  (set! (.-type script) "text/javascript")
  (set! (.-src script) "https://js.stripe.com/v3/")
  (set! (.-onload script) #(reset! payment/stripe-loaded? true))
  (.appendChild head script))

(loader/set-loaded! :company)
