(ns wh.register.events
  (:require
    [cljs-time.coerce :as time-coerce]
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch path reg-fx]]
    [wh.common.graphql-queries :as graphql]
    [wh.common.specs.primitives :as primitives]
    [wh.db :as db]
    [wh.login.db :as login]
    [wh.pages.core :refer [on-page-load]]
    [wh.register.db :as register]
    [wh.user.db :as user]
    [wh.util :as util]))

(def register-interceptors (into db/default-interceptors
                                 [(path ::register/sub-db)]))

;; In non-GitHub (non-interrupted) flow, the events triggered will be
;; no-ops. But when we arrive back here from /github-callback,
;; wh.user.db will be initialized, and we need to fill our fields
;; with values from there.
(defmethod on-page-load :register [db]
  (into [[:register/update-data-from-user]]
        (case (get-in db [::db/page-params :step])
          :skills [[::ip-location-and-riddles]
                   [:wh.pages.core/enable-no-scroll]]
          :name [[::fetch-name]]
          :verify [(when-not (str/blank? (get-in db [::user/sub-db ::user/id]))
                     [::upsert-user])]
          [])))

(reg-event-db
  ::initialize-db
  db/default-interceptors
  (fn [db [force]]
    (if (and (not force) (::register/sub-db db))
      db ;; already initialized
      (assoc db
             :wh.register.db/sub-db
             (register/default-db (::db/default-technologies db))))))

(reg-event-fx
  :register/advance
  db/default-interceptors
  (fn [{db :db} [_]]
    (let [next-step (-> db register/effective-step register/next-step)]
      (if (or (= next-step :finish)
              (and (register/step<= :verify next-step) (user/approved? db)))
        {:dispatch-n (login/redirect-post-login-or-registration db)}
        {:navigate [:register :params {:step next-step}]
         :db       (assoc-in db [::register/sub-db ::register/step] next-step)}))))

(defn location-field [type]
  (if (= type :preferred) ::register/preferred-location ::register/current-location))

(defn location-query-field [type]
  (if (= type :preferred) ::register/preferred-location-query ::register/current-location-query))

(defn location-search-error-field [type]
  (if (= type :preferred) ::register/preferred-location-search-error ::register/current-location-search-error))

(defn location-search-results-field [type]
  (if (= type :preferred) ::register/preferred-location-search-results ::register/current-location-search-results))

(reg-event-fx
 ::set-location-query
 register-interceptors
 (fn [{db :db} [type query]]
   {:dispatch [::search-location type]
    :db (assoc db (location-query-field type) query)}))

(reg-event-fx
 ::set-location
 register-interceptors
 (fn [{db :db} [type location]]
   {:dispatch (if (= type :preferred)
                [:register/select-preferred-location]
                [:register/advance])
    :db (assoc db (location-field type) location)}))

(reg-event-db
  :register/toggle-prefer-remote
  register-interceptors
  (fn [db _]
    (let [remote (not (::register/remote db))]
      (cond-> db
        true (assoc ::register/remote remote)
        remote (assoc ::register/location-stage (if (::register/preferred-location db) :confirm-current-location :ask-for-current-location)
                      ::register/skip-ask-for-preferred-location true)))))

(reg-event-db
  :register/select-preferred-location
  register-interceptors
  (fn [db _]
    (let [location (::register/preferred-location db)]
      (assoc db
             ::register/preferred-location-query (str (::register/city location) ", " (::register/country location))
             ::register/preferred-location-search-results []
             ::register/location-stage :confirm-current-location))))

(reg-event-db
 ::set-name
 register-interceptors
 (fn [db [name]]
   (assoc db ::register/name name)))

(reg-event-db
  :register/ask-for-preferred-location
  register-interceptors
  (fn [db _]
    (assoc db
           ::register/location-stage :ask-for-preferred-location
           ::register/skip-ask-for-preferred-location false)))

(reg-event-db
  :register/ask-for-current-location
  register-interceptors
  (fn [db _]
    (assoc db ::register/location-stage :ask-for-current-location)))

(reg-event-fx
  :register/confirm-current-location
  register-interceptors
  (fn [{db :db} _]
    {:db (assoc db
                ::register/current-location
                (::register/preferred-location db))
     :dispatch [:register/advance]}))

