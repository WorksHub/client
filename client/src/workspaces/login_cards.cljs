(ns workspaces.login-cards
  (:require [nubank.workspaces.card-types.react :as ct.react]
            [nubank.workspaces.core :as ws]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [wh.login.views :as views]))

(defn reagent-card [c]
  (ct.react/react-card
    (r/as-element c)))

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
  (reagent-card [views/magic-form]))

(defonce init (ws/mount))

