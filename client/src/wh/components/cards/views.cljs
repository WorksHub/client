(ns wh.components.cards.views
  (:require
    [clojure.string :as str]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [wh.common.text :refer [pluralize]]
    [wh.components.cards.subs]
    [wh.components.common :refer [link img wrap-img]]
    [wh.components.ellipsis.views :refer [ellipsis]]
    [wh.components.icons :refer [icon]]
    [wh.routes :as routes]
    [wh.subs :as subs :refer [<sub]]))

(defn state->candidate-status
  [s]
  (cond (or (= s :approved)
            (= s :pending))  "Pending"
        (= s :get_in_touch)  "Interviewing \u2728"
        (or (= s :pass)
            (= s :rejected)) "Rejected \uD83D\uDE41"
        (= s :hired)         "Hired"))

(defn polar->cartesian [center-x center-y radius angle-in-degrees]
  (let [angle-in-radians (/ (* (- angle-in-degrees 90)
                               js/Math.PI)
                            180)]
    {:x (+ center-x (* radius (js/Math.cos angle-in-radians)))
     :y (+ center-y (* radius (js/Math.sin angle-in-radians)))}))

(defn draw-shape [score]
  (let [x 9
        y 9
        radius 9
        start-angle 0
        end-angle (* 360 score)
        start (polar->cartesian x y radius end-angle)
        end (polar->cartesian x y radius start-angle)
        arc-sweep (if (<= (- end-angle start-angle) 180)
                    "0"
                    "1")]
    (->> ["M" (:x start) (:y start)
         "A" radius radius 0 arc-sweep 0 (:x end) (:y end)
         "L" x y
         "L" (:x start) (:y start)]
         (str/join " "))))

(defn match-circle
  ([score]
   (match-circle score false))
  ([score text?]
   [:div.match-circle-container
    (if (= score 1.0)
      [:div.match-circle
       [:div.foreground]]
      [:div.match-circle
       [:svg.circle-value
        [:path.circle {:d (draw-shape score)}]]
       [:div.background]])
    (when text?
      [:div.text (gstring/format "%d%% Match" (* score 100))])]))

