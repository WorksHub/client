(ns wh.subs
  (:require [re-frame.core :refer [reg-sub]]
            [re-frame.registrar :as rf-registrar]
            [wh.common.subs] ;; required for inclusion
            [wh.db :as db]
            [wh.routes :as routes]))

;;; Functions useful in subscriptions

;; Taken from https://lambdaisland.com/blog/11-02-2017-re-frame-form-1-subscriptions
(def <sub (comp deref re-frame.core/subscribe))

(defn run-sub
  "Runs subscription `sub-v` against `db` if it's defined; returns `nil` if not.
   This is useful if a subscription like `:user/admin?` is defined in a module,
   and we want to check for it if that module isn't possibly loaded."
  [db sub-v]
  (let [sub-name (first sub-v)]
    (when-let [sub-fn (rf-registrar/get-handler :sub sub-name)]
      @(sub-fn db sub-v))))

(defn error-sub-key [field]
  (keyword (namespace field) (str (name field) "-error")))

(defn with-unspecified-option
  ([options]
   (with-unspecified-option options "Unspecified"))
  ([options label]
   (constantly (into [{:id nil, :label label}] options))))

;; TODO: Not used anymore. May be safely removed.
(reg-sub ::initial-page?
         (fn [db _]
           (<= (:wh.db/page-moves db) 1)))

(reg-sub ::page
         (fn [db _]
           (:wh.db/page db)))

(reg-sub ::blockchain?
         :<- [:wh/vertical]
         (fn [vertical]
           (db/blockchain? vertical)))

(reg-sub ::loading?
         (fn [db _]
           (::db/loading? db)))

;; TODO: Not used anymore. May be safely removed.
(reg-sub ::scroll
         (fn [db _]
           (::db/scroll db)))

(reg-sub ::display-tracking-consent-popup?
         (fn [db _]
           (not (::db/tracking-consent? db))))

(reg-sub ::ssr-page?
         (fn [db _]
           (::db/server-side-rendered? db)))

(reg-sub ::show-footer?
         (fn [db _]
           (not (contains? routes/no-footer-pages (::db/page db)))))

(reg-sub ::version-mismatch
         (fn [db _]
           (::db/version-mismatch db)))

;; The following subscriptions used to live in `wh.user.subs`,
;; but are needed pervasively throughout the app.

(reg-sub :company/has-permission? ; needed to render menu
         (fn [db [_ permission]]
           (db/has-permission? db permission)))

(reg-sub ::github-app-name
         (fn [db _]
           (:wh.settings/github-app-name db)))
