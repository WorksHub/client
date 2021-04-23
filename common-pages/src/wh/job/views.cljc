(ns wh.job.views
  (:require #?(:cljs [wh.components.forms.views :refer [text-field error-component-outdated]])
            #?(:cljs [wh.components.overlay.views :refer [popup-wrapper]])
            #?(:cljs [wh.components.stats.views :refer [stats-item]])
            [clojure.string :as str]
            [wh.common.text :refer [pluralize not-blank]]
            [wh.company.listing.views :refer [company-card]]
            [wh.components.cards :refer [match-circle]]
            [wh.components.common :refer [wrap-img link img]]
            [wh.components.icons :refer [icon]]
            [wh.components.issue :refer [level->str level->icon]]
            [wh.components.job :refer [job-card highlight]]
            [wh.components.modal-publish-job :as modal-publish-job]
            [wh.components.pods.candidates :as candidate-pods]
            [wh.components.promote-button :as promote]
            [wh.interop :as interop]
            [wh.job.components :as jc]
            [wh.job.events :as events]
            [wh.job.subs :as subs]
            [wh.pages.util :as putil]
            [wh.re-frame :as r]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.job :as styles]
            [wh.util :as util]))

#?(:cljs
   (defn- latlng [{:keys [latitude longitude]}]
     (js/google.maps.LatLng. latitude longitude)))

#?(:cljs
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
                                    (.setTitle @marker title)))}))))

(defn- edit-button [can-edit?]
  (if can-edit?
    [link
     [:button.button.button--medium.button--inverted "Edit"]
     :edit-job
     :id (<sub [::subs/id])
     :html-id "job-view__edit-button"]

    [link
     [:button.button.button--medium.button--inverted "Edit"]
     :payment-setup
     :step :select-package
     :query-params {:action "edit"}
     :html-id "job-view__edit-button"]))

(defn buttons-admin-and-company-owners [{:keys [force-view-applications? condensed?]}]
  (let [can-edit?          (<sub [::subs/can-edit-jobs?])
        show-unpublished?  (<sub [::subs/show-unpublished?])
        view-applications? (or (not show-unpublished?) force-view-applications?)
        publishing?        (<sub [::subs/publishing?])
        admin?             (<sub [:user/admin?])
        published?         (<sub [::subs/published?])]
    (cond-> [:div
             (util/smc "job__admin-buttons" [condensed? "job__admin-buttons--condensed"])
             [edit-button can-edit?]
             [modal-publish-job/modal
              {:on-close               #(dispatch [::modal-publish-job/toggle-modal])
               :on-publish             #(dispatch [::events/publish-role])
               :on-publish-and-upgrade #(dispatch [::events/publish-role nil :redirect-to-payment])}]]
            ;;
            show-unpublished?
            (conj [jc/publish-button {:publishing? publishing?
                                      :on-click    [::events/attempt-publish-role]}])
            ;;
            view-applications?
            (conj [jc/view-applications-button {:href (<sub [::subs/view-applications-link])}])

            ;;
            (and admin? published?)
            (conj [promote/promote-button {:id (<sub [::subs/id]) :type :job}]))))

(defn buttons-user [{:keys [id condensed?]}]
  (let [name             (<sub [::subs/company-name])
        slug             (<sub [::subs/company-slug])
        profile-enabled? (<sub [::subs/profile-enabled?])
        show-about?      (and (not condensed?) profile-enabled?)]
    [:div
     (util/smc "job__apply-buttons" [condensed? "job__apply-buttons--condensed"])
     [jc/apply-button {:applied? (<sub [::subs/applied?])
                       :job      (<sub [::subs/apply-job])
                       :id       id}]
     [:<>
      [jc/more-jobs-link {:href         (routes/path :company-jobs :params {:slug slug})
                          :condensed?   condensed?
                          :company-name name}]
      (when show-about?
        [jc/about-company-link {:href         (routes/path :company :params {:slug (<sub [::subs/company-slug])})
                                :company-name name}])]]))

(defn buttons
  ([]
   (buttons {}))
  ([opts]
   (cond
     (not (<sub [::subs/loaded?]))
     [:div.button--skeleton]
     ;; admins and company owners
     (or (<sub [:user/admin?]) (<sub [::subs/owner?]))
     [buttons-admin-and-company-owners opts]
     ;; users
     :else
     [buttons-user opts])))

(defn save-button []
  [icon "bookmark"
   :class (util/merge-classes "job__save"
                              (when (<sub [::subs/liked?]) "job__save--saved"))
   :on-click #(dispatch [:wh.events/toggle-job-like
                         {:id    (<sub [::subs/id])
                          :company-name (<sub [::subs/company-name])
                          :title (<sub [::subs/title])}])])

;; XXX: move dupe to wh.components.overlays?
(defn notes-overlay
  [& {:keys [user-id value on-ok on-close on-change]}]
  #?(:cljs
     [popup-wrapper
      {:id :notes
       :on-ok on-ok
       :on-close on-close
       :button-label "Add Note"}
      [:form.wh-formx.wh-formx__layout
       [text-field
        value
        {:type :textarea
         :label [:span "Notes on this applicant that are relevant to this role"]
         :on-change on-change}]]]))

