(ns wh.search.views
  (:require #?(:cljs [wh.search.events])
            [re-frame.core :refer [dispatch]]
            [wh.components.activities.components :as activities]
            [wh.components.skeletons.components :as skeletons]
            [wh.re-frame.subs :refer [<sub]]
            [wh.search.components :as components]
            [wh.search.subs :as subs]
            [wh.styles.search :as styles]))

(defn search-page []
  #?(:cljs
     (let [query         (<sub [:wh.subs/query-param "query"])
           results       (<sub [::subs/search-results])
           sections      (<sub [::subs/sections-with-results])
           results-count (<sub [::subs/results-count])
           tags          (<sub [:wh.search.subs/search-results-tags])
           go-to-tab     #(dispatch [:wh.search.events/go-to-tab %])]
       [:div {:class styles/search-page}
        [components/results-title query results-count]

        [components/tags-section tags]

        [components/tabs {:go-to-tab go-to-tab}]

        [components/sections-separated sections query]])

     :clj (let [query         (<sub [::subs/query])
                results-count nil]
            [:div {:class styles/search-page}
             [components/results-title query results-count]
             [components/tabs {:go-to-tab identity}]

             [components/sections-separated components/sections-coll nil]])))
