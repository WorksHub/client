(ns wh.company.profile.views
  (:require
    #?(:cljs [wh.common.upload :as upload])
    #?(:cljs [wh.company.components.forms.views :refer [rich-text-field]])
    #?(:cljs [wh.components.forms.views :refer [tags-field]])
    [clojure.string :as str]
    [wh.company.profile.events :as events]
    [wh.company.profile.subs :as subs]
    [wh.components.common :refer [wrap-img img]]
    [wh.components.icons :refer [icon]]
    [wh.components.not-found :as not-found]
    [wh.components.videos :as videos]
    [wh.interop :as interop]
    [wh.pages.util :as putil]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch dispatch-sync]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(defn header []
  [:div.company-profile__header
   [:div.company-profile__logo
    [:img {:src (<sub [::subs/logo])}]]
   [:div.company-profile__name (<sub [::subs/name])]])

(defn videos
  []
  (let [videos (<sub [::subs/videos])]
    [:section.company-profile__videos
     [:h2.title (str "Videos (" (count videos) ")")]
     [:ul.company-profile__videos__list
      [videos/videos {:videos        videos
                      :can-add-edit? false
                      :error         (<sub [::subs/video-error])
                      :delete-event  [::events/delete-video]
                      :add-event     [::events/add-video]
                      :update-event  [::events/update-video]}]]]))

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

(defn editable
  [_args _read-body _write-body]
  (let [editing? (r/atom false)]
    (fn [{:keys [editable? prompt-before-cancel? on-editing on-cancel on-save]} read-body write-body]
      [:div.editable
       (if @editing?
         write-body
         read-body)
       #?(:cljs
          (when editable?
            (cond @editing?
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
                    "Save"]]
                  (<sub [::subs/updating?])
                  [:div.editable--loading]
                  :else
                  [:div.editable--edit-button
                   [icon "edit"
                    :on-click #(do (reset! editing? true)
                                   (when on-editing
                                     (on-editing)))]])))])))

(defn about-us
  [_admin-or-owner?]
  (let [new-desc         (r/atom nil)
        tags-collapsed?  (r/atom true)
        existing-tag-ids (r/atom #{})
        editing?         (r/atom false)
        tag-type         :company]
    (fn [admin-or-owner?]
      (let [description      (<sub [::subs/description])
            selected-tag-ids (<sub [::subs/selected-tag-ids])]
        [:section
         {:class (util/merge-classes
                   "company-profile__section--headed"
                   (when @editing? "company-profile__section--editing")
                   "company-profile__about-us")}
         [:h2.title "About us"]
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
                                             {:tag-ids selected-tag-ids})))]
                         (dispatch-sync [::events/update-company changes])))}
          [:div
           [:h2.subtitle "Who are we?"]
           [:div.company-profile__about-us__description
            [putil/html description]]
           (when-let [tags (not-empty (<sub [::subs/tags tag-type]))]
             [:div.company-profile__about-us__company-tags
              (into [:ul.tags.tags--inline]
                    (map (fn [tag]
                           [:li {:key (:id tag)}
                            (:label tag)])
                         (<sub [::subs/tags tag-type])))])]
          ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
          [:div
           [:h2.subtitle "Who are we?"]
           #?(:cljs
              [rich-text-field {:value       (or @new-desc description "")
                                :placeholder "Please enter a description..."
                                :on-change   #(if (= % description)
                                                (reset! new-desc nil)
                                                (reset! new-desc %))}])
           #?(:cljs
              (let [matching-tags (<sub [::subs/matching-tags {:include-ids selected-tag-ids :size 20 :type tag-type}])
                    tag-search    (<sub [::subs/tag-search])]
                [:form.wh-formx.wh-formx__layout.company-profile__editing-tags
                 [tags-field
                  tag-search
                  {:tags               (map #(if (contains? selected-tag-ids (:key %))
                                               (assoc % :selected true)
                                               %) matching-tags)
                   :collapsed?         @tags-collapsed?
                   :on-change          [::events/set-tag-search]
                   :label              "Enter 3-5 tags that make your company stand out from your competitors"
                   :placeholder        "e.g. flexible hours, remote working, startup"
                   :on-toggle-collapse #(swap! tags-collapsed? not)
                   :on-add-tag         #(dispatch [::events/create-new-tag % tag-type])
                   :on-tag-click
                   #(when-let [id (some (fn [tx] (when (= (:tag tx) %) (:key tx))) matching-tags)]
                      (dispatch [::events/toggle-selected-tag-id id]))}]
                 (when (<sub [::subs/creating-tag?])
                   [:div.company-profile__tag-loader [:div]])]))]]]))))

(defn page []
  (let [enabled? (<sub [::subs/profile-enabled?])
        company-id (<sub [::subs/id])
        admin-or-owner? (or (<sub [:user/admin?])
                            (<sub [:user/owner? company-id]))]
    (if enabled?
      [:div.main.company-profile
       [header]
       [:div.split-content
        [:div.company-profile__main.split-content__main
         [about-us admin-or-owner?]
         [videos]
         [photos admin-or-owner?]]
        [:div.company-profile__side.split-content__side]]]
      [:div.main.main--center-content
       [not-found/not-found]])))
