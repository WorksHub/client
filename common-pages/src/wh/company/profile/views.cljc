(ns wh.company.profile.views
  (:require
    #?(:cljs [wh.common.logo])
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.company.components.forms.views :refer [rich-text-field]])
    #?(:cljs [wh.components.forms.views :refer [tags-field text-field select-field radio-field logo-field toggle]])
    #?(:cljs [wh.components.overlay.views :refer [popup-wrapper]])
    #?(:cljs [wh.user.subs])
    [clojure.string :as str]
    [wh.common.data.company-profile :as data]
    [wh.common.specs.company :as company-spec]
    [wh.common.specs.tags :as tag-spec]
    [wh.common.text :as text]
    [wh.company.profile.events :as events]
    [wh.company.profile.subs :as subs]
    [wh.components.cards :refer [blog-card]]
    [wh.components.common :refer [link wrap-img img base-img]]
    [wh.components.github :as github]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [issue-card]]
    [wh.components.job :refer [job-card]]
    [wh.components.not-found :as not-found]
    [wh.components.tag :as tag]
    [wh.components.videos :as videos]
    [wh.how-it-works.views :as how-it-works]
    [wh.interop :as interop]
    [wh.pages.util :as putil]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn edit-button
  [editing-atom on-editing]
  #?(:cljs
     [:div.editable--edit-button
      [icon "edit"
       :on-click #(do (reset! editing-atom true)
                      (when on-editing
                        (on-editing)))]]))

