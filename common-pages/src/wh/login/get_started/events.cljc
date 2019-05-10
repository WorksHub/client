(ns wh.login.get-started.events
  #?(:cljs
     (:require
       [cljs.spec.alpha :as s]
       [wh.pages.core :as pages :refer [on-page-load]])))

#?(:cljs
   (defmethod on-page-load :get-started [db]
     (when-let [page (some-> (get-in db [:wh.db/query-params "redirect"])
                             (keyword))]
       (when (s/valid? :wh.db/page page)
         [[:login/set-redirect page]]))))
