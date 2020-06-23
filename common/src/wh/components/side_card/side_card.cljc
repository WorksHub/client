(ns wh.components.side-card.side-card
  (:require [#?(:cljs cljs-time.coerce
                :clj clj-time.coerce) :as tc]
            [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [clojure.string :as str]
            [wh.common.text :as text]
            [wh.components.common :refer [link]]
            [wh.components.forms :as forms]
            [wh.components.icons :refer [icon] :as icons]
            [wh.components.tag :as tag]
            [wh.re-frame :as r]
            [wh.routes :as routes]
            [wh.styles.side-card :as style]
            [wh.util :as util]))

(defn card-tags [tags]
  [tag/tag-list :a (->> tags
                        (take 3)
                        (map #(assoc %
                                     :href (routes/path :learn-by-tag :params {:tag (:slug %)})
                                     :with-icon? false)))])

(defn card-tag [t]
  [tag/tag :div (assoc t :with-icon? false)])

(defn footer-link [{:keys [text bold-text href]}]
  [:a {:class style/footer__link :href href}
   text [:span {:class style/footer__bold-text} bold-text]])

(defn connected-entity [{:keys [title title-type subtitle href img-src img-type]}]
  (let [wrapper-tag   (if href :a :div)
        wrapper-class (cond-> style/connected-entity
                              href (util/mc style/connected-entity--link))
        img-class     (cond-> style/connected-entity__image
                              (= img-type :rounded) (util/mc style/connected-entity__image--rounded))
        title-class   (cond->
                        style/connected-entity__title
                        (= title-type :primary) (util/mc style/connected-entity__title--primary))]
    [wrapper-tag {:class wrapper-class
                  :href  href}
     [:img {:src img-src :class img-class}]
     [:div {:class style/connected-entity__info}
      [:span {:class title-class} title]
      [:span {:class (util/mc style/connected-entity__title style/connected-entity__title--minor)} subtitle]]]))

(defn section-title [title]
  [:h3 {:class style/section__title} title])

(defn section-elements [elements card-component]
  [:div {:class style/section__elements}
   (for [elm elements]
     ^{:key (:id elm)}
     [card-component elm])])

(defn numeric-info [lines]
  [:div {:class style/numeric-info}
   (for [{:keys [number sing plural icon]} lines]
     (when (and number (pos? number))
       [:div {:key   icon
              :class style/numeric-info__line}
        [icons/icon icon :class style/icon]
        (str number " " (text/pluralize number sing plural))]))])

(defn section-button [{:keys [title href type]}]
  [(if href :a :button)
   (merge {:class (cond-> style/button
                          (= :no-border type) (util/mc style/button--text))}
          (when href {:href href}))
   title])

(defn card-link [{:keys [title href]}]
  [:a {:href  href
       :class style/element__link}
   title])

;; ----------------------------------------------

(defn card-issue [{:keys [company title id level repo compensation] :as _issue}]
  [:section {:class style/section__element}
   [connected-entity {:title    (:name company)
                      :subtitle (str "Level: " (str/capitalize (name level)))
                      :href     (routes/path :company :params {:slug (:slug company)})
                      :img-src  (:logo company)}]
   [card-link {:href  (routes/path :issue :params {:id id})
               :title title}]
   [:ul {:class style/element__tags}
    (when-let [language (:primary-language repo)]
      [card-tag {:label language
                 :type  "tech"}])
    (let [amount (or (:amount compensation) 0)]
      (when-not (zero? amount)
        [card-tag {:label (str "$" amount)
                   :type  "funding"}]))]])

(defn recent-issues [issues]
  [:section {:class style/section}
   [section-title "Live Open Source Issues"]
   [section-elements issues card-issue]
   [:div {:class (util/mc style/footer style/footer--issues)}
    [section-button {:title "All open source issues"
                     :href  (routes/path :issues)}]
    [footer-link {:text      "Post your"
                  :bold-text "open source issue here"
                  :href      (routes/path :create-job)}]]])

