(ns wh.components.github
  (:require
    #?(:cljs [re-frame.core :refer [dispatch]])
    #?(:cljs [wh.events])
    #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
    #?(:cljs [wh.subs :as subs :refer [<sub]])
    #?(:clj [wh.config :as config])))

(defn app-name []
  #?(:clj (config/get-in [:github :app :name]))
  #?(:cljs (<sub [::subs/github-app-name])))

(defn state-query-param []
  #?(:cljs (let [env (<sub [::subs/env])
                 pr-number (some-> (re-find #"-\d+" js/window.location.href)
                                   (subs 1))]
             (when (and (= :stage env) pr-number)
               (str "?state=" pr-number)))))

(defn install-gh-app-url []
  (str "https://github.com/apps/" (app-name) "/installations/new"
       (state-query-param)))

(defn install-github-app
  [{:keys [class label id]
    :or   {label "Integrate with"}}]
  #?(:cljs [:a.button.button--public.button--github.button--github-integration
            (merge
              {:class    class
               :href     (install-gh-app-url)
               :on-click #(dispatch [:company/track-install-gh-clicked])}
              (when id
                {:id id}))
            [:span label] [:div]]))