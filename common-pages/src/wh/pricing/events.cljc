(ns wh.pricing.events
  #?(:cljs
     (:require
       [wh.pages.core :refer [on-page-load]])))

;; TODO The only reason we need this is because the pricing page
;; still needs to load the app for some of its components. Fail.

#?(:cljs
   (defmethod on-page-load :pricing [_]
     [[:wh.pages.core/unset-loader]]))
