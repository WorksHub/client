(ns wh.search.components
  (:require [clojure.string :as str]
            [wh.components.side-card.components :as c]
            [wh.re-frame :refer [atom]]
            [wh.components.skeletons.components :as skeletons]
            [wh.routes :as routes]
            [wh.styles.search :as styles]
            [wh.util :as util]))


(defn job-result [{:keys [title company tagline slug]}]
  [:li
   [:a {:class (util/mc styles/result-card)
        :href  (routes/path :job :params {:slug slug})}
    [:img {:src   (:logo company)
           :class (util/mc styles/result-card__img)}]

    [:div (util/smc styles/result-card__content)
     [:h1 (util/smc styles/result-card__title) title]
     [:span (util/smc styles/result-card__sub-title) (:name company)]
     [:span (util/smc styles/result-card__description) tagline]]]])

(defn company-result [{:keys [name logo description slug]}]
  [:li
   [:a {:class (util/mc styles/result-card)
        :href  (routes/path :company :params {:slug slug})}
    [:img {:src   logo
           :class (util/mc styles/result-card__img)}]

    [:div (util/smc styles/result-card__content)
     [:span (util/smc styles/result-card__title) name]
     [:span (util/smc styles/result-card__description) description]]]])

(defn issue-result [{:keys [title company objectID]}]
  [:li
   [:a {:class (util/mc styles/result-card)
        :href  (routes/path :issue :params {:id objectID})}
    [:img {:src   (:logo company)
           :class (util/mc styles/result-card__img)}]

    [:div (util/smc styles/result-card__content)
     [:span (util/smc styles/result-card__description) (:name company)]
     [:span (util/smc styles/result-card__title) title]]]])

(defn article-result [{:keys [title author objectID] :as article}]
  [:li
   [:a {:class (util/mc styles/result-card)
        :href (routes/path :blog :params {:id objectID})}
    [:div (util/smc styles/result-card__content)
     [:span (util/smc styles/result-card__title) title]
     [:span (util/smc styles/result-card__sub-title) author]]]])

(defn skeleton-result []
  [:li (util/smc styles/result-card--skeleton)
   [skeletons/image-with-info]])

(def result-by-type*
  {:jobs      job-result
   :companies company-result
   :issues    issue-result
   :articles  article-result})

(defn result-by-type [id]
  (get result-by-type* id skeleton-result))

;; our main application div has id "app". id "app" is used here so when you click
;; on "All", it scrolls to top of application
(def tabs-coll [{:id        :app
                 :menu/name "All"}
                {:id           :jobs
                 :menu/name    "Jobs"
                 :section/name "Jobs"
                 :see-all      (routes/path :jobsboard)}
                {:id           :companies
                 :menu/name    "Companies"
                 :section/name "Companies"
                 :see-all      (routes/path :companies)}
                {:id           :issues
                 :menu/name    "Open Source Issues"
                 :section/name "Issues"
                 :see-all      (routes/path :issues)}
                {:id           :articles
                 :menu/name    "Articles"
                 :section/name "Articles"
                 :see-all      (routes/path :learn)}])

(def sections-coll
  (filter :section/name tabs-coll))


(defn tabs [{:keys [go-to-tab]}]
  (let [active-tab (atom (-> tabs-coll first :id))]
    (fn [{:keys [go-to-tab]}]
      (let [active-tab-val #{@active-tab}]
        [:ul {:class styles/tabs}
         (for [{:as tab :keys [id]} tabs-coll]
           ^{:key id}
           [:li {:class    (util/mc styles/tab
                                    [(active-tab-val id) styles/tab--active])
                 :on-click (fn []
                             (reset! active-tab id)
                             (go-to-tab (name id)))}
            (:menu/name tab)])]))))

(defn results-title [query results-count]
  [:h1 {:class styles/results-title}
   (if results-count
     (str results-count " results found for ‘" query "’")

     (str "Searching for ‘" query "’"))])

(defn skeleton-results
  ([]
   (skeleton-results 3))

  ([n]
   (map
     (fn [_]
       [skeleton-result])
     (range n))))

(defn results-section [{:keys [search-result see-all] :as tab}]
  (let [hits-counted (get search-result :nbHits 0)
        hits         (get search-result :hits [])]
    [:section {:class styles/results-section
               :id    (name (:id tab))}
     [:div (util/smc styles/results-section__header)
      [:h1 (util/smc styles/results-section__header__title)
       (:section/name tab)
       [:span (util/smc styles/results-section__header__title__hits)
        (str " (" hits-counted ")")]]

      [:div (util/smc styles/results-section__header__button)
       [c/section-button {:title (str "See " (str/lower-case (:section/name tab)))
                          :href  see-all
                          :text  :default
                          :size  :small}]]]

     [:ul {:class styles/results-section__content}
      (if-not (seq hits)
        [skeleton-results]

        (for [{:keys [objectID] :as hit} hits]
          ^{:key objectID}
          [(result-by-type (:id tab)) hit]))]]))

(defn sections-separated [sections]
  [:<>
   (->> sections
        (interpose :hr)
        (map-indexed
          (fn [idx {:keys [id] :as tab}]
            (if (= tab :hr)
              ^{:key idx}
              [:hr]

              ^{:key id}
              [results-section tab]))))])
