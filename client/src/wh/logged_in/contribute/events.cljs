(ns wh.logged-in.contribute.events
  (:require
    [camel-snake-kebab.core :as c]
    [camel-snake-kebab.extras :as ce]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch path reg-event-db reg-event-fx]]
    [wh.common.cases :as cases]
    [wh.common.upload :as upload]
    [wh.db :as db]
    [wh.graphql-cache]
    [wh.logged-in.contribute.db :as contribute]
    [wh.pages.core :refer [on-page-load] :as pages]
    [wh.user.db :as user]
    [wh.util :as util])
  (:require-macros
    [clojure.core.strint :refer [<<]]
    [wh.graphql-macros :refer [defquery]]))

(def contribute-interceptors (into db/default-interceptors
                                   [(path ::contribute/sub-db)]))

(defn simple-edit-fn [k]
  (fn [db [v]]
    (assoc db k v)))

(defn multi-edit-fn
  [k & path]
  (fn [db [i value]]
    (let [old-values (k db)
          new-values (assoc-in old-values (into [i] path) value)
          new-values (vec (remove #(str/blank? (get-in % path)) new-values))]
      (assoc db k new-values))))

(reg-event-db
  ::set-title
  contribute-interceptors
  (simple-edit-fn ::contribute/title))

(reg-event-fx
  ::set-author
  db/default-interceptors
  (fn [{db :db} [author]]
    (merge
      {:db (-> db
               (assoc-in [::contribute/sub-db ::contribute/author] author)
               (assoc-in [::contribute/sub-db ::contribute/author-id] nil))}
      (when (user/admin? db)
        {:dispatch [::search-authors author]}))))

(defn set-company-name
  [db n]
  (if (str/blank? n)
    (update db ::contribute/sub-db dissoc ::contribute/company-name)
    (assoc-in  db [::contribute/sub-db ::contribute/company-name] n)))

(reg-event-fx
  ::set-company
  db/default-interceptors
  (fn [{db :db} [company-name]]
    (merge
      {:db (-> db
               (assoc-in [::contribute/sub-db ::contribute/company-id] nil)
               (set-company-name company-name))}
      (when (user/admin? db)
        {:dispatch [::search-companies company-name]}))))

(reg-event-db
  ::set-original-source
  contribute-interceptors
  (fn [db [v]]
    (if (str/blank? v)
      (dissoc db ::contribute/original-source)
      (assoc  db ::contribute/original-source v))))

(reg-event-fx
  ::hero-upload
  db/default-interceptors
  upload/image-upload-fn)

(reg-event-db
  ::hero-upload-start
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/hero-upload-status :started)))

(reg-event-db
  ::hero-upload-success
  contribute-interceptors
  (fn [db [filename {url :url}]]
    (assoc db ::contribute/hero-upload-status :success
           ::contribute/feature url)))

(reg-event-db
  ::hero-upload-failure
  contribute-interceptors
  (fn [db [resp]]
    (assoc db ::contribute/hero-upload-status (if (= 413 (:status resp))
                                                :failure-too-big
                                                :failure))))

(reg-event-db
  ::set-body
  contribute-interceptors
  (simple-edit-fn ::contribute/body))

(reg-event-db
  ::set-body-cursor-position
  contribute-interceptors
  (fn [db [position]]
    (assoc db ::contribute/body-cursor-position position)))

(reg-event-db
  ::set-primary-vertical
  contribute-interceptors
  (simple-edit-fn ::contribute/primary-vertical))

(reg-event-db
  ::show-edit
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/body-editing? true)))

(reg-event-db
  ::show-preview
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/body-editing? false)))

(reg-event-fx
  ::image-article-upload
  db/default-interceptors
  upload/image-upload-fn)

(reg-event-db
  ::image-article-upload-start
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/image-article-upload-status :started)))

(defn markdown-image [url filename]
  (let [url (str/replace url "-" "\\-")]
    (<< "![~{filename}](~{url})")))

(reg-event-db
  ::image-article-upload-success
  contribute-interceptors
  (fn [{::contribute/keys [body body-cursor-position] :as db} [filename {url :url}]]
    (let [[before-cursor after-cursor] (split-at body-cursor-position body)]
      (assoc db ::contribute/image-article-upload-status :success
                ::contribute/body (str (apply str before-cursor)
                                       (markdown-image url filename)
                                       (apply str after-cursor))))))

(reg-event-db
  ::image-article-upload-failure
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/image-article-upload-status :failure)))

(def base-blog-fields [:id :title :feature :author :authorId
                       :published :tags :body :creator :originalSource :verticals :primaryVertical])
(def save-blog-fields (conj base-blog-fields :companyId))
(def query-blog-fields (conj base-blog-fields [:company [:name :id]]))

(defn blog-query [id]
  {:venia/queries [[:blog {:id id}
                    query-blog-fields]]})

(reg-event-fx
  ::fetch-blog
  contribute-interceptors
  (fn [{db :db} [id]]
    {:dispatch [::pages/set-loader]
     :graphql {:query (blog-query id)
               :on-success [::fetch-blog-success]
               :on-failure [::fetch-blog-failure]}}))

