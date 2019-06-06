(ns wh.logged-in.contribute.views
  (:require
    [re-frame.core :refer [dispatch dispatch-sync]]
    [reagent.core :as reagent]
    [wh.common.re-frame-helpers :refer [merge-classes]]
    [wh.common.upload :as upload]
    [wh.components.common :refer [link]]
    [wh.components.conversation.views :as codi :refer [codi-message]]
    [wh.components.forms.views :refer [multi-edit labelled-checkbox text-field text-input logo-field tags-field select-field]]
    [wh.components.verticals :as vertical-views]
    [wh.logged-in.contribute.db :as contribute]
    [wh.logged-in.contribute.events :as events]
    [wh.logged-in.contribute.subs :as subs]
    [wh.pages.core :as pages]
    [wh.subs :refer [<sub]]))

(defn hero []
  [:div.contribute__hero
   {:id (contribute/form-field-id ::contribute/feature)}
   [logo-field {:value (<sub [::subs/hero-url])
                :loading? (<sub [::subs/hero-uploading?])
                :text "add hero image"
                :error (<sub [::subs/hero-validation-error?])
                :dirty? (<sub [::subs/validation-error?])
                :on-select-file (upload/handler
                                 :launch [::events/hero-upload]
                                 :on-upload-start [::events/hero-upload-start]
                                 :on-success [::events/hero-upload-success]
                                 :on-failure [::events/hero-upload-failure])}]
   (case (<sub [::subs/hero-upload-status])
     :failure [:span.field__error.field--invalid "Failed to upload the hero image."]
     :failure-too-big [:span.field__error.field--invalid "The image was too large (max. 5MB)"]
     nil)])

(defn body []
  (let [editing? (<sub [::subs/body-editing?])
        error    (when (<sub [::subs/validation-error?])
                   (<sub [::subs/body-validation-error]))]
    [:div.contribute__body
     (merge {:id (contribute/form-field-id ::contribute/body)}
            (when error {:class "contribute__body--error"}))
     [:label.label "* Body of article"]
     [:div.contribute__body__controls
      [:div.tabs
       [:div.tab {:on-click #(dispatch [::events/show-edit])
                  :class    (when editing? "tab--active")}
        "Edit"]
       [:div.tab {:on-click #(dispatch [::events/show-preview])
                  :class    (when-not editing? "tab--active")}
        "Preview"]]
      [:div.add-image.file
       [:label.file-label
        [:input.file-input {:type      "file"
                            :name      "article-image"
                            :on-change (upload/handler
                                        :launch [::events/image-article-upload]
                                        :on-upload-start [::events/image-article-upload-start]
                                        :on-success [::events/image-article-upload-success]
                                        :on-failure [::events/image-article-upload-failure])}]
        [:span.file-cta.button.button--inverted
         {:class (when (<sub [::subs/image-article-uploading?])
                   "button--loading")}
         [:span.file-label.is-hidden-mobile "Add image to article"]
         [:span.file-label.is-hidden-desktop "Add image"]]]
       [:span.notice {:class (when (<sub [::subs/image-article-upload-hide-status?])
                               "appear")}]]]
     (if editing?
       [text-input (<sub [::subs/body])
        {:type         :textarea
         :on-change    [::events/set-body]}]
       [:div.textarea
        {:dangerouslySetInnerHTML {:__html (<sub [::subs/body-html])}}])

     [:div.contribute__body__post
      (if error
        [:span.field__error.field--invalid error]
        [:span])
      [:span [:a.a--underlined {:href   "https://guides.github.com/features/mastering-markdown/"
                                :target "_blank"
                                :rel    "noopener"}
              "Parsed with GitHub Flavoured Markdown"]]]]))