(defn editable
  [_args _read-body _write-body]
  (let [editing? (r/atom false)]
    (fn [{:keys [editable? prompt-before-cancel? on-editing on-cancel on-save modal?]
          :or   {modal?                false
                 prompt-before-cancel? false}}
         read-body write-body]
      (let [read-body' [:div.editable read-body
                        (when editable?
                          (if (<sub [::subs/updating?])
                            [:div.editable--loading]
                            [edit-button editing? on-editing]))]
            write-body'
            (if write-body [:div.editable.editable--editing write-body
                            #?(:cljs
                               (when (and editable? @editing?)
                                 [:div.editable--save-cancel-buttons
                                  [:button.button.button--tiny.button--inverted
                                   {:on-click #(when-let [ok? (if prompt-before-cancel?
                                                                (js/confirm "You have made changes to the company profile. If you cancel, these changes will be lost. Are you sure you want to cancel?")
                                                                true)]
                                                 (when on-cancel
                                                   (on-cancel))
                                                 (r/next-tick (fn [] (reset! editing? false))))}
                                   "Cancel"]
                                  [:button.button.button--tiny
                                   {:on-click #(do (when on-save
                                                     (on-save))
                                                   (r/next-tick (fn [] (reset! editing? false))))}
                                   "Save"]]))]
                read-body')]
        [:div.editable-wrapper
         (when (or (not @editing?) modal?)
           read-body')
         (when (and editable? @editing?)
           #?(:cljs
              (if modal?
                [popup-wrapper {:id       :edit-company-info
                                :codi?    false}
                 write-body']
                write-body')))]))))

(defn edit-close-button
  [editing-atom]
    (if (not @editing-atom)
      [edit-button editing-atom nil]
      [:div.editable--edit-button
       [icon "close"
        :on-click #(reset! editing-atom false)]]))

(defn header-link
  [label id]
  [:a.company-profile__header__link
   #?(:clj  {:href (str "#" id)}
      :cljs {:on-click #(.scrollIntoView (.getElementById js/document id))})
   label])

(defn header-links
  []
  [:div.company-profile__header__links
   (header-link "About"       "company-profile__about-us")
   (header-link "Technology"  "company-profile__technology")
   (when (<sub [::subs/show-jobs-link?])
     (header-link "Jobs"        "company-profile__jobs"))
   (header-link "Benefits"    "company-profile__benefits")
   (when (<sub [::subs/how-we-work])
     (header-link "How we work" "company-profile__how-we-work"))])

(defn header
  [_admin-or-owner?]
  (let [editing? (r/atom false)
        new-company-name (r/atom nil)]
    (fn [admin-or-owner?]
      (let [pending-logo (<sub [::subs/pending-logo])]
        [:section
         {:class (util/merge-classes
                   "company-profile__header"
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing"))}
         [editable
          {:editable?                       admin-or-owner?
           :prompt-before-cancel? (boolean (or @new-company-name
                                               pending-logo))
           :on-editing           #(do (reset! editing? true))
           :on-cancel            #(do (reset! editing? false)
                                      (reset! new-company-name false)
                                      (dispatch [::events/reset-pending-logo]))
           :on-save
           #(do
              (reset! editing? false)
              (when-let [changes
                         (not-empty
                           (merge {}
                                  (when (text/not-blank @new-company-name)
                                    {:name @new-company-name})
                                  (when pending-logo
                                    {:logo pending-logo})))]
                (dispatch-sync [::events/update-company changes]))
              (dispatch [::events/reset-pending-logo])
              (reset! new-company-name nil))}
          [:div.company-profile__header__inner
           [:div.company-profile__header__inner__top
            [:div.company-profile__logo
             (wrap-img img (<sub [::subs/logo]) {:w 60 :h 60})]
            [:div.company-profile__name
             (if (<sub [:user/admin?])
               [link (<sub [::subs/name])
                :company-dashboard :id (<sub [::subs/id]) :class "a--underlined"]
               (<sub [::subs/name]))]]]
          [:div.company-profile__header__inner
           #?(:cljs
              [:form.form.wh-formx
               [logo-field
                {:on-change [::events/set-logo]
                 :value (or pending-logo
                            (<sub [::subs/logo]))
                 :loading? (<sub [::subs/logo-uploading?])
                 :on-select-file (upload/handler
                                   :launch [:wh.common.logo/logo-upload]
                                   :on-upload-start [::events/logo-upload-start]
                                   :on-success [::events/logo-upload-success]
                                   :on-failure [::events/logo-upload-failure])}]
               [text-field (or @new-company-name
                               (<sub [::subs/name])
                               "")
                {:type :text
                 :label "Company name"
                 :class "company-profile__header__edit-name"
                 :on-change (fn [v] (reset! new-company-name v))}]])]]
         [header-links]]))))

(defn videos
  [_admin-or-owner?]
  (let [editing? (r/atom false)]
    (fn [admin-or-owner?]
      (let [videos (<sub [::subs/videos])]
        (when (or admin-or-owner? (not-empty videos))
          [:section.company-profile__video
           [:h2.title "Videos"]
           (if (and (empty? videos) (not @editing?))
             [:button.button.button--medium.button--inverted.company-profile__cta-button
              {:on-click #(reset! editing? true)}
              "Link to video content"]
             [:ul.company-profile__videos__list
              [videos/videos {:videos        videos
                              :can-add-edit? (and admin-or-owner? @editing?)
                              :error         (<sub [::subs/video-error])
                              :delete-event  [::events/delete-video]
                              :add-event     [::events/add-video]
                              :update-event  [::events/update-video]}]])
           (when (and admin-or-owner? @editing?)
             [edit-close-button editing?])])))))

(defn blogs
  [admin-or-owner?]
  (let [blogs (<sub [::subs/blogs])]
    (when (or admin-or-owner? (not-empty blogs))
      [:section.company-profile__blogs
       [:h2.title (str "Tech Blogs")]
       (if (empty? blogs)
         [link
          [:button.button.button--medium.button--inverted.company-profile__cta-button
           "Add a blog post"]
          :contribute]
         [:ul
          (into [:div.columns]
                (for [blog blogs]
                  [:div.column.is-half
                   [blog-card blog]]))])])))

(defn github-details
  [admin-or-owner?]
  (let [owners (<sub [::subs/github-orgs])
        repos  (<sub [::subs/repos])]
    [:div.company-profile__github-details
     [:h2.subtitle "On GitHub"]
     [:div.company-profile__github-details__owners
      (for [owner owners]
        ^{:key owner}
        [:div.company-profile__github-details__owner
         [icon "github"]
         [:a.a--underlined {:href (str "https://github.com/" owner) :target "_blank"} owner]])]
     [:div.company-profile__github-details__repos
      [:h3 "Repositories"]
      (for [{:keys [name owner description]} repos]
        ^{:key name}
        [:div.company-profile__github-details__repo
         [:a.a--underlined {:href (str "https://github.com/" owner "/" name) :target "_blank"} name]
         [:span description]])]]))

(defn issues-header
  [admin-or-owner?]
  (let [issues (<sub [::subs/issues])]
    (when (or admin-or-owner? (not-empty issues))
      [:section.company-profile__section--headed
       [:div
        [:h2.title.company-profile__issues-title "Open Source"]
        (when (not-empty issues)
          [github-details admin-or-owner?])]])))

(defn integrate-issues-banner
  []
  [:div.company-profile__banner-cta
   [:img.company-profile__banner-cta__publish-img
    {:src "/images/hiw/header.svg"
     :alt ""}]
   [:div.company-profile__banner-cta__copy
    [:h2 "Use Open Source Issues to find your next hire"]
    [:p "Connect your company Github account and add tasks to your job descriptions to get more qualified applications."]
    [github/integrate-github-button {:class "company-profile__cta-button", :user-type :company}]]])

(defn issues
  [admin-or-owner?]
  (let [issues (<sub [::subs/issues])]
    (when (or admin-or-owner? (not-empty issues))
      [:section
       {:class (util/merge-classes "company-profile__issues"
                                   (when (empty? issues) "company-profile__issues--banner"))}
       (if (not-empty issues)
         [:div
          [:div.is-flex
           [:h2 "Open source issues from this company"]
           [link "View all"
             :issues-for-company-id :company-id (<sub [::subs/id])
             :class "a--underlined"]]
          [:div
           (doall
             (for [issue issues]
               ^{:key (:id issue)}
               [issue-card issue]))]]
         [integrate-issues-banner])])))

(defn job-header
  [admin-or-owner?]
  (let [jobs (<sub [::subs/jobs])
        total-jobs (when (<sub [:user/logged-in?])
                     (<sub [::subs/total-number-of-jobs]))]
    (when (or admin-or-owner?
              (not (<sub [:user/logged-in?]))
              (and (<sub [:user/candidate?]) (not-empty jobs)))
      [:section.company-profile__section--headed
       [:div
        [:h2.title.company-profile__jobs-title (str "Jobs" (when total-jobs (str " (" total-jobs ")")))]]])))

(defn publish-job-banner
  []
  [:div.company-profile__banner-cta
   [:img.company-profile__banner-cta__publish-img
    {:src "/images/homepage/header.svg"
     :alt ""}]
   [:div.company-profile__banner-cta__copy
    [:h2 "Hire software engineers based on their interests and experience"]
    [:p "Through open-source contributions we generate objective ratings to help you hire the right engineers, faster."]
    (link [:button.button.button--medium
           {:id "company-profile__publish-job-btn"}
           "Publish a job"] :create-job)]])

(defn login-to-see-jobs-banner
  []
  [:div.company-profile__banner-cta
   [:img.company-profile__banner-cta__login-img
    {:src "/images/homepage/header.svg"
     :alt ""}]
   [:div.company-profile__banner-cta__copy
    [:h2 "Sign up and see " (<sub [::subs/name]) " roles!"]
    [:p "Discover the best opportunities in tech. We match your skills to great jobs using languages you love."]
    (link [:button.button.button--medium
           {:id "company-profile__get-started-btn"}
           "Get Started"] :get-started)]])

(defn jobs
  [admin-or-owner?]
  (let [jobs       (<sub [::subs/jobs])
        total-jobs (<sub [::subs/total-number-of-jobs])]
    [:section.company-profile__jobs
     (cond (and admin-or-owner? (empty? jobs))
           [publish-job-banner]
           (not (<sub [:user/logged-in?]))
           [login-to-see-jobs-banner]
           :else
           (when (or admin-or-owner? (and (<sub [:user/candidate?]) (not-empty jobs)))
             [:div.company-profile__jobs-list
              [:div.company-jobs__list
               (doall
                 (for [job jobs]
                   ^{:key (:id job)}
                   [job-card job (merge {:public? false}
                                        #?(:cljs
                                           {:liked?            (contains? (<sub [:wh.user/liked-jobs]) (:id job))
                                            :user-is-owner?    admin-or-owner?
                                            :user-has-applied? (some? (<sub [:wh.user/applied-jobs]))}))]))]]))
     (when (<sub [::subs/show-fetch-all?])
       [:button.button.button--inverted.company-profile__all-jobs-button
        {:on-click #(dispatch [::events/fetch-all-jobs])}
        [icon "plus"] (str "Show all " total-jobs " jobs")])]))

(defn pswp-element
  []
  [:div.pswp
   {:tab-index -1
    :role "dialog"
    :aria-hidden "true"}
   [:div.pswp__bg]
   [:div.pswp__scroll-wrap
    [:div.pswp__container
     [:div.pswp__item]
     [:div.pswp__item]
     [:div.pswp__item]]
    [:div.pswp__ui.pswp__ui--hidden
     [:div.pswp__top-bar
      [:div.pswp__counter]
      [:button.pswp__button.pswp__button--close {:title "Close (Esc)"}]
      [:button.pswp__button.pswp__button--share {:title "Share"}]
      [:button.pswp__button.pswp__button--fs    {:title "Toggle fullscreen"}]
      [:button.pswp__button.pswp__button--zoom  {:title "Zoom in/out"}]
      [:div.pswp__preloader
       [:div.pswp__preloader__icn
        [:div.pswp__preloader__cut
         [:div.pswp__preloader__donut]]]]]
     [:div.pswp__share-modal.pswp__share-modal--hidden.pswp__single-tap
      [:div.pswp__share-tooltip]]
     [:button.pswp__button.pswp__button--arrow--left {:title "Previous (arrow left)"}]
     [:button.pswp__button.pswp__button--arrow--right {:title "Next (arrow right)"}]
     [:div.pswp__caption
      [:div.pswp__caption__center]]]]])

(defn photo
  [image open-fn {:keys [_w _h _fit solo? edit? key] :as opts
                  :or {key (:url image)}}]
  [:div
   (merge {:key key
           :class (util/merge-classes "company-profile__photos__img"
                                      (when solo? "company-profile__photos__img--solo"))
           :style (videos/background-image (base-img (:url image) opts))}
          (interop/on-click-fn (open-fn (:index image))))
   (when edit?
     [:div.company-profile__photos__delete
      {:on-click #(do
                    (dispatch [::events/delete-photo (select-keys image [:url :width :height])])
                    (.stopPropagation %))}
      [icon "delete"]])])

(defn img->imgix
  [img]
  (update img :url base-img))

(defn photos
  [admin-or-owner?]
  (let [editing? (r/atom false)]
    (fn [admin-or-owner?]
      (let [images  (<sub [::subs/images])
            open-fn (fn [index] (interop/open-photo-gallery index (map img->imgix images)))
            simages (->> images
                         (take 5)
                         (map-indexed (fn [i si] {:index i :simage si})))]
        (when (or admin-or-owner? (not-empty images))
          [:section.compan-profile__photos
           [:h2.title "Photos"]
           (cond (not-empty images)
                 [:div.company-profile__photos__gallery
                  (doall
                    (for [{:keys [index simage]} simages]
                      (let [first? (zero? index)]
                        (photo simage open-fn
                               {:w     134
                                :h     103
                                :edit? admin-or-owner? :key index}))))]
                 (and admin-or-owner? (not @editing?))
                 [:button.button.button--medium.button--inverted.company-profile__cta-button
                  {:on-click #(reset! editing? true)}
                  "Upload a photo"])
           #?(:cljs
              (when (and admin-or-owner? @editing?)
                [:div.company-profile__photos__add
                 (if (<sub [::subs/photo-uploading?])
                   [:div.company-profile__photos__add--loading]
                   [:input {:type      "file"
                            :on-change (upload/handler
                                         :launch [::events/photo-upload]
                                         :on-upload-start [::events/photo-upload-start]
                                         :on-success [::events/photo-upload-success]
                                         :on-failure [::events/photo-upload-failure])}])]))
           (when (and admin-or-owner? @editing?)
             [edit-close-button editing?])
           (pswp-element)])))))


(defn profile-tag-field
  [_args]
  (let [tags-collapsed?  (r/atom true)]
    (fn [{:keys [label placeholder tag-type tag-subtype]}]
      #?(:cljs
         (let [selected-tag-ids (<sub [::subs/selected-tag-ids tag-type tag-subtype])
               matching-tags (<sub [::subs/matching-tags (merge {:include-ids selected-tag-ids :size 20 :type tag-type}
                                                                (when tag-subtype
                                                                  {:subtype tag-subtype}))])
               tag-search    (<sub [::subs/tag-search tag-type tag-subtype])]
           [:form.wh-formx.wh-formx__layout.company-profile__editing-tags
            {:on-submit #(.preventDefault %)}
            [tags-field
             tag-search
             {:tags               (map #(if (contains? selected-tag-ids (:key %))
                                          (assoc % :selected true)
                                          %) matching-tags)
              :collapsed?         @tags-collapsed?
              :on-change          [::events/set-tag-search tag-type tag-subtype]
              :label              label
              :placeholder        placeholder
              :on-toggle-collapse #(swap! tags-collapsed? not)
              :on-add-tag         #(dispatch [::events/create-new-tag % tag-type tag-subtype])
              :on-tag-click
              #(when-let [id (some (fn [tx] (when (= (:tag tx) %) (:key tx))) matching-tags)]
                 (dispatch [::events/toggle-selected-tag-id tag-type tag-subtype id]))}]
            (when (<sub [::subs/creating-tag?])
              [:div.company-profile__tag-loader [:div]])])))))

(defn tag-list
  [tag-type]
  (when-let [tags (not-empty (<sub [::subs/tags tag-type]))]
    (into [:ul.tags.tags--inline.tags--profile]
          (map (fn [tag] [tag/tag :li tag]) tags))))

(defn about-us
  [_admin-or-owner?]
  (let [new-desc         (r/atom nil)
        existing-tag-ids (r/atom #{})
        editing?         (r/atom false)
        tag-type         :company]
    (fn [admin-or-owner?]
      (let [description      (<sub [::subs/description])
            selected-tag-ids (<sub [::subs/selected-tag-ids tag-type nil])] ;; no subtype
        [:section
         {:class (util/merge-classes
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing")
                   "company-profile__about-us")}
         [editable {:editable?             admin-or-owner?
                    :prompt-before-cancel? (boolean (or @new-desc (not= selected-tag-ids @existing-tag-ids)))
                    :on-editing            #(do
                                              (reset! editing? true)
                                              (reset! existing-tag-ids (<sub [::subs/current-tag-ids tag-type]))
                                              (dispatch [::events/reset-selected-tag-ids tag-type nil])) ;; no subtype
                    :on-cancel             #(do (reset! editing? false)
                                                (reset! new-desc nil))
                    :on-save
                    #(do
                       (reset! editing? false)
                       (when-let [changes
                                  (not-empty
                                    (merge {}
                                           (when @new-desc
                                             {:description-html @new-desc})
                                           (when (not= selected-tag-ids @existing-tag-ids)
                                             {:tag-ids (concat selected-tag-ids (<sub [::subs/current-tag-ids--inverted tag-type]))})))]
                         (dispatch-sync [::events/update-company changes "company-profile__about-us"])))}
          [:div
           [:h2.title "About us"]
           [:h2.subtitle "Who are we?"]
           [:div.company-profile__about-us__description
            [putil/html description]]
           (when-let [tags (not-empty (<sub [::subs/tags tag-type]))]
             [:div.company-profile__about-us__company-tags
              [tag-list tag-type]])]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          [:div
           [:h2.title "About us"]
           [:h2.subtitle "Who are we?"]
           #?(:cljs
              [rich-text-field {:value       (or @new-desc description "")
                                :placeholder "Please enter a description..."
                                :on-change   #(if (= % description)
                                                (reset! new-desc nil)
                                                (reset! new-desc %))}])
           [profile-tag-field
            {:label       "Enter 3-5 tags that make your company stand out from your competitors"
             :placeholder "e.g. flexible hours, remote working, startup"
             :tag-type    tag-type}]]]]))))

(defn tag-display
  [tag-type k data]
  (let  [dsid (str "company-profile__tag-display--" (name k))]
    (fn [tag-type k data]
      (when-let [tags (not-empty (<sub [::subs/tags tag-type k]))]
        [:article
         {:id dsid
          :class (util/merge-classes "company-profile__tag-display"
                                     (when (empty? tags) "company-profile__tag-display--empty"))}
         [:div.company-profile__tag-display__block__info
          [:div.company-profile__tag-display__icon
           [icon (:icon data)]]
          [:h3 (:title data)]
          [:span "(" (count tags) ")"]]
         [:div.company-profile__tag-display__tag-list
          (when (not-empty tags)
            (into [:ul.tags.tags--inline.tags--profile]
                  (map (fn [tag]
                         [tag/tag :li tag])
                       tags)))]
         [:div.company-profile__tag-display__expander
          (interop/toggle-is-open-on-click dsid)
          [icon "roll-down"]]]))))

(defn edit-tag-display
  [tag-type k data]
  [:div
   {:class (util/merge-classes
             "company-profile__edit-tag-display"
             (str "company-profile__edit-tag-display--" (name k)))}
   [:h3 (:title data)]
   [profile-tag-field
    {:placeholder (:placeholder data)
     :tag-type    tag-type
     :tag-subtype k}]])

(def tech-scale-levels 5)

(defn tech-scale
  [{:keys [key atom label scale-labels force-show? editing?]}]
  (when-let [score (or (get @atom key)
                       (<sub [::subs/tech-scale key])
                       (when force-show? 0))]
    (let [scaled-score (* tech-scale-levels score)]
      [:div
       {:class (util/merge-classes "company-profile__tech-scale"
                                   (str "company-profile__tech-scale--" (name key)))}
       [:h3 label]
       [:div.company-profile__tech-scale__selector
        [:div.company-profile__tech-scale__pips
         (for [pip (range tech-scale-levels)]
           ^{:key pip}
           [:div
            {:class (util/merge-classes "company-profile__tech-scale__pip"
                                        (when (> scaled-score pip)
                                          "company-profile__tech-scale__pip--highlighted"))}
            [:div]])]
        (when editing?
          [:div.company-profile__tech-scale__input-wrapper
           [:input {:type      :range
                    :min       0
                    :max       tech-scale-levels
                    :value     scaled-score
                    :on-change #(swap! atom assoc key (double (/ (.. % -target -value) tech-scale-levels)))}]])
        [:div.company-profile__tech-scale__scale-labels
         (for [l scale-labels]
           ^{:key l}
           [:div l])]]])))

(defn technology
  [_admin-or-owner?]
  (let [existing-tag-ids (r/atom #{})
        editing?         (r/atom false)
        new-ati          (r/atom nil)
        new-tech-scales  (r/atom {})
        tag-type         :tech]
    (fn [admin-or-owner?]
      (let [selected-tag-ids (set (reduce concat (vals (<sub [::subs/selected-tag-ids--map tag-type]))))
            ati              (<sub [::subs/additional-tech-info])
            tech-scales      (not-empty (<sub [::subs/tech-scales]))
            tech-scales-view [:div.company-profile__tech-scales
                              [tech-scale {:key          :testing
                                           :atom         new-tech-scales
                                           :label        "Testing"
                                           :scale-labels ["Manual" "Fully automated"]
                                           :force-show?  admin-or-owner?
                                           :editing?     @editing?}]
                              [tech-scale {:key          :ops
                                           :atom         new-tech-scales
                                           :label        "Ops"
                                           :scale-labels ["DevOps" "Dedicated Ops team"]
                                           :force-show?  admin-or-owner?
                                           :editing?     @editing?}]
                              [tech-scale {:key          :time-to-deploy
                                           :atom         new-tech-scales
                                           :label        "Time to deploy"
                                           :scale-labels ["More than 5 hours" "Less than 1 hour"]
                                           :force-show?  admin-or-owner?
                                           :editing?     @editing?}]]]
        [:section
         {:class (util/merge-classes
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing")
                   "company-profile__technology")}
         [editable {:editable?             admin-or-owner?
                    :prompt-before-cancel? (or (not= selected-tag-ids @existing-tag-ids)
                                               (text/not-blank @new-ati)
                                               (not-empty @new-tech-scales))
                    :on-editing            #(do
                                              (reset! editing? true)
                                              (reset! existing-tag-ids #{})
                                              (reset! new-ati nil)
                                              (run! (fn [subtype]
                                                      (swap! existing-tag-ids clojure.set/union (<sub [::subs/current-tag-ids tag-type subtype]))
                                                      (dispatch-sync [::events/reset-selected-tag-ids tag-type subtype])) tag-spec/tech-subtypes))
                    :on-cancel             #(do
                                              (reset! new-ati nil)
                                              (reset! new-tech-scales {})
                                              (reset! editing? false))
                    :on-save
                    #(do
                       (reset! editing? false)
                       (when-let [changes
                                  (not-empty
                                    (merge {}
                                           (when (not= selected-tag-ids @existing-tag-ids)
                                             {:tag-ids (concat selected-tag-ids (<sub [::subs/current-tag-ids--inverted tag-type]))})
                                           (when (text/not-blank @new-ati)
                                             {:additional-tech-info @new-ati})
                                           (when (not-empty @new-tech-scales)
                                             {:tech-scales (merge tech-scales @new-tech-scales)})))]
                         (dispatch-sync [::events/update-company changes "company-profile__technology"]))
                       (reset! new-tech-scales {}))}
          [:div
           [:h2.title "Technology"]
           (doall
             (for [k (keys data/dev-setup-data)]
               ^{:key k}
               [tag-display tag-type k (get data/dev-setup-data k)]))
           [:div.company-profile__technology__additional
            (when ati
              [putil/html ati])]
           (when (or admin-or-owner? tech-scales)
             tech-scales-view)]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          [:div
           [:h2.title "Technology"]
           (doall
             (for [k (keys data/dev-setup-data)]
               ^{:key k}
               [edit-tag-display tag-type k (get data/dev-setup-data k)]))
           [:h2.subtitle "Additional Information"]
           #?(:cljs
              [rich-text-field {:value       (or @new-ati ati "")
                                :placeholder "Please enter any additional technical information..."
                                :on-change   #(if (= % ati)
                                                (reset! new-ati nil)
                                                (reset! new-ati %))}])
           tech-scales-view]]]))))

(defn company-info
  [_admin-or-owner? & [_cls]]
  (let [editing?         (r/atom false)
        new-industry-tag (r/atom nil)
        new-funding-tag  (r/atom nil)
        new-size         (r/atom nil)
        new-founded-year (r/atom nil)]
    (fn [admin-or-owner? & [cls]]
      [:section
       {:class (util/merge-classes "company-profile__company-info"
                                   (when cls (name cls)))}
       [editable {:editable?             admin-or-owner?
                  :modal?                true
                  :prompt-before-cancel? (or @new-industry-tag @new-size @new-founded-year)
                  :on-editing            #(reset! editing? true)
                  :on-cancel             #(do
                                            (dispatch [::events/reset-location-search])
                                            (reset! editing? false))
                  :on-save
                  #(do
                     (reset! editing? false)
                     (when-let [changes
                                (not-empty
                                  (merge {}
                                         (when (or @new-industry-tag @new-funding-tag (<sub [::subs/pending-location]))
                                           (let [current-tags
                                                 (cond->> (<sub [::subs/tags nil])
                                                          @new-industry-tag (remove (comp (partial = :industry) :type))
                                                          @new-funding-tag  (remove (comp (partial = :funding) :type)))]
                                             {:tag-ids (cond-> (set (map :id current-tags))
                                                               @new-industry-tag (conj @new-industry-tag)
                                                               @new-funding-tag  (conj @new-funding-tag))}))
                                         (when @new-size
                                           {:size @new-size})
                                         (when (and @new-founded-year (text/not-blank @new-founded-year))
                                           {:founded-year @new-founded-year})
                                         (when-let [location (<sub [::subs/pending-location--raw])]
                                           {:locations [location]})))] ;; TODO we only do one locatio for now
                       (dispatch-sync [::events/update-company changes])
                       (dispatch [::events/reset-location-search])))}
        (let [industry     (<sub [::subs/industry])
              funding      (<sub [::subs/funding])
              size         (some-> (<sub [::subs/size]) company-spec/size->range)
              founded-year (<sub [::subs/founded-year])
              location     (some-> (<sub [::subs/location]))]
          (if (not (or industry funding size founded-year location))
            [:div.company-profile__company-info__prompt
             [:i "Edit this section to include information about your company, such as industry and location."]]
            [:ul.company-profile__company-info__list
             (when industry
               [:li [icon "industry"] [tag/tag :div industry]])
             (when funding
               [:li [icon "funding"] [tag/tag :div funding]])
             (when size
               [:li [icon "people"] "People: " size])
             (when founded-year
               [:li [icon "founded"] "Founded: " founded-year])
             (when location
               [:li [icon "location"] location])]))
        [:form.wh-formx.wh-formx__layout
         #?(:cljs
            [:div
             [select-field (or @new-industry-tag (:id (<sub [::subs/industry])))
              {:options   (into [{:id nil, :label "--"}] ;; add unselected
                                (sort-by :label (<sub [::subs/all-tags-of-type :industry])))
               :label     "Select an industry that best describes your company"
               :on-change #(reset! new-industry-tag %)}]
             [:div.company-profile__company-info__industry-check
              [:i "Don't see your industry? " [:a {:href "mailto:hello@functionalworks.com" :target "_blank"} "Get in touch"]]]
             [radio-field (or @new-size (<sub [::subs/size]))
              {:options   (map #(hash-map :id % :label (company-spec/size->range %)) (reverse company-spec/sizes))
               :label     "How many people work for your company?"
               :on-change #(reset! new-size %)}]
             [text-field (or @new-founded-year (<sub [::subs/founded-year]) "")
              {:type      :number
               :label     "When was your company founded?"
               :on-blur   #(when (str/blank? @new-founded-year)
                             (reset! new-founded-year nil))
               :on-change (fn [v] (if v
                                    (reset! new-founded-year v)
                                    (reset! new-founded-year "")))}]
             [select-field (or @new-funding-tag (:id (<sub [::subs/funding])))
              {:options   (into [{:id nil, :label "--"}] ;; add unselected
                                (sort-by :label (<sub [::subs/all-tags-of-type :funding])))
               :label     "What is your company's primary source of funding?"
               :on-change #(reset! new-funding-tag %)}]
             [text-field (or (<sub [::subs/location-search]))
              {:type                 :text
               :label                "What's your company's HQ or primary location?"
               :on-change            [::events/update-location-suggestions]
               :suggestions          (<sub [::subs/location-suggestions])
               :on-select-suggestion [::events/select-location]}]
             (if-let [pending-location (<sub [::subs/pending-location])]
               [:div.company-profile__company-info__location.company-profile__company-info__location--pending
                [:i "New location"]
                [:p pending-location]]
               (when-let [location (<sub [::subs/location])]
                 [:div.company-profile__company-info__location
                  [:i "Current location"]
                  [:p location]]))])]]])))

(defn how-we-work
  [_admin-or-owner?]
  (let []
    (let [editing?        (r/atom false)
          new-how-we-work (r/atom nil)]
      (fn [admin-or-owner?]
        (let [hww (<sub [::subs/how-we-work])]
          (when (or admin-or-owner?
                    (text/not-blank hww))
            [:section
             {:class (util/merge-classes
                       "company-profile__section--headed"
                       (when @editing? "company-profile__section--editing")
                       "company-profile__how-we-work")}
             [editable {:editable?             admin-or-owner?
                        :prompt-before-cancel? @new-how-we-work
                        :on-editing            #(do
                                                  (reset! editing? true)

                                                  )
                        :on-cancel #(do (reset! editing? false)
                                        (reset! new-how-we-work nil))
                        :on-save
                        #(do
                           (reset! editing? false)
                           (when-let [changes
                                      (when (text/not-blank @new-how-we-work)
                                        {:how-we-work @new-how-we-work})]
                             (dispatch-sync [::events/update-company changes "company-profile__how-we-work"])))}
              [:div
               [:h2.title "How we work"]
               [putil/html hww]]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
              [:div
               [:h2.title "How we work"]
               #?(:cljs
                  [rich-text-field {:value       (or @new-how-we-work hww "")
                                    :placeholder "Please enter a description of the company's work style, project management process, product cycle etc..."
                                    :on-change   #(if (= % hww)
                                                    (reset! new-how-we-work nil)
                                                    (reset! new-how-we-work %))}])]]]))))))

(defn benefits
  [_admin-or-owner?]
  (let [editing? (r/atom false)
        existing-tag-ids (r/atom #{})
        tag-type :benefit]
    (fn [admin-or-owner?]
      (let [selected-tag-ids (set (reduce concat (vals (<sub [::subs/selected-tag-ids--map tag-type]))))]
        [:section
         {:class (util/merge-classes
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing")
                   "company-profile__benefits")}
         [editable {:editable?             admin-or-owner?
                    :prompt-before-cancel? (not= selected-tag-ids @existing-tag-ids)
                    :on-editing            #(do
                                              (reset! editing? true)
                                              (reset! existing-tag-ids #{})
                                              (run! (fn [subtype]
                                                      (swap! existing-tag-ids clojure.set/union (<sub [::subs/current-tag-ids tag-type subtype]))
                                                      (dispatch-sync [::events/reset-selected-tag-ids tag-type subtype])) tag-spec/benefit-subtypes))
                    :on-cancel #(reset! editing? false)
                    :on-save
                    #(do
                       (reset! editing? false)
                       (when-let [changes
                                  (not-empty
                                    (merge {}
                                           (when (not= selected-tag-ids @existing-tag-ids)
                                             {:tag-ids (concat selected-tag-ids (<sub [::subs/current-tag-ids--inverted tag-type]))})))]
                         (dispatch-sync [::events/update-company changes "company-profile__benefits"])))}
          [:div
           [:h2.title "Benefits"]
           (doall
             (for [k (keys data/benefits-data)]
               ^{:key k}
               [tag-display tag-type k (get data/benefits-data k)]))]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          [:div
           [:h2.title "Benefits"]
           (doall
             (for [k (keys data/benefits-data)]
               ^{:key k}
               [edit-tag-display tag-type k (get data/benefits-data k)]))]]]))))

(defn publish-toggle
  [enabled?]
  [:section.company-profile__toggle
   #?(:cljs
      [toggle {:value enabled?
               :on-change #(dispatch [::events/publish-profile])}])
   [:div.company-profile__toggle__copy
    [:h3 (if enabled? "Published" "Unpublished")]
    [:span (if enabled?
             "Awesome! Your company profile is live!"
             "Set your profile to 'Published' so that candidates can view it.")]]])

(defn hash-anchor
  [id]
  [:span.company-profile__hash-anchor
   {:id id}])

(defn sticky-nav-bar
  []
  [:div
   {:class (util/merge-classes "company-profile__sticky"
                               "sticky"
                               (when (<sub [:user/logged-in?])
                                 "company-profile__sticky--logged-in"))
    :id "company-profile__sticky"}
   [:div.main
    [:div.company-profile__sticky_logo
     (wrap-img img (<sub [::subs/logo]) {:w 32 :h 32})]
    [header-links]]])

(defn page []
  (let [enabled?        (<sub [::subs/profile-enabled?])
        company-id      (<sub [::subs/id])
        admin-or-owner? (or (<sub [:user/admin?])
                            (<sub [:user/owner? company-id]))]
    [:div
     (if (or enabled? admin-or-owner?)
       [:div.main.company-profile
        (when admin-or-owner?
          [publish-toggle enabled?])
        [header admin-or-owner?]
        [:div.split-content
         [:div.company-profile__main.split-content__main
          [hash-anchor "company-profile__about-us"]
          [about-us admin-or-owner?]
          [company-info admin-or-owner? "company-profile__section--headed is-hidden-desktop"]
          [hash-anchor "company-profile__technology"]
          [technology admin-or-owner?]
          [hash-anchor "company-profile__benefits"]
          [benefits admin-or-owner?]
          [hash-anchor "company-profile__jobs"]
          [job-header admin-or-owner?]
          [jobs admin-or-owner?]]
         [:div.company-profile__side.split-content__side.is-hidden-mobile
          [company-info admin-or-owner?]]]
        [:div.split-content
         [:div.company-profile__main.split-content__main
          [issues-header admin-or-owner?]
          [issues admin-or-owner?]
          [hash-anchor "company-profile__how-we-work"]
          [how-we-work admin-or-owner?]
          [blogs admin-or-owner?]
          [photos admin-or-owner?]
          [videos admin-or-owner?]]
         [:div.company-profile__side.split-content__side.is-hidden-mobile
          (cond
            #?(:cljs (<sub [:user/company?])
               :clj  false)
            [how-it-works/pod--company]
            (empty? (<sub [::subs/issues]))
            [:div] ;; display nothing if no issues
            :else
            [how-it-works/pod--candidate])]]]
       [:div.main.main--center-content
        [not-found/not-found]])
     [sticky-nav-bar]
     [:script (interop/set-class-on-scroll "company-profile__sticky" "sticky--shown"
                                             (if admin-or-owner? 170 100))]]))