(defn translate-blog [blog]
  (let [r (-> (util/namespace-map "wh.logged-in.contribute.db"
                                  (cases/->kebab-case blog))
              (update ::contribute/tags (comp set (partial map (fn [t] {:tag t :selected true}))))
              (update ::contribute/verticals (fnil set []))
              (assoc  ::contribute/company-id (get-in blog [:company :id]))
              (assoc  ::contribute/company-name (get-in blog [:company :name]))
              (dissoc ::contribute/company))
        orig-source? (::contribute/original-source r)] ;; remove OS if it's nil; it's optional
    (if-not orig-source?
      (dissoc r ::contribute/original-source)
      r)))

(reg-event-fx
  ::fetch-blog-success
  contribute-interceptors
  (fn [{db :db} [res]]
    {:db (merge db (translate-blog (get-in res [:data :blog])))
     :dispatch [::pages/unset-loader]}))

(reg-event-fx
  ::fetch-blog-failure
  contribute-interceptors
  (fn [{db :db} [res]]
    {:dispatch [::pages/unset-loader]}))

(def create-blog-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "create_blog"}
   :venia/variables [{:variable/name "create_blog"
                      :variable/type :CreateBlogInput!}]
   :venia/queries   [[:create_blog {:create_blog :$create_blog}
                      [:id :creator]]]})

(def update-blog-mutation
  {:venia/operation {:operation/type :mutation
                     :operation/name "update_blog"}
   :venia/variables [{:variable/name "update_blog"
                      :variable/type :UpdateBlogInput!}]
   :venia/queries   [[:update_blog {:update_blog :$update_blog}
                      [:id]]]})

(reg-event-fx
  ::save-success
  contribute-interceptors
  (fn [{db :db} [res]]
    {:db (assoc db ::contribute/save-status :success)
     :dispatch-n [[::pages/unset-loader]
                  [:graphql/invalidate :blog {:id (::contribute/id db)}]
                  [:graphql/invalidate-all-for-id :blogs]
                  [:wh.events/scroll-to-top]]}))

(reg-event-fx
  ::create-success
  contribute-interceptors
  (fn [{db :db} [res]]
    (let [blog (get-in res [:data :create_blog])
          blog-id (:id blog)]
      {:db (assoc db ::contribute/save-status :success
                  ::contribute/id blog-id
                  ::contribute/creator (:creator blog))
       :navigate [:contribute-edit :params {:id blog-id}]
       :dispatch-n [[::pages/unset-loader]
                    [:graphql/invalidate-all-for-id :blogs]
                    [:wh.events/scroll-to-top]]})))

(reg-event-fx
  ::save-failure
  contribute-interceptors
  (fn [{db :db} [res]]
    {:db (assoc db ::contribute/save-status :failure)
     :dispatch [::pages/unset-loader]}))

(defn find-first-error-id
  [sub-db]
  (some (fn [k]
          (when (contribute/has-error? k sub-db)
            (contribute/form-field-id k)))
        contribute/form-order))

(reg-event-fx
  ::save-blog
  db/default-interceptors
  (fn [{db :db} [res]]
    (if-let [ed (s/explain-data ::contribute/blog (::contribute/sub-db db))]
      (do
        (js/console.log "invalid" ed)
        {:db (assoc-in db [::contribute/sub-db ::contribute/save-status] :tried)
         :scroll-into-view (find-first-error-id (::contribute/sub-db db))})

      (let [id (get-in db [::contribute/sub-db ::contribute/id])
            blog (-> db
                     ::contribute/sub-db
                     (update ::contribute/tags (partial map :tag))
                     (assoc ::contribute/primary-vertical (::db/vertical db))
                     (->> (ce/transform-keys c/->camelCaseString))
                     (select-keys (map name save-blog-fields))
                     (dissoc (when id "creator"))
                     (util/remove-nils))]
        {:graphql {:query      (if id update-blog-mutation create-blog-mutation)
                   :variables  (if id {:update_blog blog} {:create_blog blog})
                   :on-success (if id [::save-success] [::create-success])
                   :on-failure [::save-failure]}
         :db (assoc-in db [::contribute/sub-db ::contribute/save-status] :tried)
         :dispatch [::pages/set-loader]}))))

(reg-event-db
  ::set-published
  contribute-interceptors
  (fn [db [published?]]
    (assoc db ::contribute/published published?)))

(reg-event-db
  ::init-contribute-db
  db/default-interceptors
  (fn [db _]
    (assoc db ::contribute/sub-db (contribute/default-db db))))

(reg-event-fx
  ::search-authors
  db/default-interceptors
  (fn [{db :db} [query retry-num]]
    (let [application-id (:wh.settings/algolia-application-id db)]
      {:db (assoc-in db [::contribute/sub-db ::contribute/author-searching?] true)
       :algolia {:index      :candidates
                 :params     {:query                query
                              :page                 0
                              :hitsPerPage          10
                              :attributesToRetrieve ["name" "email"]}
                 :retry-num  retry-num
                 :on-success [::search-authors-success]
                 :on-failure [::search-bad-response query retry-num]}})))

