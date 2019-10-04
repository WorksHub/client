(ns wh.jobs.job.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as r]
    [wh.common.data :refer [get-manager-name]]
    [wh.common.re-frame-helpers :refer [merge-classes]]
    [wh.common.text :refer [pluralize]]
    [wh.components.cards.views :refer [match-circle job-card]]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.ellipsis.views :refer [ellipsis]]
    [wh.components.footer :as footer]
    [wh.components.forms.views :refer [text-field error-component]]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [issue-card level->str level->icon]]
    [wh.components.job :refer [highlight]]
    [wh.components.overlay.views :refer [popup-wrapper]]
    [wh.components.stats.views :refer [stats-item]]
    [wh.events :as common-events]
    [wh.interop :as interop]
    [wh.jobs.job.events :as events]
    [wh.jobs.job.subs :as subs]
    [wh.pages.util :as putil]
    [wh.routes :as routes]
    [wh.subs :as subs-common :refer [<sub]]
    [wh.user.subs :as user-subs]))

(def content-height-offset 320)

(defn- latlng [{:keys [latitude longitude]}]
  (js/google.maps.LatLng. latitude longitude))

(defn google-map [props class]
  (let [gmap (r/atom nil)
        marker (r/atom nil)]
    (r/create-class
     {:reagent-render (fn [props] [:div {:class class}])
      :component-did-mount (fn [this]
                             (let [{:keys [zoom position title]} (r/props this)
                                   map-canvas (r/dom-node this)
                                   js-position (latlng position)
                                   map-options (clj->js {"center" js-position
                                                         "zoom"   (or zoom 16)})
                                   gmap-object (js/google.maps.Map. map-canvas map-options)
                                   marker-options (clj->js {"position" js-position
                                                            "map" gmap-object
                                                            "title" title})
                                   gmap-marker (js/google.maps.Marker. marker-options)]
                               (reset! gmap gmap-object)
                               (reset! marker gmap-marker)))
      :component-did-update (fn [this]
                              (let [{:keys [zoom position title]} (r/props this)
                                    js-position (latlng position)]
                                (.setCenter @gmap js-position)
                                (.setPosition @marker js-position)
                                (.setZoom @gmap zoom)
                                (.setTitle @marker title)))})))



