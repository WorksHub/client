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
    [wh.util :as util]))

(defn header []
  [:div.company-profile__header
   [:div.company-profile__logo
    [:img {:src (<sub [::subs/logo])}]]
   [:div.company-profile__name (<sub [::subs/name])]])

(defn videos
  []
  [:section.company-profile__videos
   [:h2.header "Videos"]
   [:ul.company-profile__videos__list
    [videos/videos {:videos        (<sub [::subs/videos])
                    :can-add-edit? false
                    :error         (<sub [::subs/video-error])
                    :delete-event  [::events/delete-video]
                    :add-event     [::events/add-video]
                    :update-event  [::events/update-video]}]]])

(defn photo
  [image {:keys [_w _h _fit solo? edit? key] :as opts
          :or {key (:url image)}}]
  [:div
   {:key key
    :class (util/merge-classes "company-profile__photos__img"
                               (when solo? "company-profile__photos__img--solo"))}
   (wrap-img img (:url image) opts)
   (when edit?
     [:div.company-profile__photos__delete
      {:on-click #(dispatch [::events/delete-photo image])}
      [icon "delete"]])])

(defn photos
  [can-add-edit?]
  (let [[first-image & [second-image] :as images] (<sub [::subs/images])
        solo? (and first-image (not second-image))
        secondary-images (->> images
                              (rest)
                              (take 4)
                              (partition-all 2)
                              (map-indexed (fn [i si] {:index i :simages si})))]
    [:section.company-profile__photos
     [:h2.header (str "Photos (" (count images) ")")]
     (if solo?
       [:div.company-profile__photos__gallery
        (photo first-image {:w 471 :h 222
                            :solo? true :edit? can-add-edit?})]
       [:div.company-profile__photos__gallery
        (photo first-image {:w 321 :h 222
                            :edit? can-add-edit?})
        (doall
          (for [{:keys [simages index]} secondary-images]
            ^{:key index}
            [:div.company-profile__photos__splitter
             [:div.company-profile__photos__splitter__inner
              (doall
                (for [{:keys [index simage]} (map-indexed (fn [i si] {:index i :simage si}) simages)]
                  (photo simage {:w 134 :h 103
                                 :edit? can-add-edit? :key index})))]]))])
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
                                   :on-failure [::events/photo-upload-failure])}])]))]))

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
