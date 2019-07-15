(ns wh.company.profile.views
  (:require
    #?(:cljs [wh.common.logo])
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.company.components.forms.views :refer [rich-text-field]])
    #?(:cljs [wh.components.forms.views :refer [tags-field text-field select-field radio-field logo-field]])
    #?(:cljs [wh.components.overlay.views :refer [popup-wrapper]])
    [clojure.string :as str]
    [wh.common.data.company-profile :as data]
    [wh.common.specs.company :as company-spec]
    [wh.common.text :as text]
    [wh.company.profile.events :as events]
    [wh.company.profile.subs :as subs]
    [wh.components.cards :refer [blog-card]]
    [wh.components.common :refer [link wrap-img img]]
    [wh.components.icons :refer [icon]]
    [wh.components.issue :refer [issue-card]]
    [wh.components.not-found :as not-found]
    [wh.components.videos :as videos]
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
                        (if (<sub [::subs/updating?])
                          [:div.editable--loading]
                          [edit-button editing? on-editing])]
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
          [:div.company-profile__name (<sub [::subs/name])]]]
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
       [:div.company-profile__header__links
        (header-link "About"       "company-profile__about-us")
        (header-link "Technology"  "company-profile__technology")
        (when (<sub [::subs/jobs])
            (header-link "Jobs"        "company-profile__jobs"))
        (header-link "Benefits"    "company-profile__benefits")
        (when (<sub [::subs/how-we-work])
        (header-link "How we work" "company-profile__how-we-work"))]]))))

(defn videos
  [_admin-or-owner?]
  (let [editing? (r/atom false)]
    (fn [admin-or-owner?]
      (let [videos (<sub [::subs/videos])]
        [:section.company-profile__video
         [:h2.title (str "Videos (" (count videos) ")")]
         [:ul.company-profile__videos__list
          [videos/videos {:videos        videos
                          :can-add-edit? (and admin-or-owner? @editing?)
                          :error         (<sub [::subs/video-error])
                          :delete-event  [::events/delete-video]
                          :add-event     [::events/add-video]
                          :update-event  [::events/update-video]}]]
         (when admin-or-owner?
           [edit-close-button editing?])]))))

(defn blogs
  []
  (when-let [blogs (<sub [::subs/blogs])]
    [:section.company-profile__blogs
     [:h2.title (str "Tech Blogs")]
     [:ul
      (into [:div.columns]
            (for [blog blogs]
              [:div.column.is-half
               [blog-card blog]]))]]))

(defn issues
  []
  (when-let [issues (<sub [::subs/issues])]
    [:section.company-profile__issues
     [:div.is-flex
      [:h2.title "Open Source Issues from this company"]
      [link "View all"
       :issues-for-company-id :company-id (<sub [::subs/id])
       :class "a--underlined"]]
     [:div
      (doall
        (for [issue issues]
          ^{:key (:id issue)}
          [issue-card issue]))]]))

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
   {:key key
    :class (util/merge-classes "company-profile__photos__img"
                               (when solo? "company-profile__photos__img--solo"))}
   (wrap-img img (:url image)
             (assoc opts :attrs (interop/on-click-fn (open-fn (:index image)))))
   (when edit?
     [:div.company-profile__photos__delete
      {:on-click #(dispatch [::events/delete-photo image])}
      [icon "delete"]])])

(defn photos
  [can-add-edit?]
  (let [images (<sub [::subs/images])
        [first-image & [second-image]] images
        solo? (and first-image (not second-image))
        open-fn (fn [index] (interop/open-photo-gallery index images))
        secondary-images (->> images
                              (rest)
                              (take 4)
                              (partition-all 2)
                              (map-indexed (fn [i si] {:index i :simages si})))]
    [:section.company-profile__photos
     [:h2.title (str "Photos (" (count images) ")")]
     (if solo?
       [:div.company-profile__photos__gallery
        (photo first-image open-fn
               {:w 471 :h 222
                :solo? true :edit? can-add-edit?})]
       (when first-image
         [:div.company-profile__photos__gallery
          (photo first-image open-fn
                 {:w 321 :h 222
                  :edit? can-add-edit?})
          (doall
            (for [{:keys [simages index]} secondary-images]
              ^{:key index}
              [:div.company-profile__photos__splitter
               [:div.company-profile__photos__splitter__inner
                (doall
                  (for [{:keys [index simage]} (map-indexed (fn [i si] {:index i :simage si}) simages)]
                    (photo simage open-fn
                           {:w 134 :h 103
                            :edit? can-add-edit? :key index})))]]))]))
     #?(:cljs
        (when can-add-edit?
          [:div.company-profile__photos__add
           (if (<sub [::subs/photo-uploading?])
             [:div.company-profile__photos__add--loading]
             [:input {:type "file"
                      :on-change (upload/handler
                                   :launch [::events/photo-upload]
                                   :on-upload-start [::events/photo-upload-start]
                                   :on-success [::events/photo-upload-success]
                                   :on-failure [::events/photo-upload-failure])}])]))
     (pswp-element)]))

