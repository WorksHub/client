(ns wh.components.pods.companies
  (:require
    [wh.common.text :as txt]
    [wh.components.common :refer [link]]
    [wh.components.forms :as forms]
    [wh.re-frame :as r]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]))

(defn company-cta
  [& [cls]]
  (when-not (<sub [:user/logged-in?])
    [:section
     {:class (util/merge-classes "pod"
                                 "pod--no-shadow"
                                 "company-profile__company-cta"
                                 cls)}
     [:h3 "Get involved - create a free profile page for your company"]
     [:div.is-flex
      [:p "Companies with profiles typically get 20% more applications. Start building your hiring pipeline immediately."]
      [link [:button.button.is-full-width.is-hidden-mobile "Get Started"] :get-started]
      [:div.company-profile__company-cta__img
       [:img {:src "/images/hiw/company/hiw/hiw4.svg"
              :alt ""}]]]
     [link [:button.button.is-full-width.is-hidden-desktop "Get Started"] :get-started]]))

(defn create-company-form-error->error-str
  [e]
  (case e
    "company-with-same-name-already-exists" "A company with this name already exists. Please use a unique company name."
    "duplicate-user"                        "A user with this email already exists. Please use a unique email address."
    (str "An unknown error occurred (" e "). Please contact support.")))

(defn company-cta-with-registration
  [_redirect & [_cls]]
  (let [user-name    (r/atom nil)
        user-email   (r/atom nil)
        company-name (r/atom nil)]
    (fn [redirect & [cls]]
      (when-not (<sub [:user/logged-in?])
        (let [existing-params (<sub [:wh/query-params])
              error           (get existing-params "create-company-form__error")]
          [:section
           {:class (util/merge-classes "pod"
                                       "pod--no-shadow"
                                       "company-profile__company-cta"
                                       cls)}
           [:h3 "Get involved - create a free profile page for your company"]
           [:div.company-profile__company-cta__img.is-hidden-mobile
            [:img {:src "/images/hiw/company/hiw/hiw4.svg"
                   :alt ""}]]
           [:div.company-profile__company-cta__form-container.is-flex
            [:p "Use this space to connect with our community. Companies with profiles typically get 20% more applications!"]
            [:form.wh-formx
             {:action (routes/path :create-company-form :query-params {:redirect (name redirect)})
              :method "post"}
             [forms/text-field (merge {:label        "* Your name"
                                       :hide-error?  true
                                       :name         "create-company-form__user-name"
                                       :value        (or @user-name
                                                         (get existing-params "create-company-form__user-name"))
                                       :force-error? (txt/not-blank error)}
                                      #?(:cljs
                                         {:on-change #(reset! user-name (.. % -target -value))}))]
             [forms/text-field (merge {:label        "* Company name"
                                       :hide-error?  true
                                       :name         "create-company-form__company-name"
                                       :value        (or @company-name
                                                         (get existing-params "create-company-form__company-name"))
                                       :force-error? (txt/not-blank error)}
                                      #?(:cljs
                                         {:on-change #(reset! company-name (.. % -target -value))}))]
             [forms/text-field (merge {:label "* Your email"
                                       :name  "create-company-form__user-email"
                                       :value (or @user-email
                                                  (get existing-params "create-company-form__user-email"))
                                       :error (when (txt/not-blank error)
                                                (create-company-form-error->error-str error))}
                                      #?(:cljs
                                         {:on-change #(reset! user-email (.. % -target -value))}))]
             [:button.button.is-full-width "Register"]]]])))))
