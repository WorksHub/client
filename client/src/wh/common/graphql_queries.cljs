(ns wh.common.graphql-queries
  (:require
    [wh.graphql.jobs :as jobs])
  (:require-macros
    [wh.graphql-macros :refer [def-query-template def-query-from-template]]))

(def-query-template update-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_user"}
   :venia/variables [{:variable/name "update_user"
                      :variable/type :UpdateUserInput!}]
   :venia/queries   [[:update_user {:update_user :$update_user}
                      $fields]]})

(def-query-from-template update-user-mutation--upsert update-user-mutation
  {:fields [:id [:approval [:status]] :email :name :consented
          [:skills [:name]]
          [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]})

(def-query-from-template update-user-mutation--cv update-user-mutation
  {:fields [[:cv [:link [:file [:type :name :url]]]]]})

(def-query-from-template update-user-mutation--consent update-user-mutation
  {:fields [:consented]})

(def-query-from-template update-user-mutation--approval update-user-mutation
  {:fields [[:approval [:status]]]})

(def-query-from-template update-user-mutation--name update-user-mutation
  {:fields [:name]})

(def-query-from-template update-user-mutation--visa-status update-user-mutation
  {:fields [:visaStatus :visaStatusOther]})

(def-query-from-template update-user-mutation--current-location update-user-mutation
  {:fields [[:currentLocation [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]]})

(def set-application-state-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "set_application_state"}
   :venia/variables [{:variable/name "input"
                      :variable/type :set_application_state_input_input!}]
   :venia/queries   [[:set_application_state {:set_application_state_input :$input}
                      [:states]]]})

(def set-application-note-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "set_application_note"}
   :venia/variables [{:variable/name "job_id"  :variable/type :String!}
                     {:variable/name "user_id" :variable/type :ID!}
                     {:variable/name "note"    :variable/type :String!}]
   :venia/queries   [[:set_application_note {:job_id  :$job_id
                                             :user_id :$user_id
                                             :note    :$note}
                      [:jobId :userId]]]})

(def recommended-jobs-for-user
  {:venia/queries [[:jobs {:filter_type "recommended"
                           :entity_type "user"
                           :page_size 3
                           :page_number 1}
                    (conj jobs/job-card-fields :score)]]})