;; -----------------------------------------------

(defn format-user-date
  [t]
  (->> t
       (tf/parse)
       (tf/unparse (tf/formatter "yyyy"))))

(defn card-user [{:keys [blog-count issue-count created name image-url] :as _user}]
  [:section {:class style/section__element}
   [connected-entity {:title      name
                      :title-type :primary
                      :subtitle   (str "Joined in " (format-user-date created))
                      :img-src    image-url
                      :img-type   :rounded}]
   [numeric-info [{:number blog-count
                   :sing   "article published"
                   :plural "articles published"
                   :icon   "article"}
                  {:number issue-count
                   :sing   "open source issue started"
                   :plural "open source issues started"
                   :icon   "commit"}]]])

(defn top-ranking-users [users]
  [:section {:class style/section}
   [section-title "Top Ranking Users"]
   [section-elements users card-user]]
  [:div
   [section-button {:title "Write an Article"
                    :href  (routes/path :contribute)}]])

;; -----------------------------------------------

(defn card-job [{:keys [title company-info slug] :as _job}]
  [:section {:class style/section__element}
   (let [jobs-count (:jobs-count company-info)
         text       (str jobs-count (text/pluralize jobs-count " live job"))]
     [connected-entity {:title    (:name company-info)
                        :subtitle text
                        :href     (routes/path :company :params {:slug (:slug company-info)})
                        :img-src  (:logo company-info)}])
   [card-link {:title title
               :href  (routes/path :job :params {:slug slug})}]])

(defn recent-jobs [jobs]
  [:section {:class style/section}
   [section-title "Hiring now"]
   [section-elements jobs card-job]
   [:div {:class (util/mc style/footer style/footer--jobs)}
    [section-button {:title "All live jobs"
                     :href  (routes/path :jobsboard)}]
    [footer-link {:text      "Hiring?"
                  :bold-text "Post your job here"
                  :href      (routes/path :create-job)}]]])

;; -----------------------------------------------

(defn card-company [{:keys [name total-published-issue-count total-published-job-count logo slug locations tags] :as _company}]
  [:section {:class style/section__element}
   (let [location     (first locations)
         location-str (str (:city location) ", " (:country location))]
     [connected-entity {:title    name
                        :subtitle location-str
                        :href     (routes/path :company :params {:slug slug})
                        :img-src  logo}])
   [numeric-info [{:number total-published-job-count
                   :icon   "case"
                   :sing   "live role"
                   :plural "live roles"}
                  {:number total-published-issue-count
                   :icon   "commit"
                   :sing   "live open source issue"
                   :plural "live open source issues"}]]
   [card-tags tags]])

(defn create-company-form-error->error-str
  [e]
  (case e
    "company-with-same-name-already-exists" "A company with this name already exists. Please use a unique company name."
    "duplicate-user"                        "A user with this email already exists. Please use a unique email address."
    "invalid-user-name"                     "The name provided is blank or invalid. Please provide a valid name."
    "invalid-company-name"                  "The company name provided is blank or invalid. Please provide a valid company name."
    "invalid-email"                         "The email address provided is not valid. Please provide a valid email address."
    (str "An unknown error occurred (" e "). Please contact support.")))

(defn create-company [_redirect _logged-in? _qps]
  (let [user-name    (r/atom nil)
        user-email   (r/atom nil)
        company-name (r/atom nil)]
    (fn [redirect logged-in? existing-params]
      (when-not logged-in?
        (let [error (some-> (get existing-params "create-company-form__error")
                            (text/not-blank)
                            (create-company-form-error->error-str))]
          [:a {:id "create-company-form"}
           [:h3 {:class style/footer__title} "Create a company page"]
           [:p {:class style/footer__text} "Create a free profile page for your company — hire, contribute and build your engineering capabilities"]
           [:form
            {:class  style/footer__form
             :action (routes/path :create-company-form :query-params {:redirect (name redirect)})
             :method "post"}
            [:input {:placeholder "Your name"
                     :class       (util/mc style/input
                                           (when error style/input--error))
                     :name        "create-company-form__user-name"
                     :value       (or @user-name
                                      (get existing-params "create-company-form__user-name"))}]
            [:input {:placeholder "Company name"
                     :class       (util/mc style/input
                                           (when error style/input--error))
                     :name        "create-company-form__company-name"
                     :value       (or @company-name
                                      (get existing-params "create-company-form__company-name"))}]
            [:input {:placeholder "Your email"
                     :class       (util/mc style/input
                                           (when error style/input--error))
                     :name        "create-company-form__user-email"
                     :value       (or @user-email
                                      (get existing-params "create-company-form__user-email"))}]

            [section-button {:title "Create company"}]]
           (when error
             [:p (util/smc style/error-message) error])])))))

