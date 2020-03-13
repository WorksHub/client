(ns wh.components.pods.companies
  (:require
    [wh.common.text :as txt]
    [wh.components.common :refer [link]]
    [wh.components.forms :as forms]
    [wh.re-frame :as r]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.util :as util]))

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
          [:section {:class (util/merge-classes "pod pod--no-shadow cta-pod" cls)}
           [:div.cta-pod__content
            [:h3.cta-pod__title "Hire with us!"]
            [:h4.cta-pod__subtitle "Create a free profile page for your company."]
            [:img.cta-pod__img.cta-pod__img--company {:src "/images/hiw/company/hiw/hiw4.svg"
                                                      :alt ""}]
            [:p.cta-pod__description "Use this space to connect with our community. Companies with profiles typically get " [:i "20%"]
             " more applications!"]]
           [:form.wh-formx.cta-pod__form
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
            [:button.button.is-full-width.cta-pod__button "Get Started"]]])))))
