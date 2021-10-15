(ns wh.company.register.graphql
  (:require
    [clojure.string :as str]
    [wh.common.text :as text]
    [wh.company.register.db :as register]
    [wh.util :as util]))

(defn- register-db->graphql-create-company-input
  [db]
  (util/remove-nils
    {:name    (str/trim (::register/company-name db))
     :domain  (::register/company-domain db)
     :package (::register/package db)
     :source  (text/not-blank (::register/source db))}))

(defn register-db->graphql-create-initial-company-user-input
  [db]
  {:name  (str/trim (::register/contact-name db))
   :email (str/trim (::register/email db))})

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
                                    [:user [:id [:approval [:status]] :email :name :onboardingMsgs :consented :type [:company [:id :name]]]]]]]}
   :variables  {:create_company (register-db->graphql-create-company-input db)
                :create_user (register-db->graphql-create-initial-company-user-input db)}
   :on-success [success]
   :on-failure [fail]
   :timeout    30000})
