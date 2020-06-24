(ns wh.components.activities.components
  (:require [clojure.string :as str]
            [wh.components.icons :as icons]
            [wh.components.tag :as tag]
            [wh.routes :as routes]
            [wh.slug :as slug]
            [wh.styles.feed-published-cards :as styles]
            [wh.util :as util]))

(def button-class
  {:filled   (util/mc styles/button)
   :inverted (util/mc styles/button styles/button--inverted)})

(defn button [{:keys    [type href]
                    :or {type :filled}}
              children]
  [:a {:class (button-class type)
       :href  href}
   children])

(def title-class
  {:medium styles/title
   :large (util/mc styles/title styles/title--large)})

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

(defn tag-component [tag]
  (let [href (routes/path :pre-set-search :params {:tag (slug/slug (:slug tag))})
        tag  (assoc tag :href href)
        tag  (update tag :label str/lower-case)]
    [tag/tag :a tag]))

(defn tags
  [tags]
  [:ul (util/smc "tags" "tags--inline")
   (for [tag (take 14 tags)] ;; FIXME magic number
     ^{:key (:slug tag)}
     [tag-component tag])])

(defn entity-icon [icon-name]
  [:div (util/smc styles/entity-icon)
   [icons/icon icon-name :class styles/entity-icon__icon]])

(defn company-info [{:keys [logo name slug] :as company}]
  (let [info-str (cond
                   (:total-published-issue-count company) (str "Live issues: " (get company :total-published-issue-count 0))
                   (:total-published-job-count company) (str "Live jobs: " (get company :total-published-job-count 0))
                   :else nil)]
    [:a {:class styles/company-info
         :href (routes/path :company :params {:slug slug})}
     [:img {:class styles/company-info__logo
            :src   logo}]
     [:h1 (util/smc styles/company-info__name) name]
     (when info-str
       [:span (util/smc styles/company-info__job-count)
        info-str])]))

(defn actions [{:keys    [like-opts save-opts share-opts]
                :or {like-opts {} save-opts {} share-opts {}}}]
  [:div (util/smc styles/actions)
   [:a (merge (util/smc styles/actions__action styles/actions__action--save) save-opts)
    [icons/icon "save"]]])

(defn author [{:keys [img]} children]
  [:div (util/smc styles/author)
   (when img
     [:img {:class (util/mc styles/author__img)
            :src   img}])
   [:p
    (util/smc styles/author__name)
    children]])

(defn footer [type & children]
  [:div (util/smc styles/footer (when (= type :compound) styles/footer--compound))
   (for [child children]
     child)])

(defn footer-buttons [& children]
  [:div (util/smc styles/footer__buttons)
   (for [child children]
     child)])

(defn header [& children]
  [:div (util/smc styles/header)
   (for [child children]
     child)])

(defn meta-row [& children]
  [:div (util/smc styles/meta-row)
   (for [child children]
     child)])

(defn text-with-icon [{:keys [icon]} children]
  [:div (util/smc styles/text-with-icon)
   [:div (util/smc styles/text-with-icon__icon) [icons/icon icon]]
   [:span children]])

(defn inner-card [& children]
  [:div (util/smc styles/inner-card)
   (for [child children]
     child)])

(defn card [& children]
  [:div (util/smc styles/card)
   (for [child children]
     child)])

(defn card-content [& children]
  [:div (util/smc styles/card-content)
   (for [child children]
     child)])
