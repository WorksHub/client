(ns wh.graphql.fragments
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [deffragment]]))

;; Jobs

(deffragment jobCardFields :Job
  [:id :slug :title :tagline [:tags :fragment/tagFields] :published :userScore
   :roleType :sponsorshipOffered :remote :companyId
   [:company [:name :slug :logo]]
   [:location [:city :state :country :countryCode]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

(deffragment jobFields :Job
  [:id :slug :title :companyId :tagline :descriptionHtml [:tags :fragment/tagFields] :roleType :manager
   :locationDescription :remote :sponsorshipOffered :applied :published :lastModified
   [:company :fragment/companyCardFields]
   [:location [:street :city :country :countryCode :state :postCode :longitude :latitude]]
   [:remuneration [:competitive :currency :timePeriod :min :max :equity]]])

;; Company

(deffragment companyCardFields :Company
  [:id :slug :name :logo :size :description :profileEnabled
   :totalPublishedJobCount :totalPublishedIssueCount
   [:tags :fragment/tagFields]
   [:locations [:city :country :countryCode :region :subRegion :state]]])

;; Issues

(deffragment issueFields :issue
  [:id :pr_count :url :number :body_html :title :viewer_contributed :published :status :level :created_at
   [:compensation [:amount :currency]]
   [:contributors [:id :name [:other_urls [:url]] [:github_info [:login]]]]
   [:repo [:owner :name :description :primary_language [:community [:readme_url :contributing_url]]]]
   [:company [:id :name :logo]]
   [:author [:login :name]]
   [:labels [:name]]])

(deffragment issueListFields :issue
  [:id :url :number :body :title :pr_count :level :status :published :created_at
   [:compensation [:amount :currency]]
   [:contributors [:id]]
   [:labels [:name]]
   [:company [:id :name :logo :slug]]
   [:repo [:name :owner :primary_language]]])

(deffragment tagFields :tag
  [:id :slug :type :subtype :label :weight])

;; Articles/Blogs

(deffragment blogCardFields :Blog
  [:id :title :feature :author
   :formattedCreationDate :readingTime
   :creator :upvoteCount :published
   [:tags :fragment/tagFields]])
