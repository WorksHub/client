(ns wh.blogs.blog.views
  (:require
    #?(:cljs [wh.common.http :refer [url-encode]]
       :clj [bidi.bidi :refer [url-encode]])
    #?(:cljs [goog.Uri :as uri])
    [clojure.string :as str]
    [wh.blogs.blog.events :as events]
    [wh.blogs.blog.subs :as subs]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon url-icons]]
    [wh.components.job :refer [job-card]]
    [wh.components.not-found :as not-found]
    [wh.pages.util :as putil]
    [wh.re-frame :as r]
    [wh.re-frame.events :refer [dispatch]]
    [wh.re-frame.subs :refer [<sub]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util])
  #?(:cljs (:require-macros [clojure.core.strint :refer [<<]])))

;; NOTE: There's a hack/gotcha here.

;; With CSS animations that are one-off (not infinite), it's not easy
;; to re-trigger them once they have already fired.  The usual way of
;; doing this is to remove and re-insert the animated element to the
;; DOM, tricking the browser to think it's a new element and re-firing
;; the animation.  We're not supposed to do it directly in React/reagent,
;; so we provide it with alternating key, forcing Virtual DOM to
;; reinsert the element. Hence the `keypart` ratom: it starts out
;; with 0 (not upvoted yet) and then alternates between 1 and 2.

#?(:cljs
   (defn upvotes []
     (let [keypart (r/atom 0)]
       (fn []
         [:div.upvote {:class (util/merge-classes (when (<sub [::subs/share-links-shown?]) "upvote--shown")
                                                  (if (<sub [:user/logged-in?]) "upvote--logged-in" "upvote--logged-out"))}
          [:div.upvote__counter (<sub [::subs/upvote-count])]
          (into
            ^{:key (str "upvote" @keypart)}
            [:div.upvote__circle {:class (if (pos? @keypart) "upvote__circle--animated" "upvote__circle--pulsing")
                                  :on-click #(do
                                               (dispatch [::events/upvote])
                                               (swap! keypart (fn [i] (if (= i 2) 1 2))))}]
            (for [i (range 1 7) :let [name (str "upvote-rocket-" i)]]
              [icon "upvote-rocket" :class name]))]))))

#?(:cljs
   (defn share-buttons []
     (let [message-prefix "Check out this blog on "
           normal-message (str message-prefix (<sub [:wh/platform-name]))
           twitter-message (str message-prefix (<sub [:wh/twitter]))
           link (-> js/window.location.href uri/parse (.setQueryData "utm_campaign=sharebutton&utm_source=blog") .toString)
           enc-link (url-encode link)
           enc-normal-message (url-encode normal-message)
           enc-twitter-message (url-encode twitter-message)]
       [:div.share-links
        {:class (util/merge-classes (when (<sub [::subs/share-links-shown?]) "share-links--shown")
                                    (if (<sub [:user/logged-in?]) "share-links--logged-in" "share-links--logged-out"))}
        [:a
         {:href   (<< "http://www.facebook.com/sharer/sharer.php?u=~{enc-link}")
          :target "_blank"
          :rel    "noopener"
          :aria-label "Share blog on Facebook"}
         [icon "facebook" :class "share-links__facebook"]]
        [:a
         {:href   (<< "https://twitter.com/intent/tweet?text=~{enc-twitter-message}&url=~{enc-link}")
          :target "_blank"
          :rel    "noopener"
          :aria-label "Share blog on Twitter"}
         [icon "twitter" :class "share-links__twitter"]]
        [:a
         {:href   (<< "https://www.linkedin.com/shareArticle?mini=true&url=~{enc-link}&title=~{enc-normal-message}&summary=&origin=")
          :target "_blank"
          :rel    "noopener"
          :aria-label "Share blog on Linkedin"}
         [icon "linkedin" :class "share-links__linkedin"]]])))

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

(defn author-info []
  (when-let [info (<sub [::subs/author-info])]
    (let [{:keys [image-url name summary other-urls]} info]
      [:div.author-info
       {:class (when (<sub [::subs/author-info-visible?]) "author-info--visible")}
       [:div.author-info__inner
        [:img.author-info__photo {:src image-url
                                  :alt "Author's avatar"}]
        [:div.author-info__data
         [:div.author-info__name name]
         [:div.author-info__summary summary]
         [url-icons other-urls "author-info__other-urls"]]]])))

#?(:cljs
   (defn social-icons []
     (when (<sub [:wh.subs/show-blog-social-icons?])
       [:div {:class "blog-social-icons"}
        [share-buttons]
        [upvotes]])))

#?(:cljs
   (defonce add-social-icons
     (do
       (swap! wh.views/extra-overlays conj [social-icons]))))

(defn page-render
  []
  (case (<sub [:graphql/error-key :blog {:id (<sub [:wh/page-param :id])}])
    :blog-not-found [:div.main.main--center-content
                     [not-found/not-found]]
    :unknown-error [:div.main "Loading error"]
    (let [skeleton? (= :executing (<sub [:graphql/state :blog {:id (<sub [:wh/page-param :id])}]))]
      [:div {:class (util/merge-classes "blog"
                                        "main"
                                        (when skeleton? "blog--skeleton"))}
       [:div.blog__hero-container
        (if skeleton?
          [:div.blog__hero]
          [:img.blog__hero
           {:src (<sub [::subs/feature])
            :alt "Blog hero image"}])]
       (if skeleton?
         [:div.blog__content
          [:div.blog-section__width [:h1.blog-header]]
          [:div.blog-body.blog-section__width
           [:p] [:p] [:p]]]
         [:div.blog__content
          [:div.blog-section__width
           [:h1.blog-header (<sub [::subs/title])]
           [:div.blog-header__edit-button
            (when (<sub [::subs/can-edit?])
              [link [:button.button "Edit Blog"]
               :contribute-edit :id (<sub [::subs/id])])
            (when (<sub [::subs/show-unpublished?])
              [:span.card__label.card__label--unpublished.card__label--blog-header "unpublished"])]
           [:div.blog-info
            [:div.author
             [:span.blog-info__author-name
              {:class (when (<sub [::subs/author-info]) "link")
               :on-click #(dispatch [::events/toggle-author-info])}
              (<sub [::subs/author])] " "
             [:span.blog-info__datetime
              (<sub [::subs/formatted-creation-date]) " "
              (when (> (<sub [::subs/reading-time]) 0)
                (str " (" (<sub [::subs/reading-time]) " min read)"))]
             [author-info]]]
           (into [:ul.tags]
                 (for [tag (<sub [::subs/tags])]
                   [:li
                    [:a {:href (routes/path :learn-by-tag :params {:tag (slug/slug+encode tag)})}
                     tag]]))]
          [:div.blog-body.blog-section__width
           [putil/html (<sub [::subs/html-body])]]
          (when (<sub [::subs/show-original-source?])
            [:div.blog-body.blog-section__width.blog-body__original-source
             [:p "Originally published on "
              [:a {:href (<sub [::subs/original-source])
                   :target "_blank" :rel "noopener"}
               (<sub [::subs/original-source-domain])]]])])
       [recommended-jobs]])))

(defn page []
  #?(:cljs
     (let [last-y (r/atom 0)
           _      (putil/attach-on-scroll-event
                    (fn [y]
                      (dispatch [::events/show-share-links (< y @last-y)])
                      (reset! last-y y)))]
       page-render)
     :clj
     (page-render)))
