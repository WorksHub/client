(ns wh.common.data
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])
    [clojure.string :as str]
    [wh.common.text :as txt]))

(def currencies ["EUR" "GBP" "USD" "BTC" "AUD" "CAD" "CHF" "KHD" "NOK" "SEK" "SGD" "PLN"])
(def time-periods ["Yearly" "Daily"])

(def visa-options #{"US Citizenship" "US Greencard" "US H1B" "EU Citizenship" "EU Visa" "Other"})

(def currency-symbols
  {"GBP" "£"
   "USD" "$"
   "EUR" "€"
   "BTC" "BTC"
   "AUD" "A$"
   "SGD" "S$"
   "CAD" "C$"
   "HKD" "HK$"
   "PLN" "zł"})

;; TODO there are far too few cities on this list
(def cities
  ["New York"
   "London"
   "Amsterdam"
   "Atlanta"
   "Austin"
   "Barcelona"
   "Bengaluru"
   "Berkeley"
   "Berlin"
   "Boston"
   "Boulder"
   "Bristol"
   "Brooklyn"
   "Budapest"
   "Cambridge"
   "Chicago"
   "Conneticut"
   "Dublin"
   "Gainesville"
   "Ghent"
   "Gothenburg"
   "Hamburg"
   "Hong Kong"
   "Huntington Beach"
   "Kaunas"
   "Lexington"
   "Los Angeles"
   "Manchester"
   "Montreal"
   "Munich"
   "Oakland"
   "Oslo"
   "Paris"
   "Portland"
   "Raleigh"
   "San Francisco"
   "San Mateo"
   "Santa Monica"
   "Sao Paulo"
   "San Jose"
   "Seattle"
   "Seoul"
   "Sheffield"
   "Singapore"
   "Sofia"
   "Stockholm"
   "Sydney"
   "Tampere"
   "Tokyo"
   "Toronto"
   "Zagreb"
   "Zurich"])

;; this was generated from the iso-3166-countries.edn file in server/resources
(def country-code-and-country
  [["AD" "Andorra"]
   ["AE" "United Arab Emirates"]
   ["AF" "Afghanistan"]
   ["AG" "Antigua and Barbuda"]
   ["AI" "Anguilla"]
   ["AL" "Albania"]
   ["AM" "Armenia"]
   ["AO" "Angola"]
   ["AQ" "Antarctica"]
   ["AR" "Argentina"]
   ["AS" "American Samoa"]
   ["AT" "Austria"]
   ["AU" "Australia"]
   ["AW" "Aruba"]
   ["AX" "Åland Islands"]
   ["AZ" "Azerbaijan"]
   ["BA" "Bosnia and Herzegovina"]
   ["BB" "Barbados"]
   ["BD" "Bangladesh"]
   ["BE" "Belgium"]
   ["BF" "Burkina Faso"]
   ["BG" "Bulgaria"]
   ["BH" "Bahrain"]
   ["BI" "Burundi"]
   ["BJ" "Benin"]
   ["BL" "Saint Barthélemy"]
   ["BM" "Bermuda"]
   ["BN" "Brunei Darussalam"]
   ["BO" "Bolivia (Plurinational State of)"]
   ["BQ" "Bonaire, Sint Eustatius and Saba"]
   ["BR" "Brazil"]
   ["BS" "Bahamas"]
   ["BT" "Bhutan"]
   ["BV" "Bouvet Island"]
   ["BW" "Botswana"]
   ["BY" "Belarus"]
   ["BZ" "Belize"]
   ["CA" "Canada"]
   ["CC" "Cocos (Keeling) Islands"]
   ["CD" "Congo (Democratic Republic of the)"]
   ["CF" "Central African Republic"]
   ["CG" "Congo"]
   ["CH" "Switzerland"]
   ["CI" "Côte d'Ivoire"]
   ["CK" "Cook Islands"]
   ["CL" "Chile"]
   ["CM" "Cameroon"]
   ["CN" "China"]
   ["CO" "Colombia"]
   ["CR" "Costa Rica"]
   ["CU" "Cuba"]
   ["CV" "Cabo Verde"]
   ["CW" "Curaçao"]
   ["CX" "Christmas Island"]
   ["CY" "Cyprus"]
   ["CZ" "Czech Republic"]
   ["DE" "Germany"]
   ["DJ" "Djibouti"]
   ["DK" "Denmark"]
   ["DM" "Dominica"]
   ["DO" "Dominican Republic"]
   ["DZ" "Algeria"]
   ["EC" "Ecuador"]
   ["EE" "Estonia"]
   ["EG" "Egypt"]
   ["EH" "Western Sahara"]
   ["ER" "Eritrea"]
   ["ES" "Spain"]
   ["ET" "Ethiopia"]
   ["FI" "Finland"]
   ["FJ" "Fiji"]
   ["FK" "Falkland Islands (Malvinas)"]
   ["FM" "Micronesia (Federated States of)"]
   ["FO" "Faroe Islands"]
   ["FR" "France"]
   ["GA" "Gabon"]
   ["GB" "United Kingdom"]
   ["GD" "Grenada"]
   ["GE" "Georgia"]
   ["GF" "French Guiana"]
   ["GG" "Guernsey"]
   ["GH" "Ghana"]
   ["GI" "Gibraltar"]
   ["GL" "Greenland"]
   ["GM" "Gambia"]
   ["GN" "Guinea"]
   ["GP" "Guadeloupe"]
   ["GQ" "Equatorial Guinea"]
   ["GR" "Greece"]
   ["GS" "South Georgia and the South Sandwich Islands"]
   ["GT" "Guatemala"]
   ["GU" "Guam"]
   ["GW" "Guinea-Bissau"]
   ["GY" "Guyana"]
   ["HK" "Hong Kong"]
   ["HM" "Heard Island and McDonald Islands"]
   ["HN" "Honduras"]
   ["HR" "Croatia"]
   ["HT" "Haiti"]
   ["HU" "Hungary"]
   ["ID" "Indonesia"]
   ["IE" "Ireland"]
   ["IL" "Israel"]
   ["IM" "Isle of Man"]
   ["IN" "India"]
   ["IO" "British Indian Ocean Territory"]
   ["IQ" "Iraq"]
   ["IR" "Iran (Islamic Republic of)"]
   ["IS" "Iceland"]
   ["IT" "Italy"]
   ["JE" "Jersey"]
   ["JM" "Jamaica"]
   ["JO" "Jordan"]
   ["JP" "Japan"]
   ["KE" "Kenya"]
   ["KG" "Kyrgyzstan"]
   ["KH" "Cambodia"]
   ["KI" "Kiribati"]
   ["KM" "Comoros"]
   ["KN" "Saint Kitts and Nevis"]
   ["KP" "Korea (Democratic People's Republic of)"]
   ["KR" "Korea (Republic of)"]
   ["KW" "Kuwait"]
   ["KY" "Cayman Islands"]
   ["KZ" "Kazakhstan"]
   ["LA" "Lao People's Democratic Republic"]
   ["LB" "Lebanon"]
   ["LC" "Saint Lucia"]
   ["LI" "Liechtenstein"]
   ["LK" "Sri Lanka"]
   ["LR" "Liberia"]
   ["LS" "Lesotho"]
   ["LT" "Lithuania"]
   ["LU" "Luxembourg"]
   ["LV" "Latvia"]
   ["LY" "Libya"]
   ["MA" "Morocco"]
   ["MC" "Monaco"]
   ["MD" "Moldova (Republic of)"]
   ["ME" "Montenegro"]
   ["MF" "Saint Martin (French part)"]
   ["MG" "Madagascar"]
   ["MH" "Marshall Islands"]
   ["MK" "Macedonia (the former Yugoslav Republic of)"]
   ["ML" "Mali"]
   ["MM" "Myanmar"]
   ["MN" "Mongolia"]
   ["MO" "Macao"]
   ["MP" "Northern Mariana Islands"]
   ["MQ" "Martinique"]
   ["MR" "Mauritania"]
   ["MS" "Montserrat"]
   ["MT" "Malta"]
   ["MU" "Mauritius"]
   ["MV" "Maldives"]
   ["MW" "Malawi"]
   ["MX" "Mexico"]
   ["MY" "Malaysia"]
   ["MZ" "Mozambique"]
   ["NA" "Namibia"]
   ["NC" "New Caledonia"]
   ["NE" "Niger"]
   ["NF" "Norfolk Island"]
   ["NG" "Nigeria"]
   ["NI" "Nicaragua"]
   ["NL" "Netherlands"]
   ["NO" "Norway"]
   ["NP" "Nepal"]
   ["NR" "Nauru"]
   ["NU" "Niue"]
   ["NZ" "New Zealand"]
   ["OM" "Oman"]
   ["PA" "Panama"]
   ["PE" "Peru"]
   ["PF" "French Polynesia"]
   ["PG" "Papua New Guinea"]
   ["PH" "Philippines"]
   ["PK" "Pakistan"]
   ["PL" "Poland"]
   ["PM" "Saint Pierre and Miquelon"]
   ["PN" "Pitcairn"]
   ["PR" "Puerto Rico"]
   ["PS" "Palestine, State of"]
   ["PT" "Portugal"]
   ["PW" "Palau"]
   ["PY" "Paraguay"]
   ["QA" "Qatar"]
   ["RE" "Réunion"]
   ["RO" "Romania"]
   ["RS" "Serbia"]
   ["RU" "Russian Federation"]
   ["RW" "Rwanda"]
   ["SA" "Saudi Arabia"]
   ["SB" "Solomon Islands"]
   ["SC" "Seychelles"]
   ["SD" "Sudan"]
   ["SE" "Sweden"]
   ["SG" "Singapore"]
   ["SH" "Saint Helena, Ascension and Tristan da Cunha"]
   ["SI" "Slovenia"]
   ["SJ" "Svalbard and Jan Mayen"]
   ["SK" "Slovakia"]
   ["SL" "Sierra Leone"]
   ["SM" "San Marino"]
   ["SN" "Senegal"]
   ["SO" "Somalia"]
   ["SR" "Suriname"]
   ["SS" "South Sudan"]
   ["ST" "Sao Tome and Principe"]
   ["SV" "El Salvador"]
   ["SX" "Sint Maarten (Dutch part)"]
   ["SY" "Syrian Arab Republic"]
   ["SZ" "Swaziland"]
   ["TC" "Turks and Caicos Islands"]
   ["TD" "Chad"]
   ["TF" "French Southern Territories"]
   ["TG" "Togo"]
   ["TH" "Thailand"]
   ["TJ" "Tajikistan"]
   ["TK" "Tokelau"]
   ["TL" "Timor-Leste"]
   ["TM" "Turkmenistan"]
   ["TN" "Tunisia"]
   ["TO" "Tonga"]
   ["TR" "Turkey"]
   ["TT" "Trinidad and Tobago"]
   ["TV" "Tuvalu"]
   ["TW" "Taiwan, Province of China"]
   ["TZ" "Tanzania, United Republic of"]
   ["UA" "Ukraine"]
   ["UG" "Uganda"]
   ["UM" "United States Minor Outlying Islands"]
   ["US" "United States of America"]
   ["UY" "Uruguay"]
   ["UZ" "Uzbekistan"]
   ["VA" "Holy See"]
   ["VC" "Saint Vincent and the Grenadines"]
   ["VE" "Venezuela (Bolivarian Republic of)"]
   ["VG" "Virgin Islands (British)"]
   ["VI" "Virgin Islands (U.S.)"]
   ["VN" "Viet Nam"]
   ["VU" "Vanuatu"]
   ["WF" "Wallis and Futuna"]
   ["WS" "Samoa"]
   ["YE" "Yemen"]
   ["YT" "Mayotte"]
   ["ZA" "South Africa"]
   ["ZM" "Zambia"]
   ["ZW" "Zimbabwe"]])

(def country-codes
  (mapv first country-code-and-country))

(def countries
  (mapv second country-code-and-country))

(def country-code->country
  (into {} country-code-and-country))

(def country->country-code
  (zipmap (map second country-code-and-country)
          (map first country-code-and-country)))

(defn ->email [n] (str n "@functionalworks.com"))

(def default-manager (->email "alex"))

(def managers
  (->> {"alex"            "Alex Mesropians"
        "charlie"         "Charlie Gower"
        "josh"            "Josh Gretton"
        "nick"            "Nick Maraj"
        "nick.walker"     "Nick Walker"
        "patrick"         "Patrick Gubbey"
        "peter"           "Peter Forteath"
        "ken.cadima"      "Ken Cadima"
        "daniel.earnshaw" "Daniel Earnshaw"
        "quan"            "Quan Truc"
        "nicole"          "Nicole Sadozai"
        "sheryl"          "Sheryl Martin"
        "hanna"           "Hanna Poplawska"
        "nikolaus"        "Nikolaus Krahé"}
       (map (fn [[k v]] [(->email k) v]))
       (into {})))

(defn get-manager-name
  [email]
  (get managers email))

(defn get-manager-email
  [n]
  (when-not (str/blank? n)
    (some (fn [[k v]] (when (= (str/lower-case v) (str/lower-case n)) k)) managers)))

(s/def ::manager (set (vals managers)))

(def salary-ranges
  {["CAD" "Yearly"] {:min 70000,  :max 165000},
   ["USD" "Yearly"] {:min 30000,  :max 300000},
   ["CHF" "Yearly"] {:min 100000, :max 150000},
   ["USD" "Daily"]  {:min 600,    :max 1000},
   ["NOK" "Yearly"] {:min 550000, :max 650000},
   ["EUR" "Yearly"] {:min 20000,  :max 180000},
   ["SEK" "Yearly"] {:min 420000, :max 960000},
   ["GBP" "Daily"]  {:min 350,    :max 850},
   ["GBP" "Yearly"] {:min 20000,  :max 175000},
   ["SGD" "Yearly"] {:min 80000,  :max 180000}
   ["PLN" "Yearly"] {:min 10000,  :max 500000}})

(defn get-min-salary
  [currency tp]
  (or (:min (get salary-ranges [currency tp]))
      (case tp
        "Yearly" 8000
        "Daily" 350
        1)))

(defn get-max-salary
  [currency tp]
  (or (:max (get salary-ranges [currency tp]))
      (case tp
        "Yearly" 200000
        "Daily" 850
        10)))

;; removed essential on 24/05/2019
;; removed :unselected and :free on 13/08/2019
(def packages #{#_:unselected #_:free #_:essential
                :explore :launch_pad :take_off})