(reg-event-db
  ::search-authors-success
  contribute-interceptors
  (fn [db [res]]
    (assoc db
           ::contribute/author-searching? false
           ::contribute/author-suggestions (:hits res))))

(reg-event-fx
  ::search-bad-response
  contribute-interceptors
  (fn [{db :db} [query retry-attempt result]]
    (if (and retry-attempt (> retry-attempt 2))
      (do
        (js/console.error "Search failed:" result)
        {:db (assoc db ::contribute/author-search-failed true)})
      (let [attempt (if-not retry-attempt 1 (inc retry-attempt))]
        {:dispatch [::search-authors query attempt]}))))

(def companies-query-page-size 10)

(defquery companies-query
  {:venia/operation {:operation/type :query
                     :operation/name "companies"}
   :venia/variables [{:variable/name "search_term" :variable/type :String}
                     {:variable/name "page_number" :variable/type :Int}
                     {:variable/name "page_size" :variable/type :Int}]
   :venia/queries   [[:companies {:search_term :$search_term
                                  :page_number :$page_number
                                  :page_size   :$page_size}
                      [[:pagination [:total :count :pageNumber]]
                       [:companies [:id :name]]]]]})

(reg-event-fx
  ::search-companies
  contribute-interceptors
  (fn [{db :db} [company-name]]
    {:db (assoc db ::contribute/company-searching? true)
     :graphql {:query      companies-query
               :variables  {:search_term company-name
                            :page_number 1
                            :page_size companies-query-page-size}
               :on-success [::fetch-companies-success]
               :on-failure [::fetch-companies-failure]}}))

(reg-event-db
  ::fetch-companies-success
  contribute-interceptors
  (fn [db [{{{companies :companies} :companies} :data}]]
    (assoc db
           ::contribute/company-suggestions companies
           ::contribute/company-searching? false)))

(reg-event-db
  ::fetch-companies-failure
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/company-searching? false)))

(reg-event-db
  ::select-author-suggestion
  contribute-interceptors
  (fn [db [id]]
    (assoc db
           ::contribute/author-id id
           ::contribute/author (:name (first (filter #(= (:objectID %) id) (::contribute/author-suggestions db))))
           ::contribute/author-suggestions nil)))

(reg-event-db
  ::select-company-suggestion
  contribute-interceptors
  (fn [db [id]]
    (assoc db
           ::contribute/company-id id
           ::contribute/company-name (:name (first (filter #(= (:id %) id) (::contribute/company-suggestions db))))
           ::contribute/company-suggestions nil)))

(reg-event-db
  ::edit-tag-search
  contribute-interceptors
  (fn [db [search]]
    (assoc db ::contribute/tag-search search)))

(reg-event-fx
  ::toggle-tag
  contribute-interceptors
  (fn [{db :db} [tag]]
    {:db (update db ::contribute/tags (fnil util/toggle #{}) {:tag tag :selected true})
     :dispatch [::edit-tag-search ""]}))

(defquery fetch-tags-query
  {:venia/operation {:operation/type :query
                     :operation/name "jobs_search"}
   :venia/variables [{:variable/name "vertical" :variable/type :vertical}
                     {:variable/name "search_term" :variable/type :String!}
                     {:variable/name "page" :variable/type :Int!}]
   :venia/queries   [[:jobs_search {:vertical    :$vertical
                                    :search_term :$search_term
                                    :page        :$page}
                      [[:facets [:attr :value :count]]]]]})

(reg-event-fx
  ::fetch-tags
  contribute-interceptors
  (fn [{db :db} _]
    (when-not (::contribute/available-tags db)
      {:graphql {:query fetch-tags-query
                 :variables  {:search_term ""
                              :page        1
                              :vertical    "functional"}
                 :on-success [::fetch-tags-success]}})))

(reg-event-db
  ::fetch-tags-success
  contribute-interceptors
  (fn [db [results]]
    (let [results (group-by :attr (get-in results [:data :jobs_search :facets]))
          results (->> (get results "tags")
                       (sort-by :count)
                       (map #(hash-map :tag (:value %)))
                       (reverse))]
      (assoc db ::contribute/available-tags results))))

(reg-event-db
  ::dismiss-codi
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/hide-codi? true)))

(reg-event-db
  ::toggle-vertical
  contribute-interceptors
  (fn [db [new-value]]
    (let [new-db (update db ::contribute/verticals util/toggle-unless-empty new-value)]
      (if (zero? (count (::contribute/verticals new-db)))
        db ;; abort - we can't have an empty set
        (if (contains? (::contribute/verticals new-db) (::contribute/primary-vertical new-db)) ;; ensure primary vertical is one of the selected verticals
          new-db
          (assoc new-db ::contribute/primary-vertical (first (::contribute/verticals new-db))))))))

(defmethod on-page-load :contribute [db]
  [[::init-contribute-db]
   [::fetch-tags]])

(defmethod on-page-load :contribute-edit [db]
  [[::fetch-blog (get-in db [::db/page-params :id])]
   [::fetch-tags]])
