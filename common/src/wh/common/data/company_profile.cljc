(ns wh.common.data.company-profile)

;; this is ordered
(def dev-setup-data
  {:software       {:icon        "cp-software"
                    :title       "Software Stack"
                    :placeholder "e.g. programming languages, frameworks and databases"}
   :ops            {:icon        "cp-ops"
                    :title       "DevOps"
                    :placeholder "e.g. source control, continuous deployment/integration, monitoring"}
   :infrastructure {:icon        "cp-infrastructure"
                    :title       "Infrastructure"
                    :placeholder "e.g. cloud services, other third-party service providers"}
   :tools          {:icon        "cp-tools"
                    :title       "Tools"
                    :placeholder "e.g. development hardware, IDEs, business apps"}})

(def benefits-data
  {:culture {:icon        "cp-culture"
             :title       "Culture"
             :placeholder "e.g. flexible working, remote working, 20% time"}
   :health {:icon        "cp-health-and-wellness"
            :title       "Health & Wellness"
            :placeholder "e.g. health insurance, dental insurance, gym passes"}
   :finance {:icon        "cp-financial"
             :title       "Financial benefits"
             :placeholder "e.g. 401K, stock options, pension"}
   :parents {:icon        "cp-parents"
             :title       "Parents"
             :placeholder "e.g. child care, parental leave"}
   :vacation {:icon        "cp-vacation"
              :title       "Vacation"
              :placeholder "e.g. paid leave, sick pay, vacation policy"}
   :extra {:icon        "cp-extra"
           :title       "Extras"
           :placeholder "e.g. beer, coffee, food, games"}
   :professional_dev {:icon        "cp-professional-dev"
                      :title       "Professional development "
                      :placeholder "e.g. training, conferences, promotions"}
   :diversity {:icon        "cp-diversity"
               :title       "Diversity"
               :placeholder "e.g. diversity policy, equal pay"}})
