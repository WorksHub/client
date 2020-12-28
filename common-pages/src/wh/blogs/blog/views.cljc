(ns wh.blogs.blog.views
  (:require #?(:cljs [reagent.core :as reagent])
            [bidi.bidi :as bidi]
            [clojure.string :as str]
            [wh.components.promote-button :as promote]
            [wh.blogs.blog.events :as events]
            [wh.blogs.blog.subs :as subs]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon url-icons]]
            [wh.components.job :refer [job-card]]
            [wh.components.newsletter :as newsletter]
            [wh.components.pods.candidates :as candidate-pods]
            [wh.components.recommendation-cards :as recommendation-cards]
            [wh.components.tag :as tag]
            [wh.components.not-found :as not-found]
            [wh.interop :as interop]
            [wh.pages.util :as putil]
            [wh.re-frame :as r]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.util :as util])
  #?(:cljs (:require-macros [clojure.core.strint :refer [<<]])))

(defn link-to-share []
  (let [path (routes/path :blog
                          :params {:id (:id (<sub [::subs/blog]))}
                          :query-params {:utm_campaign "sharebutton" :utm_source "blog"})
        vertical (<sub [:wh/vertical])]
    #?(:cljs (str js/window.location.origin path)
       :clj  (wh.url/base-url vertical path))))

(defn share-buttons []
  (let [message-prefix "Check out this blog on "
        normal-message (str message-prefix (<sub [:wh/platform-name]))
        twitter-message (str message-prefix (<sub [:wh/twitter]))
        link (link-to-share)
        enc-link (bidi/url-encode link)
        enc-normal-message (bidi/url-encode normal-message)
        enc-twitter-message (bidi/url-encode twitter-message)]
    [:div.share-links
     [:a
      {:href   (str "http://www.facebook.com/sharer/sharer.php?u=" enc-link)
       :target "_blank"
       :rel    "noopener"
       :aria-label "Share blog on Facebook"}
      [icon "facebook-circle" :class "share-links__facebook"]]
     [:a
      {:href   (str "https://twitter.com/intent/tweet?text=" enc-twitter-message "&url=" enc-link)
       :target "_blank"
       :rel    "noopener"
       :aria-label "Share blog on Twitter"}
      [icon "twitter-circle" :class "share-links__twitter"]]
     [:a
      {:href   (str "https://www.linkedin.com/shareArticle?mini=true&url=" enc-link "&title=" enc-normal-message "&summary=&origin=")
       :target "_blank"
       :rel    "noopener"
       :aria-label "Share blog on Linkedin"}
      [icon "linkedin-circle" :class "share-links__linkedin"]]]))

(defn recommended-jobs []
  (when-let [jobs (<sub [::subs/recommended-jobs])]
    (when (seq jobs)
      (let [logged-in?   (<sub [:user/logged-in?])
            company-id   (<sub [:user/company-id])
            admin?       (<sub [:user/admin?])
            has-applied? (some? (<sub [:user/applied-jobs]))
            title        (<sub [::subs/recommendations-heading])
            company-name (<sub [::subs/company-name])]
        [:section.recommended-jobs
         [:h2 (cond (and (<sub [::subs/recommendations-from-company?])
                         (not (str/blank? company-name))) (str "Check out these jobs from " company-name)
                    (not (str/blank? title))              (str "Check out these jobs using " title)
                    :else                                 "Check out these recommended jobs")]
         (into
           [:div.columns.is-mobile]
           (for [job jobs]
             [:div.column [job-card job {:logged-in?        logged-in?
                                         :user-has-applied? has-applied?
                                         :user-is-company?  (not (nil? company-id))
                                         :user-is-owner?    (or admin? (= company-id (:company-id job)))
                                         :apply-source      "blog-recommended-job"}]]))]))))

(defn upvotes []
  [:div
   {:class (util/merge-classes
             "upvote"
             (when (<sub [::subs/share-links-shown?]) "upvote--shown"))}
   [:div.upvote__counter (<sub [::subs/upvote-count])]
   [:div (merge
           {:class "upvote__circle upvote__circle--pulsing"}
           (interop/on-click-fn
             #?(:cljs #(dispatch [::events/upvote])
                :clj (interop/show-auth-popup :upvote
                                              [:blog
                                               :params {:id (:id (<sub [::subs/blog]))}
                                               :query-params {:upvote true}]))))
    [icon "upvote-rocket"]]])

(defn social-icons []
  [:div.blog__social-icons-wrapper
   [:div.blog__social-icons
    [upvotes]
    [share-buttons]]])