(defn apply-on-behalf-note [class-prefix]
  (let [note (<sub [::subs/note])]
    [:div.is-flex
     {:class (str class-prefix "__apply-on-behalf-note" (when (str/blank? note) " company-application__notes--empty"))}
     [:div.apply-on-behalf__edit-icon
      [icon "edit"]]
     [:div.clickable.apply-on-behalf__note
      {:on-click #(dispatch [::events/show-notes-overlay])}
      (if (str/blank? note)
        "Add applicant notes"
        note)]]))

(defn apply-on-behalf [class-prefix]
  #?(:cljs
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
        (error-component-outdated error {:id "error-on-behalf-of"}))]))

(defn minutes->str [offset]
  (let [diff (- offset (int offset))]
    (int (* diff 60))))

(defn plus-offset->str [offset]
  (let [minutes (minutes->str offset)]
    (str "+" (int offset)
         (when (pos? minutes) (str ":" minutes)))))

(defn minus-offset->str [offset]
  (let [minutes (minutes->str offset)]
    (str (when (zero? offset) "-") (int offset)
         (when (pos? minutes) (str ":" minutes)))))

(defn job-timezone
  ([tz]
   (job-timezone {} tz))
  ([opts {tz :timezone-name gmt :gmt delta :timezone-delta}]
   [:li opts
    (if (some #(not= 0 %) (vals delta))
      (str tz
           " ("
           (minus-offset->str (:minus delta)) "/"
           (plus-offset->str (:plus delta)) " hours)")
      tz)]))

(defn company-header
  []
  [:section.is-flex.job__company-header
   [:div.job__company-header__logo
    (let [logo (<sub [::subs/logo])]
      (if (or logo (<sub [::subs/loaded?]))
        (let [alt      (str (<sub [::subs/company-name]) " logo")
              logo-img (wrap-img img logo {:alt alt :w 80 :h 80 :class "logo" :fit "clamp"})]
          (if (<sub [::subs/profile-enabled?])
            [link logo-img :company :slug (<sub [::subs/company-slug])]
            logo-img))
        [:div.logo--skeleton]))]
   [:div.job__company-header__info
    [:h1.job__header-job-title
     {:data-test "job-title-jobpage"}
     (<sub [::subs/title])]
    (cond
      (<sub [::subs/profile-enabled?])
      [link
       [:h2.job__header-company-link (<sub [::subs/company-name])]
       :company :slug (<sub [::subs/company-slug])]

      (<sub [:user/admin?])
      [link
       [:h2.job__header-company-link (<sub [::subs/company-name])]
       :company-dashboard :id (<sub [::subs/company-id])]

      :else
      [:h2.job__header-company-link (<sub [::subs/company-name])])
    (let [remote?   (<sub [::subs/remote?])
          regions   (<sub [::subs/region-restrictions])
          timezones (<sub [::subs/timezone-restrictions])]
      [:h3.job__header-company-location
       (if remote?
         [:div
          [icon "globe" :class "job__icon--small"]
          (cond
            regions   [:span (util/smc styles/remote-info)
                       "Remote within " (interpose ", " (map str (take 3 regions)))]
            timezones [:span (util/smc styles/remote-info)
                       "Remote within "
                       [:ul (util/smc styles/timezones-list)
                        (interpose ", " (map job-timezone (take 3 timezones)))]]
            :else     "Remote | Worldwide")]

         [:div (<sub [::subs/location])])])]

   [:div.job__company-header__last-modified
    (<sub [::subs/last-modified])]
   (when (<sub [::subs/like-icon-shown?])
     [:div.job__company-header__like
      [save-button]])])

(defn tagline
  []
  (let [tagline (<sub [::subs/tagline])]
    [:section.job__tagline
     {:data-test "job-tagline"}
     tagline]))

(defn job-stats
  []
  #?(:cljs
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
                         (<sub [::subs/stats-item :applications]))]]))

(defn company-action
  []
  (let [matching-users (<sub [::subs/matching-users])]
    [:section.job__company-action
     (if (and matching-users (> matching-users 0))
       [:p "We have " matching-users " active " (pluralize matching-users "member") " with 75%+ match rates for this role. What are you waiting for?"]
       [:p "Calculating your matches..."])
     [buttons]]))

