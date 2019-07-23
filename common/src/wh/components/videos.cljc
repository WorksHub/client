(ns wh.components.videos
  (:require
    #?(:cljs [wh.components.forms.views :refer [text-field]])
    [clojure.string :as str]
    [wh.components.icons :refer [icon]]
    [wh.components.video-player :as vp]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch]]
    [wh.util :as util]))

(defn background-image
  [thumbnail]
  #?(:clj (str "background-image: url(" thumbnail ")")
     :cljs {:background-image (str "url(" thumbnail ")")}))

(defn video
  [_video _args]
  (let [editing-desc? (r/atom false)
        new-desc (r/atom nil)
        last-desc (r/atom nil)
        max-chars 56]
    (fn [{:keys [youtube-id thumbnail description loading?] :as video}
         {:keys [can-add-edit? update-event delete-event] :as args}]
      (let [desc-edit-id (str youtube-id "-desc")
            submit-desc-fn (fn []
                             (reset! editing-desc? false)
                             #?(:cljs (js/setTimeout #(set! (.-scrollTop (.getElementById js/document desc-edit-id)) 0) 10))
                             (when-let [trimmed (some-> @new-desc str/trim)]
                               (reset! new-desc trimmed)
                               (when (and (not (str/blank? trimmed)) (not= trimmed @last-desc))
                                 (reset! last-desc trimmed)
                                 (dispatch (conj update-event (assoc video :description trimmed))))))]
        [:div.video
         [:div.video_upper
          [:div.video_img
           (merge {:style (background-image thumbnail)} ;; TODO we could do with a way of rendering proper CSS (garden?)
                  (vp/open-on-click youtube-id))
           [icon "play"]]]
         [:div
          {:class (util/merge-classes "video_lower"
                                      (when can-add-edit? "video_lower--editable"))}
          [:div
           {:class (util/merge-classes "video_description"
                                       (when @editing-desc? "video_description--editing"))}
           (if loading?
             [:div.video_description_loading
              [:div]]
             [:textarea (merge
                          {:id desc-edit-id}
                          #?(:clj
                             {:readOnly true}
                             :cljs
                             {:readOnly (not (or @editing-desc?
                                                 (and can-add-edit? (not description))))
                              :value (or @new-desc description "")
                              :placeholder (if @editing-desc?
                                             "Please enter a description..."
                                             "Click to edit the description...")
                              :on-change #(reset! new-desc (.. % -target -value))
                              :on-keyDown #(cond (= 13 (.-keyCode %)) ;; ENTER press
                                                 (do
                                                   (submit-desc-fn)
                                                   false)
                                                 :else true)
                              :on-blur (fn [_e]
                                         (submit-desc-fn))}))
              #?(:clj (or @new-desc description ""))])
           #?(:cljs
              (when (and can-add-edit?
                         (not loading?)
                         (not @editing-desc?))
                [:div.video_icons
                 [icon "edit"
                  :class "video_icon_edit"
                  :on-click (fn [_]
                              (reset! editing-desc? true)
                              (js/setTimeout #(.focus (.getElementById js/document desc-edit-id)) 10))]
                 [icon "delete"
                  :class "video_icon_delete"
                  :on-click #(when (js/confirm "Are you sure you want to delete this video?")
                               (dispatch (conj delete-event youtube-id)))]]))
           #?(:cljs
              (when @editing-desc?
                [:div.video_icons
                 [:button.button.button--tiny
                  {:on-click (fn [_e]
                               (submit-desc-fn))}
                  "Save"]]))]]]))))

(defn videos
  [_args]
  (let [current-video-url (r/atom nil)]
    (fn [{:keys [videos can-add-edit? add-event update-event error] :as args}]
      [:div.videos
       [:div.videos__list
        (when (sequential? videos)
          (doall (for [v videos]
                   ^{:key (:youtube-id v)}
                   [video v (select-keys args [:update-event :delete-event :can-add-edit?])])))]
       #?(:cljs
          (when can-add-edit?
            ;; TODO re-do this
            [:div.videos__add-video
             [:form.wh-formx.wh-formx__layout
              {:on-submit #(do
                             (when-not (str/blank? @current-video-url)
                               (dispatch (conj add-event @current-video-url)))
                             (reset! current-video-url nil)
                             (.preventDefault %))}
              [:div.is-flex
               [text-field nil
                {:type "text"
                 :value @current-video-url
                 :placeholder "Add a YouTub URL here"
                 :on-change #(reset! current-video-url %)}]
               [:button.button.button--inverted.button--small {} "Add"]]
              (when error
                [:span.error error])]]))])))
