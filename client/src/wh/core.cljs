(ns ^:dev/always wh.core
  (:require [cljs.loader :as loader]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
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

(defn ^:export init []
  (pages/load-default-style!)
  (pages/hook-browser-navigation!)
  (mount-root))

(dev-setup)
(re-frame/dispatch-sync [:init])

(loader/set-loaded! :cljs-base)
(js/console.log "ok")
