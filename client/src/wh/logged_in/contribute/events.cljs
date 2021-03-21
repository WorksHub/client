(ns wh.logged-in.contribute.events
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [re-frame.core :refer [path reg-event-db reg-event-fx]]
            [wh.common.cases :as cases]
            [wh.common.errors :as errors]
            [wh.common.keywords :as keywords]
            [wh.common.upload :as upload]
            [wh.common.user :as user-common]
            [wh.components.forms.events :as form-events]
            [wh.db :as db]
            [wh.graphql-cache]
            [wh.graphql.fragments]
            [wh.logged-in.contribute.db :as contribute]
            [wh.pages.core :refer [on-page-load] :as pages]
            [wh.util :as util])
  (:require-macros [clojure.core.strint :refer [<<]]
                   [wh.graphql-macros :refer [defquery]]))

(def contribute-interceptors (into db/default-interceptors
                                   [(path ::contribute/sub-db)]))

(defn simple-edit-fn [k]
  (fn [db [v]]
    (assoc db k v)))

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
      (when (user-common/admin? db)
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
      (when (user-common/admin? db)
        {:dispatch [::search-companies company-name]}))))

(reg-event-db
  ::set-original-source
  contribute-interceptors
  (fn [db [v]]
    (if (str/blank? v)
      (dissoc db ::contribute/original-source)
      (assoc  db ::contribute/original-source v))))

(reg-event-db
  ::set-associated-jobs
  contribute-interceptors
  (fn [db [idx slug]]
    (let [jobs (::contribute/associated-jobs db)]
      (cond
        (not (vector? jobs))
        (assoc db ::contribute/associated-jobs [slug])

        ;; When user clears slug, remove element from vector
        (clojure.string/blank? slug)
        (update db ::contribute/associated-jobs #(util/drop-ith idx %))

        ;; Guards from IndexOutOfBoundsException
        (> idx (dec (count jobs)))
        (update db ::contribute/associated-jobs #(conj % slug))

        :else (assoc-in db [::contribute/associated-jobs idx] slug)))))

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
  (fn [db [_filename {url :url}]]
    (assoc db ::contribute/hero-upload-status :success
           ::contribute/feature url)))

(reg-event-fx
  ::hero-upload-failure
  contribute-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::contribute/hero-upload-status (if (= 413 (:status resp))
                                                     :failure-too-big
                                                     :failure))
     :dispatch [:error/set-global (errors/image-upload-error-message (:status resp))]}))

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

(reg-event-fx
  ::image-article-upload-failure
  contribute-interceptors
  (fn [{db :db} [resp]]
    {:db (assoc db ::contribute/image-article-upload-status :failure)
     :dispatch [:error/set-global (errors/image-upload-error-message (:status resp))]}))

(defquery query-blog-contribute
  {:venia/operation {:operation/type :query
                     :operation/name "blog"}
   :venia/variables [{:variable/name "id"
                      :variable/type :ID!}]
   :venia/queries [[:blog {:id :$id}
                    [:id :title :feature :author :authorId
                     :published :archived :body :creator :originalSource
                     :verticals :primaryVertical :associatedJobs
                     [:company [:name :id]]
                     [:tags :fragment/tagFields]]]]})

(reg-event-fx
  ::fetch-blog
  contribute-interceptors
  (fn [{_db :db} [id]]
    {:dispatch [::pages/set-loader]
     :graphql  {:query      query-blog-contribute
                :on-success [::fetch-blog-success]
                :on-failure [::fetch-blog-failure]
                :variables  {:id id}}}))

(defn translate-blog [blog]
  (-> (keywords/namespace-map "wh.logged-in.contribute.db" (cases/->kebab-case blog))
      (assoc ::contribute/selected-tag-ids (->> blog :tags (map :id) set)
             ::contribute/company-id (get-in blog [:company :id])
             ::contribute/company-name (get-in blog [:company :name])
             ::contribute/archived (:archived blog))
      (update ::contribute/verticals (fnil set []))
      (dissoc ::contribute/company)
      (util/remove-nils)))

(reg-event-fx
  ::fetch-blog-success
  contribute-interceptors
  (fn [{db :db} [res]]
    (let [blog (translate-blog (get-in res [:data :blog]))]
      {:db       (merge db blog {::contribute/body-editing? true})
       :dispatch [::pages/unset-loader]})))