(defn apply-button
  ([]
   (apply-button {}))
  ([{:keys [id force-view-applications?]}]
   (cond
     (not (<sub [::subs/loaded?]))
     [:div.button--skeleton]
     (or (<sub [:user/admin?])
         (<sub [::subs/owner?]))
     (into [:div.job__admin-buttons
            [link "Edit" :edit-job :id (<sub [::subs/id]) :class "button button--medium button--inverted" :html-id "job-view__publish-button"]]
           (concat []
                   (when (<sub [::subs/show-unpublished?])
                     [[:button.button.button--medium
                       {:id "job-view__publish-button"
                        :class (when (<sub [::subs/publishing?]) "button--inverted button--loading")
                        :on-click #(dispatch [::events/publish-role])} "Publish"]])
                   (when (or (not (<sub [::subs/show-unpublished?]))
                             force-view-applications?)
                     [[:a.button.button--medium
                       {:href (<sub [::subs/view-applications-link])
                        :id   "job-view__view-applications-button"} "View Applications"]])))

     (<sub [::subs/applied?])
     [:button.button.button--medium
      {:disabled true}
      "Applied"]
     (not (<sub [:user/logged-in?]))
     [:div
      [:button.button.button--medium
       (merge {:id (str "job-view__logged-out-apply-button" (when id (str "__" id)))}
              (interop/on-click-fn
               (interop/show-auth-popup :jobpage-apply
                                        [:job
                                         :params {:slug (:slug (<sub [::subs/apply-job]))}
                                         :query-params {:apply "true"}])))
       (if (some? (<sub [:wh.user/applied-jobs]))
         "1-Click Apply"
         "Easy Apply")]
      [:button.button.button--medium.button--inverted
       (merge {:id (str "job-view__see-more-button" (when id (str "__" id)))}
              (interop/on-click-fn
               (interop/show-auth-popup :jobpage-apply
                                        [:job
                                         :params {:slug (:slug (<sub [::subs/apply-job]))}])))
       "See More"]]

     :else
     [:button.button.button--medium
      {:id (str "job-view__apply-button" (when id (str "__" id)))
       :disabled (<sub [:user/company?])
       :on-click #(dispatch [:apply/try-apply (<sub [::subs/apply-job])])}
      (if (some? (<sub [:wh.user/applied-jobs]))
        "1-Click Apply"
        "Easy Apply")])))

(defn like-icon [class]
  [icon "job-heart"
   :class (merge-classes class
                         (when (<sub [::subs/liked?]) (str class "--liked")))
   :on-click #(dispatch [::common-events/toggle-job-like
                         {:id    (<sub [::subs/id])
                          :company-name (<sub [::subs/company-name])
                          :title (<sub [::subs/title])}])])

;; XXX: move dupe to wh.components.overlays?
(defn notes-overlay
  [& {:keys [user-id value on-ok on-close on-change]}]
  (popup-wrapper
   {:id :notes
    :on-ok on-ok
    :on-close on-close
    :button-label "Add Note"}
   [:form.wh-formx.wh-formx__layout
    [text-field
     value
     {:type :textarea
      :label [:span "Notes on this applicant that are relevant to this role"]
      :on-change on-change}]]))

(defn apply-on-behalf-note [class-prefix]
  (let [note (<sub [::subs/note])]
    [:div.is-flex
     {:class (str class-prefix "__apply-on-behalf-note" (when (str/blank? note) " company-application__notes--empty"))}
     [:div.apply-on-behalf__edit-icon
      [icon "edit"]]
     [:div.clickable
      {:on-click #(dispatch [::events/show-notes-overlay])}
      [ellipsis (if (str/blank? note)
                  "Add applicant notes"
                  note)]]]))

(defn apply-on-behalf [class-prefix]
  [:div.wh-formx
   {:class (str class-prefix "__apply-on-behalf")}
   [:h2 "Admin applications"]
   [text-field nil
    {:label "Apply on behalf of"
     :value (<sub [::subs/apply-on-behalf])
     :on-change [::events/edit-apply-on-behalf]
     :suggestions (<sub [::subs/apply-on-behalf-suggestions])
     :on-select-suggestion [::events/select-apply-on-behalf]}]
   (when (<sub [::subs/apply-on-behalf-id])
     [apply-on-behalf-note class-prefix])
   [:button.button.button--medium
    {:class (when (<sub [::subs/applying?]) " button--loading button--inverted")
     :disabled (not (<sub [::subs/apply-on-behalf-button-active?]))
     :on-click #(dispatch [::events/apply-on-behalf])}
    "Send"]
   (when (<sub [::subs/applied-on-behalf])
     [:span
      {:class (str class-prefix "__apply-on-behalf-confirmation")}
      "\uD83D\uDC4D Candidate was approved and sent to the company."])
   (when-let [error (<sub [::subs/apply-on-behalf-error])]
     (error-component error {:id "error-on-behalf-of"}))])

(defn company-header
  []
  [:section.is-flex.job__company-header
   (when (<sub [::user-subs/approved?])
     [:div.job__company-header__logo
      (let [logo (<sub [::subs/logo])]
        (if (or logo (<sub [::subs/loaded?]))
          (let [logo-img (wrap-img img logo {:alt (str (<sub [::subs/company-name]) " logo") :w 80 :h 80 :class "logo"})]
            (if (<sub [::subs/profile-enabled?])
              [link logo-img :company :slug (<sub [::subs/company-slug])]
              logo-img))
          [:div.logo--skeleton]))])
   [:div.job__company-header__info
    (when (<sub [::user-subs/approved?])
      (cond
        (<sub [::subs/profile-enabled?])
        [link [:h2.is-underlined (<sub [::subs/company-name])] :company :slug (<sub [::subs/company-slug])]

        (<sub [:user/admin?])
        [link [:h2 (<sub [::subs/company-name])] :company-dashboard :id (<sub [::subs/company-id])]

        :else
        [:h2 (<sub [::subs/company-name])]))
    [:h1 (<sub [::subs/title])]
    [:h3 (if (<sub [::subs/remote?])
           [:div "Remote 🙌" ]
           (<sub [::subs/location]))]]
   (when (<sub [::subs/like-icon-shown?])
     [:div.job__company-header__like
      [like-icon "job__like"]])])

(defn tagline
  []
  (let [tagline (<sub [::subs/tagline])]
    [:section.job__tagline
     [:h2 (when tagline "To sum it up...")]
     [:div tagline]]))

(defn job-stats
  []
  [:section.sidebar__stats.stats
   [:h2 (<sub [::subs/stats-title])]
   [stats-item (merge {:icon-name "views"
                       :caption "Views"}
                      (<sub [::subs/stats-item :views]))]
   [stats-item (merge {:icon-name "like"
                       :caption "Likes"}
                      (<sub [::subs/stats-item :likes]))]
   [stats-item (merge {:icon-name "applications"
                       :caption "Applications"}
                      (<sub [::subs/stats-item :applications]))]])

(defn company-action
  []
  (let [matching-users (<sub [::subs/matching-users])]
    [:section.job__company-action
     (if (> matching-users 0)
       [:p "We have " matching-users " active " (pluralize matching-users "member") " with 75%+ match rates for this role. What are you waiting for?"]
       [:p "Calculating your matches..."])
     [apply-button]]))

(defn admin-action
  []
  [:section.job__admin-action
   [apply-button {:force-view-applications? true}]
   (when (<sub [:user/admin?])
     [apply-on-behalf "job"])])

(defn candidate-action
  []
  (let [score (<sub [::subs/user-score])]
    [:section.job__candidate-action
     (when (and (<sub [:user/logged-in?]) score)
       (let [message (<sub [::subs/candidate-message])]
         [:div.job__candidate-action__score
          {:class (when-not message "job__candidate-action__score--empty")}
          [:span message]
          [match-circle score true]]))
     [apply-button {:id "candidate-action-box"}]]))


(defn job-highlights
  []
  [:section.job__job-highlights
   (let [salary (<sub [::subs/salary])]
     [highlight
      (when salary "Salary") "job-money"
      [:div.job__salary
       salary]])
   ;;
   (let [role-type (<sub [::subs/role-type])]
     [highlight
      (when role-type "Contract type") "job-contract"
      [:div.job__contract-type
       [:div role-type]
       (when (<sub [::subs/sponsorship-offered?]) [:div "Sponsorship offered"])
       (when (<sub [::subs/remote?]) [:div "Remote working"])]])
   ;;
   (let [tags (<sub [::subs/tags])]
     [highlight
      (when tags "Technologies & frameworks") "job-tech"
      [:div.job__technology
       [:div.row.summary__row__with-tags
        (let [skeleton? (nil? tags)
              tags (if skeleton? (map #(hash-map :key % :tag (apply str (repeat (+ 4 (rand-int 20)) " "))) (range 6)) tags)]
          (into [:ul.tags.tags--inline]
                (map (fn [tag]
                       [:li {:class (when skeleton? "tag--skeleton")
                             :key (or (:key tag) tag)}
                        (or (:tag tag) tag)])
                     tags)))]]])
   ;;
   (when-let [benefits (<sub [::subs/benefits])]
     [highlight
      "Benefits & perks" "job-benefits"
      [:div.job__benefits
       [:div.row
        (into [:ul]
              (for [item benefits]
                [:li {:key item} item]))]]])])

(defn issue
  [{:keys [id title repo level]}]
  [:div.issue
   [link title :issue :id id :class "title"]
   [:div.level
    [icon (level->icon level)] (str "Level: " (level->str level))]
   (when-let [language (:primary-language repo)]
     [:ul.tags.tags--inline
      [:li language]])])

(defn company-issues
  []
  [:section.job__company-issues
   (let [logo (<sub [::subs/logo])]
     (if (or logo (<sub [::subs/loaded?]))
       (wrap-img img logo {:alt (str (<sub [::subs/company-name]) " logo") :w 40 :h 40 :class "logo"})
       [:div.logo--skeleton]))
   [:p.cta
    "Interested in seeing how " (<sub [::subs/company-name]) " work? Contribute to their Open Source."]
   (for [item (<sub [::subs/issues])]
     ^{:key (:id item)}
     [issue item])
   [link "View All Issues" :issues-for-company-id :company-id (<sub [::subs/company-id]) :class "button"]])

(defn show-more
  [event]
  [:div.is-full-width.job__show-more-button
   [:div.is-full-width
    (if (<sub [:user/logged-in?])
      {:on-click #(dispatch [event])}
      (interop/on-click-fn
       (interop/show-auth-popup :jobpage-see-more
                                [:job
                                 :params {:id (:id (<sub [::subs/apply-job]))}])))
    [icon "plus"] "Show More"]])

(defn content
  [expanded-sub expanded-event body]
  (let [id (name (gensym))
        show-button? (r/atom false)
        check-height #(when-let [el (.getElementById  js/document id)]
                        (when (< content-height-offset (.-offsetHeight el))
                          (reset! show-button? true)))]
    (r/create-class
     {:component-did-mount  (fn [this] (check-height))
      :component-did-update (fn [this] (check-height))
      :reagent-render
      (fn [expanded-sub expanded-event body]
        (let [description-expanded? (<sub [expanded-sub])]
          [:div.job__content
           {:id id
            :class (when description-expanded? "job__content--expanded")}
           body
           (when (and @show-button? (not description-expanded?))
             [show-more expanded-event])]))})))

;; taken from taoensso.encore; we're not using it to avoid increasing
;; compiled file size (see README)
(defn as-?nblank [x] (when (string?  x) (if (str/blank? x) nil x)))

(defn information
  []
  [:section.job__information
   (let [description (<sub [::subs/description])]
     [:div.job__job-description
      [:h2 (when description "Role overview")]
      [content ::subs/job-description-expanded?
       ::events/expand-job-description
       [putil/html description]]])
   (when-let [company-description (<sub [::subs/company-description])]
     [:div.job__company-description
      [:h2 "About the company"]
      [content ::subs/company-description-expanded?
       ::events/expand-company-description
       [putil/html (<sub [::subs/company-description])]]])
   (let [description  (<sub [::subs/location-description])
         position     (<sub [::subs/location-position])
         address      (<sub [::subs/location-address-parts])
         description? (as-?nblank description)
         address?     (and (or position (seq address)) (<sub [::subs/show-address?]))]
     (when (or description? address?)
       [:div.job__location
        [:h2 "Location"]
        [content ::subs/location-description-expanded?
         ::events/expand-location-description
         [:div
          (when description?
            [putil/html description])
          (when address?
            [:div.is-flex
             (when (and (<sub [:google/maps-loaded?]) position (<sub [::subs/show-address?]))
               [google-map {:position position, :title (<sub [::subs/company-name])} "job__map"])
             (when (and (seq address) (<sub [::subs/show-address?]))
               [:div.job__location__address
                [:h3 "Address"]
                [:a.a--underlined {:target "_blank"
                                   :href   (<sub [::subs/google-map-url])
                                   :rel    "noopener"}
                 (for [part address]
                   ^{:key part}
                   [:span part])]])])]]]))])

(defn lower-cta
  []
  (let [{:keys [id message]}
        (rand-nth (keep identity
                        [(when (<sub [:user/logged-in?])
                           {:id "1-make-sure-to-update-your-profile"
                            :message "Make sure to update your profile with the most relevant skills to maximize your chances of getting this job!"})
                         {:id "2-engineers-who-find-new-job-through-site"
                          :message (str "Engineers who find a new job through " (<sub [:wh.subs/platform-name]) " average a 15% increase in salary.")}]))
        loaded? (<sub [::subs/loaded?])]
    [:section.is-flex.job__lower-cta.is-hidden-mobile
     {:class (when-not loaded? "job__lower-cta--skeleton")}
     [:div.is-flex
      [icon (if loaded? "codi" "circle")]
      (when loaded?
        [:span message])]
     [apply-button {:id id}]]))

(defn other-roles
  []
  (let [jobs (<sub [::subs/recommended-jobs])]
    [:div.job__other-roles
     [:h2 "Other roles that might interest you"]
     [:div.columns
      (doall (for [job jobs]
               ^{:key (:id job)}
               [:div.column [job-card job :public (<sub [:user/public-job-info-only?])]]))]]))

(defn apply-sticky
  []
  [:div.job__apply-sticky.is-hidden-desktop.is-flex.sticky
   {:class (when (<sub [::subs/show-apply-sticky?]) "sticky--shown")}
   [:div.job__apply-sticky__logo
    (if-let [logo (<sub [::subs/logo])]
      (wrap-img img logo {:alt (str (<sub [::subs/company-name]) " logo") :w 24 :h 24 :class "logo"})
      [icon "codi"])]
   [:div.job__apply-sticky__title (<sub [::subs/title])]
   [:button.button
    {:id "job-apply-sticky__apply-button"
     :on-click #(dispatch [:apply/try-apply (<sub [::subs/apply-job]) :jobpage-apply])}
    (if (some? (<sub [:wh.user/applied-jobs]))
      "1-Click Apply"
      "Easy Apply")]])

(defn job-details
  []
  (r/create-class
    {:component-did-mount
     (fn [this]
       (putil/attach-on-scroll-event
         (fn [y]
           (dispatch [::events/set-show-apply-sticky? (> y 160)]))))
     :reagent-render
     (fn []
       (let [admin? (<sub [:user/admin?])
             company? (<sub [:user/company?])
             actions (cond (<sub [::subs/owner?])
                           [:div.section-container
                            [company-action]
                            [job-stats]]
                           admin?
                           [:div.section-container
                            [admin-action]
                            [job-stats]]
                           company? ;; but not owner
                           [:div]
                           :else
                           [candidate-action])]
         [:div.main-container
          [:div.main.job
           [:div.is-flex
            [:div.job__main
             [company-header]
             [:div.is-hidden-desktop
              actions
              [job-highlights]]
             [tagline]
             [information]
             (when (<sub [::subs/show-issues?])
               [:div.is-hidden-desktop
                [company-issues]])
             (when-not (or admin? company? (<sub [::subs/applied?]))
               [lower-cta])]
            [:div.job__side.is-hidden-mobile
             actions
             [job-highlights]
             (when (<sub [::subs/show-issues?])
               [company-issues])]]
           [other-roles]
           (when (<sub [::subs/show-notes-overlay?])
             [notes-overlay
              :value     (<sub [::subs/note])
              :on-change [::events/edit-note]
              :on-ok     #(dispatch [::events/hide-notes-overlay])
              :on-close  #(dispatch [::events/hide-notes-overlay])])]
          [apply-sticky]]))}))

(defn admin-publish-prompt
  [permissions company-id success-event]
  (popup-wrapper
    {:id :admin-publish-prompt
     :codi? false}
    [:div.job__admin-publish-prompt
     [:div.job__admin-publish-prompt__content
      [:h1 "Company requires upgrade in order to publish role:"]
      [:hr]
      [:h1 "Here are the options:"]
      [:div.buttons
       [:a
        {:href (routes/path :create-company-offer :params {:id company-id})}
        [:button.button
         [:span "Create Take-off Offer"]]]]
      [:hr]
      [:p "Always speak to the company and inform them of your intentions. If you're unsure, please seek assistance from your manager or the dev team. "]
      [:button.button.button--inverted
       {:on-click #(dispatch [::events/show-admin-publish-prompt? false])}
       "Cancel"]]
     (when (<sub [::subs/admin-publish-prompt-loading?])
       [:div.job__admin-publish-prompt__loader])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn page []
  [:div
   (case (<sub [::subs/error])
     :no-matching-job
     [:div.main-wrapper
      [:div.main
       [:h2 "Sorry! That job is either no longer live or never existed."]
       [:h3 "If you think this might be a mistake, double check the link, otherwise browse the available jobs on our "
        [link "Job board." :jobsboard :class "a--underlined"]]]]
     :unknown-error
     [:div.main-wrapper
      [:div.main
       [:h2 "There was a problem loading the page, please try again later"]]]
     :unauthorised
     [:div.main-wrapper
      [:div.main
       [:h2 "You don't have the right permissions to see this page."]]]
     [job-details])
   [footer/footer (<sub [::subs-common/vertical]) "footer--job"]
   (when (<sub [:wh.job/show-admin-publish-prompt?])
     [admin-publish-prompt
      (<sub [::subs/company-permissions])
      (<sub [::subs/company-id])
      nil])])
