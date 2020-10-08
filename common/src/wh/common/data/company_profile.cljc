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
  {:culture          {:icon        "cp-culture"
                      :title       "Culture"
                      :placeholder "e.g. flexible working, remote working, 20% time"}
   :health           {:icon        "cp-health-and-wellness"
                      :title       "Health & Wellness"
                      :placeholder "e.g. health insurance, dental insurance, gym passes"}
   :finance          {:icon        "cp-financial"
                      :title       "Financial benefits"
                      :placeholder "e.g. 401K, stock options, pension"}
   :parents          {:icon        "cp-parents"
                      :title       "Parents"
                      :placeholder "e.g. child care, parental leave"}
   :vacation         {:icon        "cp-vacation"
                      :title       "Vacation"
                      :placeholder "e.g. paid leave, sick pay, vacation policy"}
   :extra            {:icon        "cp-extra"
                      :title       "Extras"
                      :placeholder "e.g. beer, coffee, food, games"}
   :professional_dev {:icon        "cp-professional-dev"
                      :title       "Professional development "
                      :placeholder "e.g. training, conferences, promotions"}
   :diversity        {:icon        "cp-diversity"
                      :title       "Diversity"
                      :placeholder "e.g. diversity policy, equal pay"}})

(def information-tooltips
  {:about-us        "Think of this as your elevator pitch. What are you building and how do you plan to achieve it? Be as clear and concise as you can."
   :company-info    "Edit this section to include information about your company, such as industry and location."
   :additional-info "Feel free to provide any additional information about how your tech team setup here. What's your process. What methodology do you use? Do you have mandatory code reviews? What's the size of your teams and how are they managed and structured?"
   :how-we-work     "Describe your management team. Do they come from a technical or non-technical background? Describe your hiring process. What are the stages like for the candidate and how many will there be? Do you ask candidates to take a coding test? Can you give more information about what this involves? What's your working evnvironment?"})

;; underscores to match :wh.company.onboarding-task/id
(def company-onboarding-tasks
  {:complete_profile {:title    "Complete your company profile"
                      :subtitle "Sell your company to our community. What are you building and how?"
                      :icon     "company-building"
                      :time     2}
   :add_job          {:title    "Advertise a new role"
                      :subtitle "Start hiring top talent right away!"
                      :icon     "plus-circle"
                      :time     3}
   :add_integration  {:title    "Connect your integrations"
                      :subtitle "WorksHub can integrate with popular services such as Greenhouse and Slack."
                      :icon     "settings"
                      :time     2}
   :add_issue        {:title    "Get started with Open Source Issues"
                      :subtitle "Scope talent by using issues from your open source projects."
                      :icon     "git"
                      :time     2}})
