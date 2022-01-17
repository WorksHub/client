(ns wh.components.github
  (:require
    #?(:clj [wh.config :as config])
    #?(:cljs [re-frame.core :refer [dispatch]])
    #?(:cljs [wh.events])
    #?(:cljs [wh.subs :as subs :refer [<sub]])
    [wh.common.subs]
    [wh.components.icons :refer [icon]]))

(defn app-name []
  #?(:clj (config/get-in [:github :app :name]))
  #?(:cljs (<sub [:wh.subs/github-app-name])))

(defn state-query-param []
  #?(:cljs (let [env (<sub [:wh/env])
                 pr-number (some-> (re-find #"-\d+" js/window.location.href)
                                   (subs 1))]
             (when (and (= :stage env) pr-number)
               (str "?state=" pr-number)))))

(defn install-gh-app-url []
  (str "https://github.com/apps/" (app-name) "/installations/new"
       (state-query-param)))

(defn install-github-app
  [{:keys [class label id]
    :or   {label "Integrate with GitHub"}}]
  #?(:cljs [:a.button.button--public.button--github
            (merge
              {:class    class
               :href     (install-gh-app-url)
               :on-click #(dispatch [:company/track-install-gh-clicked])}
              (when id
                {:id id}))
            [icon "github" :class "button__icon"]
            [:span label]]))
