(ns wh.components.www-homepage
  (:require
    [wh.components.common :refer [companies-section link]]
    [wh.components.icons :refer [icon]]
    [wh.util :as util]
    [wh.common.text :as txt]
    [wh.common.data :as data]
    [clojure.string :as str]
    #?(:cljs [reagent.core :as r])))

(def num-clouds 23)

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
    :img "/images/homepage/feature01.svg"
    :id :promo}
   {:description "Easy-to-use tools to track and share applicants"
    :img "/images/homepage/feature02.svg"
    :id :tools}
   {:description "Real-time analytics on applicants and job viewers"
    :img "/images/homepage/feature03.svg"
    :id :analytics}
   {:description "Recommended candidates who are matched to your job"
    :img "/images/homepage/feature04.svg"
    :id :recommendations}])

(def testimonials-data
  [{:quote "WorksHub’s understanding of the market and other companies in the space has contributed heavily to our ability to develop and expand our team of Haskell and PureScript developers."
    :source "Will Jones, VP of Engineering"
    :logo "/images/homepage/logos/habito.png"}
   {:quote "WorksHub has consistently been able to deliver an excellent standard of candidate and we have been successful in recruiting for our needs in the knowledge that they have a thorough understanding of our growth requirements."
    :source "Chris Percival, CTO"
    :logo "/images/homepage/logos/inchora.png"}
   {:quote "WorksHub is not only able to consistently find top-tier talent in a hyper-competitive market for Engineering hires but also takes the time to deeply understand our business and culture to ensure a great match and a great interview experience. They are the best in the game!"
    :source "Greg Ratner, CTO "
    :logo "/images/homepage/logos/troops.svg"}
   {:quote "WorksHub has been an invaluable way to find amazing talent. No one comes close to their depth of experience"
    :source "Reuben, CTO"
    :logo "/images/homepage/logos/avantstay.png"}
   {:quote "WorksHub has enabled us to constantly find out the great talents of Scala and functional programming globally and keep us very competitive in Japan to grow rapidly."
    :source "Ken Izumi, VP Engineering"
    :logo "/images/homepage/logos/paidy.svg"}])

(defn header
  [market]
  [:div.homepage__header
   [:div.homepage__header__img
    [:div.homepage__header__img-inner
     [:img {:src "/images/homepage/header.svg"}]]]
   [:div.homepage__header__copy
    [:h1 "Hire " market " based on their interests and experience"]
    [:p "Through open-source contributions we generate objective ratings to help you hire the right engineers, faster."]
    (link [:button.button
           {:id "www-landing__hero"}
           "Get Started"] :get-started)]])

(defn features
  []
  [:div.columns.homepage__features
   (for [{:keys [description img id]} features-data]
     ^{:key id}
     [:div.column.homepage__feature-column
      [:div.homepage__feature-column__img-container
       [:img {:src img}]]
      [:div.homepage__feature-column__description-container
       [:span description]]])])

(defn walkthrough
  []
  [:div.homepage__walkthrough
   [:h2 "READY FOR TAKE-OFF?"]
   [:h3 "Let's get started!"]
   ;; one
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Create a new role and post to one or all of our technical hubs. Our " [:strong "matching algorithm"] " will instantly recommend your jobs to vetted suitable Software Engineers "]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__jobsboard"}
            "View jobsboard"] :jobsboard)]
    [:div.column.homepage__step__img.homepage__step__img--offset
     [:img {:src "/images/homepage/walkthrough01.svg"}]]]
   ;; two
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Your " [:strong "real time dashboard"] " allows you to monitor the performance of each job, helping you get more applications and engage a wider community, building brand awareness"]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__features"}
            "View all our features"] :pricing)]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough02.svg"}]]]
   ;; three
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Have " [:strong "Open Source Issues"] " that need attention? Connect your company GitHub account and add issues to start building your talent pool and get more qualified applications."]
     (link [:button.button.button--inverted
            {:id "www-landing__walkthrough__opensource"}
            "View Open Source Issues"] :issues :company-id "workshub-f0774")]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough03.svg"}]]]
   ;; four
   [:div.columns.homepage__step
    [:div.column.homepage__step__description
     [:span "Have a question along the way? We have a " [:strong "team of experts"] " that can help at every step of the process helping you make the best possible hire."]
     (link [:button.button
            {:id "www-landing__walkthrough__experts"}
            "Get Started"] :get-started)]
    [:div.column.homepage__step__img
     [:img {:src "/images/homepage/walkthrough04.svg"}]]]])

