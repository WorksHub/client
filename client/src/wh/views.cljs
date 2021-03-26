(ns wh.views
  (:require ["smoothscroll-polyfill"]
            [clojure.walk :as walk]
            [reagent.core :as r]
            [wh.components.banner :as banner]
            [wh.components.error.subs :as error-subs]
            [wh.components.error.views :refer [global-status-box]]
            [wh.components.footer :as footer]
            [wh.components.icons :refer [icon]]
            [wh.components.loader :refer [loader]]
            [wh.components.navbar.navbar :as navbar]
            [wh.logged-in.apply.views :as apply-views]
            [wh.pages.router :refer [current-page]]
            [wh.subs :as subs :refer [<sub]]
            [wh.user.views :as user-views]))

(defn version-mismatch []
  [:div.version-mismatch
   [icon "codi"]
   [:h1 "Sorry to interrupt you..."]
   [:p "...but we've just released a new version of our platform and we need you to reload the page so you can use it."]
   [:button.button.button--medium {:on-click #(js/window.location.reload true)}
    "Reload"]])

(defn main-panel []
  (let [page             (<sub [:wh.pages.core/page])
        user-type        (<sub [:user/type])
        ;; specify links on the menu that should be restricted
        restricted-links (when-not (<sub [:company/has-permission?
                                          :can_see_applications])
                           #{:company-applications})
        query-params     (walk/keywordize-keys (<sub [::subs/query-params]))
        logged-in?       (<sub [:user/logged-in?])]

    (if (<sub [::subs/version-mismatch])
      [version-mismatch]
      [:div.main-panel
       [user-views/consent-popup]
       [apply-views/overlay-apply]

       [banner/banner {:page       page
                       :logged-in? logged-in?}]
       [navbar/top-bar
        {:env               (<sub [::subs/env])
         :vertical          (<sub [::subs/vertical])
         :logged-in?        logged-in?
         :query-params      query-params
         :page              page
         :user-type         user-type
         :restricted-links  restricted-links}]

       [:div.page-container
        (when (<sub [::error-subs/message])
          [global-status-box])
        (if (and (not (<sub [::subs/ssr-page?]))
                 (<sub [::subs/loading?]))
          [:div.main-wrapper
           [:div.loader-wrapper
            [loader]]]
          [current-page])
        (when (<sub [::subs/show-footer?])
          [footer/footer (<sub [::subs/vertical]) logged-in?])]])))

(defonce remove-all-bsl-locks-when-app-loads
  (js/disableNoScroll))
