(ns wh.logged-in.profile.components
  (:require #?(:cljs [wh.components.forms.views :as f :refer [tags-field]])
            #?(:cljs [wh.logged-in.profile.subs])
            [clojure.string :as str]
            [re-frame.core :refer [dispatch-sync]]
            [wh.common.data :as data]
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
            [wh.profile.db :as profile]
            [wh.re-frame :as r]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.routes :as routes]
            [wh.styles.profile :as styles]
            [wh.util :as util]))

(defn container [& elms]
  (into [:div {:class styles/container}] elms))

(defn meta-row [{:keys [type text icon href new-tab? on-click social-provider]}]
  (let [tag (if (= type :text) :div :a)]
    [tag (cond-> {:class (util/mc styles/meta-row
                                  [(= social-provider :stackoverflow) styles/meta-row--stackoverflow]
                                  [(= social-provider :twitter) styles/meta-row--twitter])}
                 on-click (merge (interop/on-click-fn on-click))
                 href (assoc :href href)
                 new-tab? (merge {:target "_blank"
                                  :rel    "noopener"}))
     [icons/icon icon :class styles/meta-row__icon]
     [:span (when href {:class styles/meta-row__description}) text]]))

(defn social-row [social-provider {:keys [display url] :as social} type]
  (let [public? (= type :public)]
    (if social
      [meta-row {:text     display
                 :icon     (name social-provider)
                 :href     url
                 :new-tab? true}]
      (when (and (not public?) (not (= social-provider :web)))
        [meta-row {:text            (str "Connect " (str/capitalize (name social-provider)))
                   :icon            (name social-provider)
                   :on-click        (interop/save-redirect [:profile])
                   :href            (signin-buttons/type->href social-provider)
                   :social-provider social-provider}]))))

(defn sec-title [text]
  [:div {:class styles/title} text])