(defn top-ranking-companies [companies redirect logged-in? query-params]
  [:div {:class (util/mc style/tabs__tab-content style/tabs__tab-content--companies)}
   [section-elements companies card-company]
   [:div {:class (util/mc style/footer style/footer--companies)}
    [section-button {:title "All companies"
                     :href  (routes/path :companies)}]
    [create-company redirect logged-in? query-params]]])

;; -------------------------------------------

(defn format-blog-date
  [t]
  (->> t
       (tf/parse)
       (tf/unparse (tf/formatter "MMM d"))))

(defn card-blog [{:keys [title author-info tags creation-date id upvote-count reading-time] :as _blog}]
  [:section {:class style/section__element}
   [card-link {:title title
               :href  (routes/path :blog :params {:id id})}]
   [connected-entity {:title    (:name author-info)
                      :subtitle (str/join " • " [(format-blog-date creation-date)
                                                 (str reading-time " min read")
                                                 (str upvote-count " boosts")])
                      :img-src  (:image-url author-info)
                      :img-type :rounded}]
   [card-tags tags]])

(defn write-article []
  [:div
   [:h3 {:class style/footer__title} "Write an article"]
   [:p {:class style/footer__text} "Share your fresh thinking and unique perspectives with developers from around the world."]
   [section-button {:title "Write an article"
                    :href  (routes/path :contribute)
                    :type  :no-border}]])

(defn top-ranking-blogs [blogs]
  [:div {:class (util/mc style/tabs__tab-content style/tabs__tab-content--blogs)}
   [section-elements blogs card-blog]
   [:div {:class (util/mc style/footer style/footer--blogs)}
    [section-button {:title "All articles"
                     :href  (routes/path :learn)}]
    [write-article]]])

;; -------------------------------------------

(defn top-ranking [{:keys [companies blogs default-tab redirect logged-in? query-params]
                    :or   {default-tab :companies}}]
  (let [input1-id  (gensym "top-ranking-input")
        input2-id  (gensym "top-ranking-input")
        input-name (gensym "top-ranking-name")]
    [:section {:class (util/mc style/section style/section--with-tabs)}
     [section-title "Top rankings"]
     [:input (cond-> {:id    input1-id
                      :class (util/mc style/tabs__input style/tabs__input--companies)
                      :type  "radio"
                      :name  input-name}
                     (= :companies default-tab) (assoc #?(:clj :checked :cljs :default-checked) true))]
     [:input (cond-> {:id    input2-id
                      :class (util/mc style/tabs__input style/tabs__input--blogs)
                      :type  "radio"
                      :name  input-name}
                     (= :blogs default-tab) (assoc #?(:clj :checked :cljs :default-checked) true))]
     [:nav {:class style/tabs__wrapper}
      [:ul {:class style/tabs}
       [:li
        [:label {:for input1-id :class (util/mc style/tabs__tab style/tabs__tab--companies)} "Companies"]]
       [:li
        [:label {:for input2-id :class (util/mc style/tabs__tab style/tabs__tab--blogs)} "Blogs"]]]]
     [:section {:class style/tabs__content}
      [top-ranking-companies companies redirect logged-in? query-params]
      [top-ranking-blogs blogs]]]))
