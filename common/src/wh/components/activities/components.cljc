(ns wh.components.activities.components
  (:require [clojure.string :as str]
            [wh.common.data :refer [currency-symbols]]
            [wh.common.text :as text]
            [wh.common.time :as time]
            [wh.components.card-actions.components :as card-actions-components]
            [wh.components.common :refer [wrap-img img]]
            [wh.components.icons :as icons]
            [wh.components.skeletons.components :as skeletons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.landing-new.events :as events]
            [wh.re-frame.events :refer [dispatch]]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn keyed-collection [children]
  (->> children
       (filter (complement nil?))
       (map-indexed (fn [idx child]
                      (with-meta child {:key idx})))))

(def button-class
  {:filled              (util/mc styles/button)
   :filled-short        (util/mc styles/button styles/button--short)
   :dark                (util/mc styles/button styles/button--dark)
   :inverted            (util/mc styles/button styles/button--inverted)
   :inverted-highlighed (util/mc styles/button styles/button--inverted-highlighted)})

(defn button [{:keys [type href event-handlers on-click]
               :or   {type :filled event-handlers {}}}
              children]
  [:a (merge {:class (button-class type)}
             (when href
               {:href href})
             (when on-click
               (interop/on-click-fn on-click)) ;; we assume it's already interop-friendly
             event-handlers)
   children])

(def title-class
  {:medium styles/title
   :large  (util/mc styles/title styles/title--large)})

(defn title
  [{:keys [href type margin]
    :or   {type :large}}
   children]
  [:h1 {:class (cond-> (title-class type)
                       margin (util/mc styles/title--margin))}
   [:a {:class (util/mc styles/title__link)
        :href  href}
    children]])

(def description-class
  {:full-text (util/mc styles/description)
   :cropped   (util/mc styles/description styles/description--cropped)})

(defn description
  ([children]
   (description {} children))
  ([{:keys [type class]
     :or   {type :full-text}}
    children]
   (when children
     [:p (util/smc (description-class type) class) children])))

(defn tag-component [tag ref]
  (let [href (routes/path :search
                          :query-params (merge {:query (:label tag)}
                                               (when ref {:ref ref})))]

    [tag/tag :a (-> tag
                    (assoc :href href)
                    (update :label str/lower-case))]))

(defn tags
  ([tags-list]
   (tags tags-list nil))
  ([tags-list ref]
   [:ul (util/smc "tags" "tags--inline")
    (for [tag (take 14 tags-list)]                               ;; FIXME magic number
      ^{:key (:id tag)}
      [tag-component tag ref])]))

(defn entity-icon
  ([icon-name]
   [entity-icon icon-name :default])
  ([icon-name type]
   [:div (util/smc styles/entity-icon
                   [(= type :highlight) styles/entity-icon--highlight])
    [icons/icon icon-name :class styles/entity-icon__icon]]))


(def promoted-entity-description
  {:job     "Hiring Now "
   :issue   "Help Needed "
   :blog    "Our Pick "
   :company "Hot Company "})

(def promoted-entity-title
  {:blog           "Article"
   :issue          "Issue"
   :company        "Company"
   :job            "Job"
   :company-hiring "Hiring Now"})

(defn entity-description [type entity-type]
  (let [title (promoted-entity-title type)]
    [:div (util/smc styles/entity-description
                    [(= entity-type :highlight) styles/entity-description--highlight]
                    [(= entity-type :promote) styles/entity-description--promote]
                    [(= entity-type :interview-requests) styles/entity-description--interview-requests])
     (case entity-type
       :publish   (str "New " title)
       :highlight [:span (util/smc styles/entity-description--icon-wrapper)
                   [:span (util/smc styles/entity-description--adjective)
                    (str "Trending " title)]
                   [icons/icon "trends" :class styles/entity-description--icon]]
       :promote   [:span (util/smc styles/entity-description--icon-wrapper)
                   [:span (util/smc styles/entity-description--adjective)
                    (promoted-entity-description type)]
                   [icons/icon "trends"
                    :class styles/entity-description--icon]]
       :interview-requests [:span (util/smc styles/entity-description--icon-wrapper)
                            [:span (util/smc styles/entity-description--adjective) title]
                            [icons/icon "trends" :class styles/entity-description--icon]])]))

(defn company-info-stat [company-actor type verb]
  (let [live-issues        (get company-actor :total-published-issue-count 0)
        job-count          (get company-actor :total-published-job-count 0)
        company-joined-msg (str "Joined " (some-> (:creation-date company-actor)
                                                  (time/str->time :date-time)
                                                  (time/human-time)))
        live-jobs-msg      (str "has " job-count " " (text/pluralize job-count "live job"))
        live-issues-msg    (str "has " live-issues " " (text/pluralize live-issues "issue"))]
    (match [type verb]
           [:company :interview-requests] live-jobs-msg
           [:company _] company-joined-msg
           [:job _] live-jobs-msg
           [:issue _] live-issues-msg
           :else company-joined-msg)))

(defn company-info
  ([company-actor]
   (company-info company-actor :company))
  ([company-actor type]
   (company-info company-actor type nil))
  ([{:keys [image-url name slug] :as company-actor} type verb]
   (let [info-str (company-info-stat company-actor type verb)]
     [:a {:class styles/company-info
          :href  (routes/path :company
                              :params {:slug slug}
                              :query-params (if (= verb :interview-requests)
                                              {:ref "interview-requests-activity"}
                                              {}))}
      (wrap-img img image-url {:w 40 :h 40 :fit "clip" :class styles/company-info__logo})
      [:h1 (util/smc styles/company-info__name) name]
      (when info-str
        [:span (util/smc styles/company-info__job-count) info-str])])))

(defn company-info-small [{:keys [image-url name slug]}]
  [:a {:class styles/company-info--small
       :href  (routes/path :company :params {:slug slug})}
   (wrap-img img image-url {:w 30 :h 30 :fit "clip" :class styles/company-info--small__logo})
   [:h1 (util/smc styles/company-info--small__name) name]])

(def actions card-actions-components/actions)

(defn- candidate-container [{:keys [id]} & children]
  [:a {:class (util/mc styles/author)
       :href  (routes/path :user :params {:id id})}
   children])

(defn- author-container [{:as _opts} & children]
  [:span {:class (util/mc styles/author)}
   children])

(defn author [{:keys [img name id]}]
  (let [container (if id candidate-container author-container)]
    [container {:id id}
     (when img
       ^{:key "author-img"}
       [:img {:class (util/mc styles/author__img)
              :src   img}])

     ^{:key "author-name"}
     [:p
      (util/smc styles/author__name
                [id styles/author__name--candidate])
      name]]))

(defn footer [type & children]
  [:div (util/smc styles/footer (when (= type :compound) styles/footer--compound))
   (keyed-collection children)])

(defn footer-buttons [& children]
  [:div (util/smc styles/footer__buttons)
   (keyed-collection children)])

(defn header [& children]
  [:div (util/smc styles/header)
   (keyed-collection children)])

(defn meta-row [& children]
  [:div (util/smc styles/meta-row)
   (keyed-collection children)])

(defn title-with-icon [& children]
  [:div (util/smc styles/title-with-icon)
   (keyed-collection children)])

(defn text-with-icon [{:keys [icon]} children]
  (when children
    [:div (util/smc styles/text-with-icon)
     [:div (util/smc styles/text-with-icon__icon) [icons/icon icon]]
     [:span children]]))

(defn inner-card [& children]
  [:div (util/smc styles/inner-card)
   (keyed-collection children)])

(defn card [type & children]
  [:div (merge (util/smc styles/card
                         [(= type :highlight) styles/card--highlight]
                         [(= type :promote) styles/card--promote]
                         [(= type :interview-requests) styles/card--interview-requests])
               {:data-test "activity"})
   (keyed-collection children)])

(defn card-content [& children]
  [:div (util/smc styles/card-content)
   (keyed-collection children)])

(defn card-skeleton []
  [card :skeleton
   [skeletons/image-with-info]
   [skeletons/text]
   [inner-card
    [skeletons/title]
    [skeletons/text]
    [skeletons/tags]]
   [footer :default
    [skeletons/button]]])

(defn card-not-found [selected-tags]
  (let [[text1 text2] (if (< (count selected-tags) 2)
                        ["Hmm, looks likes there’s currently no content that features that particular tag."
                         "Be the first to create some content."]
                        ["Hmm, looks likes there’s currently no content that features these tags."
                         "Be the first to create some content."])]
    [card :not-found
     [:div (util/smc styles/not-found)
      [tag/tag-list
       :li
       (map #(assoc %
                    :inverted? true
                    :label (:slug %))
            selected-tags)]
      [:span (util/smc styles/not-found__title) "Oops, it's empty"]
      [:span (util/smc styles/not-found__subtitle) text1]
      [:span (util/smc styles/not-found__subtitle) text2]
      [button {:href (routes/path :contribute) :type :filled-short} "Write an article"]]]))

