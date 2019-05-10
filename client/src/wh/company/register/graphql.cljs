(ns wh.company.register.graphql
  (:require
    [clojure.string :as str]
    [wh.common.cases :as cases]
    [wh.company.register.db :as register]
    [wh.graphql.company :as company]
    [wh.util :as util]
    [wh.verticals :as verticals]))

(defn- register-db->graphql-create-company-input
  [db]
  (util/remove-nils
   {:name    (str/trim (::register/company-name db))
    :domain  (::register/company-domain db)
    :package (::register/package db)}))

(defn register-db->graphql-create-initial-company-user-input
  [db]
  {:name  (str/trim (::register/contact-name db))
   :email (str/trim (::register/email db))})

(defn db->graphql-create-job-input
  [db]
  (let [sub-db (::register/sub-db db)]
    {:title              (::register/job-title sub-db)
     :privateDescriptionHtml (::register/job-description sub-db)
     :verticals          [(if (= "www" (:wh.db/vertical db))
                            verticals/default-vertical ;; TODO would be nice to have a drop-down really...
                            (:wh.db/vertical db))]
     :tags               (map :tag (::register/tags sub-db))
     :location           (->> sub-db
                              ::register/location-details
                              (util/remove-nils)
                              (cases/->camel-case))
     :companyId          (::register/company-id sub-db)}))

;;;

(defn create-company-and-user-mutation
  [db success fail]
  {:query      {:venia/operation {:operation/type :mutation
                                  :operation/name "create_company_and_user"}
                :venia/variables [{:variable/name "create_company"
                                   :variable/type :CreateCompanyInput!}
                                  {:variable/name "create_user"
                                   :variable/type :CreateInitialCompanyUserInput!}]
                :venia/queries   [[:create_company_and_user {:create_company :$create_company
                                                             :create_user :$create_user}
                                   [[:company [:id]]
                                    [:user [:id [:approval [:status]] :email :name :welcomeMsgs :consented :type [:company [:id :name]]]]]]]}
   :variables  {:create_company (register-db->graphql-create-company-input db)
                :create_user (register-db->graphql-create-initial-company-user-input db)}
   :on-success [success]
   :on-failure [fail]
   :timeout    30000})

(defn create-job-mutation
  [db success fail]
  {:query      {:venia/operation {:operation/type :mutation
                                  :operation/name "create_onboard_job"}
                :venia/variables [{:variable/name "create_onboard_job"
                                   :variable/type :CreateOnboardJobInput!}]
                :venia/queries   [[:create_onboard_job {:create_onboard_job :$create_onboard_job}
                                   [:id]]]}
   :variables  {:create_onboard_job (db->graphql-create-job-input db)}
   :on-success [success]
   :on-failure [fail]
   :timeout    30000})
