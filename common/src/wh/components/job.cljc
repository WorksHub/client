(ns wh.components.job
  (:require
    [wh.components.common :refer [wrap-img img]]
    [wh.components.icons :refer [icon]]
    [wh.interop :as interop]
    [wh.re-frame.events :refer [dispatch]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]))

(defn job-card
  [{:keys [company-name tagline title logo display-salary remote sponsorship-offered display-location role-type tags id] :as job}
   {:keys [public? liked? user-has-applied?]
    :or   {public?           true
           liked?            false
           user-has-applied? false}}]
  (let [skeleton? (and job (empty? (dissoc job :id)))
        job-tags (if skeleton?
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
         :class (util/merge-classes "job__icon" "like" (when liked? "selected"))
         :on-click #(dispatch [:wh.events/toggle-job-like job])])
      [:a {:href (routes/path :job :params {:id id})}
       [:div.info
        [:div.logo
         (if (or skeleton? (not logo))
           [:div]
           (wrap-img img logo {:alt (str company-name " logo") :w 48 :h 48}))]
        [:div.basic-info
         [:div.job-title title]
         (when (not public?)
           [:div.company-name company-name])
         [:div.location display-location]
         (cond-> [:div.card__perks]
           remote (conj [icon "job-remote" :class "job__icon--small"] "Remote")
           (not= role-type "Full time") (conj [icon "profile" :class "job__icon--small"] role-type)
           sponsorship-offered (conj [icon "job-sponsorship" :class "job__icon--small"] "Sponsorship"))
         [:div.salary display-salary]]]]]
     (into [:ul.tags.tags__job]
           (map (fn [tag] [:li [:a {:href (routes/path :pre-set-search :params {:tag (slug/slug tag)})} tag]])
                job-tags))
     [:div.tagline tagline]
     [:div.apply
      [:div.buttons
       [:a.button {:href (routes/path :job :params {:id id})}
        "More Info"]
       [:button.button (if public?
                    (interop/on-click-fn
                      (interop/show-auth-popup :jobcard-apply [:job :params {:id id} :query-params {:apply true}]))
                    {:href (routes/path :job :params {:id id} :query-params {:apply true})})
        (if user-has-applied?
          "1-Click Apply"
          "Easy Apply")]]]]))
