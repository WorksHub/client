(ns workspaces.login-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [wh.login.views :as views]))

(rf/reg-sub
  :wh.login.subs/magic-email
  (fn [db _]
    (or (:magic-email db) "")))

(rf/reg-sub
  :wh.login.subs/magic-status
  (fn [db _]
    :not-posted))

(rf/reg-event-db
  :wh.login.events/set-magic-email
  (fn [db [_ new-color-value]]
    (assoc (or db {}) :magic-email new-color-value)))


(ws/defcard hello-card
  (ct.react/react-card
    (r/as-element [views/page])))

(defonce init (ws/mount))