(defn profile-tag-field
  [_tag-type _selected-tag-ids _args]
  (let [tags-collapsed?  (r/atom true)]
    (fn [tag-type selected-tag-ids {:keys [label placeholder]}]
      #?(:cljs
         (let [matching-tags (<sub [::subs/matching-tags {:include-ids selected-tag-ids :size 20 :type tag-type}])
               tag-search    (<sub [::subs/tag-search tag-type])]
           [:form.wh-formx.wh-formx__layout.company-profile__editing-tags
            {:on-submit #(.preventDefault %)}
            [tags-field
             tag-search
             {:tags               (map #(if (contains? selected-tag-ids (:key %))
                                          (assoc % :selected true)
                                          %) matching-tags)
              :collapsed?         @tags-collapsed?
              :on-change          [::events/set-tag-search tag-type]
              :label              label
              :placeholder        placeholder
              :on-toggle-collapse #(swap! tags-collapsed? not)
              :on-add-tag         #(dispatch [::events/create-new-tag % tag-type])
              :on-tag-click
              #(when-let [id (some (fn [tx] (when (= (:tag tx) %) (:key tx))) matching-tags)]
                 (dispatch [::events/toggle-selected-tag-id tag-type id]))}]
            (when (<sub [::subs/creating-tag?])
              [:div.company-profile__tag-loader [:div]])])))))

(defn tag-list
  [tag-type]
  (into [:ul.tags.tags--inline.tags--profile]
        (map (fn [tag]
               [:li {:key (:id tag)}
                (:label tag)])
             (<sub [::subs/tags tag-type]))))

(defn about-us
  [_admin-or-owner?]
  (let [new-desc         (r/atom nil)
        existing-tag-ids (r/atom #{})
        editing?         (r/atom false)
        tag-type         :company]
    (fn [admin-or-owner?]
      (let [description      (<sub [::subs/description])
            selected-tag-ids (<sub [::subs/selected-tag-ids tag-type])]
        [:section
         {:id "company-profile__about-us"
          :class (util/merge-classes
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing")
                   "company-profile__about-us")}
         [editable {:editable?             admin-or-owner?
                    :prompt-before-cancel? (boolean (or @new-desc (not= selected-tag-ids @existing-tag-ids)))
                    :on-editing            #(do
                                              (reset! editing? true)
                                              (reset! existing-tag-ids (<sub [::subs/current-tag-ids tag-type]))
                                              (dispatch [::events/reset-selected-tag-ids tag-type]))
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
                         (dispatch-sync [::events/update-company changes])))}
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
           [profile-tag-field tag-type selected-tag-ids
            {:label              "Enter 3-5 tags that make your company stand out from your competitors"
             :placeholder        "e.g. flexible hours, remote working, startup"}]]]]))))

