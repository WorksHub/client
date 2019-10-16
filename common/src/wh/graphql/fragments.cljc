(ns wh.graphql.fragments
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [deffragment]]))

;; Jobs

(deffragment jobCardFields :Job
  [:id :slug :title :tagline :tags :published :userScore
   :roleType :sponsorshipOffered :remote :companyId
   [:company [:name :slug :logo]]
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

(deffragment jobFields :Job
  [:id :slug :title :companyId :tagline :descriptionHtml :tags :roleType :manager
   ;; [:company [[:issues [:id :title [:labels [:name]]]]]] ; commented out until company is leonaized
   [:company :fragment/companyCardFields]
   [:location [:street :city :country :countryCode :state :postCode :longitude :latitude]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]
   :locationDescription :remote :sponsorshipOffered :applied :published])

;; Company

(deffragment companyCardFields :Company
  [:id :slug :name :logo :size :descriptionHtml :profileEnabled
   :totalPublishedJobCount :totalPublishedIssueCount
   [:tags [:id :label :slug :type :subtype :weight]]
   [:locations [:city :country :countryCode :region :subRegion :state]]])
