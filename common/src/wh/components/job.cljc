(ns wh.components.job
  (:require
    #?(:cljs [wh.components.ellipsis.views :refer [ellipsis]])
    [wh.common.job :as jobc]
    [wh.components.cards :refer [match-circle]]
    [wh.components.common :refer [wrap-img link img]]
    [wh.components.icons :refer [icon]]
    [wh.interop :as interop]
    [wh.re-frame.events :refer [dispatch]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]))

(defn state->candidate-status
  [s]
  (cond (or (= s :approved)
            (= s :pending))  "Pending"
        (= s :get_in_touch)  "Interviewing \u2728"
        (or (= s :pass)
            (= s :rejected)) "Rejected \uD83D\uDE41"
        (= s :hired)         "Hired"))

(defn save-button
  [{:keys [id] :as job}
   {:keys [on-close saved?]}]
  [icon "bookmark"
   :id (str "job-card__like-button_job-" id)
   :class (util/merge-classes "job__icon" "like" (when saved? "selected"))
   :on-click #(dispatch [:wh.events/toggle-job-like job on-close])])

(defn job-card--tags
  [job-tags]
  (into [:ul.tags.tags__job]
        (map (fn [tag] [:li [:a {:href (routes/path :pre-set-search :params {:tag (slug/slug tag)})} tag]])
             job-tags)))

(defn job-card--buttons
  [{:keys [slug id state published] :as job}
   {:keys [user-has-applied? user-is-owner? user-is-company?
           applied? logged-in? small? liked? skeleton? on-close apply-source]}]
  [:div.apply
   (when state
     [:div.state
      [:span.applied-state "Status: " (state->candidate-status state)]])

   [:div.buttons
    [:a.button.button--inverted {:href (routes/path :job :params {:slug slug})}
     (if user-is-owner? "View" "More Info")]

    (let [apply-id (str "job-card__apply-button_job-" id)
          button-opts (merge {:id apply-id}
                             (when (or applied?
                                       (and user-is-company?
                                            (not user-is-owner?)))
                               {:disabled true}))]
      (if (not user-is-owner?)
        (when published
          (let [job-page-path [:job
                               :params {:slug slug}
                               :query-params {"apply" true "apply_source" apply-source}]]
            [:a (if logged-in?
                  {:href (apply routes/path job-page-path)}
                  (interop/on-click-fn
                    (interop/show-auth-popup :jobcard-apply job-page-path)))
             [:button.button button-opts
              (cond applied?
                    "Applied"
                    user-has-applied?
                    "Instant Apply"
                    :else
                    "Apply")]]))
        [:a {:href (routes/path :edit-job :params {:id id})}
         [:button.button "Edit"]]))

    (when (and logged-in? (not skeleton?) small?)
      [save-button job {:on-close   on-close
                        :saved? liked?}])]])

(defn card-perks [remote role-type sponsorship-offered]
  (cond-> [:div.card__perks]
    remote                       (conj [icon "globe"
                                        :class "job__icon--small"]
                                       [:span.card__perks__perk-name "Remote"])
    (not= role-type "Full time") (conj [icon "contract"
                                        :class "job__icon--small"]
                                       [:span.card__perks__perk-name role-type])
    sponsorship-offered          (conj [icon "award"
                                        :class "job__icon--small"]
                                       [:span.card__perks__perk-name "Sponsorship"])))

(defn job-card--header
  [{:keys [id slug logo title display-location salary published] :as job}
   {logo :logo company-name :name}
   {:keys [logged-in? skeleton? liked? small? on-close user-is-owner? perks?]}]
  [:div.job-card__header
   (when (and logged-in? (not skeleton?) (not small?))
     [save-button job {:on-close   on-close
                       :saved? liked?}])
   (when (and logged-in? (not skeleton?) on-close)
     [icon "close"
      :id (str "job-card__blacklist-button_job-" id)
      :class "job__icon blacklist"
      :on-click #(dispatch [:wh.events/blacklist-job job on-close])])
   [:a {:href (when (or published user-is-owner?) (routes/path :job :params {:slug slug}))}
    [:div
     {:class (util/merge-classes "info" (when perks? "info--with-perks"))}
     [:div.basic-info
      [:div.job-title title]

      [:div.salary salary]

      [:div.company-name
       [icon "home"]
       [:span company-name]]
      [:div.location
       [icon "location-round"]
       [:span display-location]]]

     [:div.logo
      (if (or skeleton? (not logo))
        [:div]
        (wrap-img img logo {:alt (str company-name " logo") :w 48 :h 48}))]]]])

(def job-card-class
  {:cards ""
   :list  "full-width"})

(defn job-card
  [{:keys [company tagline tags published score user-score applied role-type
           liked display-salary remuneration remote sponsorship-offered]
    :or   {published true}
    :as   job}
   {:keys [liked? applied? user-is-owner? small? view-type]
    :or   {liked?            (or liked false)   ;; old style job handlers added 'liked' bool to the job itself
           applied?          (or applied false) ;; old style job handlers added 'applied' bool to the job itself
           user-is-owner?    false
           small?            false}
    :as opts}]
  (let [skeleton? (and job (empty? (dissoc job :id :slug)))
        salary    (or display-salary (jobc/format-job-remuneration remuneration))
        job-tags  (if skeleton?
                    (map (fn [_i] (apply str (repeat (+ 8 (rand-int 30)) " "))) (range 6))
                    tags)
        score     (or (:user-score job) score)
        opts      (assoc opts
                         :liked?            liked?
                         :applied?          applied?)
        unpublished-label [:div.card__label.card__label--unpublished.card__label--job
                           "Unpublished"]
        perks? (or remote (not= role-type "Full time") sponsorship-offered)]
    [:div {:class (util/merge-classes "card"
                                      "card--job"
                                      (str "i-cur-" (rand-int 9))
                                      (when remote "card--job--remote")
                                      (when small? "card--small")
                                      (when skeleton? "job-card--skeleton")
                                      (job-card-class view-type))}
     [job-card--header (assoc job :salary salary) company (assoc opts :perks? perks?)]

     [:div.tagline
      (when-not small?
        [:div.tagline-content #?(:cljs [ellipsis tagline]
                                 :clj tagline)])
      (when score
        [match-circle {:score score
                       :text? (not small?)
                       :percentage? small?}])]

     [card-perks remote role-type sponsorship-offered]

     (if (and small? (not published))
       unpublished-label
       [job-card--tags job-tags])

     [job-card--buttons job opts]

     (when (and (not published) (not small?) user-is-owner?)
       unpublished-label)]))

(defn highlight
  [title icon-name body]
  [:div.job__highlight
   [:div.job__highlight_icon
    (when title [icon icon-name])]
   [:div.job__highlight__content
    [:h2.job__highlight__title title]
    body]])