(defn development-setup
  [k editing-atom]
  (let [data (get data/dev-setup-data k)
        current-text (<sub [::subs/development-setup k])]
    (when (or editing-atom
              (text/not-blank current-text)
              (and editing-atom (text/not-blank (get @editing-atom k))))
      [:div
       {:class (util/merge-classes
                 "company-profile__development-setup"
                 (str "company-profile__development-setup--" k)
                 (when editing-atom "company-profile__development-setup--editing"))}
       (when-not editing-atom
         [:div.company-profile__development-setup__icon
          [icon (:icon data)]])
       [:div.company-profile__development-setup__info
        [:h3 (:title data)]
        (if editing-atom
          [:div.company-profile__development-setup__info__text-field
           #?(:cljs
              [text-field (or (get @editing-atom k)
                              current-text
                              "")
               {:type :textarea
                :placeholder (:placeholder data)
                :on-change (fn [v] (swap! editing-atom assoc k v))}])]
          [:p current-text])]])))

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
  (let [tags-collapsed?  (r/atom true)
        existing-tag-ids (r/atom #{})
        tech-editing?    (r/atom false)
        scales-editing?  (r/atom false)
        new-dev-setup    (r/atom {})
        new-tech-scales  (r/atom {})
        tag-type         :tech]
    (fn [admin-or-owner?]
      (let [selected-tag-ids (<sub [::subs/selected-tag-ids tag-type])]
        [:section
         {:id    "company-profile__technology"
          :class (util/merge-classes
                   "company-profile__section--headed"
                   (when (or @tech-editing? @scales-editing?) "company-profile__section--editing")
                   "company-profile__technology")}
         [editable {:editable?             admin-or-owner?
                    :prompt-before-cancel? (or (not-empty @new-dev-setup)
                                               (not= selected-tag-ids @existing-tag-ids))
                    :on-editing            #(do
                                              (reset! tech-editing? true)
                                              (reset! existing-tag-ids (<sub [::subs/current-tag-ids tag-type]))
                                              (dispatch [::events/reset-selected-tag-ids tag-type]))
                    :on-cancel             #(reset! tech-editing? false)
                    :on-save
                    #(do
                       (reset! tech-editing? false)
                       (when-let [changes
                                  (not-empty
                                    (merge {}
                                           (when (not= selected-tag-ids @existing-tag-ids)
                                             {:tag-ids (concat selected-tag-ids (<sub [::subs/current-tag-ids--inverted tag-type]))})
                                           (when (not-empty @new-dev-setup)
                                             {:dev-setup (merge (:dev-setup (<sub [::subs/company])) @new-dev-setup)})))]
                         (dispatch-sync [::events/update-company changes])))}
          [:div
           [:h2.title "Technology"]
           [:h2.subtitle "Tech stack"]
           (when-let [tags (not-empty (<sub [::subs/tags tag-type]))]
             [:div.company-profile__technology__tech-tags
              [tag-list tag-type]])

           [:h2.subtitle "Development setup"]
           (doall
             (for [k (keys data/dev-setup-data)]
               ^{:key k}
               [development-setup k nil]))]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          [:div
           [:h2.title "Technology"]
           [:h2.subtitle "Tech stack"]
           [profile-tag-field tag-type selected-tag-ids
            {:label       "Enter 3-5 tags that describe your company's technology"
             :placeholder "e.g. haskell, scala, clojure, javascript etc"}]
           [:h2.subtitle "Development setup"]
           [:form.wh-formx.wh-formx__layout.company-profile__editing-dev-setup
            (doall
              (for [k (keys data/dev-setup-data)]
                ^{:key k}
                [development-setup k new-dev-setup]))]]]

         (let [tech-scales (not-empty (<sub [::subs/tech-scales]))
               tech-scales-view
               (when (or admin-or-owner? tech-scales)
                 [:div
                  (when @scales-editing?
                    [:hr])
                  [:h2.subtitle "Testing and Ops"]
                  [tech-scale {:key          :testing
                               :atom         new-tech-scales
                               :label        "Testing"
                               :scale-labels ["Manual" "Fully automated"]
                               :force-show?  admin-or-owner?
                               :editing?     @scales-editing?}]
                  [tech-scale {:key          :ops
                               :atom         new-tech-scales
                               :label        "Ops"
                               :scale-labels ["DevOps" "Dedicated Ops team"]
                               :force-show?  admin-or-owner?
                               :editing?     @scales-editing?}]
                  [tech-scale {:key          :time-to-deploy
                               :atom         new-tech-scales
                               :label        "Time to deploy"
                               :scale-labels ["More than 5 hours" "Less than 1 hour"]
                               :force-show?  admin-or-owner?
                               :editing?     @scales-editing?}]])]

           [editable {:editable?             admin-or-owner?
                      :prompt-before-cancel? (boolean (not-empty @new-tech-scales))
                      :on-editing            #(do (reset! scales-editing? true))
                      :on-cancel             #(do (reset! scales-editing? false)
                                                  (reset! new-tech-scales {}))
                      :on-save
                      #(do
                         (reset! scales-editing? false)
                         (when-let [changes
                                    (when (not-empty @new-tech-scales)
                                      {:tech-scales (merge tech-scales @new-tech-scales)})]
                           (dispatch-sync [::events/update-company changes]))
                         (reset! new-tech-scales {}))}
          tech-scales-view
          tech-scales-view])]))))

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
         [:ul.company-profile__company-info__list
          (when-let [industry (:label (<sub [::subs/industry]))]
           [:li [icon "industry"] "Industry: " industry])
          (when-let [size (some-> (<sub [::subs/size]) company-spec/size->range)]
           [:li [icon "people"] "People: " size])
          (when-let [founded-year (<sub [::subs/founded-year])]
           [:li [icon "founded"] "Founded: " founded-year])
          (when-let [funding (:label (<sub [::subs/funding]))]
           [:li [icon "funding"] "Funding: " funding])
          (when-let [location (some-> (<sub [::subs/location]) )]
           [:li [icon "location"] "Location: " location])]
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
                :on-blur #(when (str/blank? @new-founded-year)
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
                 {:type :text
                  :label "What's your company's HQ or primary location?"
                  :on-change [::events/update-location-suggestions]
                  :suggestions (<sub [::subs/location-suggestions])
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
             {:id    "company-profile__how-we-work"
              :class (util/merge-classes
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
                             (dispatch-sync [::events/update-company changes])))}
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

(defn page []
  (let [enabled? (<sub [::subs/profile-enabled?])
        company-id (<sub [::subs/id])
        admin-or-owner? (or (<sub [:user/admin?])
                            (<sub [:user/owner? company-id]))]
    (if enabled?
      [:div.main.company-profile
       [header admin-or-owner?]
       [:div.split-content
        [:div.company-profile__main.split-content__main
         [about-us admin-or-owner?]
         [company-info admin-or-owner? "company-profile__section--headed is-hidden-desktop"]
         [videos admin-or-owner?]
         [blogs]
         [photos admin-or-owner?]
         [technology admin-or-owner?]
         [issues]
         [how-we-work admin-or-owner?]]
        [:div.company-profile__side.split-content__side.is-hidden-mobile
         [company-info admin-or-owner?]]]]
      [:div.main.main--center-content
       [not-found/not-found]])))