(def location-and-riddles-query
  {:venia/queries [[:location [:city :administrative :country :countryCode :subRegion
                               :region :longitude :latitude]]
                   [:code_riddles [:language :riddle]]]})

(reg-event-fx
  ::ip-location-and-riddles
  (fn [{db :db} _]
    {:graphql {:query      location-and-riddles-query
               :on-success [::ip-location-and-riddles-response]
               :on-failure [::ip-location-and-riddles-bad-response]}}))

(defn parse-riddles [riddles selected-skills]
  (let [all-riddles (mapv (fn [{:keys [language riddle]}]
                            {::register/language language
                             ::register/riddle   riddle}) riddles)
        languages (map ::register/language all-riddles)
        selected-language (or (->> (set/intersection (set languages) (set selected-skills))
                                   first)
                              (-> all-riddles first ::register/language))
        selected-riddle (first (filter #(= selected-language (::register/language %)) all-riddles))]
    {:all-riddles     all-riddles
     :selected-riddle selected-riddle}))

(defn parse-location
  [{:keys [city administrative country countryCode subRegion region longitude latitude]}]
  (cond-> {::register/city         city
           ::register/country      country
           ::register/country-code countryCode}
          administrative (assoc ::register/administrative administrative)
          subRegion (assoc ::register/sub-region subRegion)
          region (assoc ::register/region region)
          longitude (assoc ::register/longitude longitude)
          latitude (assoc ::register/latitude latitude)))

(reg-event-db
  ::ip-location-and-riddles-response
  register-interceptors
  (fn [db [{{:keys [location code_riddles]} :data :as data}]]
    (let [{:keys [all-riddles selected-riddle]} (parse-riddles code_riddles (::register/selected-skills db))
          db (assoc db
                    ::register/code-riddles all-riddles
                    ::register/selected-riddle selected-riddle)
          location (when location (parse-location location))]
      (if-not location
        (assoc db ::register/location-stage :ask-for-preferred-location)
        (assoc db
               ::register/location-stage :confirm-preferred-location
               ::register/preset-location location)))))

(reg-event-db
  ::ip-location-and-riddles-bad-response
  register-interceptors
  (fn [db [resp]]
    (js/console.error "Location auto-detection and riddles failed" resp)
    (assoc db ::register/manual-location? true
              ::register/code-riddle-error true)))

(reg-event-fx
  ::search-location
  register-interceptors
  (fn [{db :db} [type retry-num]]
    {:algolia {:index      :places
               :retry-num  retry-num
               :params     {:query       (get db (location-query-field type))
                            :type        "city"
                            :language    "en"
                            :hitsPerPage 5}
               :on-success [:process-search-location type]
               :on-failure [:search-location-bad-response type retry-num]}}))

(defn- algolia->location
  [{:keys [administrative country country_code locale_names _geoloc]}]
  (cond-> {::register/city         (first locale_names)
           ::register/country      country
           ::register/country-code (str/upper-case country_code)}
    (seq administrative) (assoc ::register/administrative (first administrative))
    (seq _geoloc) (assoc ::register/longitude (:lng _geoloc)
                         ::register/latitude (:lat _geoloc))))

(reg-event-db
  :process-search-location
  register-interceptors
  (fn [db [type {hits :hits}]]
    (let [locations (mapv algolia->location hits)]
      (assoc db
             (location-search-error-field type) nil
             (location-search-results-field type) locations))))

(reg-event-fx
  :search-location-bad-response
  register-interceptors
  (fn [{db :db} [type retry-attempt result]]
    (js/console.error "Retry attempt:" retry-attempt)
    (js/console.error "Location search failed:" result)
    (if (and retry-attempt (> retry-attempt 2))
      {:db (assoc db (location-search-error-field type) "Error searching location, please try again later")}
      (let [attempt (if-not retry-attempt 1 (inc retry-attempt))]
        {:dispatch [::search-location type attempt]}))))

(def preverify-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "preverify_email"}
   :venia/variables [{:variable/name "email"
                      :variable/type :String!}]
   :venia/queries   [[:preverify_email {:email :$email}]]})

(reg-event-fx
  :register/preverify-email
  register-interceptors
  (fn [{db :db} [email]]
    {:db      (assoc db ::register/loading? true)
     :graphql {:query      preverify-mutation
               :variables  {:email email}
               :on-success [::preverify-email-success]
               :on-failure [::preverify-email-failure]}}))

(reg-event-fx
  ::preverify-email-success
  register-interceptors
  (fn [{db :db} [{:keys [data]}]]
    {:dispatch [:register/advance]
     :db (assoc db
                ::register/loading? false
                ::register/show-verify? (not (:preverify_email data)))}))

(reg-event-fx
  ::preverify-email-failure
  db/default-interceptors
  (fn [{db :db} [resp]]
    (let [error (util/gql-errors->error-key resp)]
      (if (and (= error :duplicate-user) (db/logged-in? db))
        {:db (assoc-in db [::register/sub-db ::register/loading?] false)
         :dispatch [:register/advance]} ; we've just hit github, advance anyway
        {:db (-> db
                 (assoc-in [::register/sub-db ::register/loading?] false)
                 (assoc-in [::register/sub-db ::register/upsert-user-errors] error))}))))

(reg-event-fx
  :register/proceed-from-email
  register-interceptors
  (fn [{{email ::register/email consented ::register/consented :as db} :db} _]
    (cond
      (not consented) {:db (assoc db ::register/upsert-user-errors :missing-consent)}
      (not (s/valid? ::primitives/email email)) {:db (assoc db ::register/upsert-user-errors :invalid-arguments)}
      :otherwise {:db (assoc db ::register/upsert-user-errors nil)
                  :dispatch [:register/preverify-email email]})))

(def skill-cap
  "Maximum number of skills that we pick from Github."
  8)

;; This gets called on every step. It fills in data from ::user/sub-db
;; if we have it but the corresponding field in ::register/sub-db is
;; empty. As a result, it's idempotent, and can be safely called anywhere
;; github-callback redirects us to.
(reg-event-db
  :register/update-data-from-user
  db/default-interceptors
  (fn [db _]
    (let [{:keys [::user/github-info ::user/email ::user/name ::user/id]} (::user/sub-db db)
          skills (take skill-cap (map :name (:skills github-info)))
          name (if (str/blank? name) (:name github-info) name)
          old-register (::register/sub-db db)]
      (update db ::register/sub-db merge
              (cond-> {}
                (empty? (::register/selected-skills old-register))
                (assoc ::register/available-skills (vec (distinct (into (::register/available-skills old-register) skills)))
                       ::register/selected-skills  (set skills))
                (and email (str/blank? (::register/email old-register))) (assoc ::register/email email)
                (and name (str/blank? (::register/name old-register))) (assoc ::register/name name ::register/preset-name? (not (str/blank? name)))
                (and id (str/blank? (::register/id old-register))) (assoc ::register/id id))))))

(defn toggle-bubble
  [db k item level]
  (update db k (if (= level :base) disj conj) item))

(reg-event-db
  ::select-skill
  register-interceptors
  (fn [db [skill level]]
    (toggle-bubble db ::register/selected-skills skill level)))

(reg-event-db
  ::show-add-skill
  register-interceptors
  (fn [db _]
    (assoc db ::register/add-skill-visible? true)))

(reg-event-db
  ::hide-add-skill
  register-interceptors
  (fn [db _]
    (assoc db ::register/add-skill-visible? false)))

(reg-event-db
  ::set-new-skill
  register-interceptors
  (fn [db [new-skill]]
    (assoc db ::register/new-skill new-skill)))

(reg-event-fx
  ::pick-suggested-skill
  register-interceptors
  (fn [db [new-skill]]
    {:dispatch-n [[::set-new-skill new-skill]
                  [::add-skill]]}))

(reg-event-db
  ::add-skill
  register-interceptors
  (fn [db _]
    (-> db
        (update ::register/available-skills conj (::register/new-skill db))
        (update ::register/selected-skills conj (::register/new-skill db))
        (dissoc ::register/new-skill)
        (assoc ::register/add-skill-visible? false))))

(reg-event-db
  ::set-code-answer
  register-interceptors
  (fn [db [code-answer]]
    (assoc db ::register/code-answer code-answer)))

(def check-riddle-query
  {:venia/operation {:operation/type :query
                     :operation/name "verify_code_riddle"}
   :venia/variables [{:variable/name "language"
                      :variable/type :String!}
                     {:variable/name "answer"
                      :variable/type :String!}]
   :venia/queries   [[:verify_code_riddle {:language :$language
                                           :answer   :$answer}
                      [:validSolution]]]})

(def name-query
  {:venia/operation {:operation/type :query
                     :operation/name "clearbit_name"}
   :venia/queries [[:clearbit_name {:email :$email}]]
   :venia/variables [{:variable/name "email"
                      :variable/type :String!}]})

(reg-event-fx
  ::fetch-name
  register-interceptors
  (fn [{db :db} _]
    {:graphql {:query      name-query
               :variables  {:email (::register/email db)}
               :on-success [::set-name-from-server]}}))

(reg-event-db
  ::set-name-from-server
  register-interceptors
  (fn [db [{{name :clearbit_name} :data}]]
    (cond-> db
      (and name (str/blank? (::register/name db)))
      (assoc ::register/name name ::register/preset-name? (not (str/blank? name))))))

(reg-event-fx
  ::check-code-riddle
  register-interceptors
  (fn [{db :db} _]
    {:graphql {:query      check-riddle-query
               :variables  {:language (get-in db [::register/selected-riddle ::register/language])
                            :answer (::register/code-answer db)}
               :method     :post
               :on-success [::check-riddle-response]
               :on-failure [::riddle-error-handler]}}))

(reg-event-fx
  ::check-riddle-response
  db/default-interceptors
  (fn [{db :db} [{{{valid-solution :validSolution} :verify_code_riddle} :data}]]
    (cond
      valid-solution {:dispatch [::upsert-user]
                      :db       (-> db
                                    (assoc-in [::register/sub-db ::register/failed-code-riddle-check?] (not valid-solution))
                                    (assoc-in [::user/sub-db ::user/approval :status] "approved"))}
      (get-in db [::register/sub-db ::register/failed-code-riddle-check?]) {:db (assoc-in db [::register/sub-db ::register/approval-fail?] true)}
      :else {:db (assoc-in db [::register/sub-db ::register/failed-code-riddle-check?] true)})))

(reg-event-db
  ::change-riddle
  register-interceptors
  (fn [db [language]]
    (let [selected-riddle (->> (::register/code-riddles db)
                               (filter #(= language (::register/language %)))
                               first)]
      (if selected-riddle
        (assoc db ::register/selected-riddle selected-riddle)
        db))))

(reg-event-db
  ::riddle-error-handler
  register-interceptors
  (fn [db [error]]
    (js/console.error "Error processing code riddle:" error)
    (assoc db ::register/code-riddle-error true)))

(reg-event-db
  ::set-email
  register-interceptors
  (fn [db [email]]
    (assoc db ::register/email email)))

(def upsert-user-fields
  [:id [:approval [:status]] :email :name :consented
   [:skills [:name]]
   [:preferredLocations [:city :administrative :country :countryCode :subRegion :region :longitude :latitude]]])

(def create-user-mutation {:venia/operation {:operation/type :mutation
                                             :operation/name "create_user"}
                           :venia/variables [{:variable/name "create_user"
                                              :variable/type :CreateUserInput!}
                                             {:variable/name "riddle_language"
                                              :variable/type :String}
                                             {:variable/name "riddle_answer"
                                              :variable/type :String}
                                             {:variable/name "force_unapproved"
                                              :variable/type :Boolean}]
                           :venia/queries   [[:create_user {:create_user :$create_user
                                                            :riddle_language :$riddle_language
                                                            :riddle_answer :$riddle_answer
                                                            :force_unapproved :$force_unapproved}
                                              upsert-user-fields]]})
(defn db->graphql-user
  [{:keys [::register/email ::register/name ::register/preferred-location
           ::register/selected-skills ::register/id
           ::register/consented ::register/subscribed?
           ::register/current-location ::register/remote]}]
  (cond-> {:email    email
           :name     name
           :skills   (mapv #(hash-map :name %) selected-skills)
           :preferred-locations (when preferred-location [preferred-location])
           :current-location current-location
           :remote remote
           :consented consented
           :subscribed subscribed?}
    id (assoc :id id)))

;; We don't want to pass empty/blank values because otherwise we might
;; override what's already set.
(defn remove-value-for-upsert?
  "True if x is a blank string, empty seq or nil."
  [x]
  (cond (string? x) (str/blank? x)
        (seqable? x) (empty? x)
        :otherwise (nil? x)))

(reg-event-fx
  ::upsert-user
  db/default-interceptors
  (fn [{db :db} _]
    (let [sub-db (::register/sub-db db)
          {:keys [id consented] :as user} (db->graphql-user sub-db)
          user (util/transform-keys remove-value-for-upsert? user)
          verify-riddle? (= (::register/step sub-db) :test)]
      (if-not consented
        {:db (assoc-in db [::register/sub-db ::register/upsert-user-errors] :missing-consent)}
        {:db      (update db ::register/sub-db merge
                          {::register/loading? true
                           ::register/upsert-user-errors nil})
         :graphql {:query      (if id
                                 graphql/update-user-mutation--upsert
                                 create-user-mutation)
                   :variables  (cond
                                 id {:update_user user}
                                 verify-riddle? {:create_user user
                                                 :riddle_language (get-in sub-db [::register/selected-riddle ::register/language])
                                                 :riddle_answer (::register/code-answer sub-db)
                                                 :force_unapproved (::register/failed-code-riddle-check? sub-db)}
                                 :otherwise {:create_user user
                                             :force_unapproved (db/blockchain? (::db/vertical db))})
                   :timeout    30000
                   :on-success [::upsert-user-response]
                   :on-failure [::upsert-user-error verify-riddle?]}}))))

(defn upsert-dispatch-events [new-user? approval blockchain? db email]
  (cond-> [[:user/init]]
    (or (not new-user?) (= (:status approval) "approved") blockchain?) (conj [:register/advance])
    new-user? (conj [:register/track-account-created {:source :email :email email}])))

(reg-event-fx
  ::upsert-user-response
  db/default-interceptors
  (fn [{db :db} [{data :data}]]
    (let [{:keys [email id approval] :as user} (or (:update_user data) (:create_user data))
          new-user? (boolean (:create_user data))
          db (-> db
                 (assoc-in [::register/sub-db ::register/loading?] false)
                 (update ::user/sub-db #(merge % (-> user user/translate-user (assoc ::user/type "candidate")))))
          db (cond-> db
               (not= (:status approval) "approved") (assoc-in [::register/sub-db ::register/approval-fail?] true))
          blockchain? (db/blockchain? (::db/vertical db))]
      (cond-> {:db         db
               :dispatch-n (upsert-dispatch-events new-user? approval blockchain? db email)}
              new-user? (assoc :analytics/alias {:id id})))))

(reg-event-db
  ::upsert-user-error
  register-interceptors
  (fn [db [verify-riddle? resp]]
    (let [error (util/gql-errors->error-key resp)]
      (cond-> db
        true (assoc ::register/loading? false
                    ::register/upsert-user-errors error)
        (and verify-riddle? (= error :unapproved)) (assoc ::register/failed-code-riddle-check? true)))))

(reg-event-db
  :register/hide-skills-info
  register-interceptors
  (fn [db _]
    (assoc db ::register/skills-info-hidden? true)))

(reg-event-db
  ::set-consent
  register-interceptors
  (fn [db [consent]]
    (assoc db ::register/consented (when consent (-> (cljs-time.core/now) (time-coerce/to-string))))))

(reg-event-db
  ::set-subscribed
  register-interceptors
  (fn [db [subcribed?]]
    (assoc db ::register/subscribed? subcribed?)))

(reg-event-db
  :register/confirm-preferred-location
  register-interceptors
  (fn [db _]
    (assoc db
           ::register/preferred-location (::register/preset-location db)
           ::register/remote false
           ::register/skip-ask-for-preferred-location true
           ::register/location-stage :confirm-current-location)))

(reg-event-fx
  ::proceed-from-name
  db/default-interceptors
  (fn [{db :db} _]
    (let [name (get-in db [::register/sub-db ::register/name])
          valid? (and (string? name)
                      (re-find #"[^ ]+ +[^ ]+" name))
          show-verify? (get-in db [::register/sub-db ::register/show-verify?])]
      (cond
        (and valid? show-verify? (not (db/blockchain? (::db/vertical db)))) {:dispatch [:register/advance]}
        valid? {:dispatch [::upsert-user]}
        :otherwise {:db (assoc-in db [::register/sub-db ::register/upsert-user-errors] :invalid-name)}))))
