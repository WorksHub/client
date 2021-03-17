(ns wh.pages.modules)

(def page-modules
  "The mapping from pages to app modules. If a mapping isn't found here,
  the page is presumed to be mapped to core and nothing is loaded."
  {:admin-applications         :company
   :admin-company-applications :company
   :admin-edit-company         :company
   :blog                       :blogs
   :candidate                  :company
   :candidate-edit-header      :company
   :candidate-edit-private     :company
   :candidates                 :admin
   :admin-articles             :admin
   :admin-companies            :admin
   :companies                  :company-profile
   :company                    :company-profile
   :company-applications       :company
   :company-dashboard          :company
   :company-issues             :issues
   :company-jobs               :company-profile
   :company-articles           :company-profile
   :contribute                 :logged-in
   :contribute-edit            :logged-in
   :create-candidate           :admin
   :create-company             :company
   :create-company-offer       :admin
   :create-job                 :company
   :edit-company               :company
   :edit-job                   :company
   :feed                       :landing-page
   :feed-preview               :admin
   :get-started                :login
   :github-callback            :login
   :stackoverflow-callback     :login
   :twitter-callback           :login
   :homepage                   {"candidate" :logged-in
                                "company"   :company
                                "admin"     :company
                                ;; when there is no user type (i.e. not logged in)
                                nil         :landing-page}
   :homepage-dashboard         :logged-in
   :issue                      :issues
   :issues                     :issues
   :issues-by-language         :issues
   :job                        :jobs
   :jobsboard                  :jobs
   :learn                      :blogs
   :learn-by-tag               :blogs
   :login                      :login
   :manage-issues              :issues
   :manage-repository-issues   :issues
   :notifications-settings     :logged-in
   :pre-set-search             :jobs
   :pricing                    :pricing
   :employers                  :pricing
   :profile                    :logged-in
   :profile-edit-header        :logged-in
   :profile-edit-private       :logged-in
   :profile-edit-company-user  :logged-in
   :register-company           :company
   :payment-setup              :company
   :register                   :register
   :liked                      :logged-in
   :recommended                :logged-in
   :applied                    :logged-in
   :tags-edit                  :admin
   :search                     :search
   :user                       :profile
   :create-promotion           :admin
   :promotions-preview         :admin})

(defn module-for
  [handler user-type]
  (let [res (page-modules handler)]
    (if (map? res)
      (get res user-type)
      res)))