(defn submit-button
  [desktop?]
  [:div.contribute__submit
   {:class (if desktop? "is-hidden-mobile" "is-hidden-desktop")}
   (cond
     (= :failure (<sub [::subs/save-status]))
     [:div.save-status.message--error
      "There was an error updating your blog. Please try again later"]
     (<sub [::subs/validation-error?])
     [:div.save-status.message--error
      "One of the fields has an error!"]
     :else
     [:div])
   [:button.button.button--medium
    {:on-click #(dispatch [::events/save-blog])}
    (str (if (and (<sub [::subs/contribute-page?])
                  (not (<sub [:user/admin?]))) "Submit" "Save") " Article")]])

(defn main [new?]
  (let [admin? (<sub [:user/admin?])]
    [:div.columns.contribute__main
     [:div.column.is-7.contribute__form
      (when (and (= :success(<sub [::subs/save-status]))
                 (not admin?))
        [:div.contribute__saved
         "Your article has been saved and sent to our team. We will make sure it is in good shape before publishing it to our members dashboards. In the meantime, you can edit the post via the link in your profile anytime before it goes live."])
      [:div.wh-formx
       [hero]
       (when admin?
         [text-field (<sub [::subs/author])
          {:id                   (contribute/form-field-id ::contribute/author)
           :label                "* Author"
           :class                (merge-classes "author" (when (<sub [::subs/author-searching?]) "field--loading"))
           :on-change            [::events/set-author]
           :suggestions          (<sub [::subs/author-suggestions])
           :on-select-suggestion [::events/select-author-suggestion]
           :error                (<sub [::subs/author-validation-error])
           :force-error?         (<sub [::subs/validation-error?])}])
       (when admin?
         [text-field (<sub [::subs/company-name])
          {:id                   (contribute/form-field-id ::contribute/company-name)
           :label                "Company"
           :class                (merge-classes "company" (when (<sub [::subs/company-searching?]) "field--loading"))
           :on-change            [::events/set-company]
           :suggestions          (<sub [::subs/company-suggestions])
           :on-select-suggestion [::events/select-company-suggestion]
           :error                (<sub [::subs/company-validation-error])
           :force-error?         (<sub [::subs/validation-error?])}])
       [text-field (<sub [::subs/title])
        {:id           (contribute/form-field-id ::contribute/title)
         :label        "* Title"
         :on-change    [::events/set-title]
         :error        (<sub [::subs/title-validation-error])
         :force-error? (<sub [::subs/validation-error?])}]
       [body]
       [tags-field
        (<sub [::subs/tag-search])
        {:id           (contribute/form-field-id ::contribute/tags)
         :label        "* Tags"
         :placeholder  "e.g. Clojure, Haskell, Scala"
         :on-change    [::events/edit-tag-search]
         :on-tag-click #(dispatch [::events/toggle-tag %])
         :on-add-tag   #(dispatch [::events/toggle-tag %])
         :tags         (<sub [::subs/matching-tags])
         :error        (<sub [::subs/tags-validation-error])
         :dirty?       (<sub [::subs/validation-error?])}]
       [text-field (<sub [::subs/original-source])
        {:id           (contribute/form-field-id ::contribute/original-source)
         :label        "Originally published on"
         :class        "original-source"
         :placeholder  "URL to original article, leave empty if publishing on WorksHub originally"
         :on-change    [::events/set-original-source]
         :error        (<sub [::subs/original-source-validation-error])
         :force-error? (<sub [::subs/validation-error?])}]
       (when admin?
         [:div.control
          [labelled-checkbox
           (<sub [::subs/published?])
           {:label     "Published"
            :on-change [::events/set-published]}]])
       [:div.contribute__checklist
        [:strong [:i "Before you submit..."]]
        [:p "Check that there are no h1's (" [:code "#"] ") in body of the blog - the biggest heading in body should be h2 (" [:code "##"] ")."]
        [:p "If there is video in post make sure that following tags have specified clasess to enable responsive behaviour:" [:code "<center class=\"embed-responsive embed-responsive-16by9\"><iframe class=\"embed-responsive-item\" ...></center>"]]
        (when (<sub [:user/admin?])
          [:p "Check that all the images in the post are hosted by us, if they are not, upload them and update links."])]
       [submit-button true]]]
     [:div.column.is-offset-1.is-4.contribute__side
      (cond
        (<sub [::subs/show-vertical-selector?])
        [:div.vertical-selector
         [vertical-views/verticals-pod
          {:toggleable? true
           :on-verticals (<sub [::subs/verticals])
           :off-verticals (<sub [::subs/off-verticals])
           :toggle-event [::events/toggle-vertical]
           :class-prefix "contribute"}]
         (when (<sub [:wh.user/super-admin?])
           [:div.wh-formx
            [select-field (<sub [::subs/primary-vertical])
             {:options (<sub [::subs/verticals])
              :label "* Primary Vertical"
              :on-change [::events/set-primary-vertical]}]])]
        (<sub [::subs/contribute-page?])
        [:div.contribute__codi.is-hidden-mobile
         (when (<sub [::subs/hide-codi?])
           {:class "contribute__codi--hidden"})
         [codi-message
          [:strong "Got something to share with our community?"]]
         [codi-message "This is your space to write learnings, ideas or even just anecdotes about working in tech. Your posts will be shown to other coders with similar interests as well as job posters."]
         [codi-message "It's your chance to compliment your resume, brag about successes or talk about anything that can't fit in an application."]
         [codi-message "If the article was originally written on another site, make sure you give attribution before publishing."]
         [codi/button "Thanks, got it!" [::events/dismiss-codi]]])
      [submit-button false]]]))

(defn page []
  (let [new? (<sub [::subs/contribute-page?])]
    [:div.main.contribute
     (if new?
       [:h1.title "Contribute"]
       [:h1.title "Editing " [link (<sub [::subs/title]) :blog :id (or (:id (<sub [::pages/page-params])) "") :class "a--underlined"]])
     (if (<sub [::subs/edit-page-authorized?])
       [main new?]
       [:div "You can only edit blogs that you have created and which are not yet published."])]))
