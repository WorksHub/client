(ns wh.components.www-homepage
  (:require
    #?(:cljs [reagent.core :as r])
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.common.text :as txt]
    [wh.components.carousel :refer [carousel]]
    [wh.components.common :refer [companies-section link]]
    [wh.components.icons :refer [icon]]
    [wh.how-it-works.views :as hiw]
    [wh.re-frame.subs :refer [<sub]]
    [wh.util :as util]))

(def num-clouds 23)
(def get-started-cta-string "Get Started for Free")

(comment
  "Used to generate the SASS for clouds"
  (println
   (loop [result "" total -50 idx 0]
     (if (< total 4000)
       (let [x (rand-int 120)
             y (rand-int 80)
             s (rand-nth [:s :m :l])]
         (recur (str result
                     (format ".homepage__animated-hr__bg__cloud%s\n  width: %spx; height: %spx; left: %spx; top: %spx;\n"
                             idx
                             (case s
                               :s 34
                               :m 68
                               :l 136)
                             (case s
                               :s 17
                               :m 34
                               :l 68) (+ total x) y)) (+ total 180) (inc idx))) result))))

(def features-data
  [{:description "Targeted promotion of your job post to relevant members"
    :img         "/images/homepage/feature01.svg"
    :id          :promo}
   {:description "Easy-to-use tools to track and share applicants"
    :img         "/images/homepage/feature02.svg"
    :id          :tools}
   {:description "Connect with GitHub and get contributions to your open source code"
    :img         "/images/homepage/feature03.svg"
    :id          :analytics}
   {:description "Recommended candidates who are matched to your job"
    :img         "/images/homepage/feature04.svg"
    :id          :recommendations}])

(def integrations-data
  [{:description "Get notifications in Slack when people apply to your jobs or start work on your Open Source issues"
    :img         "/images/company/slack-icon.svg"
    :id          :slack}
   {:description "Applications automatically forwarded from WorksHub into Greenhouse"
    :img         "/images/company/greenhouse-icon.svg"
    :id          :greenhouse}
   {:description "Keep your setup in Workable and let WorksHub push applications in real-time"
    :img         "/images/company/workable-icon.svg"
    :id          :workable}
   {:description "Got your own ATS? Webhooks coming soon..."
    :img         "/images/company/webhooks.svg"
    :id          :webhooks}])

(def testimonials-data
  [{:quote  "WorksHub’s understanding of the market and other companies in the space has contributed heavily to our ability to develop and expand our team of Haskell and PureScript developers."
    :source "Will Jones, VP of Engineering"
    :logo   "/images/homepage/logos/habito.png"}
   {:quote  "WorksHub has consistently been able to deliver an excellent standard of candidate and we have been successful in recruiting for our needs in the knowledge that they have a thorough understanding of our growth requirements."
    :source "Chris Percival, CTO"
    :logo   "/images/homepage/logos/inchora.png"}
   {:quote  "WorksHub is not only able to consistently find top-tier talent in a hyper-competitive market for Engineering hires but also takes the time to deeply understand our business and culture to ensure a great match and a great interview experience. They are the best in the game!"
    :source "Greg Ratner, CTO "
    :logo   "/images/homepage/logos/troops.svg"}
   {:quote  "WorksHub has been an invaluable way to find amazing talent. No one comes close to their depth of experience"
    :source "Reuben, CTO"
    :logo   "/images/homepage/logos/avantstay.png"}
   {:quote  "WorksHub has enabled us to constantly find out the great talents of Scala and functional programming globally and keep us very competitive in Japan to grow rapidly."
    :source "Ken Izumi, VP Engineering"
    :logo   "/images/homepage/logos/paidy.svg"}])

(defn header
  [market get-started-route]
  [:div.homepage__header
   [:div.homepage__header__img
    [:div.homepage__header__img-inner
     [:img {:src "/images/homepage/header.svg"
            :alt ""}]]]
   [:div.homepage__header__copy
    [:h1  (data/www-hero-title market)]
    [:p data/www-hero-copy]
    (link [:button.button
           {:id "www-landing__hero"}
           get-started-cta-string] get-started-route)]])

(defn features
  []
  [:div.columns.homepage__features
   (for [{:keys [description img id]} features-data]
     ^{:key id}
     [:div.column.homepage__feature-column
      [:div.homepage__feature-column__img-container
       [:img {:src img
              :alt ""}]]
      [:div.homepage__feature-column__description-container
       [:span description]]])])

(defn integrations
  []
  [:div.columns.homepage__integrations
   (for [{:keys [description img id]} integrations-data]
     ^{:key id}
     [:div.column.homepage__integration-column
      [:div.homepage__integration-column__img-container
       [:img {:class (str "homepage__integration-column__img homepage__integration-column__img-" (name id))
              :src   img
              :alt   ""}]]
      [:div.homepage__integration-column__description-container
       [:span description]]])])

