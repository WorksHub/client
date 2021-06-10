(ns ^:dev/always wh.core
  (:require [cljs.loader :as loader]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [wh.common.tracking-pixels :as tracking-pixels]
            [wh.events]
            [wh.pages.core :as pages]
            [wh.subs]
            [wh.views :as views]))

(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (js/console.log "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

;;TODO CH4832 figure out why init is called two times
;;for some reason init function is called twice,
;;we put an atom in order to mitigate this issue"
(defonce init-called? (atom false))

(defn ^:export init []
  (when (= @init-called? false)
    (tracking-pixels/add-registration-tracking-pixels)
    (pages/hook-browser-navigation!)
    (mount-root)
    (reset! init-called? true)))

(dev-setup)
(re-frame/dispatch-sync [:init])

(loader/set-loaded! :wh)
(js/console.log "js app loaded")
