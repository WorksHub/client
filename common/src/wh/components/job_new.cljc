(ns wh.components.job-new
  (:require #?(:cljs [goog.string :as gstring])
            [clojure.string :as str]
            [wh.common.job :as jobc]
            [wh.common.specs.company :as company]
            [wh.common.subs]
            [wh.common.time :as time]
            [wh.components.cards :refer [draw-shape]]
            [wh.components.common :refer [wrap-img img]]
            [wh.components.icons :refer [icon]]
            [wh.components.tag :as tag]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.slug :as slug]
            [wh.styles.job-card :as styles]
            [wh.util :as util]))


(defn- tag [tag]
  (let [vertical-tags (<sub [:wh/vertical-tags-ids])
        add-nofollow? (not (contains? vertical-tags (:id tag)))
        href          (routes/path :pre-set-search :params {:tag (slug/slug (:slug tag))})
        tag           (cond-> (-> tag
                                  (assoc :href href)
                                  (update :label str/lower-case))
                              add-nofollow? (assoc :rel "nofollow"))]
    [tag/tag :a tag]))

(defn- tags [tags]
  [:ul (util/smc styles/tags "tags")
   (->> tags
        (filter identity)
        (map-indexed
          (fn [idx {:keys [id] :as tag-data}]
            ^{:key (or id idx)}
            [tag tag-data])))])

(defn- buttons [{:keys [slug id published] :as _job}
                {:keys [user-is-owner? user-is-company? applied?
                        logged-in? apply-source skeleton?]}]
  [:div (util/smc styles/buttons)
   [:a {:href      (when-not skeleton? (routes/path :job :params {:slug slug}))
        :class     (util/mc styles/button
                            styles/button--inverted-highlighted)
        :data-test (when-not skeleton? "job-info")}
    (if user-is-owner? "View" "More Info")]

   (cond
     skeleton?
     [:a (util/smc styles/button) ""]

     (and (not user-is-owner?) published)
     (let [job-page-path [:job
                          :params {:slug slug}
                          :query-params {"apply" true
                                         "apply_source" apply-source
                                         "interaction" 1}]
           disabled?     (or applied?
                             (and user-is-company?
                                  (not user-is-owner?)))]
       [:a (merge
            {:id        (str "job-card__apply-button_job-" id)
             :class     (util/mc styles/button
                                 [disabled? styles/button--disabled])
             :data-test "job-apply"}
            (when disabled?
              {:disabled true})
            {:href (apply routes/path job-page-path)})

        (if applied? "Applied" "Apply")])

     user-is-owner?
     [:a {:href  (routes/path :edit-job :params {:id id})
          :class styles/button}
      "Edit"])])