(defn walkthrough
  [get-started-route]
  [:div.homepage__walkthrough
   [:h2 "READY FOR TAKE-OFF?"]
   [:h3 "Let's get started!"]
   ;; one
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Create a " [:strong "profile page"] " for your company. Use this space to sell your company to our community. Tell them all about what you’re building and how"]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__companies"}
            "View company profiles"] :companies)]
    [:div.column.homepage__step__img.homepage__step__img--offset
     [:img {:src "/images/homepage/walkthrough01.svg"
            :alt ""}]]]
   ;; two
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Your " [:strong "real time dashboard"] " allows you to monitor the performance of each job, helping you get more applications and engage a wider community, building brand awareness"]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__features"}
            "View all our packages"] :pricing)]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough02.svg"
            :alt ""}]]]
   ;; three
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Have " [:strong "Open Source Issues"] " that need attention? Connect your company GitHub account and add issues to start building your talent pool and get more qualified applications."]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__opensource"}
            "View Open Source Issues"] :issues :company-id "workshub-f0774")]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough03.svg"
            :alt ""}]]]
   ;; four
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Have a question along the way? We have a " [:strong "team of experts"] " that can help at every step of the process helping you make the best possible hire."]
     (link [:button.button
            {:id "www-landing__walkthrough__experts"}
            get-started-cta-string] get-started-route)]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough04.svg"
            :alt ""}]]]])

(defn animated-hr
  [img-src img-class & [class]]
  [:div
   {:class (util/merge-classes "homepage__animated-hr"
                               (when (txt/not-blank class) class))}
   [:div.homepage__animated-hr__bg
    (for [idx (range num-clouds)]
      [:img {:key (str "cloud" idx)
             :src "/images/homepage/cloud.svg"
             :class (str "homepage__animated-hr__bg__cloud" idx)
             :alt ""}])]
   [:div
    {:class (util/merge-classes "homepage__animated-hr__img"
                                (when (txt/not-blank img-class) img-class))}
    [:img {:src img-src
           :alt ""}]]])

(defn testimonial
  [{:keys [quote source logo]}]
  [:div.homepage__testimonial_wrapper
   {:key source}
   [:div
    {:class "homepage__testimonial"}
    [:div.homepage__testimonial__quote "\"" quote "\""]
    [:div.homepage__testimonial__source source]
    [:div.homepage__testimonial__logo
     [:img {:src logo
            :alt ""}]]]])

(defn testimonials
  ([]
   [testimonials nil])

  ([get-started-route]
   [:div.homepage__testimonials
    [:h3 "Join our satisfied worldwide clients"]
    (when get-started-route
      (link [:button.button.button__public
             {:id "www-landing__testimonials"}
             get-started-cta-string] get-started-route))
    [carousel
     (for [item testimonials-data]
       [testimonial item])]]))

(defn looking-to-hire
  [title hide-boxes? get-started-route]
  [:div.homepage__looking-to-hire
   [:div.homepage__looking-to-hire__header
    [:h3 "Who are you looking to hire?"]
    [:p (or title "Whether you’re looking to hire software developers or engineers, from front-end to full-stack to back-end, we’ve got you covered.")]]
   (when-not hide-boxes?
     [:ul.homepage__looking-to-hire__types
      (for [{:keys [title description logo href]} (vals data/in-demand-hiring-data)]
        ^{:key title}
        [:li.homepage__looking-to-hire__type
         [:div.homepage__looking-to-hire__type__title (str "Hire " title)]
         [:div.homepage__looking-to-hire__type__description description]
         [:a.a--underlined.homepage__looking-to-hire__type__link
          {:href href}
          (str "Hire " title)]
         [:div.homepage__looking-to-hire__type__logo
          (icon logo)]])])
   (link [:button.button
          {:id "www-landing__looking-to-hire"}
          get-started-cta-string] get-started-route)])

(defn homepage
  ([]
   (homepage nil))
  ([{:keys [template]}]
   (let [logged-in?        (<sub [:user/logged-in?])
         get-started-route (if logged-in? :register-company :get-started)
         hiring-target     (data/find-hiring-target template)]
     [:div.homepage
      [:div.homepage__top-content
       [:div.homepage__top-content__container
        (header (or (:title hiring-target)
                    (when template (txt/capitalize-words (str/lower-case (str/replace template #"[-_/]" " "))))
                    "software engineers")
                get-started-route)]
       [:div.homepage__companies-section
        [:div.homepage__companies-section__container
         (companies-section "Trusted by 300+ companies:")]]]
      [:div.homepage__middle-content
       [:div.homepage__middle-content__container
        [:h2 "THE NEW STANDARD FOR TECHNICAL HIRING"]
        [:h3 "How it works"]
        (features)
        [:h2 "INTEGRATIONS"]
        (integrations)
        [:div.homepage__feature-ctas
         ;; TODO ABOUT US PAGE
         #_(link [:button.button.button--inverted
                  "Discover how we're different"] :pricing)
         (link [:button.button
                {:id "www-landing__barriers-try"}
                get-started-cta-string] get-started-route)]]
       [animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket homepage__animated-hr__rocket--start"]
       [hiw/stats :company false get-started-route]
       [animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket homepage__animated-hr__rocket--mid"]
       [hiw/benefits :company false get-started-route]
       [animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket homepage__animated-hr__rocket--end"]
       [:div.homepage__middle-content__container
        (walkthrough get-started-route)]
       [animated-hr "/images/homepage/globe.svg" "homepage__animated-hr__globe" "homepage__animated-hr__globe-wrapper"]
       [:div.homepage__middle-content__container
        [testimonials get-started-route]]
       [animated-hr nil nil]]
      [:div.homepage__bottom-content
       [:div.homepage__bottom-content__container
        (looking-to-hire (:description2 hiring-target) (boolean template) get-started-route)]]])))
