(ns wh.admin.create-candidate.events
  (:require [cljs-time.coerce :as tc]
            [cljs-time.core :as t]
            [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.admin.create-candidate.db :as sub-db]
            [wh.common.data :as data]
            [wh.common.fx.google-maps]
            [wh.common.upload :as upload]
            [wh.common.url :as url]
            [wh.db :as db]
            [wh.pages.core :as pages :refer [on-page-load]]
            [wh.util :as util])
  (:require-macros [wh.graphql-macros :refer [defquery]]))

(def create-candidate-interceptors
  (into db/default-interceptors [(path ::sub-db/sub-db)]))

(defquery fetch-companies
  {:venia/operation {:operation/type :query
                     :operation/name "companies"}
   :venia/variables [{:variable/name "search_term"
                      :variable/type :String!}]
   :venia/queries   [[:companies
                      {:search_term :$search_term
                       :page_number 1
                       :page_size   10}
                      [[:pagination [:total :count :pageNumber]]
                       [:companies [:id :name
                                    [:integrations [[:greenhouse [:enabled]]]]]]]]]})

(reg-event-fx
  ::fetch-companies
  create-candidate-interceptors
  (fn [_ [search]]
    {:graphql {:query      fetch-companies
               :variables  {:search_term search}
               :on-success [::fetch-companies-success]
               :on-failure [::fetch-companies-failure]}}))

(reg-event-db
  ::fetch-companies-success
  create-candidate-interceptors
  (fn [db [{{{companies :companies} :companies} :data}]]
    (assoc db ::sub-db/companies companies)))

(reg-event-db
  ::fetch-companies-failure
  create-candidate-interceptors
  (fn [db _] db))

(reg-event-fx
  ::cv-upload
  db/default-interceptors
  upload/cv-upload-fn)

(reg-event-db
  ::cv-upload-start
  create-candidate-interceptors
  (fn [db _]
    (assoc db ::sub-db/cv-uploading? true)))

(reg-event-db
  ::cv-upload-success
  create-candidate-interceptors
  (fn [db [filename {:keys [url hash]}]]
    (assoc db
           ::sub-db/cv-uploading? false
           ::sub-db/cv-url url
           ::sub-db/cv-filename filename
           ::sub-db/cv-hash hash)))

(reg-event-db
  ::cv-upload-failure
  create-candidate-interceptors
  (fn [db _]
    (assoc db
           ::sub-db/cv-uploading? false)))

(doseq [[field {:keys [event?] :or {event? true}}] sub-db/fields
        :when event?
        :let [event-name (keyword "wh.admin.create-candidate.events"
                                  (str "edit-" (name field)))
              db-field (keyword "wh.admin.create-candidate.db" (name field))]]
  (reg-event-db event-name
                create-candidate-interceptors
                (fn [db [new-value]]
                  (assoc db db-field new-value))))

(reg-event-fx
  ::edit-location-search
  create-candidate-interceptors
  (fn [{db :db} [new-value]]
    (merge
      {:db (assoc db ::sub-db/location-search new-value)}
      (if (seq new-value)
        {:dispatch [::search-location]}
        {}))))

(reg-event-db
  ::edit-other-links
  create-candidate-interceptors
  (fn [db [i new-value]]
    (assoc-in db [::sub-db/other-links i] new-value)))

(reg-event-db
  ::add-link
  create-candidate-interceptors
  (fn [db _]
    (update db ::sub-db/other-links conj "")))

(reg-event-fx
  ::search-location
  create-candidate-interceptors
  (fn [{db :db} [retry-num]]
    {:algolia {:index      :places
               :retry-num  retry-num
               :params     {:query       (::sub-db/location-search db)
                            :type        "city"
                            :language    "en"
                            :hitsPerPage 5}
               :on-success [::process-search-location]
               :on-failure [::search-location-bad-response retry-num]}}))

(reg-event-fx
  ::search-location-bad-response
  create-candidate-interceptors
  (fn [{db :db} [retry-attempt result]]
    (if (and retry-attempt (> retry-attempt 2))
      {:db (assoc db ::sub-db/location-search-error
                  "Error searching location, please try again later")}
      (let [attempt (if-not retry-attempt 1 (inc retry-attempt))]
        {:dispatch [::search-location attempt]}))))

(reg-event-db
  ::select-location-suggestion
  create-candidate-interceptors
  (fn [db [id]]
    (let [{:keys [label city country country-code administrative longitude latitude]}
          (nth (::sub-db/location-suggestions db) id)]
      (-> db
          (assoc ::sub-db/location__city city)
          (assoc ::sub-db/location__country country)
          (assoc ::sub-db/location__country-code country-code)
          (assoc ::sub-db/location__state (if (= country-code "US")
                                            administrative
                                            ""))
          (assoc ::sub-db/location__latitude latitude)
          (assoc ::sub-db/location__longitude longitude)
          (assoc ::sub-db/location-search label)
          (assoc ::sub-db/location-suggestions [])))))

(reg-event-fx
  ::edit-current-company-search
  create-candidate-interceptors
  (fn [{db :db} [new-value]]
    {:db (assoc db
                ::sub-db/current-company-search new-value
                ::sub-db/current-company nil)
     :dispatch [::fetch-companies new-value]}))

