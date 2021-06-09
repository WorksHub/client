(ns wh.logged-in.contribute.subs
  (:require [cljs.spec.alpha :as s]
            [clojure.set :as set]
            [clojure.string :as str]
            [markdown.core :refer [md->html]]
            [re-frame.core :refer [reg-sub]]
            [wh.components.tag :refer [->tag tag->form-tag]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.common.text :as txt]
            [wh.logged-in.contribute.db :as contribute]
            [wh.pages.core :as pages]
            [wh.verticals :as verticals])
  (:require-macros [clojure.core.strint :refer [<<]]))

(reg-sub
  ::contribute
  (fn [db _]
    (::contribute/sub-db db)))

(reg-sub
  ::title
  :<- [::contribute]
  (fn [db _]
    (::contribute/title db)))

(reg-sub
  ::tags
  (fn [_ _]
    (->> [:list-tags :tags]
         (get-in (<sub [:graphql/result :tags {}]))
         (map ->tag))))

(reg-sub
  ::selected-tag-ids
  :<- [::contribute]
  (fn [db _]
    (::contribute/selected-tag-ids db)))

(reg-sub
  ::tag-search
  :<- [::contribute]
  (fn [db _]
    (::contribute/tag-search db)))

(reg-sub
  ::matching-tags
  :<- [::tags]
  :<- [::tag-search]
  :<- [::selected-tag-ids]
  (fn [[tags tag-search selected-tag-ids]]
    (let [matching-but-not-selected
          (filter (fn [tag] (and (or (str/blank? tag-search)
                                    (str/includes? (str/lower-case (:label tag)) tag-search))
                                (not (contains? selected-tag-ids (:id tag))))) tags)
          selected-tags (->> tags
                             (filter
                               (fn [tag] (contains? selected-tag-ids (:id tag))))
                             (map #(assoc % :selected true)))]
      (->> (concat selected-tags matching-but-not-selected)
           (map tag->form-tag)
           (take (+ 20 (count selected-tag-ids)))))))

(reg-sub
  ::author
  :<- [::contribute]
  (fn [db _]
    (::contribute/author db)))

(reg-sub
  ::author-searching?
  :<- [::contribute]
  (fn [db _]
    (::contribute/author-searching? db)))

(reg-sub
  ::company-name
  :<- [::contribute]
  (fn [db _]
    (::contribute/company-name db)))

(reg-sub
  ::company-searching?
  :<- [::contribute]
  (fn [db _]
    (::contribute/company-searching? db)))

(reg-sub
  ::original-source
  :<- [::contribute]
  (fn [db _]
    (::contribute/original-source db)))

(reg-sub
 ::associated-jobs
 :<- [::contribute]
 (fn [db _]
   (::contribute/associated-jobs db)))

(reg-sub
  ::verticals
  :<- [::contribute]
  (fn [db _]
    (::contribute/verticals db)))

(reg-sub
  ::all-verticals
  (fn [_ _]
    verticals/future-blog-verticals))

(reg-sub
  ::off-verticals
  :<- [::verticals]
  :<- [::all-verticals]
  (fn [[verticals all-verticals] _]
    (set/difference all-verticals verticals)))

(reg-sub
  ::hero-upload-status
  :<- [::contribute]
  (fn [db _]
    (::contribute/hero-upload-status db)))

(reg-sub
  ::hero-uploading?
  :<- [::hero-upload-status]
  (fn [status]
    (= :started status)))

(reg-sub
  ::hero-url
  :<- [::contribute]
  (fn [db _]
    (::contribute/feature db)))

(reg-sub
  ::body
  :<- [::contribute]
  (fn [db _]
    (::contribute/body db)))

(reg-sub
  ::primary-vertical
  :<- [::contribute]
  (fn [db _]
    (::contribute/primary-vertical db)))

(reg-sub
  ::body-editing?
  :<- [::contribute]
  (fn [db _]
    (::contribute/body-editing? db)))

(reg-sub
  ::body-html
  :<- [::body]
  (fn [body]
    (md->html body)))

(reg-sub
  ::image-article-upload-status
  :<- [::contribute]
  (fn [db]
    (::contribute/image-article-upload-status db)))

(reg-sub
  ::image-article-upload-hide-status?
  :<- [::image-article-upload-status]
  (fn [status]
    (#{:not-started :success} status)))

(reg-sub
  ::image-article-uploading?
  :<- [::image-article-upload-status]
  (fn [status]
    (= :started status)))

(reg-sub
  ::image-article-upload-error?
  :<- [::image-article-upload-status]
  (fn [status]
    (= status :failure)))

(reg-sub
  ::save-status
  :<- [::contribute]
  (fn [db]
    (::contribute/save-status db)))

(reg-sub
  ::validation-error?
  :<- [::contribute]
  (fn [db]
    (and
      (= :tried (::contribute/save-status db))
      (not (s/valid? (contribute/select-spec db) db)))))

(reg-sub
  ::title-validation-error
  :<- [::contribute]
  (fn [db]
    (when (contribute/has-error? ::contribute/title db)
      "This field cannot be left blank.")))

(reg-sub
  ::author-validation-error
  :<- [::contribute]
  (fn [db]
    (when (contribute/has-error? ::contribute/author db)
      "Select a valid author from the list.")))

(reg-sub
  ::company-validation-error
  :<- [::contribute]
  (fn [db]
    (when (contribute/has-error? ::contribute/company-id db)
      "Select a valid company from the list.")))

(reg-sub
  ::hero-validation-error?
  :<- [::contribute]
  (fn [db]
    (contribute/has-error? ::contribute/feature db)))

(reg-sub
  ::body-validation-error
  :<- [::contribute]
  (fn [db]
    (when (contribute/has-error? ::contribute/body db)
      "This field cannot be left blank.")))

(reg-sub
  ::tags-validation-error
  :<- [::contribute]
  (fn [db]
    (when (contribute/has-error? ::contribute/selected-tag-ids db)
      "Please select one or more tags.")))

(reg-sub
  ::original-source-validation-error
  :<- [::contribute]
  (fn [db]
    (when (and (txt/not-blank (::contribute/original-source db))
               (contribute/has-error? ::contribute/original-source db))
      "Please provide a valid URL or leave blank.")))

(reg-sub
  ::published?
  :<- [::contribute]
  (fn [db]
    (::contribute/published db)))

(reg-sub
  ::archived?
  :<- [::contribute]
  (fn [db]
    (boolean (::contribute/archived db))))

(reg-sub
  ::paid?
  :<- [::contribute]
  (fn [db]
    (boolean (::contribute/paid db))))

(reg-sub
  ::cross-posts
  :<- [::contribute]
  (fn [db]
    (::contribute/cross-posts db)))

(reg-sub
  ::creator
  :<- [::contribute]
  (fn [db]
    (::contribute/creator db)))

(reg-sub
  ::author-suggestions
  :<- [:user/admin?]
  :<- [::contribute]
  (fn [[admin? db] _]
    (when admin?
      (mapv (fn [{:keys [name email objectID]}]
              {:label (<< "~{name} (~{email})"), :id objectID})
            (::contribute/author-suggestions db)))))

(reg-sub
  ::company-suggestions
  :<- [:user/admin?]
  :<- [::contribute]
  (fn [[admin? db] _]
    (when admin?
      (mapv (fn [{:keys [name id]}]
              {:label name :id id})
            (::contribute/company-suggestions db)))))

(reg-sub
  ::edit-page-authorized?
  :<- [:user/admin?]
  :<- [:user/email]
  :<- [::contribute]
  :<- [::published?]
  :<- [::creator]
  (fn [[admin? email db published? creator] _]
    (or admin?
        (not (::contribute/id db))
        (and (= creator email)
             (not published?)))))

(reg-sub
  ::contribute-page?
  :<- [::pages/page]
  (fn [page]
    (= :contribute page)))

(reg-sub
  ::hide-codi?
  :<- [::contribute]
  (fn [db]
    (::contribute/hide-codi? db)))

(reg-sub
  ::user-selected?
  :<- [::contribute]
  (fn [db]
    (::contribute/author-id db)))

(reg-sub
  ::show-vertical-selector?
  :<- [:user/admin?]
  :<- [:user/company?]
  (fn [[admin? company?]]
    (or admin? company?)))

(reg-sub
  ::author-info
  (fn [db _]
    (get-in db [::contribute/sub-db ::contribute/author-info])))

(reg-sub
  ::summary
  :<- [::author-info]
  (fn [ai _]
    (:summary ai)))

(reg-sub
  ::other-urls
  :<- [::author-info]
  (fn [ai _]
    (->> (:other-urls ai)
         (map :url))))

(reg-sub
  ::skills
  :<- [::author-info]
  (fn [ai _]
    (:skills ai)))

(reg-sub
  ::avatar-url
  :<- [::author-info]
  (fn [ai _]
    (:avatar-url ai)))

(reg-sub
  ::avatar-uploading?
  :<- [::author-info]
  (fn [ai _]
    (:avatar-uploading? ai)))
