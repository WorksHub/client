(ns wh.common.data.company-profile)

(def dev-setup-data
  {:hardware       {:icon  "cp-hardware"
                    :title "Hardware"
                    :placeholder "What hardware do you work with?"}
   :software       {:icon  "cp-software"
                    :title "Software"
                    :placeholder "Which software packages do you use?"}
   :sourcecontrol  {:icon  "cp-source-control"
                    :title "Source control"
                    :placeholder "How does this fit into your workflow?"}
   :ci             {:icon  "cp-ci-cd"
                    :title "CI/CD"
                    :placeholder "How does this fit into your workflow?"}
   :infrastructure {:icon  "cp-infrastructure"
                    :title "Infrastructure"
                    :placeholder "How does this fit into your workflow?"}})