(def all-package-perks
  ["Unlimited hubs"
   "Performance analytics"
   "Github integration"
   "Company profile"
   "Review, track & share applicants"
   "ATS Integrations"
   "Account manager"
   "Active sourcing"
   "Screened applicants"
   "Guaranteed hires"])

(def get-started-cta-string "Get Started")
(def free-trial-days 0)
(def launch-pad-trial-days 0)
(def free-week-code "FREEWEEK")

(def package-data
  {:free {:name "Trial"
          :cost 0
          :per nil
          :trial free-trial-days
          :button "Start Trial"
          :live-jobs "Unlimited"
          :img {:src "/images/employers/free.svg"
                :alt "Free icon"}
          :perks #{"Unlimited hubs"
                   "Performance analytics"}
          :order 1
          :description (str "Our " free-trial-days "-day trial allows you to post a job in minutes and experience our core features before upgrading.")}
   :explore {:name "Explore"
             :cost 0
             :per nil
             :button "Get Started"
             :live-jobs "None"
             :img {:src "/images/employers/essential.svg"
                   :alt "Explore icon"}
             :perks #{"Unlimited hubs"
                      "Performance analytics"
                      "Github integration"
                      "Company profile"}
             :order 1
             :description (str "Our basic package allows you to attract talent by creating a company profile and posting open-soure issues. This way you can experience some of our core features before upgrading.")}
   :essential {:name "Essential"
               :cost 500
               :per "month"
               :button "Start Hiring"
               :button-alt "Select & Pay"
               :live-jobs "One"
               :img {:src "/images/employers/essential.svg"
                     :alt "Essential icon"}
               :perks #{"Unlimited hubs"
                        "Performance analytics"
                        "Github integration"
                        "Company profile"
                        "Review, track & share applicants"}
               :order 2
               :description "For those looking to get good exposure to our dedicated pool of technical talent with limited hiring needs."}
   :launch_pad {:name "Launch Pad"
                :cost 1500
                :per "month"
                :button "Start Hiring"
                :extra "1st week free!"
                :button-alt "Get Started"
                :live-jobs "Unlimited"
                :img {:src "/images/employers/launch_pad.svg"
                      :alt "Launch Pad icon"}
                :perks #{"Unlimited hubs"
                         "Performance analytics"
                         "Github integration"
                         "Company profile"
                         "Review, track & share applicants"
                         "ATS Integrations"
                         "Account manager"}
                :order 3
                :description "Take control of your hiring with unlimited job adverts and applications. Our GitHub integration allows you to further streamline your interview process as well incentivising more candidates to apply."}
   :take_off {:name "Take-Off"
              :cost nil
              :per nil
              :button "Get Started"
              :button-alt "Get in touch"
              :live-jobs "Unlimited"
              :img {:src "/images/employers/take_off.svg"
                    :alt "Take-Off icon"}
              :perks (set all-package-perks)
              :order 4
              :description "For ambitious hiring plans, access a dedicated Talent Manager to speed up your results and act as your recruiting partner."}})

