(ns ^:figwheel-always wh.core
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
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (pages/hook-browser-navigation!)
  (mount-root))

(dev-setup)
(re-frame/dispatch-sync [:init])

(loader/set-loaded! :cljs-base)

(js/window.addEventListener "load" init)
