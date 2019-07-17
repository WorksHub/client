(ns wh.common.data.company-profile)

;; this is ordered
(def dev-setup-data
  {:software       {:icon        "cp-software"
                    :title       "Software Stack"
                    :placeholder "e.g. programming languages, frameworks and databases"}
   :ops            {:icon        "cp-source-control"
                    :title       "DevOps"
                    :placeholder "e.g. source control, continuous deployment/integration, monitoring"}
   :infrastructure {:icon        "cp-infrastructure"
                    :title       "Infrastructure"
                    :placeholder "e.g. cloud services, other third-party service providers"}
   :tools          {:icon        "cp-hardware"
                    :title       "Tools"
                    :placeholder "e.g. development hardware, IDEs, business apps"}})
