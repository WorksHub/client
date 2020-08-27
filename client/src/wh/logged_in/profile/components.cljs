(ns wh.logged-in.profile.components
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch dispatch-sync]]
            [wh.components.signin-buttons :as signin-buttons]
            [wh.common.text :refer [pluralize]]
            [wh.components.common :refer [link]]
            [wh.components.error.views :refer [error-box]]
            [wh.components.forms.views :refer [field-container
                                               labelled-checkbox
                                               multi-edit multiple-buttons
                                               select-field select-input
                                               text-field text-input
                                               avatar-field]]
            [wh.components.icons :as icons]
            [wh.logged-in.profile.subs :as subs]
            [wh.routes :as routes]
            [wh.subs :refer [<sub]]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn container [& elms]
  (into [:div {:class styles/container}] elms))

(defn meta-row [{:keys [text icon href new-tab?]}]
  [:a (cond-> {:class styles/meta-row
               :href href}
              new-tab? (merge {:target "_blank"
                               :rel    "noopener"}))
   [icons/icon icon :class styles/meta-row__icon]
   [:span {:class styles/meta-row__description} text]])

(defn social-row [type {:keys [display url] :as social}]
  (if social
    [meta-row {:text display
               :icon (name type)
               :href url
               :new-tab? true}]
    [meta-row {:text (str "Connect " (str/capitalize (name type)))
               :icon (name type)
               :href (signin-buttons/type->href type)}]))

(defn title [text]
  [:div {:class styles/title} text])

(defn small-link [{:keys [text href]}]
  [:a
   {:class (util/mc styles/button styles/button--small) :href href}
   text])

(defn small-button [opts text]
  [:button (merge (util/smc styles/button styles/button--small) opts) text])

(defn upload-button [{:keys [document uploading? on-change data-test]}]
  (if uploading?
    [small-button {:disabled true} "Uploading..."]
    [:label {:class (util/mc styles/button styles/button--small)
             :data-test data-test}
     [:input.visually-hidden {:type "file"
                              :name "avatar"
                              :on-change on-change}]
     [:span (str "Upload " document)]]))

(defn edit-link [{:keys [href text data-test type]
                  :or {text "Edit"}}]
  (let [small? (= type :small)]
    [:a {:href      href
         :data-test data-test
         :class     (util/mc styles/edit [small? styles/edit--small])}
     [icons/icon "edit" :class (when small? styles/edit__small-icon)] [:span.visually-hidden text]]))

(defn edit-profile [{:keys [type]}]
  (let [[user-route admin-route] (if (= type :private)
                                   [:profile-edit-private :candidate-edit-private]
                                   [:profile-edit-header :candidate-edit-header])
        href (if (= (<sub [:wh.pages.core/page]) :profile)
               (routes/path user-route)
               (routes/path admin-route))]
    [edit-link {:href href
                :data-test (if (= type :private) "edit-profile-private" "edit-profile")
                :text "Edit"}]))

(defn profile [user]
  (let [{:keys [name image-url summary]}
        (util/strip-ns-from-map-keys user)]
    [:div (util/smc styles/section styles/section--profile)
     [edit-profile {:type :default}]
     [:div {:class styles/username} name]
     [:img {:src image-url
            :class styles/avatar}]
     (when summary [:div {:class styles/summary
                          :title summary} summary])
     [:hr {:class styles/separator}]
     [:div {:class styles/meta-rows}
      [social-row :github (<sub [::subs/social :github])]
      [social-row :stackoverflow (<sub [::subs/social :stackoverflow])]
      [social-row :twitter (<sub [::subs/social :twitter])]]
     [:hr {:class styles/separator}]
     [:a {:data-pushy-ignore "true"
          :class styles/button
          :href (routes/path :logout)}
      "Logout"]]))

(defn view-field
  ([label content]
   (view-field nil label content))
  ([data-test label content]
   [:div {:class styles/view-field
          :data-test data-test}
    [:label {:class styles/view-field__label} label]
    [:div {:class styles/view-field__content} content]]))

(defn section [& children]
  (into [:div {:class styles/section}] children))

(defn section-buttons [& children]
  (into [:div {:class styles/section__buttons}] children))

(defn resource [{:keys [href text]}]
  [:a {:class styles/resource :href href :target "_blank" :rel "noopener"} text])

(defn content [& children]
  (into [:div {:class styles/content}] children))

(defn subtitle [text]
  [:div {:class styles/subtitle} text])

(defn top-tech [tag]
  (let [name (str/lower-case (:name tag))]
    [:div {:class styles/top-tech
           :title name}
     [icons/icon (str name "-tag") :class styles/top-tech__icon]
     [:div {:class styles/top-tech__label} name]]))

(defn skills [tags]
  (into [:div {:class styles/skills}] tags))

(defn meta-separator []
  [:span {:class styles/meta-separator} "•"])

(defn article [{:keys [id title formatted-creation-date reading-time upvote-count published] :as article-data}]
  [:div {:class styles/article}
   [edit-link {:href (routes/path :contribute-edit :params {:id id})
               :type :small}]
   [:a {:class styles/article__title
        :href (routes/path :blog :params {:id id})} title]
   [:div {:class styles/article__meta}
    (when-not published [:<> [:span "not published"]
                             [meta-separator]])
    formatted-creation-date
    [meta-separator]
    reading-time " min read"
    [meta-separator]
    upvote-count " " (pluralize upvote-count "boost")]])