(defn closed-job
  [{:keys [company-name tagline title liked display-salary tags sponsorship-offered remote role-type display-location score state] :as job}
   public]
  [:div.card.card--job {:class (str "card-border-color-" (rand-int 9)
                                    " i-cur-" (rand-int 9)
                                    (when public
                                      " job-card--public"))}
   [:span
    (when (and (not public) liked)
      [icon "like"
       :class (str "job__icon like job__icon__private" (when liked " selected"))])
    [:div.info
     [:div.logo
      (when-not public
        [:img {:src (:logo job)
               :alt (str company-name " logo")}])]
     [:div.basic-info
      [:div.job-title title]
      (when-not public
        [:div.company-name company-name])
      [:div.location display-location]
      (cond-> [:div.card__perks]
        remote (conj [icon "job-remote" :class "job__icon--small"] "Remote")
        (not= role-type "Full time") (conj [icon "profile" :class "job__icon--small"] role-type)
        sponsorship-offered (conj [icon "job-sponsorship" :class "job__icon--small"] "Sponsorship"))
      [:div.salary display-salary]]]]
   (into [:ul.tags.tags__job]
         (map (fn [tag]
                [:li {:on-click #(dispatch [:wh.search/search-by-tag tag false])} tag])
              tags))

   [:div.tagline tagline]
   (when score
     [:div.match
      [match-circle score]
      [:span.match__label (str (gstring/format "%.0f%%" (* 100 score)) " match")]])
   [:div.apply
    (when state
      [:div.state
       [:span.applied-state "Status: " (state->candidate-status state)]])
    [:div.buttons
     [:div.card__label.card__label--unpublished
      "Closed"]]]])

(defn job-card-header
  [{:keys [id slug liked logo remote sponsorship-offered company-name title role-type display-location display-salary] :as job} on-close public]
  (let [skeleton? (and job (empty? (dissoc job :id)))]
    [:span
     (when-not (or public skeleton? (<sub [:user/company?]))
       [icon "like"
        :id (str "job-card__like-button_job-" id)
        :class (str "job__icon like job__icon__private job__icon--pointer" (when liked " selected"))
        :on-click #(dispatch [:wh.events/toggle-job-like job])])
     (when (and on-close (not skeleton?) (not public))
       [icon "close"
        :id (str "job-card__blacklist-button_job-" id)
        :class "job__icon blacklist"
        :on-click #(dispatch [:wh.events/blacklist-job job on-close])])
     [link
      [:div.info
       [:div.logo
        (cond skeleton?
              [:div]
              (not public)
              (wrap-img img logo {:alt (str company-name " logo") :w 48 :h 48}))]
       [:div.basic-info
        [:div.job-title title]
        (when-not public
          [:div.company-name company-name])
        [:div.location display-location]
        (cond-> [:div.card__perks]
          remote (conj [icon "job-remote" :class "job__icon--small"] "Remote")
          (not= role-type "Full time") (conj [icon "profile" :class "job__icon--small"] role-type)
          sponsorship-offered (conj [icon "job-sponsorship" :class "job__icon--small"] "Sponsorship"))
        [:div.salary display-salary]]]
      :job :slug (or slug "")
      :on-click #(dispatch-sync [:wh.job/preset-job-data job])]]))

(defn job-card-tags
  [{:keys [id tags] :as job}]
  (let [skeleton? (and job (empty? (dissoc job :id)))]
    (into [:ul.tags.tags__job]
          (map (fn [tag]
                 [:li {:id (str "job-card__tag-" (str/replace tag #" " "_") "_job-" id)
                       :on-click #(dispatch [:wh.search/search-by-tag tag false])} tag])
               (if skeleton?
                 (map #(apply str (repeat (+ 8 (rand-int 30)) " ")) (range 6))
                 tags)))))

(defn job-card-buttons
  [{:keys [id slug applied company-id state] :as job}]
  [:div.apply
   (when state
     [:div.state
      [:span.applied-state "Status: " (state->candidate-status state)]])
   [:div.buttons
    [:a.button
     {:id       (str "job-card__more-info-button_job-" id)
      :href     (routes/path :job :params {:slug slug})}
     "More Info"]
    (let [user-loaded? (cljs.loader/loaded? :user)]
      (cond
        (and user-loaded?
             (or (<sub [:user/admin?])
                 (<sub [:user/owner? company-id])))
        [:button.button
         {:id (str "job-card__edit-button_job-" id)
          :on-click #(dispatch [:wh.events/nav :edit-job :params {:id id}])} "Edit"]
        applied
        [:button.button {:disabled true} "Applied"]

        :else
        [:button.button
         {:id (str "job-card__apply-button_job-" id)
          :disabled (if user-loaded? (<sub [:user/company?]) false)
          :on-click #(dispatch [:apply/try-apply job :jobcard-apply])}
         (if (and user-loaded? (some? (<sub [:wh.user/applied-jobs])))
           "1-Click Apply"
           "Easy Apply")]))]])

(defn job-card
  [{:keys [tagline tags id applied published score company-id] :as job}
   & {:keys [on-close public]
      :or   {on-close nil public false}}]
  (let [skeleton? (and job (empty? (dissoc job :id)))
        score (or (:user-score job) score)]
    (when job
      (if (<sub [:job-card/show-closed? published])
        [closed-job job public]
        [:div.card.card--job {:class (str "card-border-color-" (rand-int 9)
                                          " i-cur-" (rand-int 9)
                                          (when public
                                            " job-card--public")
                                          (when skeleton?
                                            " job-card--skeleton"))}
         [job-card-header job on-close public]
         [job-card-tags job]
         [:div.tagline [ellipsis tagline]]
         (when score
           [:div.match
            [match-circle score]
            [:span.match__label (str (gstring/format "%.0f%%" (* 100 score)) " match")]])
         (when (<sub [:job-card/show-unpublished? published company-id])
           [:div.card__label.card__label--unpublished.card__label--job
            "Unpublished"])
         [job-card-buttons job]]))))