(defn animated-hr
  [img-src class]
  [:div.homepage__animated-hr
   [:div.homepage__animated-hr__bg
    (for [idx (range num-clouds)]
      [:img {:key (str "cloud" idx)
             :src "/images/homepage/cloud.svg"
             :class (str "homepage__animated-hr__bg__cloud" idx)}])]
   [:div
    {:class (util/merge-classes "homepage__animated-hr__img"
                                (when (txt/not-blank class) class))}
    [:img {:src img-src}]]])

(defn testimonial-pips
  [n active-n on-click]
  [:div.homepage__testimonial-pips-wrapper
   [:div.homepage__testimonial-pips
    (for [i (range n)]
      ^{:key i}
      [:div
       (merge {:class (util/merge-classes "homepage__testimonial-pip"
                                          (when on-click "homepage__testimonial-pip--clickable")
                                          (when (= i active-n) "homepage__testimonial-pip--active"))}
              (when on-click
                {:on-click #(on-click i)}))
       (icon "circle")])]])

(defn testimonial
  [{:keys [quote source logo]} active?]
  [:div.homepage__testimonial_wrapper
   {:key source}
   [:div
    {:class (util/merge-classes "homepage__testimonial"
                                (when active? "homepage__testimonial--active"))}
    [:div.homepage__testimonial__quote "\"" quote "\""]
    [:div.homepage__testimonial__source source]
    [:div.homepage__testimonial__logo
     [:img {:src logo}]]]])

(defn inner-testimonials
  [n on-click]
  [:div.homepage__testimonials
   [:h3 "Join our satisfied worldwide clients"]
   (link [:button.button
          {:id "www-landing__testimonials"}
          "Sign up for your trial today"] :get-started)
   [:div.homepage__testimonials__carousel
    (for [idx (range (count testimonials-data))]
      (testimonial (nth testimonials-data idx) (= n idx) ))
    (testimonial-pips (count testimonials-data) n on-click)]])

(defn testimonials
  []
  #?(:clj
     (inner-testimonials 0 nil)
     :cljs
     (let [active-testimonial (r/atom 0)
           rotate (.setInterval js/window #(swap! active-testimonial inc) 5000)]
       (fn []
         (inner-testimonials (mod @active-testimonial (count testimonials-data))
                             #(do (.clearInterval js/window rotate)
                                  (reset! active-testimonial %)))))))

(defn looking-to-hire
  [title hide-boxes?]
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
          "Get Started"] :get-started)])

(defn homepage
  ([]
   (homepage nil))
  ([{:keys [template]}]
   (let [hiring-target (data/find-hiring-target template)]
     [:div.homepage
      [:div.homepage__top-content
       [:div.homepage__top-content__container
        (header (or (:title hiring-target)
                    (when template (txt/capitalize-words (str/lower-case (str/replace template #"[-_/]" " "))))
                    "software engineers"))]
       [:div.homepage__companies-section
        [:div.homepage__companies-section__container
         (companies-section "Trusted by 300+ companies:")]]]
      [:div.homepage__middle-content
       [:div.homepage__middle-content__container
        [:h2 "THE NEW STANDARD FOR TECHNICAL HIRING"]
        [:h3 "We are breaking down the barriers of hiring software engineers"]
        (features)
        [:div.homepage__feature-ctas
         ;; TODO ABOUT US PAGE
         #_(link [:button.button.button--inverted
                  "Discover how we're different"] :pricing)
         (link [:button.button
                {:id "www-landing__barriers-try"}
                "Get Started"] :get-started)]]
       [animated-hr "/images/homepage/rocket.svg" "homepage__animated-hr__rocket"]
       [:div.homepage__middle-content__container
        (walkthrough)]
       [animated-hr "/images/homepage/globe.svg" "homepage__animated-hr__globe"]
       [:div.homepage__middle-content__container
        [testimonials]]
       [animated-hr nil nil]]
      [:div.homepage__bottom-content
       [:div.homepage__bottom-content__container
        (looking-to-hire (:description2 hiring-target) (boolean template))]]])))
