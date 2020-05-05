(ns wh.jobs.jobsboard.queries
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(defquery jobs-query
  {:venia/operation {:operation/type :query
                     :operation/name "jobs_search"}
   :venia/variables [{:variable/name "vertical" :variable/type :vertical}
                     {:variable/name "search_term" :variable/type :String!}
                     {:variable/name "preset_search" :variable/type :String}
                     {:variable/name "page" :variable/type :Int!}
                     {:variable/name "filters" :variable/type :SearchFiltersInput}]
   :venia/queries   [[:jobs_search {:vertical      :$vertical
                                    :search_term   :$search_term
                                    :preset_search :$preset_search
                                    :page          :$page
                                    :filters       :$filters}
                      [:numberOfPages
                       :numberOfHits
                       :hitsPerPage
                       :page
                       [:facets [:attr :value :count]]
                       [:searchParams [:label
                                       :query
                                       [:filters [:remote :roleType :sponsorshipOffered :published :tags :manager
                                                  [:location [:cities :countryCodes :regions]]
                                                  [:remuneration [:min :max :currency :timePeriod]]]]]]
                       [:promoted [:fragment/jobCardFields]]
                       [:jobs [:fragment/jobCardFields]]]]
                     [:city_info [:city :country :countryCode :region]]
                     [:remuneration_ranges [:currency :timePeriod :min :max]]]})