(defn small-link [{:keys [text href class on-click inverted? data-test]}]
  [:a
   (merge {:class (util/mc styles/button styles/button--small class
                           [inverted? styles/button--inverted])}
          (when href {:href href})
          (when data-test {:data-test data-test})
          #?(:cljs
             (when on-click
               {:on-click
                (cond (fn? on-click)     on-click
                      (vector? on-click) #(dispatch on-click))})))
   text])

(defn underline-link [{:keys [text href new-tab? data-test]}]
  [:a
   (cond-> {:class styles/underline-link
            :href href
            :data-test data-test}
           new-tab? (merge {:target "_blank" :rel "noopener"}))
   text])

(defn job-link [job]
  [underline-link
   {:text (:title job)
    :href (routes/path :job :params {:slug (:slug job)})}])

(defn company-link [{:keys [company] :as job}]
  [underline-link
   {:text (str (:name company) ", " (-> (:package company)
                                        keyword
                                        data/package-data
                                        :name))
    :href (routes/path :company :params {:slug (:slug company)})}])

(defn application-state [{:keys [state timestamp job] :as _application} user type]
  (let [other? (= type :other)]
    [:div {:class styles/job-application__state-wrapper}
     [:span {:class styles/job-application__applied-on} "Applied " (time/str->human-time timestamp)]
     [:span {:class styles/job-application__state} (profile/state->str state)]
     (when other?
       [:a {:href (routes/path :user
                               :params {:id (:id user)}
                               :query-params {:job-id (:id job)})
            :class styles/admin__secondary-link} "View application"])]))

(defn small-button [{:keys [inverted? data-test] :as opts} text]
  [:button (merge
             (util/smc styles/button styles/button--small
                       [inverted? styles/button--inverted])
             (dissoc opts :inverted?)
             (when data-test {:data-test data-test}))
   text])

(defn upload-button [{:keys [text input-name uploading? on-change data-test inverted?]}]
  (if uploading?
    [small-button {:disabled true :inverted? inverted?} "Uploading..."]
    [:label {:class (util/mc styles/button styles/button--small
                             [inverted? styles/button--inverted])}
     [:input.visually-hidden {:type      "file"
                              :name      input-name
                              :on-change on-change
                              :data-test data-test}]
     [:span text]]))

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
           :data-test "public-info"}
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
                      :type :text
                      :text (->> (time/str->time last-seen :date-time)
                                 time/human-time
                                 (str "Last seen "))}])
         (when updated
           [meta-row {:icon "refresh"
                      :type :text
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
  (let [opts         (first children)
        {:keys [class data-test]
         :or   {class ""}
         :as   opts} (when (map? opts) opts)
        children     (if opts (rest children) children)]
    (into [:div {:class (util/mc styles/section class)
                 :data-test data-test}] children)))

(defn section-custom [_ & _]
  (let [company-cls (util/mc styles/section--highlighted
                             styles/section--admin)]
    (fn [{:keys [type data-test] :as opts} & children]
      (into [:div {:class (util/mc styles/section
                                   [(= type :highlighted) styles/section--highlighted]
                                   [(= type :company) company-cls])
                   :data-test data-test}]
            children))))

(defn section-buttons [& children]
  (into [:div {:class styles/section__buttons}] children))

(defn resource [{:keys [href text data-test download]}]
  [:a (merge {:class    styles/resource
              :href     href
              :target   "_blank"
              :rel      "noopener"
              :download download}
             (when data-test {:data-test data-test}))
   text])

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

(defn job-list [content]
  [:ul {:class styles/job-list}
   content])

(defn subsection [title content]
  [:div {:class styles/subsection}
   [:div {:class styles/subsection__title} title]
   content])

(defn skills-container [tags]
  (into [:div {:class styles/skills}] tags))

(defn meta-separator []
  [:span {:class styles/meta-separator} "•"])

(defn article-card [{:keys [id title formatted-date reading-time upvote-count published editable?] :as article-data}]
  [:div {:class styles/article}
   (when editable? [edit-link {:href (routes/path :contribute-edit :params {:id id})
                               :type :small}])
   [:a {:class styles/article__title
        :href  (routes/path :blog :params {:id id})} title]
   [:div {:class styles/article__meta}
    (when-not published [:<> [:span "not published"]
                         [meta-separator]])
    formatted-date
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
    (nil? p)  "New user"
    (<= p 25) (str "Top " p "% user")
    (<= p 50) (str "Rising star")
    :else     (str "New user")))

(defn percentile->image
  [p]
  (cond
    (nil? p)  "/images/profile/new_user.svg"
    (<= p 50) "/images/profile/top_user.svg"
    :else     "/images/profile/new_user.svg"))

(defn section-stats
  [{:keys [is-owner? percentile created articles-count issues-count]}]
  [section-custom
   {:data-test "section-stats"}
   [sec-title "Activity & Stats"]
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
     [sec-title "Top Skills"]
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
  [{:keys [editable? on-edit read-body edit-body editing? focused? anchor data-test display-toggle?]}]
  [:div (util/smc styles/editable-section
                  [focused? styles/editable-section--editing])
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
     (when (:on-cancel opts)
       [small-link {:text     "Cancel"
                    :on-click (:on-cancel opts)
                    :class    styles/button--inverted}])
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
  (let [public?            (= type :public)
        skills?            (seq skills)
        interests?         (seq interests)
        on-save-fn         #(do (scroll-to-skills)
                                (when on-save (dispatch on-save)))
        on-cancel-fn       #(do (when (or (not changes?) (confirm-save!))
                                  (scroll-to-skills)
                                  (when on-cancel (dispatch on-cancel))))
        candidate?         #?(:cljs (<sub [:wh.logged-in.profile.subs/candidate?])
                              :clj false)
        ;; When candidate edits profile and both skills and interests are empty,
        ;; we make "Skills and interests" section editable by default, to encourage
        ;; candidates to fill it
        editing-by-default (and (not public?) candidate? (not skills?) (not interests?))]
    [editable-section
     {:editable?       (not public?)
      :editing?        (or editing? editing-by-default)
      :focused?        editing?
      :anchor          "skills"
      :on-edit         on-edit
      :display-toggle? true
      :read-body       [:<>
                        [:div (util/smc styles/skills__top)
                         [sec-title "Skills"]]
                        [:div (util/smc styles/skills__content)
                         (if skills?
                           [experience skills (:max-skills opts)]
                           [:p "This person has not selected any skills yet!"])
                         (when interests?
                           [display-interests interests])]]
      :edit-body       [:<>
                        [sec-title "Experience and interests"]
                        [:p (util/smc styles/skills__paragraph)
                         "This is a key part of your profile. List out your skills and experience, and give companies an insight into what else interests you in a role."]
                        [edit-tech
                         (cond->
                           (assoc opts
                                  :on-save on-save-fn
                                  :on-cancel on-cancel-fn)
                           (not editing?) (dissoc :on-cancel))]]}]))

;; articles ----------------------------------------------------------

(defn section-articles [articles type]
  (let [public? (= type :public)
        message (if public? "User hasn't written any articles yet. "
                    "You haven't written any articles yet.")]
    [section
     {:data-test "user-articles"}
     [internal-anchor "articles"]
     [sec-title "Articles"]
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

(defn articles-cta []
  [section (util/smc styles/cta)
   [:div (util/smc styles/cta__content)
    [:h1 (util/smc styles/cta__title)
     "Share your thoughts"]

    [:ul (util/smc styles/cta__list)
     [:li "Share your thoughts & expertise with our community"]
     [:li "The more articles you write, the more people recognize your profile and want to work with you"]]

    [:a {:data-pushy-ignore "true"
         :class             (util/mc styles/button
                                     styles/button--inverted
                                     styles/cta__button--full)
         :href              (routes/path :contribute)}
     "Write an article"]]

   [:div (util/smc styles/cta__image__container--girl
                   styles/cta__image__container)
    [:img {:src   "/images/profile/girl.png"
           :class (util/mc styles/cta__image)}]]])

(defn connect-gh-cta []
  [section (util/smc styles/cta styles/cta__container)
   [:div (util/smc styles/cta__content)
    [:h1 (util/smc styles/cta__title)
     "Connect GitHub account"]

    [:ul (util/smc styles/cta__list)
     [:li "Link to GitHub to show off your productivity"]
     [:li "Help companies to see technologies you are comfortable with"]]

    [:a {:data-pushy-ignore "true"
         :class             (util/mc styles/button
                                     styles/button--github
                                     styles/cta__button--full)
         :href              (routes/path :login :params {:step :github})}
     [icons/icon "github" :class styles/button--github__icon]
     [:span "Connect to GitHub"]]]

   [:div (util/smc styles/cta__image__container--computer-guy
                   styles/cta__image__container)
    [:img {:src   "/images/profile/computer_guy.png"
           :class (util/mc styles/cta__image)}]]])

(defn section-contributions [contributions contributions-count
                             contributions-repos months]
  [section
   [internal-anchor "open-source"]
   [sec-title "Open Source"]

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
   [:div {:class styles/profile-hidden
          :data-test "profile-hidden"}
    [icons/icon "lock" :class styles/profile-hidden__icon]
    "This user's profile is hidden"]])

(defn target-value [ev]
  (-> ev .-target .-value))

(defn text-field [value {:keys [label on-change placeholder class on-enter data-test auto-focus] :as _opts}]
  [:input {:type        "text"
           :value       value
           :class       (util/mc styles/text-field class)
           :placeholder placeholder
           :data-test   data-test
           :autoFocus   auto-focus
           :on-key-down (fn [e]
                          (when (and (= "Enter" (.-key e)) on-enter)
                            (on-enter)))
           :on-change   #(let [new-value (target-value %)]
                           (if (fn? on-change)
                             (on-change new-value)
                             (dispatch-sync (conj on-change new-value))))}])

(defn section-cv [url]
  [section (util/smc styles/cv)
   [sec-title "CV"]
    [:div [:iframe {:src url
              :title "CV"
              :class styles/cv-iframe}]]])
;; --------------------------------------------------------------------------------

(defn itemize [items & {:keys [class no-data-message]}]
  (if (seq items)
    (into [:ul {:class class}]
          (for [item items]
            [:li item]))
    no-data-message))

(defn email-link [email]
  [:a {:href (str "mailto:" email)} email])

(defn owner? [user-type] (= user-type :owner))

(defn edit-user-private-info
  [user-type {:keys [email phone job-seeking-status role-types remote
                     salary visa-status current-location
                     preferred-locations fields title]
              :or   {fields #{:email :phone :status :traits :salary :visa :remote :preferred-types :current-location :preferred-locations}
                     title  "Preferences"}}]
  [:<>
   [sec-title title]
   (when (owner? user-type)
     [:div "This section is for our info only — we won’t show this directly to anyone 🔐"])
   [:div {:data-test :private-info}
    (when (:email fields) [view-field "Email:" [email-link email]])
    (when (:phone fields) [view-field "Phone Number:" (or phone "Not specified")])
    (when (:status fields) [view-field "Status:" (or job-seeking-status "Not specified")])
    (when (:salary fields) [view-field "Expected compensation:" salary])
    (when (:visa fields) [view-field "Visa status:" (or visa-status "Not specified")])
    (when (:remote fields) [view-field "Prefer remote working:" (if remote "Yes" "No")])
    (when (:preferred-types fields) [view-field "Preferred role types:" (itemize
                                                                          (map #(str/replace % #"_" " ") role-types)
                                                                          :no-data-message "None")])
    (when (:current-location fields) [view-field "Current location:" (or current-location "Not specified")])
    (when (:preferred-locations fields) [view-field "Preferred locations:" (itemize preferred-locations
                                                                                    :no-data-message "No locations selected")])]])
