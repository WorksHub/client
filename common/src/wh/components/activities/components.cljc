(ns wh.components.activities.components
  (:require [clojure.string :as str]
            [wh.common.data :refer [currency-symbols]]
            [wh.common.text :as text]
            [wh.common.time :as time]
            [wh.components.common :refer [wrap-img img]]
            [wh.components.icons :as icons]
            [wh.components.skeletons.components :as skeletons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.styles.activities :as styles]
            [wh.util :as util]
            [wh.verticals :as verticals]))

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
  {:full-text (util/smc styles/description)
   :cropped   (util/smc styles/description styles/description--cropped)})

(defn description
  ([children]
   (description {} children))
  ([{:keys [type]
     :or   {type :full-text}}
    children]
   (when children
     [:p (description-class type) children])))

(defn quoted-description
  ([text]
   (quoted-description nil text))
  ([{:keys [class] :as _opts} text]
   [:p (util/smc styles/quoted-description class) (str "“" text "”")]))

(defn tag-component [tag]
  (let [href (routes/path :search :query-params {:query (:slug tag)})]
    [tag/tag :a (-> tag
                    (assoc :href href)
                    (update :label str/lower-case))]))

(defn tags
  [tags]
  [:ul (util/smc "tags" "tags--inline")
   (for [tag (take 14 tags)] ;; FIXME magic number
     ^{:key (:id tag)}
     [tag-component tag])])

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
  {:blog    "Article"
   :issue   "Issue"
   :company "Company"
   :job     "Job"})

(defn entity-description [type entity-type]
  (let [title (promoted-entity-title type)]
    [:div (util/smc styles/entity-description
                    [(= entity-type :highlight) styles/entity-description--highlight]
                    [(= entity-type :promote) styles/entity-description--promote])
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
                    :class styles/entity-description--icon]])]))

(defn company-info [{:keys [image-url name slug] :as company}]
  (let [info-str (let [live-issues (get company :total-published-issue-count 0)
                       job-count (get company :total-published-job-count 0)]
                   (cond
                     (pos? live-issues)
                     (str "has " live-issues " " (text/pluralize live-issues "issue"))
                     ;;
                     (pos? job-count)
                     (str "has " job-count " " (text/pluralize job-count "live job"))
                     ;;
                     (:creation-date company)
                     (str "Joined " (-> (:creation-date company)
                                        (time/str->time :date-time)
                                        (time/human-time)))))]

    [:a {:class styles/company-info
         :href  (routes/path :company :params {:slug slug})}
     (wrap-img img image-url {:w 40 :h 40 :class styles/company-info__logo})
     [:h1 (util/smc styles/company-info__name) name]
     (when info-str
       [:span (util/smc styles/company-info__job-count) info-str])]))

(defn company-info-small [{:keys [image-url name slug] :as _company}]
  [:a {:class styles/company-info--small
       :href  (routes/path :company :params {:slug slug})}
   (wrap-img img image-url {:w 30 :h 30 :class styles/company-info--small__logo})
   [:h1 (util/smc styles/company-info--small__name) name]])

(defn share-controls
  [share-opts share-id share-toggle-on-click]
  (let [social-button-opts (merge share-toggle-on-click
                                  {:class  styles/actions__share-button
                                   :rel    "noopener"
                                   :target "_blank"})]
    [:div (util/smc styles/actions__share)
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://twitter.com/intent/tweet?text=Check out %s at %s %s"
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :twitter)
                      (:url share-opts))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-twitter.svg"}]]
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://www.linkedin.com/shareArticle?mini=true&url=%s&title=%s&summary=Check+out+%s+at+%s&source=%s"
                      (:url share-opts)
                      (:content-title share-opts)
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :platform-name)
                      (verticals/config (:vertical share-opts) :platform-name))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-linkedin.svg"}]]
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://www.facebook.com/dialog/share?app_id=%s&display=popup&href=%s&quote=Check+out+%s+at+%s"
                      (:facebook-app-id share-opts)
                      (:url share-opts)
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :platform-name))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-facebook.svg"}]]
     [:div (merge (interop/multiple-on-click
                    share-toggle-on-click
                    (interop/copy-str-to-clipboard-on-click (:url share-opts)))
                  (util/smc styles/actions__share-button styles/actions__share-button--center))
      [icons/icon "copy"]]
     [:a (merge share-toggle-on-click
                (util/smc styles/actions__share-button))
      [icons/icon "close"]]]))

(defn actions [{:keys [like-opts save-opts share-opts saved?]}]
  (let [share-id              (str (gensym "actions") "-" (:id share-opts))
        share-toggle-on-click (interop/toggle-class-on-click share-id styles/actions__inner--open)]
    [:div (util/smc styles/actions)
     [:div {:class (util/mc styles/actions__inner)
            :id    share-id}
      ;;
      (when share-opts
        [share-controls share-opts share-id share-toggle-on-click])
      ;;
      [:div (util/smc styles/actions__container)
       (when save-opts
         [:a (merge (util/smc styles/actions__action
                              styles/actions__action--save) save-opts)
          [icons/icon "save"
           :class (when saved? styles/actions__action--saved-icon)]])
       (when share-opts
         [:a (merge (util/smc styles/actions__action styles/actions__action--share)
                    share-toggle-on-click)
          [icons/icon "network"]])]]]))


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
       [:img {:class (util/mc styles/author__img)
              :src   img}])
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
                         [(= type :promote) styles/card--promote])
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
                        ["Hmm, looks likes there’s currently no content that features that particular language"
                         "Be the first to create some content for that language"]
                        ["Hmm, looks likes there’s currently no content that features these languages"
                         "Be the first to create some content for these languages"])]
    [card :not-found
     [:div (util/smc styles/not-found)
      [tag/tag-list
       :li
       (map #(assoc %
                    :inverted? true
                    :label (:slug %))
            selected-tags)]
      [:span (util/smc styles/not-found__title) text1]
      [:span (util/smc styles/not-found__subtitle) text2]
      [button {:href (routes/path :contribute) :type :filled-short} "Write an article"]]]))

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
    {:keys [image-url name] :as _promoter-data}]
   [:div (when class (util/smc class))
    [:div (util/smc styles/promoter__details)
     (wrap-img img image-url {:w 40 :h 40 :class styles/promoter__logo})
     [:h1 (util/smc styles/promoter__name) name]
     [:span (util/smc styles/promoter__position) "Talent Manager, WorksHub"]]]))
