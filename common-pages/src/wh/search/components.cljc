(ns wh.search.components
  (:require #?(:cljs [wh.pages.core :as pages])
            [clojure.string :as str]
            [re-frame.core :refer [dispatch]]
            [wh.components.side-card.components :as c]
            [wh.components.skeletons.components :as skeletons]
            [wh.components.tag :as tag]
            [wh.re-frame :refer [atom]]
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
                 :cta-text     #(str "More " % " Jobs")
                 :cta-link     #(routes/path :jobsboard :query-params {:search %})}
                {:id           :companies
                 :menu/name    "Companies"
                 :section/name "Companies"
                 :cta-text     #(str "All Companies")
                 :cta-link     #(routes/path :companies)}
                {:id           :issues
                 :menu/name    "Issues"
                 :section/name "Issues"
                 :cta-text     #(str "All Issues")
                 :cta-link     #(routes/path :issues)}
                {:id           :articles
                 :menu/name    "Articles"
                 :section/name "Articles"
                 :cta-text     #(str "All Articles")
                 :cta-link     #(routes/path :learn)}])

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
  [:h1 {:class     styles/results-title
        :data-test "search-results-title"}
   (cond
     (and results-count (not-empty query))
     (str results-count " results found for ‘" query "’")

     (and results-count (empty? query))
     "Latest results"

     (not-empty query)
     (str "Searching for ‘" query "’")

     :else (str "Loading…"))])

(defn skeleton-results
  ([]
   (skeleton-results 3))

  ([n]
   [:<>
    (map
      (fn [k]
        ^{:key k}
        [skeleton-result])
      (range n))]))

(defn results-section [{:keys [search-result cta-link cta-text] :as section} query]
  (let [hits-counted (get search-result :nbHits 0)
        hits         (get search-result :hits)
        empty        (get search-result :empty)]
    [:section {:class styles/results-section
               :id    (name (:id section))}
     [:div (util/smc styles/results-section__header)
      [:h1 (util/smc styles/results-section__header__title)
       (:section/name section)
       [:span (util/smc styles/results-section__header__title__hits)
        (str " (" hits-counted ")")]]

      [:div (util/smc styles/results-section__header__button)
       [c/section-button {:title (cta-text query)
                          :href  (cta-link query)
                          :text  :default
                          :size  :small}]]]

     [:ul {:class styles/results-section__content}
      (if (or (not hits) empty)
        [skeleton-results]

        (for [{:keys [objectID] :as hit} hits]
          ^{:key objectID}
          [(result-by-type (:id section)) hit]))]]))

(defn sections-separated [sections query]
  [:<>
   (->> sections
        (interpose :hr)
        (map-indexed
          (fn [idx {:keys [id] :as tab}]
            (if (= tab :hr)
              ^{:key idx}
              [:hr]

              ^{:key id}
              [results-section tab query]))))])

(defn tags-section [tags]
  [:div {:data-test "search-results-tags"}
   (for [{:keys [label] :as tag} tags]
     [tag/tag :a
      (assoc tag :on-click #(dispatch [:wh.search/search-with-value label]))])])
