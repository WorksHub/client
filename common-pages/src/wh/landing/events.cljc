(ns wh.landing.events
  (:require
    #?(:cljs [wh.pages.core :refer [on-page-load] :as pages])))

#?(:cljs
   (defmethod on-page-load :employers [db]
     [[:wh.pages.core/unset-loader]]))
