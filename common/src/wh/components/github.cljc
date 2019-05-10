(ns wh.components.github
  #?(:cljs (:require [wh.pages.core :refer [load-and-dispatch]])))

(defn connect-github-button []
  [:div
   [:p "To import issues from GitHub, you need to set your "
    [:a.a--underlined {:href "https://github.com/settings/organizations" :target "_blank" :rel "noopener"}
     "organization membership"]
    " status set to public."]
   [:div.company-edit__integrations
    [:button.button.company-edit__integration
     {:id       "company-edit__integration--github"
      :on-click #?(:cljs #(do (.preventDefault %)
                              (load-and-dispatch [:login [:github/call nil :company]]))
                   :clj nil)}
     [:img {:src "/images/company/github.svg"}]]]])

(defn integrate-github-button [class]
  [:button.button.button--black.button--public.button--github
   {:class class
    :on-click #?(:cljs #(do (.preventDefault %)
                            (load-and-dispatch [:login [:github/call nil :company]]))
                 :clj nil)}
   [:span "Integrate with"] [:div]])
