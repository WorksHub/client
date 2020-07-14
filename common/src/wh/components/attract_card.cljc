(ns wh.components.attract-card
  (:require [re-frame.core :refer [dispatch]]
            [wh.common.data :as data]
            [wh.components.branding :as branding]
            [wh.components.icons :refer [icon]]
            [wh.components.signin-buttons :as signin-button]
            [wh.interop :as interop]
            [wh.styles.attract-card :as style]
            [wh.util :as util]
            [wh.verticals :as verticals]))

(defn intro
  ([vertical]
   [intro vertical :default])
  ([vertical type]
   [:div (merge (util/smc style/attract-card
                          style/intro
                          [(= type :main-column) style/intro--main-column])
                (util/test-selector "intro-attract"))
    [:div (util/smc style/intro__branding)
     [icon vertical :class style/intro__icon]
     [branding/vertical-title vertical
      {:size :small :multiline? true}]]
    [:p (util/smc style/intro__description)
     [:span (str (get-in data/in-demand-hiring-data [vertical :discover]) " with ")]
     [:span (util/smc style/intro__vertical-title) (verticals/config vertical :platform-name)]]]))

(defn contribute
  ([logged-in?]
   [contribute logged-in? :default])
  ([logged-in? type]
   [:div (merge (util/smc style/attract-card
                          style/contribute
                          [(= type :side-column) style/contribute--side-column])
                (util/test-selector "contribute-attract"))
    [:div (util/smc style/contribute__info)
     [:h2 (util/smc style/contribute__heading) "Write an article"]
     [:p (util/smc style/contribute__copy) "Share your thoughts & expertise with a huge community of users"]
     [:a
      (merge {:class (util/mc style/button)}
             #?(:clj  (when-not logged-in?
                        (interop/on-click-fn
                          (interop/show-auth-popup :contribute [:contribute])))
                :cljs {:on-click #(dispatch (if logged-in?
                                              [:wh.events/nav :contribute]
                                              [:wh.events/contribute]))})) "Write article"]]
    [:div (util/smc style/contribute__illustration)
     [:img {:src "/images/hiw/company/benefits/benefit2.svg"}]]]))

(defn signin
  ([]
   [signin :default])
  ([type]
   [:div (merge (util/smc style/attract-card
                          style/signin
                          [(= type :side-column) style/signin--side-column])
                (util/test-selector "signin-attract"))
    [:h2 (util/smc style/signin__heading) "Sign in"]
    [:p (util/smc style/signin__copy) "to customize your feed & discover cool stuff"]
    [:div (util/smc style/signin__buttons)
     [signin-button/github]
     [signin-button/stack-overflow]
     [signin-button/twitter]
     [signin-button/email]]]))

(defn all-cards [{:keys [vertical logged-in?]}]
  [:div (util/smc style/cards)
   [intro vertical]
   [contribute logged-in?]
   [signin]])