(defn- save [{:keys [id] :as job} logged-in? skeleton? liked? on-save]
  [:button (merge (util/smc styles/save)
                  on-save
                  {:data-test "job-save"})
   (when-not skeleton?
     [icon "save"
      :id (str "job-card__like-button_job-" id)
      :class (util/mc styles/save__icon
                      styles/save__icon--save
                      [liked? styles/save__icon--selected])
      :on-click (when logged-in?
                  #(dispatch [:wh.events/toggle-job-like job]))])])


(defn- match [score]
  (let [percentage     (* score 100)
        percentage-fmt #?(:cljs (gstring/format "%d%%" percentage)
                          :clj (format "%s%%" (int percentage)))
        size           16]
    [:div
     (util/smc styles/perks__item
               styles/match)
     (if (= score 1.0)
       [:div (util/smc styles/match__circle)
        [:div (util/smc styles/match__foreground)]]

       [:div (util/smc styles/match__circle)
        [:svg (util/smc styles/match__circle__value)
         [:path.circle {:d (draw-shape score size)}]]
        [:div (util/smc styles/match__background)]])

     [:div (str percentage-fmt " Match")]]))


(defn- unpublished []
  [:div {:class     (util/mc styles/label--unpublished)
         :data-test "job-unpublished"}
   "Unpublished"])

(defn- perks [remote sponsorship-offered score unpublished?]
  (let [container     [:div (util/smc styles/perks)]
        remote-job    [:div (util/smc styles/perks__item)
                       [icon "globe"
                        :class (util/mc styles/perks__icon
                                        styles/perks__icon--remote)]
                       [:span (util/smc styles/perks__item__name) "Remote"]]
        sponsored-job [:div (util/smc styles/perks__item)
                       [icon "award"
                        :class (util/mc styles/perks__icon
                                        styles/perks__icon--sponsorship)]
                       [:span (util/smc styles/perks__item__name) "Sponsored"]]]

    (cond-> container
            score               (conj [match score])
            remote              (conj remote-job)
            sponsorship-offered (conj sponsored-job)
            unpublished?        (conj [unpublished]))))


(defn- header
  [{:keys [_id slug title display-location] :as _job}
   {logo :logo company-name :name}
   {:keys [skeleton?]}]

  [:a {:class (util/mc styles/header)
       :href  (when-not skeleton? (routes/path :job :params {:slug slug}))}
   [:div {:class     (util/mc styles/title)
          :data-test "job-title"}
    title]

   [:div {:class     (util/mc styles/company__name)
          :data-test "company-name"}
    (str/join ", " (remove nil? [company-name display-location]))]

   [:div {:class     (util/mc styles/company__logo)
          :data-test "company-logo"}
    (if (or skeleton? (not logo))
      [:div]
      (wrap-img img logo {:alt (str company-name " logo") :w 40 :h 40 :fit "clip"}))]])


(defn- details
  [{:keys [salary last-modified role-type] :as _job}
   {:keys [size] :as _company}]
  [:div (util/smc styles/details)
   (when salary
     [:div {:class     (util/mc styles/salary styles/details__item)
            :data-test "job-salary"}
      salary])

   (when role-type
     [:div (util/smc styles/details__item)
      [icon "clock" :class (util/mc styles/details__icon
                                    styles/details__icon--clock)]
      role-type])

   (when (string? last-modified)
     [:div (util/smc styles/details__item)
      [icon "calendar" :class (util/mc styles/details__icon
                                       styles/details__icon--calendar)]
      (time/str->human-time last-modified)])

   (when size
     [:div (util/smc styles/details__item)
      [icon "couple" :class (util/mc styles/details__icon
                                     styles/details__icon--couple)]
      (str (company/size->range (keyword size)) " Staff")])])


(defn- skeleton-tags []
  (map (fn [i]
         {:label   (apply str (repeat (+ 8 (rand-int 30)) " "))
          :key     i
          :type    :tech
          :subtype :software
          :slug    ""})
       (range 6)))

(defn- tagline [value]
  [:div (util/smc styles/tagline) value])


(defn job-card
  [{:keys [company published score user-score applied liked
           display-salary remuneration remote sponsorship-offered]
    :as   job}
   {:keys [liked? applied? user-is-owner? logged-in? view-type on-save]
    :or   {liked?         (or liked false)   ;; old style job handlers added 'liked' bool to the job itself
           applied?       (or applied false) ;; old style job handlers added 'applied' bool to the job itself
           user-is-owner? false}
    :as   opts}]
  (let [skeleton?    (and job (empty? (dissoc job :id :slug)))
        salary       (or display-salary (jobc/format-job-remuneration remuneration))
        score        (or user-score score)
        opts         (merge opts
                            {:skeleton? skeleton?
                             :liked?    liked?
                             :applied?  applied?})
        unpublished? (and (not published) user-is-owner?)
        show-perks?  (or sponsorship-offered score remote unpublished?)]
    [:div {:class     (util/mc styles/card
                               [show-perks? styles/card--with-perks]
                               [skeleton? styles/card--skeleton]
                               [(= view-type :cards) styles/card--block]
                               [(= view-type :list) styles/card--list])
           :data-test (when-not skeleton? "job-card")}
     [header job company opts]

     [details (assoc job :salary salary) company]

     [tags (if skeleton? (skeleton-tags) (:tags job))]

     [tagline (:tagline job)]

     [buttons job opts]

     [perks remote sponsorship-offered score unpublished?]

     [save job logged-in? skeleton? liked? on-save]]))
