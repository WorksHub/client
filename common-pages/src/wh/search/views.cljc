(ns wh.search.views
  (:require #?(:cljs [wh.search.events])
            [re-frame.core :refer [dispatch]]
            [wh.common.search :as search]
            [wh.re-frame.subs :refer [<sub]]
            [wh.search.components :as components]
            [wh.search.subs :as subs]
            [wh.styles.search :as styles]))

(defn search-page []
  (let [query (search/->search-term (<sub [:wh/page-params]) (<sub [:wh/query-params]))]
    #?(:cljs (let [sections      (<sub [::subs/sections-with-results])
                   results-count (<sub [::subs/results-count])
                   tags          (<sub [:wh.search.subs/search-results-tags])
                   go-to-tab     #(dispatch [:wh.search.events/go-to-tab %])]
               [:div {:class styles/search-page}
                [components/results-title query results-count]

                [components/tags-section results-count tags]

                [components/tabs {:go-to-tab go-to-tab}]

                [components/sections-separated sections query]])

       :clj  (let [results-count nil]
               [:div {:class styles/search-page}
                [components/results-title query results-count]

                [components/tags-section results-count]

                [components/tabs {:go-to-tab identity}]

                [components/sections-separated components/sections-coll nil]]))))
