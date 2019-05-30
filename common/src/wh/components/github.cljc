(ns wh.components.github
  (:require
    #?(:cljs [wh.pages.core :refer [load-and-dispatch]])
    [wh.routes :as routes]))

(defn connect-github-button []
  [:div
   [:p "To import issues from GitHub, you need to set your "
    [:a.a--underlined {:href "https://github.com/settings/organizations" :target "_blank" :rel "noopener"}
     "organization membership"]
    " status set to public."]
   [:div.company-edit__integrations
    [:button.button.button--black.button--github
     {:id       "company-edit__integration--github"
      :on-click #?(:cljs #(do (.preventDefault %)
                              (load-and-dispatch [:login [:github/call nil :company]]))
                   :clj nil)}
     [:div]]]])

(defn integrate-github-button
  [{:keys [class label track-context user-type]
    :or {label "Integrate with"}}]
  [:a.button.button--black.button--public.button--github
   {:class class
    :href (routes/path :login :params {:step :github})}
   [:span label] [:div]])
