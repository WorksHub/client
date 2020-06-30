(ns wh.components.side-card.side-card
  (:require [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [clojure.string :as str]
            [wh.common.text :as text]
            [wh.components.common :refer [link]]
            [wh.components.side-card.components :as c]
            [wh.re-frame :as r]
            [wh.routes :as routes]
            [wh.styles.side-card :as style]
            [wh.util :as util]))

(defn card-issue [{:keys [company title id level repo compensation] :as _issue}]
  [:section {:class style/section__element}
   [c/connected-entity {:title    (:name company)
                        :subtitle (str "Level: " (str/capitalize (name level)))
                        :href     (routes/path :company :params {:slug (:slug company)})
                        :img-src  (:logo company)}]
   [c/card-link {:href  (routes/path :issue :params {:id id})
                 :title title}]
   [:ul {:class style/element__tags}
    (when-let [language (:primary-language repo)]
      [c/card-tag {:label language
                   :type  "tech"}])
    (let [amount (or (:amount compensation) 0)]
      (when-not (zero? amount)
        [c/card-tag {:label (str "$" amount)
                     :type  "funding"}]))]])

(defn recent-issues [issues loading?]
  [:section {:class style/section}
   [c/section-title "Live Open Source Issues"]
   (if loading?
     [c/section-elements-skeleton {:type :tags}]
     [c/section-elements issues card-issue])
   (when-not loading?
     [:div {:class (util/mc style/footer style/footer--issues)}
      [c/section-button {:title "All open source issues"
                         :href  (routes/path :issues)}]
      [c/footer-link {:text      "Post your"
                      :bold-text "open source issue here"
                      :href      (routes/path :create-job)}]])])

;; -----------------------------------------------

(defn format-user-date
  [t]
  (->> t
       (tf/parse)
       (tf/unparse (tf/formatter "yyyy"))))

(defn card-user [{:keys [blog-count issue-count created name image-url] :as _user}]
  [:section {:class style/section__element}
   [c/connected-entity {:title      name
                        :title-type :primary
                        :subtitle   (str "Joined in " (format-user-date created))
                        :img-src    image-url
                        :img-type   :rounded}]
   [c/numeric-info [{:number blog-count
                     :sing   "article published"
                     :plural "articles published"
                     :icon   "article"}
                    {:number issue-count
                     :sing   "open source issue started"
                     :plural "open source issues started"
                     :icon   "commit"}]]])

(defn top-ranking-users [users loading?]
  [:section {:class style/section}
   [c/section-title "Top Ranking Users"]
   (if loading?
     [c/section-elements-skeleton]
     [c/section-elements users card-user])
   (when-not loading?
     [:div
      [c/section-button {:title "Write an Article"
                         :href  (routes/path :contribute)}]])])

;; -----------------------------------------------

(defn card-job [{:keys [title company-info slug] :as _job}]
  [:section {:class style/section__element}
   (let [jobs-count (:total-published-job-count company-info)
         text       (str jobs-count (text/pluralize jobs-count " live job"))]
     [c/connected-entity {:title    (:name company-info)
                          :subtitle text
                          :href     (routes/path :company :params {:slug (:slug company-info)})
                          :img-src  (:logo company-info)}])
   [c/card-link {:title title
                 :href  (routes/path :job :params {:slug slug})}]])

(defn recent-jobs [jobs loading?]
  [:section {:class style/section}
   [c/section-title "Hiring now"]
   (if loading?
     [c/section-elements-skeleton]
     [c/section-elements jobs card-job])
   (when-not loading?
     [:div {:class (util/mc style/footer style/footer--jobs)}
      [c/section-button {:title "All live jobs"
                         :href  (routes/path :jobsboard)}]
      [c/footer-link {:text      "Hiring?"
                      :bold-text "Post your job here"
                      :href      (routes/path :create-job)}]])])

;; -----------------------------------------------

(defn card-company [{:keys [name total-published-issue-count total-published-job-count logo slug locations tags] :as _company}]
  [:section {:class style/section__element}
   (let [location     (first locations)
         location-str (str (:city location) ", " (:country location))]
     [c/connected-entity {:title    name
                          :subtitle location-str
                          :href     (routes/path :company :params {:slug slug})
                          :img-src  logo}])
   [c/numeric-info [{:number total-published-job-count
                     :icon   "case"
                     :sing   "live role"
                     :plural "live roles"}
                    {:number total-published-issue-count
                     :icon   "commit"
                     :sing   "live open source issue"
                     :plural "live open source issues"}]]
   [c/card-tags tags]])

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

            [c/section-button {:title "Create company"}]]
           (when error
             [:p (util/smc style/error-message) error])])))))

(defn top-ranking-companies [companies redirect logged-in? query-params loading?]
  [:div {:class (util/mc style/tabs__tab-content style/tabs__tab-content--companies)}
   (if loading?
     [c/section-elements-skeleton {:type :tags}]
     [:div
      [c/section-elements companies card-company]
      [:div {:class (util/mc style/footer style/footer--companies)}
       [c/section-button {:title "All companies"
                          :href  (routes/path :companies)}]
       [create-company redirect logged-in? query-params]]])])

;; -------------------------------------------

(defn format-blog-date
  [t]
  (->> t
       (tf/parse)
       (tf/unparse (tf/formatter "MMM d"))))

(defn card-blog [{:keys [title author-info tags creation-date id upvote-count reading-time] :as _blog}]
  [:section {:class style/section__element}
   [c/card-link {:title title
                 :href  (routes/path :blog :params {:id id})}]
   [c/connected-entity {:title    (:name author-info)
                        :subtitle (str/join " • " [(format-blog-date creation-date)
                                                   (str reading-time " min read")
                                                   (str upvote-count " boosts")])
                        :img-src  (:image-url author-info)
                        :img-type :rounded}]
   [c/card-tags tags]])

(defn write-article []
  [:div
   [:h3 {:class style/footer__title} "Write an article"]
   [:p {:class style/footer__text} "Share your fresh thinking and unique perspectives with developers from around the world."]
   [c/section-button {:title "Write an article"
                      :href  (routes/path :contribute)
                      :type  :no-border}]])

(defn top-ranking-blogs [blogs loading?]
  [:div {:class (util/mc style/tabs__tab-content style/tabs__tab-content--blogs)}
   (if loading?
     [c/section-elements-skeleton {:type :default}]
     [:div
      [c/section-elements blogs card-blog]
      [:div {:class (util/mc style/footer style/footer--blogs)}
       [c/section-button {:title "All articles"
                          :href  (routes/path :learn)}]
       [write-article]]])])

;; -------------------------------------------

(defn top-ranking [{:keys [companies blogs default-tab redirect logged-in? query-params loading?]
                    :or   {default-tab :companies}}]
  (let [input1-id  (gensym "top-ranking-input")
        input2-id  (gensym "top-ranking-input")
        input-name (gensym "top-ranking-name")]
    [:section {:class (util/mc style/section style/section--with-tabs)}
     [c/section-title "Top rankings"]
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
      [top-ranking-companies companies redirect logged-in? query-params loading?]
      [top-ranking-blogs blogs loading?]]]))