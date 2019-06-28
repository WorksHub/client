(ns wh.components.common
  (:require
    #?(:clj [wh.config :as config])
    #?(:cljs [re-frame.core :refer [subscribe]])
    [clojure.string :as str]
    [wh.routes :as routes]))

(defn link
  ([{:keys [text handler options]}]
   (let [{:keys [class on-click query-params html-id]} options
         params (dissoc options :on-click :class :query-params)]
     [:a (cond-> {:class class
                  :href (if (map? query-params)
                          (routes/path handler :params params :query-params query-params)
                          (routes/path handler :params params))}
                 on-click (merge #?(:cljs {:on-click on-click}
                                    :clj {:onClick on-click}))
                 html-id (assoc :id html-id)) text]))
  ([text handler & {:as options}]
   (link {:text text :handler handler :options options})))

(def aws-bucket-regex #"functionalworks-backend--(prod|qa)\.s3\.amazonaws\.com")

(defn wrap-img [img-component src {:keys [alt class w h] :as params}]
  (if (and (string? src)
           (re-find aws-bucket-regex src))
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
  ([src]
   (base-img src nil))
  ([src {:keys [fit crop auto]
         :or {fit "crop" crop "entropy" auto "format"}}]
   (let [img-hash (some-> src (str/split #"/") last)
         params {:fit fit :crop crop :auto auto}
         conf-env #?(:cljs @(subscribe [:wh.subs/env])
                     :clj (keyword (config/get :environment)))
         env (if (= :prod conf-env)
               conf-env
               (or (some-> (re-find aws-bucket-regex src) second keyword) conf-env))]
     (str "https://"
          (if ( = env :prod) "workshub" "workshub-dev")
          ".imgix.net/" img-hash "?" (routes/serialize-query-params params)))))

(defn img [src {:keys [alt w h class attrs] :or {alt "Missing alt" w 1.0 h 1.0 class ""} :as opts}]
  (let [url (base-img src opts)]
    [:img (merge {#?(:clj :srcset) #?(:cljs :src-set) (generate-srcset (str url "&" (routes/serialize-query-params {:w w :h h})))
                  :src url
                  :alt alt
                  :class class}
                 attrs)]))

(defn generate-blog-srcset [url]
  (->> [(str url "&w=380&h=150 380w")
        (str url "&w=756&h=300 756w")
        (str url "&w=960&h=300 960w")]
       (str/join ", ")))

(defn blog-card-hero-img [src {alt :alt}]
  (let [url (base-img src)]
    [:img {#?(:clj :srcset) #?(:cljs :src-set) (generate-blog-srcset url)
           :src (str url "&w=960&h=150")
           :alt alt
           :sizes "(min-width: 1008px) 360px, calc(100vw - 36px)"}]))

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
