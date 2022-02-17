(ns wh.components.common
  (:require
    #?(:clj [wh.config :as config])
    [clojure.string :as str]
    [wh.common.subs]
    [wh.common.url :as url]
    [wh.routes :as routes]
    [wh.re-frame.subs :refer [<sub]]))

(defn link
  ([{:keys [text handler options]}]
   (let [{:keys [class on-click query-params html-id]} options
         params (dissoc options :on-click :class :query-params)]
     [:a (cond-> {:class class
                  :href  (if (map? query-params)
                           (routes/path handler :params params :query-params query-params)
                           (routes/path handler :params params))}
                 on-click (merge #?(:cljs {:on-click on-click}
                                    :clj  {:onClick on-click}))
                 html-id (assoc :id html-id)) text]))
  ([text handler & {:as options}]
   (link {:text text :handler handler :options options})))

;; TODO: 5218, remove this helper when we have only one user profile implementation
(defn link-user [text admin? & {:as options}]
  [link
   {:text text
    :handler (if admin? :profile-by-id :user)
    :options options}])

(def aws-bucket-regex #"functionalworks-backend--(prod|qa)\.s3\.amazonaws\.com")

(defn aws-bucket-source? [src]
  (and (string? src)
       (re-find aws-bucket-regex src)))

(defn wrap-img [img-component src {:keys [alt class w h] :as params}]
  (if (aws-bucket-source? src)
    (img-component src params)
    [:img (merge {:src src
                  :alt alt
                  :class class}
                 (when w
                   {:width w})
                 (when h
                   {:height h}))]))

(defn generate-srcset [url]
  (->> [(str url " 1x")
        (str url "&q=40&dpr=2 2x")
        (str url "&q=20&dpr=3 3x")]
       (str/join ", ")))

(defn base-img
  "hides image under imgix"
  ([src]
   (base-img src nil))
  ([src {:keys [fit crop auto]
         :or   {fit "crop" crop "entropy" auto "format"}}]
   (when src
     (let [img-hash          (some-> src (str/split #"/") last)
           params            {:fit fit :crop crop :auto auto}
           conf-env #?(:cljs (<sub [:wh/env])
                       :clj  (keyword (config/get :environment)))
           env               (if (= :prod conf-env)
                               conf-env
                               (or (some-> (re-find aws-bucket-regex src) second keyword) conf-env))]
       (str "https://"
            (if (= env :prod) "workshub" "workshub-dev")
            ".imgix.net/" img-hash "?" (url/serialize-query-params params))))))

(defn og-img [src]
  (some-> (base-img src)
          (str "&w=128")))

(defn og-img-blog [src]
  (some-> (base-img src)
          (str "&w=512&ar=1.9")))

(def src-set #?(:clj  :srcset
                :cljs :src-set))

(defn img [src {:keys [alt w h class attrs] :or {alt "Missing alt" w 1.0 h 1.0 class ""} :as opts}]
  (let [url (base-img src opts)]
    [:img (merge {src-set (generate-srcset (str url "&" (url/serialize-query-params {:w w :h h})))
                  :src    url
                  :alt    alt
                  :class  class}
                 attrs)]))

(defn generate-blog-srcset [url]
  (->> [(str url "&w=380&h=150 380w")
        (str url "&w=756&h=300 756w")
        (str url "&w=960&h=300 960w")]
       (str/join ", ")))

(defn blog-card-hero-img [src {alt :alt}]
  (let [url (base-img src)]
    [:img {src-set (generate-blog-srcset url)
           :src    (str url "&w=960&h=150")
           :alt    alt
           :sizes  "(min-width: 1008px) 360px, calc(100vw - 36px)"}]))

(defn companies-section
  [text & logos]
  [:div.companies-section
   (into [:div.columns.is-mobile]
         (conj
           (for [company (concat (or logos []) ["troops.svg" "facebook.svg" "two-sigma.svg" "airbnb.svg" "spotify.svg"])]
             [:div.column
              [:img {:src (if (str/starts-with? company "http")
                            company
                            (str "/images/homepage/logos/" company))
                     :alt (str company " logo")}]])
           [:div.column
            [:span text]]))])