(defn author-info []
  (when-let [info (<sub [::subs/author-info])]
    (let [{:keys [image-url name summary other-urls skills author-id] :as user} info
          skill-names (map :name skills)]
      [:div.author-info
       [:div.author-info__inner
        [:img.author-info__photo {:src image-url
                                  :alt "Author's avatar"}]
        [:div.author-info__data
         (if author-id
           [:a {:class (util/mc "author-info__name" "author-info__name--link")
                :href (routes/path :user :params {:id author-id})} name]
           [:div.author-info__name name])
         (when summary [:div.author-info__summary summary])
         (when other-urls [url-icons other-urls "author-info__other-urls"])]]
       (when (seq skill-names)
         [:div.author-info__tags
          [tag/strs->tag-list :div skill-names nil]])])))

(defn blog-info []
  [:div.blog-info
   [:div.blog-info__author
    [:span.blog-info__author-name
     {:on-click #(dispatch [::events/toggle-author-info])}
     (or
       (:name (<sub [::subs/author-info]))
       (<sub [::subs/author]))] " "
    [:span.blog-info__datetime
     (<sub [::subs/formatted-creation-date]) " "
     (when (> (<sub [::subs/reading-time]) 0)
       (str "| " (<sub [::subs/reading-time]) " min read"))]]])

(defn blog-original-source []
  (when (<sub [::subs/show-original-source?])
    [:div.blog-body.blog-section__width.blog-body__original-source
     [:p "Originally published on "
      [:a {:href (<sub [::subs/original-source])
           :target "_blank" :rel "noopener"}
       (<sub [::subs/original-source-domain])]]]))

(defn header []
  [:div.blog-header__edit-button
   (when (<sub [::subs/can-edit?])
     [link [:button.button "Edit Blog"]
      :contribute-edit :id (<sub [::subs/id])])
   (when (<sub [::subs/show-unpublished?])
     [:span.card__label.card__label--unpublished.card__label--blog-header "unpublished"])])

(defn tags []
  [tag/tag-list :a (->> (<sub [::subs/tags])
                        (map #(assoc % :href (routes/path :learn-by-tag :params {:tag (:slug %)}))))])

(defn title []
  [:h1.blog-header (<sub [::subs/title])])

(defn blog-content []
  (let [body-parts (<sub [::subs/html-body-parts])
        show-newsletter? (= (count body-parts) 2)
        user-logged-in? (<sub [:user/logged-in?])]
    [:div
     [:div.blog__content
      [:div.blog-section__width
       [title]
       [header]
       [blog-info]
       [tags]
       [:div.blog-body.blog-section__width
        [putil/html (first body-parts)]
        (when-not show-newsletter?
          [blog-original-source])]]]
     (when show-newsletter?
       [newsletter/newsletter {:logged-in? user-logged-in?
                               :type :blog-content}])
     (when show-newsletter?
       [:div.blog__content
        [:div.blog-section__width
         [:div.blog-body.blog-section__width
          [putil/html (second body-parts)]
          [blog-original-source]]]])]))

(defn blog-hero []
  (let [feature (<sub [::subs/feature])]
    [:div.blog__hero-container
     (if-not feature
       [:div.blog__hero.blog__hero--skeleton]
       [:img.blog__hero
        {:src feature
         :alt "Blog hero image"}])]))

(defn page-render []
  (case (<sub [::subs/blog-error])
    :blog-not-found [:div.main.main--center-content
                     [not-found/not-found]]
    :unknown-error  [:div.main "Loading error"]
    [:div {:class (util/merge-classes "blog" "main")}
     [blog-hero]
     [:div.split-content
      [:div.split-content__main
       [blog-content]]
      (let [{:keys [blogs issues jobs]} (<sub [::subs/recommendations])
            admin?                      (<sub [:user/admin?])
            published?                  (<sub [::subs/published?])]
        [:div.split-content__side
         (when (and admin? published?)
           [promote/promote-button {:id (:id (<sub [::subs/blog])) :type :article}])

         [author-info]
         [recommendation-cards/jobs {:jobs           jobs
                                     :instant-apply? (some? (<sub [:user/applied-jobs]))
                                     :company-id     (<sub [:user/company-id])
                                     :logged-in?     (<sub [:user/logged-in?])
                                     :admin?         admin?}]
         [recommendation-cards/blogs {:blogs blogs}]
         [recommendation-cards/issues {:issues issues}]
         [candidate-pods/candidate-cta]])]
     [social-icons]]))

(defn page []
  #?(:cljs
     (let [code-highlighted? (r/atom false)
           last-y (r/atom 0)
           _      (putil/attach-on-scroll-event
                    (fn [y]
                      (dispatch [::events/show-share-links (< y @last-y)])
                      (reset! last-y y)))
           highlight-code (fn []
                            (when (and (not @code-highlighted?)
                                       (seq (<sub [::subs/html-body-parts])))
                              (reset! code-highlighted? true)
                              (interop/highlight-code-snippets)))]
       (reagent/create-class
         {:component-did-update highlight-code
          :component-did-mount highlight-code
          :render page-render}))
     :clj
     (conj (page-render)
           [:script (interop/highlight-code-snippets)])))