(defn admin-action
  []
  [:section.job__admin-action
   [buttons {:force-view-applications? true}]
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
          [match-circle {:score score
                         :text? true}]]))
     [buttons {:id "candidate-action-box"}]]))

(defn create-skeleton-tags []
  (map (fn [i]
         {:label (apply str (repeat (+ 8 (rand-int 30)) " "))
          :_key   i
          :slug  ""})
       (range 6)))

(defn job-highlights
  []
  [:section.job__job-highlights
   (let [salary (<sub [::subs/salary])]
     [highlight
      {:title     (when salary "Salary")
       :icon-name "job-money"
       :children  [:div.job__salary
                   {:data-test "job-salary"}
                   salary]}])
   ;;
   (let [regions   (<sub [::subs/region-restrictions])
         timezones (<sub [::subs/timezone-restrictions])]
     (when (or timezones regions)
       [highlight
        {:title      "Remote within"
         :icon-name  "world"
         :icon-class styles/highlight__icon
         :children   [:div
                      [:ul (util/smc styles/highlight__content)
                       (map (fn [r] [:li (util/smc styles/highlight__list__element) r])
                            regions)]

                      [:span
                       (util/smc "job__highlight__title")
                       "Timezones"]
                      [:ul (util/smc styles/highlight__content)
                       (map
                         (partial job-timezone
                                  (util/smc styles/highlight__list__element))
                         timezones)]]}]))
   ;;
   (let [role-type (<sub [::subs/role-type])]
     [highlight
      {:title     (when role-type "Contract type")
       :icon-name "job-contract"
       :children  [:div.job__contract-type
                   [:div role-type]
                   (when (<sub [::subs/sponsorship-offered?])
                     [:div "Sponsorship offered"])
                   (when (<sub [::subs/remote?]) [:div "Remote working"])]}])
   ;;

   (let [tags (<sub [::subs/tags])]
     [highlight
      {:title     (when tags "Technologies & frameworks")
       :icon-name "job-tech"
       :children  [:div.job__technology
                   [:div.row.summary__row__with-tags
                    (let [skeleton? (nil? tags)
                          tags      (if skeleton? (create-skeleton-tags) tags)]
                      (into [:ul.tags.tags--inline]
                            (map (fn [{:keys [label _key] :as tag}]
                                   [:li {:class (when skeleton? "tag--skeleton")
                                         :key   (or _key label)}
                                    label])
                                 tags)))]]}])
   ;;
   (when-let [benefits (<sub [::subs/benefits])]
     [highlight
      {:title     "Benefits & perks"
       :icon-name "job-benefits"
       :children  [:div.job__benefits
                   [:div.row
                    (into [:ul]
                          (for [item benefits]
                            [:li (merge (util/smc styles/highlight__list__element)
                                        {:key item})
                             item]))]]}])])

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
     (if (and logo (<sub [::subs/loaded?]))
       (wrap-img img logo {:alt (str (<sub [::subs/company-name]) " logo") :w 40 :h 40 :class "logo"})
       [:div.logo--skeleton]))
   [:p.cta
    "Interested in seeing how " (<sub [::subs/company-name]) " work? Contribute to their Open Source."]
   (for [item (<sub [::subs/issues])]
     ^{:key (:id item)}
     [issue item])
   [link "View All Issues" :issues-for-company-id :company-id (<sub [::subs/company-id]) :class "button"]])

(defn content
  [_body]
  (let [id (name (gensym))]
    (fn [body]
      [:div.job__content
       {:id id}
       body])))

(defn information
  []
  [:section.job__information
   (let [description (<sub [::subs/description])]
     [:div.job__job-description
      [:h2.job__job-description-title (when description "Role overview")]
      [content [putil/html description]]])
   (let [description  (<sub [::subs/location-description])
         position     (<sub [::subs/location-position])
         address      (<sub [::subs/location-address-parts])
         description? (and (string? description) (not-blank description))
         address?     (and (or position (seq address)) (<sub [::subs/show-address?]))
         remote?      (<sub [::subs/remote?])]
     (when (and (or description? address?) (not remote?))
       [:div.job__location
        [:h2 "Location"]
        [content
         [:div
          (when description?
            [putil/html description])
          (when (and address? (not remote?))
            [:div.is-flex
             #?(:cljs (when (and (<sub [:google/maps-loaded?]) position (<sub [::subs/show-address?]))
                        [google-map {:position position, :title (<sub [::subs/company-name])} "job__map"]))
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
                          :message (str "Engineers who find a new job through " (<sub [:wh/platform-name]) " average a 15% increase in salary.")}]))
        loaded? (<sub [::subs/loaded?])]
    [:section.is-flex.job__lower-cta.is-hidden-mobile
     {:class (when-not loaded? "job__lower-cta--skeleton")}
     [:div.is-flex
      [icon (if loaded? "codi" "circle")]
      (when loaded?
        [:span message])]
     [buttons {:id id :condensed? true}]]))

