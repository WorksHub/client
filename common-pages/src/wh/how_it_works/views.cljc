(ns wh.how-it-works.views
  (:require #?(:cljs [reagent.core :as r])
            #?(:cljs [wh.components.github :as github])
            [wh.common.data :as data]
            [wh.components.carousel :refer [carousel]]
            [wh.components.common :refer [link]]
            [wh.components.faq :as faq]
            [wh.components.icons :refer [icon]]
            [wh.how-it-works.subs :as subs]
            [wh.re-frame.subs :refer [<sub]]
            [wh.util :as util]))

(defn github-button [class]
  #?(:cljs [github/install-github-app (if class {:class class} {})]))

(defn get-started-link
  [github? get-started-route & [{:keys [class label] :or {label data/get-started-cta-string}}]]
  (let [btn [:button.button.button--public label]]
    (cond
      (or github? (<sub [::subs/show-github-buttons?]))
      [github-button class]
      :else
      (link btn
            get-started-route
            :query-params {:redirect "issues"}
            :class class))))

(defn slider
  [selected-site & [on-slide]]
  [:div.how-it-works__type-slider
   [:div
    {:class (util/merge-classes
              "how-it-works__type-slider__option" "how-it-works__type-slider__option--company"
              (when-not (= :company selected-site)
                "how-it-works__type-slider__option--disabled"))}
    [:img {:src "/images/get_started/company.svg"
           :alt ""}]
    (if on-slide
      [:button
       {:class (util/merge-classes
                 "button"
                 (when-not (= :company selected-site)
                   "button--disabled"))
        :on-click #(on-slide :company)}
       "Company"]
      [link
       [:button {:class (util/merge-classes
                          "button"
                          (when-not (= :company selected-site)
                            "button--disabled"))}
        "Company"]
       :how-it-works :query-params {:site "company"}])]
   [:div
    {:class (util/merge-classes
              "how-it-works__type-slider__option" "how-it-works__type-slider__option--candidate"
              (when-not (= :candidate selected-site)
                "how-it-works__type-slider__option--disabled"))}
    [:img {:src "/images/get_started/candidate.svg"
           :alt ""}]
    (if on-slide
      [:button
       {:class (util/merge-classes
                 "button"
                 (when-not (= :candidate selected-site)
                   "button--disabled"))
        :on-click #(on-slide :candidate)}
       "Candidate"]
      [link
       [:button
        {:class (util/merge-classes
                  "button"
                  (when-not (= :candidate selected-site)
                    "button--disabled"))}
        "Candidate"]
       :how-it-works :query-params {:site "candidate"}])]])

(defn header
  [selected-site github?]
  [:div.how-it-works__header
   [:div.how-it-works__header__inner
    [:div.how-it-works__header__img
     [:div.how-it-works__header__img-inner
      [:img {:src "/images/hiw/header.svg"
             :alt ""}]]]
    [:div.how-it-works__header__copy
     [:h1 "Use Open Source to hire or get hired"]
     [:p data/www-hero-copy]
     (if github?
       [github-button nil]
       [slider selected-site])]]])

(defn step-line
  [n]
  [:div.how-it-works__step-line.is-hidden-mobile
   (let [forms (butlast (interleave (range n) (map (partial str "border") (range n))))]
     (for [form forms]
       (if (number? form)
         [:div.number-circle.how-it-works__step-line__circle {:key form} (inc form)]
         [:div.how-it-works__step-line__line   {:key form}])))])

(defn explanation-step
  [img-src description n]
  [:div.how-it-works__explanation-step
   [:div.how-it-works__explanation-step__inner
    [:img {:src img-src
           :alt ""}]
    [:div.how-it-works__explanation-step__description
     [:div.number-circle.how-it-works__step-line__circle.is-hidden-desktop n]
     [:p description]]]])

(defn explanation
  ([selected-site github?]
   [explanation selected-site github? :register])

  ([selected-site github? get-started-route]
   [:div
    {:class (util/merge-classes"how-it-works__explanation"
                               (str "how-it-works__explanation--" (name selected-site)))}
    [:div.how-it-works__explanation__inner
     [:div.how-it-works__explanation__copy
      [:h2 (if (= :company selected-site) "COMPANIES" "CANDIDATES")]
      [:h3 "How it works"]]
     [:div.how-it-works__explanation__steps
      (let [steps (get data/how-it-works-explanation-steps selected-site)]
        (doall
          (for [i (range (count steps))]
            (let [{:keys [img txt]} (nth steps i)]
              ^{:key img}
              [explanation-step img txt (inc i)]))))]
     [get-started-link github? get-started-route]]
    [step-line 4]]))

(defn benefit
  [{:keys [img title txt]}]
  [:div.how-it-works__benefit
   [:div.how-it-works__benefit__inner
    [:div.how-it-works__benefit__header
     [:img {:src img
            :alt ""}]
     (when title
       [:h4 title])]
    [:p txt]]])

(defn benefits-list
  [items {:keys [class]}]
  [:div {:class (util/merge-classes "how-it-works__benefits-list" class)}
   (doall
     (for [item items]
       ^{:key (:img item)}
       [benefit item]))])

(defn stats-ball
  [[title para icon-logo] colour]
  [:div
   {:class (util/merge-classes
             "how-it-works__stats__ball"
             (str "how-it-works__stats__ball--" (name colour)))}
   (when icon-logo
     [icon icon-logo])
   [:strong title]
   [:p para]])

(defn stats
  ([selected-site github?]
   [stats selected-site github? :register])
  ([selected-site github? get-started-route]
   (let [{:keys [title subtitle info blue grey orange]} (get data/how-it-works-stats selected-site)]
     [:div
      {:class (util/merge-classes
                "how-it-works__stats"
                (str "how-it-works__stats--" (name selected-site)))}
      [:div.how-it-works__stats__inner
       [get-started-link github? get-started-route {:class "is-hidden-desktop how-it-works__github-button" :label "Get Started for Free"}]
       [:div.how-it-works__stats__balls.is-hidden-mobile
        [:div.how-it-works__stats__balls-container
         (stats-ball blue :blue)
         (stats-ball grey :grey)
         (stats-ball orange :orange)]]
       [:div.how-it-works__stats__balls.is-hidden-desktop
        [:div.how-it-works__stats__balls-container
         (stats-ball blue :blue)
         (stats-ball orange :orange)
         (stats-ball grey :grey)]]
       [:div.how-it-works__stats__info
        [:h2 "THE POTENTIAL"]
        [:h3 "Why open source software is the future of hiring"]
        (for [i info]
          ^{:key i}
          [:p i])
        [get-started-link github? get-started-route {:class "is-hidden-mobile how-it-works__github-button" :label "Get Started for Free"}]]]])))

(defn faq
  ([selected-site github?]
   [faq selected-site github? :register])
  ([selected-site github? get-started-route]
   [:div.how-it-works__faq.how-it-works__faq--company
    [:div.how-it-works__faq__inner
     [:h2 "FAQs"]
     [:h3 "What else would you like to know?"]
     [faq/faq-component (get data/how-it-works-questions selected-site) selected-site]]]))

(defn render
  [selected-site github?]
  [:div.how-it-works
   [header selected-site github?]
   [explanation selected-site github?]
   [stats selected-site github?]
   [faq selected-site github?]])

(defn page
  [& [{:keys [github? site-type] :or {github? false}}]]
  [render (or site-type (<sub [::subs/selected-site])) github?])

(defn pod--choice
  []
  [:div.pod.how-it-works-pod.how-it-works-pod--choice
   [:div.how-it-works-pod__basic-info-container
    [:h2 "Use Open Source to hire or get hired"]
    [:p.is-hidden-mobile "To find out more, please tell us about yourself"]
    [:p.is-hidden-desktop "Please tell us if you're a "]
    [link [:button.button
           [:span.is-hidden-mobile "I am a candidate"]
           [:span.is-hidden-desktop "candidate"]] :how-it-works]
    [link [:button.button
           [:span.is-hidden-mobile "I am a company"]
           [:span.is-hidden-desktop "company"]] :how-it-works]]
   [:div.how-it-works-pod__img
    [:img {:src "/images/hiw/header.svg"
           :alt ""}]]])

(defn pod--basic
  []
  [:div.pod.how-it-works-pod.how-it-works-pod--basic
   [:div.how-it-works-pod__basic-info-container
    [:h2 "Use Open Source to hire or get hired"]
    [link [:button.button "How it works"] :how-it-works]]
   [:div.how-it-works-pod__img
    [:img {:src "/images/hiw/header.svg"
           :alt ""}]]])

(defn pod--benefits-render
  [user-type selected-site on-slide]
  (when-not (= user-type "admin")
    (let [img-selector {:company "/images/hiw/company/benefits/benefit2.svg"
                        :candidate "/images/hiw/candidate/hiw/hiw4.svg"}]
      [:div
       {:class (util/merge-classes
                 "pod"
                 "how-it-works-pod"
                 "how-it-works-pod--benefits"
                 (when (nil? user-type)
                   "how-it-works-pod--benefits--slider"))}
       (when (nil? user-type)
         [slider selected-site on-slide])
       [:div.is-flex
        [:div.how-it-works-pod--benefits-info
         [:h2 "What's in it for you?"]
         [:ul
          (for [{:keys [txt]} (get data/how-it-works-benefits selected-site)]
            ^{:key txt}
            [:li [icon "cutout-tick"] txt])]]
        [:div.how-it-works-pod--benefits-img
         [:img {:src (img-selector selected-site)
                :alt ""}]]]])))

(defn pod--benefits
  [user-type]
  #?(:clj (pod--benefits-render nil (<sub [::subs/selected-site]) nil)
     :cljs (let [selected-site (r/atom (if user-type (keyword user-type) (<sub [::subs/selected-site])))]
             (fn [& [user-type]]
               (pod--benefits-render user-type @selected-site #(reset! selected-site %))))))

(defn pod--side-render
  [mode arrows?]
  (let [steps (get data/how-it-works-explanation-steps mode)]
    [:div
     {:class (util/merge-classes
               "pod" "how-it-works-pod"
               (str "how-it-works-pod--" (name mode)))}
     [:h2 "How it works"]
     [carousel
      (for [{:keys [img txt idx]} (map-indexed #(assoc %2 :idx %1) steps)]
        [:div
         [:div.how-it-works-pod__carousel
          [:div.how-it-works-pod__selection-img
           [:img {:src img
                  :alt ""}]]]
         [:p.how-it-works-pod__selection-txt
          txt]])
      {:arrows? arrows?}]

     [link [:button.button "Tell me more"] :how-it-works :query-params (when (= mode :candidate) {:site "candidate"})]]))

(defn pod--candidate
  []
  [pod--side-render :candidate true])

(defn pod--company
  []
  [pod--side-render :company true])