;; added three on 25/06/2019
(def billing-periods #{:one :three :six #_:twelve})
(def default-billing-period :one)

(def billing-data
  {:one    {:title "Monthly"
            :number 1
            :description nil}
   :three    {:title "Quarterly"
              :number 3
              :description "billed every three months"
              :discount 0.05}
   :six    {:title "Six Monthly"
            :number 6
            :description "billed every six months"
            :discount 0.1}
   :twelve {:title "Annually"
            :number 12
            :description "billed annually"
            :discount 0.15}})

(def job-seeking-status->name
  {"open-to-offers" "Open to offers"
   "looking" "Looking for a new job"
   "no-offers-please" "No offers please"})

(def name->job-seeking-status
  (zipmap (vals job-seeking-status->name)
          (keys job-seeking-status->name)))

(def super-admins
  ["charlie@functionalworks.com"
   "laco@functionalworks.com"
   "nick@functionalworks.com"])

(def default-notification-settings
  {:matching-job {:frequency "weekly"}})

(def take-off-offers
  {:a {:fixed 15000 :percentage 0}
   :b {:fixed 5000  :percentage 7.5}
   :c {:fixed 1500  :percentage 12.5}
   :d {:fixed 1000  :percentage 15}
   :e {:fixed 500   :percentage 17.5}
   :f {:fixed 0     :percentage 20}})

(def in-demand-hiring-data
  {"functional" {:title        "Functional Programmers"
                 :logo         "functional"
                 :description  "Developers working with Scala, Haskell, Clojure, OCaml, Elixir, Elm, Typescript, Purescript."
                 :discover     "Discover the best functional programming opportunities"
                 :href         "/hire-functional-programmers"
                 :description2 "Whether you're looking to hire front-end or backend functional engineers, we've got you covered"}
   "javascript" {:title        "Javascript Developers"
                 :logo         "javascript"
                 :discover     "Discover the best JavaScript opportunities"
                 :description  "Working across a range of frameworks and libraries incuding React.js, Node.js, Angular and many more."
                 :href         "/hire-javascript-developers"
                 :description2 "Whether you’re looking to hire front-end developers, full-stack or even back-end, we’ve got you covered"}
   "blockchain" {:title        "Blockchain Developers"
                 :logo         "blockchain"
                 :discover     "Discover the best Blockchain opportunities"
                 :description  "Get access to the top developers that work with Solidity, Ethereum and experts in Cryptography and Distributed Ledger Technology."
                 :href         "/hire-blockchain-developers"
                 :description2 "Whether you’re looking to hire front-end developers, full-stack or backend, we’ve got you covered"}
   "golang"     {:title        "Golang Developers"
                 :logo         "golang"
                 :discover     "Discover the best Golang opportunities"
                 :description  "Software engineers that build infrastructure, scalable and resilient systems, IoT, with a solid grasp of concurrency."
                 :href         "/hire-golang-developers"
                 :description2 "Whether you’re looking to hire developers, programmers or software engineers to work with Golang, we've got you covered"}
   "ai"         {:title        "Data Scientists"
                 :logo         "ai"
                 :discover     "Discover the best opportunities in Data Science"
                 :description  "Experts in Data Science, Machine Learning, Data Engineering, Artificial Intelligence as well as Scala, Spark, Python and R."
                 :href         "/hire-data-scientists"
                 :description2 "Whether you’re looking to hire data scientists, machine learning experts, or data engineers to work with Spark, Python and R, we've got you covered"}
   "java"       {:title        "Java developers"
                 :logo         "java"
                 :discover     "Discover the best Java opportunities"
                 :description  "Developers working with Java, Akka, Kafka, Spring and more."
                 :href         "/hire-java-developers"
                 :description2 "Whether you're looking to hire full stack or backend Java developers, we've got you covered"}
   "python"     {:title        "Python Developers"
                 :logo         "python"
                 :discover     "Discover the best Python opportunities"
                 :description  "Developers working with Python, Django, Flask, Ansible, Numpy and more."
                 :href         "/hire-python-developers"
                 :description2 "Whether you're looking to hire full stack or backend Python developers, Data Scientists or Engineers, we've got you covered"}
   "remote"     {:title        "Remote Developers"
                 :logo         "remote"
                 :discover     "Discover the best remote opportunities"
                 :description  "Remote developers working with JavaScript, Go, Scala, Java, Python and more."
                 :href         "/hire-remote-developers"
                 :description2 "Whether you're looking to hire full stack, frontend or backend remote developers, Data Scientists or Engineers, we've got you covered"}})

(def in-demand-location-data
  ;; TODO Dedupe with location as below
  [{:title "developers in NYC"
    :href "/hire-developers-in-nyc"}
   {:title "developers in London"
    :href "/hire-developers-in-london"}
   {:title "developers in San Francisco"
    :href "/hire-developers-in-san-francisco"}
   {:title "developers in Berlin"
    :href "/hire-developers-in-berlin"}])

(def preset-job-cities ["New York" "London" "San Francisco" "Berlin" "Barcelona"])
(def preset-job-country-codes ["DE" "GB" "US"])

(defn find-hiring-target
  [template]
  (when (txt/not-blank template)
    (some (fn [{:keys [href] :as m}]
            (when (str/ends-with? href template) m)) (concat (vals in-demand-hiring-data)
                                                             in-demand-location-data))))

(def pricing-questions
  [{:title "Why should I sign up?"
    :answer "Signing up to WorksHub is really easy. Just click “Get Started”, and we’ll get your dashboard set up with just a couple of clicks. From here if you select our Launch Pad or Take-Off plans you will be able to create as many jobs as you like across all of our hubs. You can also manage all of your applications and add colleagues to your account. Once signed up your jobs will be seen by a highly qualified pool of candidates — both full-time and contract — that are ready to interview now."}
   {:title "How does the 'free week' work?"
    :answer (str "When you first sign up you will automatically be put on our Explore package, so you can get to know how our platform works and create a company profile. Once you're ready to start hiring, use the code '" free-week-code"' to get a whole week for free on our Launch Pad package. Once upgraded you can post unlimited jobs and talk directly to candidates.")}
   {:title "How much does Take-Off cost?"
    :answer "Depending on your company size and headcount goals, our Take-Off package is built to be flexible for you. Whether you're hiring two or two hundred, combining a subscription plan with a success fee will help give you access to our direct sourcing team to ensure you hit your hiring goals. Your account manager who you can find in your settings page will discuss the best option with you and create a custom pricing plan."}
   {:title "Are there discounts?"
    :answer "Yes! If you pay for six or three months in advance you save 10% or 5% compared to paying per month. You can see what that does to your monthly cost by changing the payment toggle on the top right of this page."}
   {:title "How do you vet your candidates?"
    :answer "In order to provide you with a high-quality pipeline, all candidates are approved before being given access to the WorksHub platform. To do this we use a combination of human and artificial curation to review all candidate profiles, both for experience, technical knowledge and their interest in finding a new job. We look at the type of roles the candidate is looking for, the location, skills, salary expectations and match that to the open positions across our platform. Because we focus on creating engaged and active candidates across our hubs, you can expect to see highly relevant candidates applying to your jobs."}
   {:title "GitHub integration, what does that do?"
    :answer "With one click you can connect your company GitHub account, allowing you to test, engage, and develop your own community of Software Engineers. With 84% of our users more open to joining a company whose open source they had already contributed to and 90% more likely to consider companies with open source code it’s a great way to increase your hiring pipeline. You can find out how to get started with connecting your GitHub and posting open source issues here."}
   {:title "How does WorksHub compare with Hired, Vettery, Triplebyte, and others?"
    :answer "Whether it’s hiring managers or CTOs looking to hire Software Engineers, our aim is to connect the right engineers with the right companies. When we decided to build WorksHub, we knew that in order to tackle the hiring problem, we needed a tech-first platform with a human touch for understanding nuance. Unlike competitors, we have built our hubs around specific technologies, ensuring you have no delay in gaining instant visibility for your company into the right (vetted) talent pools. With one account, you will be able to test, hire and engage candidates across all of our hubs."}])

(def default-for-you-section
  {:section "For You"
   :class   "for-you"
   :items   [[:homepage               "dashboard"  "Dashboard"]
             [:recommended            "recommend"  "Recommended"]
             [:liked                  "like"       "Liked"]
             [:applied                "applications" "Applied"]
             [:notifications-settings "resources"  "Notifications"]
             [:contribute             "contribute" "Contribute"]
             [:profile                "profile"    "Profile & Preferences"]]})

(def default-explore-section
  {:section "Explore"
   :class   "explore"
   :items   [[:learn     "resources"  "Learn"]
             [:issues    "pr"         "Open Source Issues"]
             [:jobsboard "jobs-board" "Job Board"]
             [:companies "company"    "Companies"]]})

(defn menu
  [type user]
  (case type
    "candidate"
    [default-for-you-section
     default-explore-section]
    "admin"
    [{:section "Admin"
      :class   "admin"
      :items   [[[:admin-applications :homepage] "applications" "Applications"]
                [:admin-companies                "recommend"    "Companies"]
                [:candidates                     "profile"      "Candidates"]
                [:create-job                     "resources"    "Create role"]
                [:create-company                 "jobs-board"   "Create company"]
                [:create-candidate               "add-new"      "Create candidate"]]}
     default-explore-section
     default-for-you-section]
    "company"
    [{:section "For You"
      :class   "for-you"
      :show-notifications? true
      :items   [[[:company-dashboard :homepage] "dashboard" "Dashboard"]
                [[:company [:slug (get-in user [:wh.user.db/company :slug])]]
                 "company" "Company Profile"]
                [:company-applications "applications" "Applications"]
                [:create-job "add-new" "Add new role"]
                [:company-issues "pr" "Your issues"]
                [:edit-company "settings" "Settings"]
                [:profile "profile" "Your Profile"]]}
     default-explore-section]))

(def how-it-works-benefits
  {:company
   [{:img "/images/hiw/company/benefits/benefit2.svg"
     :txt "Build communities around open source issues"}
    {:img "/images/hiw/company/benefits/benefit1.svg"
     :txt "Access a better standard of candidate"}
    {:img "/images/hiw/company/benefits/benefit3.svg"
     :txt "Cut the potential costs of hiring"}
    {:img "/images/hiw/company/benefits/benefit4.svg"
     :txt "Streamline your hiring process"}]
   :candidate
   [{:img "/images/hiw/candidate/benefits/benefit2.svg"
     :txt "Get paid to learn new technologies!"}
    {:img "/images/hiw/candidate/benefits/benefit1.svg"
     :txt "Raise your profile and add skills to your resume"}
    {:img "/images/hiw/candidate/benefits/benefit3.svg"
     :txt "Find out what it’s like to work for different companies"}
    {:img "/images/hiw/candidate/benefits/benefit4.svg"
     :txt "Improve your applications"}]})

(def how-it-works-stats
  {:company
   {:info ["About 80% of companies in the world run open-source software, yet very few of them actively use OSS as a hiring strategy."
           "Developers are more likely to join a company if they already contributed to one of their open-source projects."
           "Companies of all sizes use GitHub for their software development process."]
    :blue ["89%" "of developers prefer companies with OSS"]
    :grey ["90,000+" "software engineers in our network"]
    :orange ["2,100,000" "businesses on GitHub" "github"]}
   :candidate
   {:info ["58% of companies have hired software engineers based on their open-source software contributions."
           "64% of developers are open to solving a company’s open-source issues as part of the technical hiring process."
           "Most companies in the world use open-source software, yet very few of them actively use OSS as a hiring strategy."]
    :blue ["88%" "of companies encourage OSS contributions"]
    :grey [nil "Get real production experience"]
    :orange ["76%" "of companies engage with OSS communities"]}})

(def how-it-works-explanation-steps
  {:company
   [{:img "/images/hiw/company/hiw/hiw1.svg"
     :txt "Connect your GitHub account to your WorksHub company profile."}
    {:img "/images/hiw/company/hiw/hiw2.svg"
     :txt "Select the appropriate repositories and add issues you’d like help with."}
    {:img "/images/hiw/company/hiw/hiw3.svg"
     :txt "Let us recommend your issues to Engineers who can start contributing to your code base."}
    {:img "/images/hiw/company/hiw/hiw4.svg"
     :txt "Merge the best solution and start building a community of talent."}]
   :candidate
   [{:img "/images/hiw/candidate/hiw/hiw1.svg"
     :txt "Explore issues using tech you are interested in or want to learn."}
    {:img "/images/hiw/candidate/hiw/hiw2.svg"
     :txt "Express your interest in a task or get started straight away."}
    {:img "/images/hiw/candidate/hiw/hiw3.svg"
     :txt "Solve the issue in your own time and submit your code."}
    {:img "/images/hiw/candidate/hiw/hiw4.svg"
     :txt "Improve your profile and get real production experience."}]})

(def how-it-works-questions
  {:company
   [{:title "How do I get started?"
     :answer "With one click you can connect your company GitHub account, choose which repositories and issues you would like to be displayed.  These issues can be used to test, engage, and develop your own community of Software Engineers. With 84% of our users more open to joining a company whose open source they had already contributed to and 90% more likely to consider companies with open source code it’s a great way to increase your hiring pipeline."}
    {:title "Why should I post our open source issues?"
     :answer "Open source issues is a way of engaging and testing our pool of talent before hiring them full time.  It allows you to build a community of engineers who are interested in the tech you are using and the work you are doing.  89% of the engineers on WorksHub want to work with open source software so It’s the quickest way to engage our talent pools."}
    {:title "How do I prepare my repositories?"
     :answer "In order to get contributions to your repo, our engineers will need to set up a basic build environment. We recommend setting up a detailed README that documents the pull request acceptance guidelines and how to set up the dev environment.  If you want feedback on your issues before pushing them live get in touch with the WorksHub team at: hello@works-hub.com\n"}
    {:title "How do I fund an issue and what do you charge for it?"
     :answer "If you wish to fund an issue, you can add a value to each individual issue.  Just head to your issue listings and edit the right issue.  Currently all issues will be value against $USD.   Once an issue has been completed and work approved you will be responsible for making the payment to the successful contributor.  This transaction will currently happen outside of the WorksHub platform and is the responsibility of issue maintainer and posting company. We are currently offering this as a free service for companies on our Essential, Launch Pad or Take Off plans."}]
   :candidate
   [{:title "How do I get started?"
     :answer "To complete an issue it’s easy, just find an issue that you find interesting and hit start work. Then follow these steps: Fork the repo, clone the project and you should then find setup instructions in README.  Take your time to complete the issue and once it’s ready submit your PR on GitHub and don't forget to link the issue to it.  Relax and wait for the maintainer to review your PR.  As soon as you start work on an issue the maintainer will be alerted via the WorksHub platform."}
    {:title "I want to work on an issue but someone else has already started?"
     :answer "We do not limit the number of people that can start work on an issue. You can see the number of contributors from the Issue page and the Issue listing page.  If you are concerned that your work might not get seen you can always comment on the issue and ask how the other contributors are getting on."}
    {:title "Why should I contribute?"
     :answer "Around 80% of the world’s companies run on some open source software. The future of our platform is to use this code as a way to test, engage and build engineering teams. In the meantime, if you want to provide value to your experience (beyond resume buzz words) or learn a new tech using production code then our issues are made for you.  We ask all company maintainers to write a repo README that makes it as easy as possible for new developers to set up and contribute."}
    {:title "How do I get paid for my work?"
     :answer "Only issues that indicate a value will result in you being paid for your work. The repo maintainer is solely responsible for the approval of your work and the pay out will be issued following approval. The company responsible for the issue retains the access to money and upon completion will transfer the money through Paypal or their payment provider of choice. In the future we hope to implement a solution in which the money is held in escrow with payments being released when work is merged."}]})

(def size-strings
  {"micro"  "1-9"
   "small"  "10-49"
   "medium" "50-249"
   "large"  "250+"})

(def www-hero-copy "We help you connect with and build a community of engineers through open-source contributions, removing the existing barriers to hiring")

(defn www-hero-title
  [market]
  (str "Hire " market " using your open source code"))

(def logged-in-menu-id "logged-in-menu")
