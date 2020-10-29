(ns wh.logged-in.profile.components
  (:require #?(:cljs [wh.components.forms.views :as f :refer [tags-field]])
            [clojure.set :as set]
            [clojure.string :as str]
            [wh.common.keywords :as keywords]
            [wh.common.text :refer [pluralize]]
            [wh.common.time :as time]
            [wh.components.activities.components :as activities-components]
            [wh.components.common :refer [link]]
            [wh.components.icons :as icons]
            [wh.components.signin-buttons :as signin-buttons]
            [wh.components.tag :as tag]
            [wh.interop :as interop]
            [wh.logged-in.profile.components.contributions :as contributions]
            [wh.re-frame :as r]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn container [& elms]
  (into [:div {:class styles/container}] elms))

(defn meta-row [{:keys [text icon href new-tab? on-click social-provider]}]
  [:a (cond-> {:class (util/mc styles/meta-row
                               [(= social-provider :stackoverflow) styles/meta-row--stackoverflow]
                               [(= social-provider :twitter) styles/meta-row--twitter])}
              on-click (merge (interop/on-click-fn on-click))
              href     (assoc :href href)
              new-tab? (merge {:target "_blank"
                               :rel    "noopener"}))
   [icons/icon icon :class styles/meta-row__icon]
   [:span (when href {:class styles/meta-row__description})  text]])

(defn social-row [social-provider {:keys [display url] :as social} type]
  (let [public? (= type :public)]
    (if social
      [meta-row {:text     display
                 :icon     (name social-provider)
                 :href     url
                 :new-tab? true}]
      (when-not public?
        [meta-row {:text            (str "Connect " (str/capitalize (name social-provider)))
                   :icon            (name social-provider)
                   :on-click        (interop/save-redirect [:profile])
                   :href            (signin-buttons/type->href social-provider)
                   :social-provider social-provider}]))))

(defn title [text]
  [:div {:class styles/title} text])

(defn small-link [{:keys [text href class on-click]}]
  [:a
   (merge {:class (util/mc styles/button styles/button--small class)}
          (when href {:href href})
          #?(:cljs
             (when on-click
               {:on-click
                (cond (fn? on-click)     on-click
                      (vector? on-click) #(dispatch on-click))})))
   text])

(defn underline-link [{:keys [text href new-tab?]}]
  [:a
   (cond-> {:class styles/underline-link :href href}
           new-tab? (merge {:target "_blank" :rel "noopener"}))
   text])

(defn small-button [opts text]
  [:button (merge (util/smc styles/button styles/button--small) opts) text])

(defn upload-button [{:keys [document uploading? on-change data-test]}]
  (if uploading?
    [small-button {:disabled true} "Uploading..."]
    [:label {:class     (util/mc styles/button styles/button--small)
             :data-test data-test}
     [:input.visually-hidden {:type      "file"
                              :name      "avatar"
                              :on-change on-change}]
     [:span (str "Upload " document)]]))

(defn edit-link [{:keys [href text data-test type on-click]
                  :or   {text "Edit"}}]
  (let [small? (= type :small)]
    [:a {:href      href
         :data-test data-test
         :class     (util/mc styles/edit [small? styles/edit--small])
         :on-click  on-click}
     [icons/icon "pen" :class (when small? styles/edit__small-icon)] [:span.visually-hidden text]]))

(defn edit-profile [{:keys [type on-click]}]
  (let [[user-route admin-route] (if (= type :private)
                                   [:profile-edit-private :candidate-edit-private]
                                   [:profile-edit-header :candidate-edit-header])
        href                     (if (= (<sub [:wh.pages.core/page]) :profile)
                                   (routes/path user-route)
                                   (routes/path admin-route))]
    [edit-link (cond-> {:data-test (if (= type :private) "edit-profile-private" "edit-profile")
                        :text      "Edit"}
                       (not on-click) (assoc :href href)
                       on-click       (assoc :on-click on-click))]))

(defn profile [user {:keys [github stackoverflow twitter website last-seen updated on-edit display-toggle?]} type]
  (let [public?                          (= type :public)
        {:keys [name image-url summary]} (keywords/strip-ns-from-map-keys user)
        show-meta?                       (->> [github stackoverflow twitter last-seen updated]
                                              (map boolean)
                                              (some true?))]
    [:div {:class (util/mc styles/section styles/section--profile)
           :data-test :public-info}
     (when (and (not public?) display-toggle?)
       [edit-profile {:type     :default
                      :on-click on-edit}])
     [:div {:class styles/username} name]
     [:img {:src   image-url
            :class styles/avatar}]
     (when summary [:div {:class styles/summary
                          :title summary} summary])
     (if show-meta?
       [:<> [:hr {:class styles/separator}]
        [:div {:class styles/meta-rows}
         (when last-seen
           [meta-row {:icon "clock"
                      :text (->> (time/str->time last-seen :date-time)
                                 time/human-time
                                 (str "Last seen "))}])
         (when updated
           [meta-row {:icon "refresh"
                      :text (->> (time/str->time updated :date-time)
                                 time/human-time
                                 (str "Updated "))}])
         [social-row :web website type]
         [social-row :github github type]
         [social-row :stackoverflow stackoverflow type]
         [social-row :twitter twitter type]]]
       ;; add div so it looks good with css grid
       [:div])
     (when-not public?
       [:<>
        [:hr {:class styles/separator}]
        [:a {:data-pushy-ignore "true"
             :class             styles/button
             :href              (routes/path :logout)}
         "Logout"]])]))

(defn view-field
  ([label content]
   (view-field nil label content))
  ([data-test label content]
   [:div {:class     styles/view-field
          :data-test data-test}
    [:label {:class styles/view-field__label} label]
    [:div {:class styles/view-field__content} content]]))

(defn section [& children]
  (into [:div {:class styles/section}] children))

(defn section-highlighted [& children]
  (into [:div (util/smc styles/section styles/section--highlighted)] children))

(defn section-buttons [& children]
  (into [:div {:class styles/section__buttons}] children))

(defn resource [{:keys [href text]}]
  [:a {:class styles/resource :href href :target "_blank" :rel "noopener"} text])

(defn content [& children]
  (into [:div {:class styles/content}] children))

(defn subtitle [text]
  [:div {:class styles/subtitle} text])

(defn top-tech [tag]
  (let [name (str/lower-case (:name tag))]
    [:div {:class styles/top-tech
           :title name}
     [icons/icon (str name "-tag") :class styles/top-tech__icon]
     [:div {:class styles/top-tech__label} name]]))

(defn skills-container [tags]
  (into [:div {:class styles/skills}] tags))

(defn meta-separator []
  [:span {:class styles/meta-separator} "•"])

(defn article-card [{:keys [id title formatted-creation-date reading-time upvote-count published editable?] :as article-data}]
  [:div {:class styles/article}
   (when editable? [edit-link {:href (routes/path :contribute-edit :params {:id id})
                               :type :small}])
   [:a {:class styles/article__title
        :href  (routes/path :blog :params {:id id})} title]
   [:div {:class styles/article__meta}
    (when-not published [:<> [:span "not published"]
                         [meta-separator]])
    formatted-creation-date
    [meta-separator]
    reading-time " min read"
    [meta-separator]
    upvote-count " " (pluralize upvote-count "boost")]])

(defn issue-creator [{:keys [company]}]
  (let [img-src (:logo company)
        name    (:name company)]
    [:div {:class styles/issue__creator}
     [:img {:src   img-src
            :class styles/issue__creator-avatar}]
     [:span {:class styles/issue__creator-name} name]]))

(defn issue-meta-elm [{:keys [icon text]}]
  [:div {:class styles/issue__meta-elm}
   [icons/icon icon :class styles/issue__meta-icon] text])

(defn issue-meta [{:keys [repo compensation level] :as issue}]
  [:div {:class styles/issue__meta}
   [issue-meta-elm {:icon (str "issue-level-" (name level))
                    :text (-> level name str/capitalize)}]
   [:div {:class styles/issue__additional-meta}
    [activities-components/compensation-amount compensation]
    [activities-components/primary-language repo]]])

(defn issue-card [{:keys [id title company] :as issue}]
  [:div {:class styles/issue}
   [issue-creator {:company company}]
   [:a {:class styles/issue__title
        :href  (routes/path :issue :params {:id id})} title]
   [issue-meta issue]])

(defn internal-anchor
  [id]
  [:div {:class styles/anchor
         :id    id}])

(defn internal-link
  [id label]
  [:a
   #?(:clj  {:href (str "#" id)}
      :cljs {:on-click #(.scrollIntoView (.getElementById js/document id))})
   label])

;; stats -----------------------------------------------------------

(defn four-grid
  [& items]
  [:div (util/smc styles/four-grid)
   (for [item items]
     item)])

(defn stat-container
  [{:keys [title text image key]}]
  [:div {:class (util/mc styles/grid-cell styles/stat-container)
         :key   key}
   [:div (util/smc styles/stat-container__title) title]
   [:div (util/smc styles/stat-container__text) text]
   [:img {:class styles/stat-container__image
          :src   image}]])

(defn percentile->title
  [p]
  (cond
    (<= p 25) (str "Top " p "% user")
    (<= p 50) (str "Rising star")
    :else     (str "New user")))

(defn percentile->image
  [p]
  (cond
    (<= p 50) "/images/profile/top_user.svg"
    :else     "/images/profile/new_user.svg"))

(defn section-stats
  [{:keys [is-owner? percentile created articles-count issues-count]}]
  [section
   [title "Activity & Stats"]
   [four-grid
    [stat-container
     {:title "WorksHub Rating"
      :text  (percentile->title percentile)
      :image (percentile->image percentile)
      :key   :rating}]
    [stat-container
     {:title "Member Since"
      :text  (time/month+year (time/str->time created :date-time))
      :image "/images/profile/member_since.svg"
      :key   :created}]
    [stat-container
     {:title "Author Of"
      :text  (if (pos-int? articles-count)
               [internal-link "articles" (str articles-count (pluralize articles-count " article"))]
               (if is-owner?
                 [link "Write an article" :contribute :class styles/underline-link]
                 "No articles yet"))
      :image "/images/profile/articles.svg"
      :key   :articles}]
    [stat-container
     {:title "Open Source Issues"
      :text  (if (pos-int? issues-count)
               [internal-link "issues" (str issues-count (pluralize issues-count " contribution"))]
               (if is-owner?
                 [link "View Open Source Issues" :issues :class styles/underline-link]
                 "No contributions yet"))
      :image "/images/profile/contributions.svg"
      :key   :issues}]]])

;; skills ----------------------------------------------------------

(defn add-skills-cta []
  [:<>
   [:p "You haven't specified your skills yet"]
   [section-buttons
    [small-link {:href (if (= (<sub [:wh.pages.core/page]) :profile)
                         (routes/path :profile-edit-header)
                         (routes/path :candidate-edit-header))
                 :text "Specify skills"}]]])

(defn display-skills [user-skills]
  (let [top-skill    (take 5 user-skills)
        other-skills (drop 5 user-skills)]
    [:<>
     [skills-container (for [skill top-skill]
                         [top-tech skill])]
     (when (seq other-skills)
       [:<>
        [subtitle "also likes to work with"]
        [tag/strs->tag-list :li (map :name other-skills) nil]])]))

(defn section-skills-old [user-skills type]
  (let [public? (= type :public)
        skills? (seq user-skills)]
    [section
     (when (and skills? (not public?))
       [edit-profile {:type :default}])
     [title "Top Skills"]
     (cond
       skills?                     [display-skills user-skills]
       (and (not skills?) public?) [:p "User hasn't specified his skills yet. "]
       :else                       [add-skills-cta])]))

(defn edit-button
  [{:keys [editing? on-edit-event data-test]}]
  [:button
   {:class styles/edit-button
    :data-test data-test
    :on-click (fn [_]
                (when on-edit-event
                  (dispatch on-edit-event)))}
   [icons/icon "pen"
    :class (util/mc styles/edit-button__icon [editing? styles/edit-button__icon--editing])]])

(defn editable-section
  [{:keys [editable? on-edit read-body edit-body editing? anchor data-test display-toggle?]}]
  [:div (util/smc styles/editable-section [editing? styles/editable-section--editing])
   [section
    (when anchor
      [internal-anchor anchor])
    (if editing?
      edit-body
      read-body)
    (when (and editable? (not editing?) display-toggle?)
      [edit-button {:editing? editing?
                    :on-edit-event on-edit
                    :data-test data-test}])]])

(defn rating->percentage
  ([r]
   (rating->percentage r 12))
  ([r n]
   (int (* 100 (/ (inc (or r 0)) n)))))

(defn experience-text
  [label rating]
  (str label " - "
       (cond
         (or (nil? rating) (zero? rating))  "less than a year"
         (> rating 10)  "10+ years"
         :else          (str rating (pluralize rating " year")))))

(defn experience-bar
  [label rating]
  (let [width (rating->percentage rating)]
    [:div (util/smc styles/experience-bar)
     [:div {:class (util/mc styles/experience-bar__bg)
            :style #?(:cljs {:width (str width "%")}
                      :clj (str "width: " width "%"))}
      [:div (util/smc styles/experience-bar__text-inner)
       [experience-text label rating]]]]))

(defn experience-slider
  [label rating on-change-event]
  [:div (util/smc styles/experience-slider)
   [:div (util/smc styles/experience-slider__text)
    [experience-text label rating]]
   [:input {:type      "range"
            :min       0
            :max       11
            :step      1
            :value     rating
            :class     styles/experience-slider__input
            :on-change #(dispatch (conj on-change-event (.. % -target -value)))}]])

(defn experience
  [skills max-skills]
  [:div (util/smc styles/experiences)
   [:div (util/smc styles/experiences__graph)
    (for [skill (take max-skills skills)]
      (let [n (get-in skill [:tag :label])]
        [:div {:class styles/experience
               :key   (get-in skill [:tag :id])}
         [:div (util/smc styles/experience__visual)
          (if (contains? tag/labels-with-icons (str/lower-case n))
            [icons/icon (str (str/lower-case n) "-tag") :class styles/experience__icon]
            [:div (util/smc styles/experience__initial) (first (str/upper-case n))])]
         [experience-bar n (:rating skill)]]))]
   (when-let [excess-skills (seq (drop max-skills skills))]
     [:div (util/smc styles/experiences__excess)
      [subtitle "Also has experience with"]
      [:ul
       [tag/tag-list :li (map :tag excess-skills)]]])])

(defn skills-ratings
  [{:keys [class skills on-change]}]
  [:div (util/smc styles/experiences__graph styles/skills-ratings class)
   (for [skill skills]
     (let [n (:label skill)]
       [:div {:class styles/experience
              :key   (:id skill)}
        [:div (util/smc styles/experience__visual)
         (if (contains? tag/labels-with-icons (str/lower-case n))
           [icons/icon (str (str/lower-case n) "-tag") :class styles/experience__icon]
           [:div (util/smc styles/experience__initial) (first (str/upper-case n))])]
        [experience-slider n (:rating skill) (conj on-change (:id skill))]]))])

(defn edit-tech
  [opts]
  [:div (util/smc styles/edit-tech)
   [:div (util/smc styles/edit-tech__number) 1]
   [:div (util/smc styles/edit-tech__title) "Experience"]
   [:div (util/smc styles/edit-tech__description styles/edit-tech__offset)
    (str "Add up to "
         (:max-skills opts)
         " skills that you have the most experience with. What do you work with the most? What are you an expert in? Add your years of experience using the slider.")]
   [:div (util/smc styles/edit-tech__offset 'wh-formx)
    #?(:cljs [tags-field
              (:search-term (:skills-search opts))
              (:skills-search opts)])]
   [skills-ratings {:class     styles/edit-tech__offset
                    :skills    (filter :selected (:tags (:skills-search opts)))
                    :on-change (:on-skill-rating-change opts)}]
   ;;
   [:div (util/smc styles/edit-tech__number) 2]
   [:div (util/smc styles/edit-tech__title) "Interests"]
   [:div (util/smc styles/edit-tech__description styles/edit-tech__offset)
    "Tell potential employers about your other interests. What is it you’re looking for in your next role? What kinds of tech would you like to work with? What skills are you currently learning? Search and add them here."]
   [:div (util/smc styles/edit-tech__offset 'wh-formx)
    #?(:cljs [tags-field
              (:search-term (:interests-search opts))
              (:interests-search opts)])]
   [:div (util/smc styles/edit-tech__offset styles/edit-tech__buttons)
    [section-buttons
     [small-link {:text     "Cancel"
                  :on-click (:on-cancel opts)
                  :class    styles/button--inverted}]
     [small-link {:text     "Save"
                  :on-click (:on-save opts)}]]]])

(defn display-interests
  [interests]
  [:div (util/smc styles/interests)
   [subtitle "Current Interests"]
   [:div (util/smc styles/interests__text)
    "These are specific technologies that this person is interested right now."]
   [tag/tag-list :ul interests]])

(defn scroll-to-skills
  []
  #?(:cljs (.scrollIntoView (.getElementById js/document "skills"))))

(defn confirm-save!
  []
  #?(:cljs (js/confirm "Are you sure you want to discard unsaved changes?")
     :clj  false))

(defn section-skills [{:keys [skills interests type on-edit on-cancel on-save editing? changes?] :as opts}]
  (let [public?      (= type :public)
        skills?      (seq skills)
        interests?   (seq interests)
        on-save-fn   #(do (scroll-to-skills)
                          (when on-save (dispatch on-save)))
        on-cancel-fn #(do (when (or (not changes?) (confirm-save!))
                            (scroll-to-skills)
                            (when on-cancel (dispatch on-cancel))))]
    [editable-section
     {:editable? (not public?)
      :editing?  editing?
      :anchor    "skills"
      :on-edit   on-edit
      :display-toggle? true
      :read-body [:<>
                  [:div (util/smc styles/skills__top)
                   [title "Skills"]]
                  [:div (util/smc styles/skills__content)
                   (if skills?
                     [experience skills (:max-skills opts)]
                     [:p "This person has not selected any skills yet!"])
                   (when interests?
                     [display-interests interests])]]
      :edit-body [:<>
                  [title "Experience and interests"]
                  [:p (util/smc styles/skills__paragraph)
                   "This is a key part of your profile. List out your skills and experience, and give companies an insight into what else interests you in a role."]
                  [edit-tech (assoc opts
                                    :on-cancel on-cancel-fn
                                    :on-save on-save-fn)]]}]))

;; articles ----------------------------------------------------------

(defn section-articles [articles type]
  (let [public? (= type :public)
        message (if public? "User hasn't written any articles yet. "
                            "You haven't written any articles yet.")]
    [section
     [internal-anchor "articles"]
     [title "Articles"]
     (if (seq articles)
       (for [article articles]
         ^{:key (:id article)}
         [article-card (assoc article :editable? (not public?))])
       [:p message
        (when public?
          [underline-link
           {:text "Browse all articles" :href (routes/path :learn)}])])
     (when-not public?
       [section-buttons
        [small-link {:text "Write article"
                     :href (routes/path :contribute)}]])]))

;; issues ----------------------------------------------------------

(defn section-issues [issues type]
  (let [public? (= type :public)
        message (if public? "User hasn't started working on any issue yet. "
                            "You haven't started working on any issue yet")]
    [section
     [internal-anchor "issues"]
     [title "Open Source Issues"]
     (if (seq issues)
       (for [issue issues]
         ^{:key (:id issue)}
         [issue-card issue])
       [:p message
        (when public? [underline-link
                       {:text "Browse all issues" :href (routes/path :issues)}])])
     (when-not public?
       [section-buttons
        [small-link {:text "Explore issues"
                     :href (routes/path :issues)}]])]))

(defn section-contributions [contributions contributions-count
                             contributions-repos months]
  [section
   [internal-anchor "open-source"]
   [title "Open Source"]

   [four-grid
    ^{:key :contributions-count}
    [stat-container
     {:title "GitHub contributions"
      :text  (str contributions-count " commits")
      :image "/images/profile/commits.svg"
      :key   :rating}]

    ^{:key :repos-count}
    [stat-container
     {:title "Over a total of"
      :text  (str contributions-repos " repositories")
      :image "/images/profile/repositories.svg"
      :key   :created}]

    ^{:key :contributions-grid}
    [contributions/contributions-grid contributions months]]])

(defn profile-hidden-message []
  [section
   [:div {:class styles/profile-hidden}
    [icons/icon "lock" :class styles/profile-hidden__icon]
    "This user's profile is hidden"]])
