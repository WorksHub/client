(ns wh.how-it-works.subs
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])
    [re-frame.core :refer [reg-sub]]
    [wh.common.subs])) ;; required for inclusion

(reg-sub
  ::selected-site
  (fn [db _]
    (let [vertical (:wh.db/vertical db)
          default-site (if (= "www" vertical) "company" "candidate")]
      (keyword (get-in db [:wh.db/query-params "site"]
                       default-site)))))

(reg-sub
  ::show-github-buttons?
  :<- [:user/company?]
  :<- [:user/company-connected-github?]
  (fn [[company? connected?] _]
    (and company? (not connected?))))

(reg-sub
  ::show-candidate-buttons?
  :<- [:user/candidate?]
  (fn [candidate? _]
    candidate?))

(reg-sub
  ::show-company-buttons?
  :<- [:user/company?]
  (fn [company? _]
    company?))