(reg-event-fx
  ::fetch-blog-failure
  contribute-interceptors
  (fn [{_db :db} [_res]]
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
  (fn [{db :db} [_res]]
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
    {:db       (assoc db ::contribute/save-status :failure)
     :dispatch-n [[:error/set-global (-> res
                                         util/gql-errors->error-key
                                         errors/get-blog-error-message)]
                  [::pages/unset-loader]]}))

(defn find-first-error-id
  [sub-db]
  (some (fn [k]
          (when (contribute/has-error? k sub-db)
            (contribute/form-field-id k)))
        contribute/form-order))

(defn prepare-author-info [db]
  (let [contribute-db (::contribute/sub-db db)
        author-name   (::contribute/author contribute-db)
        author-id     (::contribute/author-id contribute-db)
        user-admin?   (user-common/admin? db)]
    (when (and (not author-id)
               user-admin?)
      (-> contribute-db
          ::contribute/author-info
          (select-keys [:summary
                        :other-urls
                        :skills])
          ;; TODO: remove :author from blog, use :author-info instead
          (assoc :name author-name
                 :image-url (-> contribute-db
                                ::contribute/author-info
                                :avatar-url))
          (util/remove-nil-blank-or-empty)))))

(defn add-archived-field
  "add archived field only during blog update"
  [blog db]
  (let [updating-blog? (get-in db [::contribute/sub-db ::contribute/id])]
    (if updating-blog?
      (assoc blog "archived" (get-in db [::contribute/sub-db ::contribute/archived]))
      blog)))

(reg-event-fx
  ::save-blog
  db/default-interceptors
  (fn [{db :db} [_res]]
    (let [blog (::contribute/sub-db db)
          ed (spec/explain-data (contribute/select-spec blog) blog)]
      (if ed
        (do
          (js/console.log "invalid" ed)
          {:db (assoc-in db [::contribute/sub-db ::contribute/save-status] :tried)
           :scroll-into-view (find-first-error-id (::contribute/sub-db db))
           :dispatch [:error/close-global]})
        (let [id (get-in db [::contribute/sub-db ::contribute/id])
              selected-tag-ids (get-in db [::contribute/sub-db ::contribute/selected-tag-ids])
              save-blog-fields [:id :title :feature :author :authorId
                                :published :archived :body :creator :originalSource
                                :verticals :primaryVertical :companyId :tagIds
                                :associatedJobs :authorInfo]
              blog (-> db
                       ::contribute/sub-db
                       (dissoc ::contribute/tags)
                       (assoc ::contribute/tag-ids selected-tag-ids
                              ::contribute/primary-vertical (::db/vertical db)
                              ::contribute/author-info (prepare-author-info db))
                       cases/->camel-case-keys-str
                       (select-keys (map name save-blog-fields))
                       (dissoc (when id "creator"))
                       (util/remove-nils)
                       ;; authorId should be nil sometimes
                       (assoc "authorId" (get-in db [::contribute/sub-db ::contribute/author-id]))
                       (add-archived-field db))]
          {:graphql {:query      (if id update-blog-mutation create-blog-mutation)
                     :variables  (if id {:update_blog blog} {:create_blog blog})
                     :on-success (if id [::save-success] [::create-success])
                     :on-failure [::save-failure]}
           :db (assoc-in db [::contribute/sub-db ::contribute/save-status] :tried)
           :dispatch-n [[:error/close-global]
                        [::pages/set-loader]]})))))

(reg-event-db
  ::set-published
  contribute-interceptors
  (fn [db [published?]]
    (assoc db ::contribute/published published?)))

(reg-event-db
  ::toggle-archived
  contribute-interceptors
  (fn [db _]
    (update db ::contribute/archived #(when-not % (js/Date.)))))

(reg-event-db
  ::init-contribute-db
  db/default-interceptors
  (fn [db _]
    (assoc db ::contribute/sub-db (contribute/default-db db))))

(reg-event-fx
  ::search-authors
  db/default-interceptors
  (fn [{db :db} [query retry-num]]
    (let [_application-id (:wh.settings/algolia-application-id db)]
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
    {:db       (update db ::contribute/selected-tag-ids
                       (fnil util/toggle #{}) (:id tag))
     :dispatch [::edit-tag-search ""]}))

(reg-event-fx
  ::fetch-tags
  (fn [_ _]
    {:dispatch [:graphql/query :tags {}]}))

(reg-event-db
  ::dismiss-codi
  contribute-interceptors
  (fn [db _]
    (assoc db ::contribute/hide-codi? true)))

(reg-event-db
  ::toggle-vertical
  db/default-interceptors
  (fn [db [vertical]]
    (if (and (not (user-common/admin? db))
             (= "www" vertical))
      db ;; abort - only admins can deselect 'www' vertical
      (let [new-db     (update-in db [::contribute/sub-db ::contribute/verticals] util/toggle-unless-empty vertical)
            new-sub-db (::contribute/sub-db new-db)]
        (if (zero? (count (::contribute/verticals new-sub-db)))
          db ;; abort - we can't have an empty set
          (if (contains? (::contribute/verticals new-sub-db) (::contribute/primary-vertical new-sub-db)) ;; ensure primary vertical is one of the selected verticals
            new-db
            (assoc-in new-db [::contribute/sub-db ::contribute/primary-vertical] (first (::contribute/verticals new-sub-db)))))))))

;; AUTHOR info

(def author-info-interceptos
  (into db/default-interceptors [(path ::contribute/sub-db) (path ::contribute/author-info)]))

(reg-event-db
  ::edit-summary
  author-info-interceptos
  (fn [db [summary]]
    (assoc db :summary summary)))

(reg-event-db
  ::edit-url
  author-info-interceptos
  (form-events/multi-edit-fn :other-urls :url))

(reg-event-db
  ::edit-skill
  author-info-interceptos
  (form-events/multi-edit-fn :skills :name))

(reg-event-fx
  ::avatar-upload
  db/default-interceptors
  upload/image-upload-fn)

(reg-event-db
  ::avatar-upload-start
  author-info-interceptos
  (fn [db _]
    (assoc db :avatar-uploading? true)))

(reg-event-db
  ::avatar-upload-success
  author-info-interceptos
  (fn [db [_filename {:keys [url]}]]
    (assoc db
           :avatar-url url
           :avatar-uploading? false)))

(reg-event-fx
  ::avatar-upload-failure
  author-info-interceptos
  (fn [{db :db} [resp]]
    {:db       (assoc db :avatar-uploading? false)
     :dispatch [:error/set-global (errors/image-upload-error-message (:status resp))]}))

(defmethod on-page-load :contribute [_db]
  [[::init-contribute-db]
   [::fetch-tags]])

(defmethod on-page-load :contribute-edit [db]
  [[::fetch-blog (get-in db [::db/page-params :id])]
   [::fetch-tags]])
