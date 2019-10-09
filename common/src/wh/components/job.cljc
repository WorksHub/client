(ns wh.components.job
  (:require
    [wh.common.job :as jobc]
    [wh.components.common :refer [wrap-img link img]]
    [wh.components.icons :refer [icon]]
    [wh.interop :as interop]
    [wh.re-frame.events :refer [dispatch]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]))

(defn job-card
  [{:keys [company tagline title display-salary remuneration remote sponsorship-offered display-location role-type tags slug id published]
    :or   {published true}
    :as   job}
   {:keys [public? liked? user-has-applied? user-is-owner?]
    :or   {public?           true
           liked?            false
           user-has-applied? false
           user-is-owner?    false}}]
  (let [{company-name :name logo :logo} company
        skeleton? (and job (empty? (dissoc job :id :slug)))
        salary    (or display-salary (jobc/format-job-remuneration remuneration))
        job-tags  (if skeleton?
                    (map (fn [_i] (apply str (repeat (+ 8 (rand-int 30)) " "))) (range 6))
                    tags)]
    [:div {:class (util/merge-classes "card"
                                      "card--job"
                                      (str "card-border-color-" (rand-int 9))
                                      (str "i-cur-" (rand-int 9))
                                      (when public? "job-card--public")
                                      (when skeleton? "job-card--skeleton"))}
     [:span
      (when (not public?)
        [icon "like"
         :class (util/merge-classes "job__icon" "like" (when liked? "se
lected"))
         :on-click #(dispatch [:wh.events/toggle-job-like job])])
      [:a {:href (routes/path :job :params {:slug slug})}
       [:div.info
        [:div.logo
         (if (or skeleton? (not logo) public?)
           [:div]
           (wrap-img img logo {:alt (str company-name " logo") :w 48 :h 48}))]
        [:div.basic-info
         [:div.job-title title]
         (when (not public?)
           [:div.company-name company-name])
         [:div.location display-location]
         (cond-> [:div.card__perks]
                 remote                       (conj [icon "job-remote" :class "job__icon--small"] "Remote")
                 (not= role-type "Full time") (conj [icon "profile" :class "job__icon--small"] role-type)
                 sponsorship-offered          (conj [icon "job-sponsorship" :class "job__icon--small"] "Sponsorship"))
         [:div.salary salary]]]]]
     (into [:ul.tags.tags__job]
           (map (fn [tag] [:li [:a {:href (routes/path :pre-set-search :params {:tag (slug/slug tag)})} tag]])
                job-tags))
     [:div.tagline tagline]
     [:div.apply
      [:div.buttons
       [:a.button.button--inverted {:href (routes/path :job :params {:slug slug})}
        (if user-is-owner? "View" "More Info")]
       (when (not user-is-owner?)
         [:button.button (if public?
                           (interop/on-click-fn
                             (interop/show-auth-popup :jobcard-apply [:job :params {:slug slug} :query-params {:apply true}])) ;; TODO upate with slug
                           {:href (routes/path :job :params {:slug slug} :query-params {:apply true})})
          (if user-has-applied?
            "1-Click Apply"
            "Easy Apply")])]]
     (when (not published)
       [:div.card__label.card__label--unpublished.card__label--job
        "Unpublished"])]))

(defn highlight
  [title icon-name body]
  [:div.job__highlight
   [:div.job__highlight__content
    [:h2 title]
    body]
   [:div.job__highlight_icon
    (when title [icon icon-name])]])