(reg-event-db
  ::select-company-suggestion
  create-candidate-interceptors
  (fn [db [id]]
    (assoc db
           ::sub-db/current-company id
           ::sub-db/current-company-search
           (some #(when (= (:id %) id) (:name %)) (::sub-db/companies db)))))


(defn- process-location [i {:keys [_value administrative country country_code
                                   locale_names _geoloc]}]
  (cond-> {:id           i
           :label        (str (first locale_names) ", " country)
           :city         (first locale_names)
           :country      country
           :country-code (str/upper-case country_code)}
          (seq administrative) (assoc :administrative (first administrative))
          (seq _geoloc)        (assoc :longitude (:lng _geoloc)
                                      :latitude (:lat _geoloc))))

(reg-event-db
  ::process-search-location
  create-candidate-interceptors
  (fn [db [{:keys [hits]}]]
    (->> hits
         (map-indexed process-location)
         (assoc db ::sub-db/location-suggestions))))

(reg-event-fx
  ::fetch-tags
  (fn [_ _]
    {:dispatch [:graphql/query :tags {:type :tech}]}))

(reg-event-fx
  ::toggle-tech-tag
  create-candidate-interceptors
  (fn [{db :db} [tag]]
    ;; We use :id to select/unselect tags and :label to be sent as user skill name
    (let [tag (select-keys tag [:id :label])]
      {:db       (update db ::sub-db/tech-tags (fnil util/toggle #{}) tag)
       :dispatch [::edit-tech-tag-search ""]})))

(reg-event-fx
  ::toggle-company-tag
  create-candidate-interceptors
  (fn [{db :db} [tag]]
    {:db       (update db ::sub-db/company-tags util/toggle {:tag tag :selected true})
     :dispatch [::edit-company-tag-search ""]}))

(reg-event-fx
  ::initialize-db
  db/default-interceptors
  (fn [{db :db} _]
    (let [new-db (sub-db/initial-db db)]
      {:db (assoc db ::sub-db/sub-db new-db)})))

(def create-user-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_user"}
   :venia/variables [{:variable/name "create_user"
                      :variable/type :CreateUserInput!}]
   :venia/queries   [[:create_user {:create_user :$create_user}
                      [:id :email]]]})

(defn db->location
  [{:keys [::sub-db/location__street ::sub-db/location__city
           ::sub-db/location__post-code ::sub-db/location__state
           ::sub-db/location__country ::sub-db/location__country-code
           ::sub-db/location__latitude ::sub-db/location__longitude]}]
  {:street       location__street
   :post-code    location__post-code
   :city         location__city
   :state        (get data/us-states location__state)
   :country      location__country
   :country-code location__country-code
   :latitude     location__latitude
   :longitude    location__longitude})

(defn db->graphql-user
  [{:keys [::sub-db/email ::sub-db/name ::sub-db/notify ::sub-db/tech-tags
           ::sub-db/company-tags ::sub-db/github-url ::sub-db/other-links
           ::sub-db/cv-url ::sub-db/cv-hash ::sub-db/cv-filename
           ::sub-db/current-company ::sub-db/current-company-search ::sub-db/phone]
    :as   db}]
  (merge (util/transform-keys
           #(or (nil? %) (str/blank? %))
           {:email                email
            :name                 name
            :phone                phone
            :notify               notify
            :reset-session        false
            :skills               (mapv #(hash-map :name (:label %)) tech-tags)
            :preferred-locations  [(db->location db)]
            :company-perks        (mapv #(hash-map :name (:tag %)) company-tags)
            :other-urls           (->> (into [(when-not (str/blank? github-url)
                                                {:title "GitHub", :url github-url})]
                                             (map #(hash-map :url %) other-links))
                                       (remove (comp str/blank? :url))
                                       (map #(update % :url url/sanitize-url)))
            :current-company-id   current-company
            :current-company-name current-company-search
            :consented            (-> (t/now) (tc/to-string))
            :subscribed           false})
         (when (and cv-url cv-filename cv-hash)
           {:cv {:file {:url cv-url :name cv-filename :hash cv-hash}}})))

(reg-event-fx
  ::save
  create-candidate-interceptors
  (fn [{db :db} _]
    (let [errors (sub-db/invalid-fields db)]
      (if errors
        {:db                (-> db (assoc ::sub-db/form-errors (set errors)))
         :dispatch-debounce {:id       :scroll-to-error-after-pause
                             :dispatch [::scroll-into-view (db/key->id (first errors))]
                             :timeout  50}}
        {:graphql {:query      create-user-mutation
                   :variables  {:create_user (db->graphql-user db)}
                   :timeout    30000
                   :on-success [::save-success]
                   :on-failure [::save-failure]}}))))

(reg-event-fx
  ::save-success
  db/default-interceptors
  (fn [{db :db} [{{candidate :create_user} :data}]]
    (let [admins-email (get-in db [:wh.user.db/sub-db :wh.user.db/email])]
      {:navigate                       [:candidate :params {:id (:id candidate)}]
       :register/track-account-created {:source admins-email
                                        :email  (:email candidate)}})))

(reg-event-db
  ::save-failure
  create-candidate-interceptors
  (fn [db [resp]]
    (assoc db ::sub-db/error (util/gql-errors->error-key resp))))

(reg-event-fx
  ::scroll-into-view
  db/default-interceptors
  (fn [_ [id]]
    {:scroll-into-view id}))

(defmethod on-page-load :create-candidate [_db]
  [[:google/load-maps]
   [::initialize-db]
   [::fetch-tags]])