(defn other-roles
  []
  (let [jobs         (<sub [::subs/recommended-jobs])
        logged-in?   (<sub [:user/logged-in?])
        has-applied? (some? (<sub [:user/applied-jobs]))
        company-id   (<sub [:user/company-id])
        admin?       (<sub [:user/admin?])]
    [:div.job__other-roles
     [:h2 "Other roles that might interest you"]
     [:div.card-grid-list
      (doall (for [job jobs]
               ^{:key (:id job)}
               [job-card job {:logged-in?        logged-in?
                              :user-has-applied? has-applied?
                              :user-is-company?  (or admin? (= company-id (:company-id job)))
                              :user-is-owner?    (= company-id (:company-id job))
                              :apply-source      "job-page-other-job"}]))]]))

(defn apply-sticky
  []
  [:div.job__apply-sticky.is-hidden-desktop.is-flex.sticky
   {:id "job__apply-sticky"}
   [:div.job__apply-sticky__logo
    (if-let [logo (<sub [::subs/logo])]
      (wrap-img img logo {:alt (str (<sub [::subs/company-name]) " logo") :w 24 :h 24 :class "logo"})
      [icon "codi"])]
   [:div.job__apply-sticky__title (<sub [::subs/title])]
   (let [{:keys [text options]} (jc/apply-button-options {:applied? (<sub [::subs/applied?])
                                                          :job      (<sub [::subs/apply-job])})]
     [:button.button
      (merge {:id "job-apply-sticky__apply-button"}
             options)
      text])])

(defn actions []
  (let [admin?   (<sub [:user/admin?])
        company? (<sub [:user/company?])]
    (cond (<sub [::subs/owner?])
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
          [candidate-action])))

(defn job-details
  []
  (let [admin?   (<sub [:user/admin?])
        company? (<sub [:user/company?])]
    [:div.main-container
     [:div.main.job
      [:div.is-flex
       [:div.job__main
        [company-header]
        [:div.is-hidden-desktop
         [actions]
         [job-highlights]]
        [tagline]
        [information]
        [company-card (<sub [::subs/company-card])]
        (when (<sub [::subs/show-issues?])
          [:div.is-hidden-desktop
           [company-issues]])
        (when-not (or admin? company? (<sub [::subs/applied?]))
          [lower-cta])]
       [:div.job__side.is-hidden-mobile
        [actions]
        [job-highlights]
        [candidate-pods/candidate-cta]
        (when (<sub [::subs/show-issues?])
          [company-issues])]]
      [:div.is-hidden-desktop [candidate-pods/candidate-cta]]
      [other-roles]
      (when (<sub [::subs/show-notes-overlay?])
        [notes-overlay
         :value     (<sub [::subs/note])
         :on-change [::events/edit-note]
         :on-ok     #(dispatch [::events/hide-notes-overlay])
         :on-close  #(dispatch [::events/hide-notes-overlay])])]
     (when (<sub [::subs/show-apply-sticky?])
         [apply-sticky])]))

(defn admin-publish-prompt
  [permissions company-id success-event]
  #?(:cljs
     [popup-wrapper
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
          [:div.job__admin-publish-prompt__loader])]]))

(defn page []
  [:div
   (case (<sub [::subs/error])
     :job-not-found
     [:div.main-wrapper
      [:div.main
       [:h2 {:data-test "job-doesnt-exist"}
        "Sorry! That job is either no longer live or never existed."]
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
   (when (<sub [:wh.job/show-admin-publish-prompt?])
     [admin-publish-prompt
      (<sub [::subs/company-permissions])
      (<sub [::subs/company-id])
      nil])
   ;; TODO add script for SSR version to detect `apply=true` and trigger auth popup
   [:script (interop/set-class-on-scroll "job__apply-sticky" "sticky--shown" 160)]
   ;; tracking pixels
   ;; TEMPORARILY DISABLE THE TRACKING PIXEL UNTIL WE KNOW WHERE IT NEEDS TO GO
   #_(when-let [adzuna-tracking-url (<sub [::subs/adzuna-tracking-url])]
       [:iframe {:src adzuna-tracking-url :scrolling "no" :frameborder "0" :width "1" :height "1"}])])
