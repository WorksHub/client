(ns wh.login.get-started.views
  (:require [wh.components.auth :as auth]
            [wh.components.icons :as icons]
            [wh.routes :as routes]
            [wh.styles.register :as styles]))

(defn option [{:keys [img title subtitle href data-test]}]
  [:a {:class styles/option
       :href href
       :data-test data-test}
   [:img {:src img
          :class styles/option__image
          :width 56
          :height 56}]
   [:div [:div {:class styles/option__title} title]
         [:div {:class styles/option__subtitle} subtitle]]
   [icons/icon "chevron_right" :class styles/option__icon]])

(defn options []
  [:div {:class styles/options}
   [option {:title "Candidate"
            :subtitle "I'm looking for a job"
            :img "/images/get_started/candidate-red.svg"
            :href (routes/path :register)
            :data-test "candidate"}]
   [option {:title "Company"
            :subtitle "I'm looking to hire"
            :img "/images/get_started/company-red.svg"
            :href (routes/path :register-company)
            :data-test "company"}]])

(defn page []
  [auth/page
   [auth/card {:type :default}
    [auth/title "Choose account type"]
    [options]]])

