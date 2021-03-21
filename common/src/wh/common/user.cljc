(ns wh.common.user
  (:require [clojure.string :as str]
            [wh.common.keywords :as keywords]
            [wh.util :as util])
  (#?(:clj :require :cljs :require-macros)
    [wh.graphql-macros :refer [defquery]]))

(defn absolute-image-url [image-url]
  (if (and (not (str/blank? image-url))
           (str/starts-with? image-url "/images/"))
    (str "https://www.works-hub.com" image-url)
    image-url))

(defn predefined-avatar? [image-url]
  (and (not (str/blank? image-url))
       (or (re-matches #"^https://www.works-hub.com/images/avatar-([0-9])+.png$" image-url)
           (re-matches #"^/images/avatar-([0-9])+.svg$" image-url))))

(defn url->predefined-avatar [url]
  (when-let [[_ i] (predefined-avatar? url)]
    #?(:cljs (js/parseInt i)
       :clj  (Integer/parseInt i))))

(defn avatar-url [i]
  (str "https://www.works-hub.com/images/avatar-" i ".png"))

(defn random-avatar-url []
  (avatar-url (inc (rand-int 5))))

(defn pngify-image-url [image-url]
  ;; We ensure the image is a PNG so the email clients can properly display it
  (when-not (str/blank? image-url)
    (str/replace image-url #"svg" "png")))

(defn full-name? [name]
  (not (or (str/blank? name)
           (str/blank? (second (re-find #"(.*) (.*)$" (str/trim (str/replace name #"\s+" " "))))))))

(defn user->segment-traits
  [{:keys [id email name skills visa-status visa-status-other
           approval salary image-url summary created
           board job-seeking-status remote platform-url
           hubspot-profile-url other-urls current-location
           preferred-locations type company]}]
  (cond-> {:id                  id
           :email               email
           :name                name
           :type                type
           :vertical            board
           :platform-url        platform-url
           :hubspot-url         hubspot-profile-url
           :skills              (mapv :name skills)
           :visa                (str (str/join ", " visa-status) (when visa-status-other (str " " visa-status-other)))
           :salary              salary
           :avatar              image-url
           :description         summary
           :created-at          created
           :job-seeking-status  job-seeking-status
           :remote              remote
           :urls                (mapv :url other-urls)
           :address             (when current-location
                                  {:city    (:city current-location)
                                   :country (:country current-location)})
           :preferred-locations (mapv #(str (:city %) ", " (:country %)) (map keywords/strip-ns-from-map-keys preferred-locations))}
          approval (assoc :approval-status (:status approval)
                          :approval-source (:source approval))
          company (assoc :company-name (:name company)
                         :company-id (:id company)
                         :company-vertical (:vertical company)
                         :package (:package company))
          true util/remove-nil-blank-or-empty))


(defn candidate-type? [type]
  (= type "candidate"))

(defn candidate? [db]
  (candidate-type? (get-in db [:wh.user.db/sub-db :wh.user.db/type])))

(defn company-type? [type]
  (= type "company"))

(defn company? [db]
  (company-type? (get-in db [:wh.user.db/sub-db :wh.user.db/type])))

(defn admin-type? [type]
  (= type "admin"))

(defn admin? [db]
  (admin-type? (get-in db [:wh.user.db/sub-db :wh.user.db/type])))

(defn user-id
  [db]
  (get-in db [:wh.user.db/sub-db :wh.user.db/id]))

(defquery permissions-query
  {:venia/operation {:operation/type :query
                     :operation/name "profile"}
   :venia/variables [{:variable/name "user_id" :variable/type :ID}]
   :venia/queries   [[:me [[:company [:permissions]]]]]})

(defn translate-permissions [permissions]
  (into #{} (map keyword permissions)))

(defn cv-url [user]
  (get-in user [:cv :file :url]))

(defn company-member? [{:keys [user company]}]
  (= (:id company) (:company-id user)))

(defn has-access-to-company? [{:keys [user company]}]
  (or (company-member? {:company company, :user user})
      (admin-type? (:type user))))
