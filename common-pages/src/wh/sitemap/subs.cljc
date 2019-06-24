(ns wh.sitemap.subs
  (:require
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :wh/sitemap
  (fn [db _]
    (:wh/sitemap db)))
