(ns wh.company.profile.views
  (:require
    #?(:cljs [wh.common.upload :as upload])
    [wh.company.profile.events :as events]
    [wh.company.profile.subs :as subs]
    [wh.components.common :refer [wrap-img img]]
    [wh.components.icons :refer [icon]]
    [wh.components.videos :as videos]
    [wh.re-frame.events :refer [dispatch]]
    [wh.components.not-found :as not-found]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]
    [wh.interop :as interop]))

(defn header []
  [:div.company-profile__header
   [:div.company-profile__logo
    [:img {:src (<sub [::subs/logo])}]]
   [:div.company-profile__name (<sub [::subs/name])]])

(defn videos
  []
  (let [videos (<sub [::subs/videos])]
    [:section.company-profile__videos
     [:h2.header (str "Videos (" (count videos) ")")]
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
     [:h2.header (str "Photos (" (count images) ")")]
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

(defn page []
  (let [enabled? (<sub [::subs/profile-enabled?])
        company-id (<sub [::subs/id])
        admin-or-owner? (or (<sub [:user/admin?])
                            (<sub [:user/owner? company-id]))]
    (if enabled?
      [:div.main.company-profile
       [header]
       [:div.is-flex
        [:div.company-profile__main.split-content__main
         [videos]
         [photos admin-or-owner?]]
        [:div.company-profile__side.split-content__side]]]
      [:div.main.main--center-content
       [not-found/not-found]])))
