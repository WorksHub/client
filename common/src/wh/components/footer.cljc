(ns wh.components.footer
  (:require
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.components.icons :refer [icon]]
    [wh.routes :as routes]
    [wh.slug :as slug]
    [wh.util :as util]
    [wh.verticals :as verticals]))

(defn vertical-links []
  (for [vertical verticals/ordered-job-verticals]
    {:title (verticals/config vertical :platform-name)
     :href  (str "https://" vertical ".works-hub.com")}))

(defn footer-links [vertical]
  {:workshub  (concat
                [{:title "Careers"
                  :href  "https://hire.withgoogle.com/public/jobs/functionalworkscom"}
                 ;; TODO ABOUT US PAGE
                 #_{:title "About us"
                    :href  "#"}

                 ;; TODO BLOG PAGE FROM BOGDAN
                 #_{:title "Hired & Vettery alternatives"
                    :href  "#"}
                 ;; TODO BLOG PAGE FROM BOGDAN
                 #_{:title "Case studies"
                    :href  "#"}
                 {:title "Companies"
                  :route [:companies]}
                 {:title "Sitemap"
                  :route [:sitemap]}
                 {:title "Metrics"
                  :route [:metrics]}]
                (vertical-links))
   :employers [{:title "For employers"
                :route  [:employers]}
               {:title "Register company"
                :route [:register-company]}
               {:title "Pricing and plans"
                :href  (str routes/company-landing-page "/pricing")}
               {:title "Request a demo"
                :href  verticals/demo-link}
               {:title "Terms of Service"
                :route [:terms-of-service]}]})


(defn create-link
  [prefix {:keys [title href]}]
  [:a {:key title :href href}
   (str prefix title)])

(defn footer-sitemap-www []
  (let [link-data (footer-links "www")]
    [:div.footer__sitemap__list__container
     [:div.footer__sitemap__list
      [:div "Highest in-demand talent"]
      (for [m (vals data/in-demand-hiring-data)]
        (create-link "Hire " m))]
     [:div.footer__sitemap__list
      [:div "Locations"]
      (for [m data/in-demand-location-data]
        (create-link "Hire " m))]
     [:div.footer__sitemap__list
      [:div "WorksHub"]
      (for [{:keys [title href route]} (:workshub link-data)]
        ^{:key title}
        [:a (cond
              route {:href (apply routes/path route)}
              href {:href   href
                    :target "_blank"
                    :rel    "noopener"}) title])]
     [:div.footer__sitemap__list
      [:div "Employers"]
      (for [{:keys [title href route]} (:employers link-data)]
        ^{:key title}
        [:a (cond route {:href (apply routes/path route)}
                  href {:href   href
                        :target "_blank"
                        :rel    "noopener"}) title])]]))

(defn footer-sitemap-job-vertical [vertical]
  (let [link-data (footer-links vertical)]
    [:div.footer__sitemap__list__container
     [:div.footer__sitemap__list
      [:div "WorksHub"]
      (for [{:keys [title href route]} (:workshub link-data)]
        ^{:key title}
        [:a (cond
              route {:href (apply routes/path route)}
              href  {:href   href
                     :target "_blank"
                     :rel    "noopener"}) title])]
     [:div.footer__sitemap__list
      [:div "For companies"]
      (for [{:keys [title href route]} (:employers link-data)]
        ^{:key title}
        [:a (cond route {:href (apply routes/path route)}
                  href  {:href   href
                         :target "_blank"
                         :rel    "noopener"}) title])]
     [:div.footer__sitemap__list
      [:div "Jobs"]
      (for [label (verticals/config vertical :footer-job-links)]
        [:a {:key  (str label "-jobs")
             :href (routes/path :pre-set-search :params {:tag (str (slug/slug label))})}
         (str label " jobs")])]
     [:div.footer__sitemap__list
      [:div "Locations"]
      (for [city data/preset-job-cities
            :let [label (str "Jobs in " city)]]
        [:a {:key  label
             :href (routes/path :pre-set-search :params {:location (str (slug/slug city))})}
         label])
      [:a {:key  "remote-jobs"
           :href (routes/path :pre-set-search :params {:tag "remote"})}
       "Remote Jobs"]]
     [:div.footer__sitemap__list
      [:div "Articles"]
      (for [tag (verticals/config vertical :footer-article-links)]
        [:a {:key  (str tag "-articles")
             :href (routes/path :learn-by-tag :params {:tag (slug/tag-label->slug tag)})}
         (str tag " articles")])]]))

(defn social-links [vertical]
  [{:logo "linkedin-circle"
    :href (verticals/config vertical :linkedin)}
   {:logo "twitter-circle"
    :href (str "https://twitter.com/" (verticals/config vertical :twitter))}
   {:logo "facebook-circle"
    :href "https://business.facebook.com/weareworkshub/"}
   {:logo "github"
    :href "https://github.com/workshub"}])

(defn footer [vertical logged-in? & [additional-class]]
  [:div.footer {:class additional-class}
   [:div.footer__top
    [:div.footer__info
     [:div.footer__info__logo
      [:div.footer__info__logo__codi
       [icon "www"]]
      [:div.footer__info__logo__title
       "WorksHub"]]
     [:div.footer__info__contact
      [:div [:span "\uD83D\uDCE7"] [:span [:a {:href "mailto:hello@works-hub.com"} "hello@works-hub.com"]]]
      [:div [:span "\uD83C\uDDEC\uD83C\uDDE7"] [:span [:a {:href   "https://goo.gl/maps/U8zoGSJ17wQ2"
                                                           :target "_blank"
                                                           :rel    "noopener"}
                                                       "36 New Inn Yard, London, EC2A 3EY"]]]
      [:div [:span "\uD83C\uDDFA\uD83C\uDDF8"] [:span [:a {:href   "https://goo.gl/maps/aymEPiDebYS2"
                                                           :target "_blank"
                                                           :rel    "noopener"}
                                                       "108 E 16th Street, New York, NY 10003"]]]]]
    [:div.footer__graphic
     [:img {:src "/images/homepage/footer.svg"
            :alt ""}]]]
   [:div.footer__sitemap
    (if (and (= "www" vertical) (not logged-in?))
      (footer-sitemap-www)
      (footer-sitemap-job-vertical vertical))]
   [:div.footer__bottom
    [:div.footer__social-icons
     (for [{:keys [logo href]} (social-links vertical)]
       ^{:key logo}
       [:a {:class  (util/merge-classes "footer__social-icon" logo)
            :href   href
            :target "_blank"
            :rel    "noopener"}
        (icon logo)])]
    [:div.footer__legal
     [:div.footer__legal__item "© 2020 WorksHub"]
     [:div.footer__legal__item [:a {:href (routes/path :privacy-policy)} [:span "Privacy Policy"]]]
     [:div.footer__legal__item [:a {:href "https://www.works-hub.com"} [:span "Developed by WorksHub"]]]]]])
