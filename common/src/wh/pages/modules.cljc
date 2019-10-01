(ns wh.pages.modules)

(def page-modules
  "The mapping from pages to app modules. If a mapping isn't found here,
  the page is presumed to be mapped to core and nothing is loaded."
  {:admin-applications :company
   :admin-company-applications :company
   :admin-edit-company :company
   :blog :blogs
   :candidate :company
   :candidate-edit-header :company
   :candidate-edit-cv :company
   :candidate-edit-private :company
   :candidates :admin
   :admin-companies :admin
   :companies :company-profile
   :company :company-profile
   :company-applications :company
   :company-dashboard :company
   :company-issues :issues
   :contribute :logged-in
   :contribute-edit :logged-in
   :create-candidate :admin
   :create-company :company
   :create-company-offer :admin
   :create-job :company
   :edit-company :company
   :edit-job :company
   :get-started :login
   :github-callback :login
   :homepage {"candidate" :logged-in
              "company" :company
              "admin" :company}
   :homepage-dashboard :logged-in
   :improve-recommendations :logged-in
   :invalid-magic-link :login
   :issue :issues
   :issues :issues
   :job :jobs
   :jobsboard :jobs
   :learn :blogs
   :learn-by-tag :blogs
   :login :login
   :manage-issues :issues
   :manage-repository-issues :issues
   :notifications-settings :logged-in
   :pre-set-search :jobs
   :pricing :pricing
   :privacy-policy :login
   :profile :logged-in
   :profile-edit-header :logged-in
   :profile-edit-cv :logged-in
   :profile-edit-private :logged-in
   :profile-edit-company-user :logged-in
   :register-company :company
   :payment-setup :company
   :terms-of-service :company
   :register :register
   :liked :logged-in
   :recommended :logged-in
   :applied :logged-in
   :tags-edit :admin})

(defn module-for
  [handler user-type]
  (let [res (page-modules handler)]
    (if (map? res)
      (get res user-type)
      res)))