(defn improve-feed-recommendations []
  [card :default
   [:div (util/smc styles/not-found)
    [:span (util/smc styles/not-found__title) "Improve recommendations"]
    [:span (util/smc styles/not-found__subtitle) "This is all the content we have selected for you. Add skills to see more great content."]
    [button {:href (routes/path :profile) :type :filled-short} "Add skills"]]])

(defn see-all-content []
  [card :default
   [:div (util/smc styles/not-found)
    [:span (util/smc styles/not-found__title) "See all content"]
    [:span (util/smc styles/not-found__subtitle) "See all jobs, articles & issues without your personal filter."]
    [button (merge
              {:type :filled-short}
              (interop/on-click-fn
                #?(:clj  "setPublicFeed();"
                   :cljs #(dispatch [::events/set-public-feed]))))
     "See all content"]]])

(defn currency-symbol [compensation]
  (some-> compensation
          :currency
          name
          currency-symbols))

(defn compensation-amount [compensation]
  (let [amount (or (:amount compensation) 0)]
    (when-not (zero? amount)
      [tag/tag :div  {:label (str (currency-symbol compensation) amount)
                      :type  "funding"}])))

(defn primary-language [repo]
  [:div (util/smc styles/issue__tag-primary-language)
   [tag/tag :div  {:label (:primary-language repo)
                   :type  "tech"}]])

(defn promoter
  ([promoter-data]
   [promoter {} promoter-data])
  ([{:keys [class] :as _opts}
    {:keys [image-url name id] :as _promoter-data}]
   [:div (when class (util/smc class))
    [:a {:href  (routes/path :user :params {:id id})
         :class styles/promoter__details}
     (wrap-img img image-url {:w 40 :h 40 :class styles/promoter__logo})
     [:h1 (util/smc styles/promoter__name) name]
     [:span (util/smc styles/promoter__position) "Talent Manager, WorksHub"]]]))

(defn ->interviews-display-value [{:keys [interviews-count interviews-period bold-count?]}]
  (let [count-msg (str interviews-count " " (text/pluralize interviews-count "interview"))]
    [:<>
     (if bold-count? [:b count-msg] [:span count-msg])
     " "
     interviews-period]))
