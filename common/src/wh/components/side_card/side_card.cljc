(ns wh.components.side-card.side-card
  (:require [clojure.string :as str]
            [#?(:cljs cljs-time.coerce
                :clj clj-time.coerce) :as tc]
            [#?(:cljs cljs-time.format
                :clj clj-time.format) :as tf]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon] :as icons]
            [wh.components.tag :as tag]
            [wh.common.text :refer [pluralize]]
            [wh.routes :as routes]
            [wh.styles.side-card :as style]
            [wh.util :refer [merge-classes random-uuid]]))

(defn card-tags [tags]
  [tag/tag-list :a (->> tags
                        (filter #(and (= (:subtype %) "software")
                                      (= (:type %) "tech")))
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
  (let [wrapper-tag (if href :a :div)
        wrapper-class (cond-> style/connected-entity
                              href (merge-classes style/connected-entity--link))
        img-class (cond-> style/connected-entity__image
                          (= img-type :rounded) (merge-classes style/connected-entity__image--rounded))
        title-class (cond->
                      style/connected-entity__title
                      (= title-type :primary) (merge-classes style/connected-entity__title--primary))]
    [wrapper-tag {:class wrapper-class
                  :href href}
     [:img {:src img-src :class img-class}]
     [:div {:class style/connected-entity__info}
      [:span {:class title-class} title]
      [:span {:class (merge-classes style/connected-entity__title style/connected-entity__title--minor)} subtitle]]]))

(defn section-title [title]
  [:h3 {:class style/section__title} title])

(defn section-elements [elements card-component]
  [:div {:class style/section__elements}
   (for [elm elements]
     [card-component elm])])

(defn numeric-info [lines]
  [:div {:class style/numeric-info}
   (for [{:keys [number sing plural icon]} lines]
     (when (and number (pos? number))
       [:div {:class style/numeric-info__line}
        [icons/icon icon :class style/icon]
        (str number " " (pluralize number sing plural))]))])

(defn section-button [{:keys [title href type]}]
  [:a {:class (cond-> style/button
                      (= :no-border type) (merge-classes style/button--text))
       :href href}
   title])

(defn card-link [{:keys [title href]}]
  [:a {:href  href
       :class style/element__link}
   title])

; ----------------------------------------------

(defn card-issue [{:keys [company title id level repo compensation] :as _issue}]
  [:section {:class style/section__element}
   [connected-entity {:title (:name company)
                      :subtitle (str "Level: " (str/capitalize level))
                      :href (routes/path :company :params {:slug (:slug company)})
                      :img-src (:logo company)}]
   [card-link {:href (routes/path :issue :params {:id id})
               :title title}]
   [:ul {:class style/element__tags}
    (when-let [language (:primary-language repo)]
      [card-tag {:label language
                 :type "tech"}])
    (let [amount (or (:amount compensation) 0)]
      (when-not (zero? amount)
        [card-tag {:label (str "$" amount)
                   :type "funding"}]))]])

(defn live-issues [issues]
  [:section {:class style/section}
   [section-title "Live Open Source Issues"]
   [section-elements issues card-issue]
   [:div {:class (merge-classes style/footer style/footer--issues)}
    [section-button {:title "All open source issues"
                     :href (routes/path :issues)}]
    [footer-link {:text "Post your"
                  :bold-text "open source issue here"
                  :href (routes/path :create-job)}]]])

; -----------------------------------------------

(defn format-user-date
  [t]
  (->> t
       (tc/from-date)
       (tf/unparse (tf/formatter "yyyy"))))

(defn card-user [{:keys [blogs-count issues-count created] :as user}]
  [:section {:class style/section__element}
   [connected-entity {:title (:name user)
                      :title-type :primary
                      :subtitle (str "Joined in " (format-user-date created))
                      :img-src (:image_url user)
                      :img-type :rounded}]
   [numeric-info [{:number blogs-count
                   :sing "article published"
                   :plural "articles published"
                   :icon "article"}
                  {:number issues-count
                   :sing "open source issue started"
                   :plural "open source issues started"
                   :icon "commit"}]]])

(defn top-ranking-users [users]
  [:section {:class style/section}
   [section-title "Top Ranking Users"]
   [section-elements users card-user]
   [:div
    [section-button {:title "Write an Article"
                     :href (routes/path :contribute)}]]])

; -----------------------------------------------

(defn card-job [{:keys [title company slug] :as _job}]
  [:section {:class style/section__element}
   (let [jobs-count (:jobs-count company)
         text (str jobs-count (pluralize jobs-count " live job"))]
     [connected-entity {:title (:name company)
                        :subtitle text
                        :href (routes/path :company :params {:slug (:slug company)})
                        :img-src (:logo company)}])
   [card-link {:title title
               :href (routes/path :job :params {:slug slug})}]])

(defn recent-jobs [jobs]
  [:section {:class style/section}
   [section-title "Hiring now"]
   [section-elements jobs card-job]
   [:div {:class (merge-classes style/footer style/footer--jobs)}
    [section-button {:title "All live jobs"
                     :href (routes/path :jobsboard)}]
    [footer-link {:text "Hiring?"
                  :bold-text "Post your job here"
                  :href (routes/path :create-job)}]]])

; -----------------------------------------------

(defn card-company [{:keys [name tags jobs-count issues-count logo locations] :as company}]
  [:section {:class style/section__element}
   (let [location (first locations)
         location-str (str (:city location) ", " (:country location))]
     [connected-entity {:title name
                        :subtitle location-str
                        :href (routes/path :company :params {:slug (:slug company)})
                        :img-src logo}])
   [numeric-info [{:number jobs-count
                   :icon "case"
                   :sing "live role"
                   :plural "live roles"}
                  {:number issues-count
                   :icon "commit"
                   :sing "live open source issue"
                   :plural "live open source issues"}]]
   [card-tags tags]])

(defn create-company []
  [:div
   [:h3 {:class style/footer__title} "Create a company page"]
   [:p {:class style/footer__text} "Create a free profile page for your company — hire, contribute and build your engineering capabilities"]
   [:div {:class style/footer__form}
    [:input {:placeholder "Email" :class style/input}]
    [:input {:placeholder "Password" :class style/input}]]
   [section-button {:title "Create company"
                    :href (routes/path :create-company)}]])

(defn top-ranking-companies [companies]
  [:div {:class (merge-classes style/tabs__tab-content style/tabs__tab-content--companies)}
   [section-elements companies card-company]
   [:div {:class (merge-classes style/footer style/footer--companies)}
    [section-button {:title "All companies"
                     :href (routes/path :companies)}]
    [create-company]]])

; -------------------------------------------

(defn format-blog-date
  [t]
  (->> t
       (tc/from-date)
       (tf/unparse (tf/formatter "MMM d"))))

(defn card-blog [{:keys [title author-info tags creation-date] :as blog}]
  [:section {:class style/section__element}
   [card-link {:title title
               :href (routes/path :blog :params {:id (:id blog)})}]
   [connected-entity {:title (:name author-info)
                      :subtitle (str/join " • " [(format-blog-date creation-date)
                                                 (str (:reading-time blog) " min read")
                                                 (str (count (:upvotes blog)) " boosts")])
                      :img-src (:image_url author-info)
                      :img-type :rounded}]
   [card-tags tags]])

(defn write-article []
  [:div
   [:h3 {:class style/footer__title} "Write an article"]
   [:p {:class style/footer__text} "Share your fresh thinking and unique perspectives with developers from around the world."]
   [section-button {:title "Write an article"
                    :href (routes/path :contribute)
                    :type :no-border}]])

(defn top-ranking-blogs [blogs]
  [:div {:class (merge-classes style/tabs__tab-content style/tabs__tab-content--blogs)}
   [section-elements blogs card-blog]
   [:div {:class (merge-classes style/footer style/footer--blogs)}
    [section-button {:title "All articles"
                     :href (routes/path :learn)}]
    [write-article]]])

; -------------------------------------------

(defn top-ranking [{:keys [companies blogs default-tab]
                    :or {default-tab :companies}}]
  (let [input1-id (random-uuid)
        input2-id (random-uuid)
        name (random-uuid)]
    [:section {:class (merge-classes style/section style/section--with-tabs)}
     [section-title "Top rankings"]
     [:input (cond-> {:id      input1-id
                      :class   (merge-classes style/tabs__input style/tabs__input--companies)
                      :type    "radio"
                      :name    name}
                     (= :companies default-tab) (assoc :checked true))]
     [:input (cond-> {:id    input2-id
                      :class (merge-classes style/tabs__input style/tabs__input--blogs)
                      :type  "radio"
                      :name  name}
                     (= :blogs default-tab) (assoc :checked true))]
     [:nav {:class style/tabs__wrapper}
      [:ul {:class style/tabs}
       [:li
        [:label {:for input1-id :class (merge-classes style/tabs__tab style/tabs__tab--companies)} "Companies"]]
       [:li
        [:label {:for input2-id :class (merge-classes style/tabs__tab style/tabs__tab--blogs)} "Blogs"]]]]
     [:section {:class style/tabs__content}
      [top-ranking-companies companies]
      [top-ranking-blogs blogs]]]))
