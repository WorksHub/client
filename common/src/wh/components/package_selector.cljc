(ns wh.components.package-selector
  (:require #?(:cljs [reagent.core :as r])
            [clojure.string :as str]
            [wh.common.cost :as cost]
            [wh.common.data :as data :refer [all-package-perks billing-data]]
            [wh.common.text :as text]
            [wh.components.icons :refer [icon]]
            [wh.components.selector :refer [selector]]
            [wh.re-frame :refer [atom]]
            [wh.styles.payment :as styles]
            [wh.util :as util]))

(defn info-message-display
  [{:keys [info-message collapsed? restrict-packages mobile-fullscreen? mobile-selected-idx]}]
  [:div
   {:class (util/merge-classes "package-selector__columns package-selector__info-message columns"
                               (when @collapsed? "is-collapsed"))}
   [:div
    {:class (util/merge-classes "package-selector__column column"
                                (str "is-" (* 4 (count restrict-packages)))
                                (when (and mobile-fullscreen?
                                           (>= mobile-selected-idx (count restrict-packages)))
                                  "is-collapsed"))}
    [:div.package-selector__column-inner
     [icon "info"  :class "info-icon"]
     [icon "close"
      :class "close-icon"
      :on-click #?(:cljs (fn [_] (reset! collapsed? true)))]
     [:p info-message]]]])


(defn package-quota [{:keys [quota active-quota open? job-quota
                             coupon package billing-data]}]
  (let [checked    (= (:cost active-quota)
                      (:cost quota))
        quota-name (:name quota)]
    [:div {:class    (util/mc styles/job-quota
                              [checked styles/job-quota--checked])
           :on-click (fn []
                       (when @open?
                         (reset! job-quota quota))
                       (swap! open? not))}

     [:div (util/smc styles/job-quota__name)
      [:span (util/smc styles/job-quota__name__quantity) quota-name]
      [:span " live " (text/pluralize (:quota quota) "job")]]

     [:div {:class styles/package-selector__price}
      [:span (util/smc styles/package-selector__amount)
       (cost/int->dollars
         (cost/calculate-monthly-cost
           (:cost quota) (:discount billing-data) coupon))]

      (when (:per package)
        [:span (util/smc styles/package-selector__amount-per)
         (str "per " (:per package))])]]))

(defn package-quotas [quotas coupon package job-quota billing-data]
  (let [open? (atom false)]
    (fn [quotas coupon package job-quota billing-data]
      (let [active-quota @job-quota]
        [:div (util/smc styles/job-quota-list)
         [:div (util/smc styles/job-quota-list__content
                         [@open? styles/job-quota-list__content--open])
          (for [quota quotas]
            [package-quota
             {:quota        quota
              :active-quota active-quota
              :open?        open?
              :job-quota    job-quota
              :coupon       coupon
              :package      package
              :billing-data billing-data}])]

         (when-let [discount (:discount billing-data)]
           [:div.package-selector__discount-badge
            [:span (str (int (* 100 discount)) "%")]
            [:span "off"]])

         [icon "arrow-down"
          :class styles/job-quota-list__arrow]]))))

(defn package-cost [package job-quota coupon billing-data]
  (let [quotas (:job-quotas package)
        cost   (:cost package)]
    (cond
      quotas [package-quotas quotas coupon package job-quota billing-data]

      cost [:div {:class styles/package-selector__price}
            [:span.package-selector__amount
             (if (zero? cost)
               "Free"
               (cost/int->dollars
                 (cost/calculate-monthly-cost cost (:discount billing-data) coupon)))]

            (when (:per package)
              [:span (util/smc styles/package-selector__amount-per)
               (str "per " (:per package))])

            [:span (util/smc styles/package-selector__amount-billed)
             (when (pos? cost) (:description billing-data))]

            (when (pos? cost)
              (when-let [discount (:discount billing-data)]
                [:div.package-selector__discount-badge
                 [:span (str (int (* 100 discount)) "%")]
                 [:span "off"]]))]

      :else [:div.package-selector__get-in-touch
             [:span "Contact us so we can discuss your specific requirements"]])))


(defn package
  [{:as _args}]
  (let [job-quota (atom (get-in data/package-data [:launch_pad :job-quotas 0]))]
    (fn [{:keys [id package style restricted? coupon current? show-trials? show-extras?
                billing-period signup-button contact-button]}]
      (let [billing-data (get billing-data billing-period)]
        [:div
         {:key   id
          :class (util/merge-classes "package-selector__column column"
                                     (when restricted? "package-selector__restricted"))
          :style style}
         [:div.package-selector__column-inner

          [:div (util/smc styles/package-selector__header)
           [:div.package-selector__logo
            [:img (:img package)]]
           [:h2.package-selector__title (:name package)]

           [package-cost package job-quota coupon billing-data]

           (cond
             current?
             [:div.package-selector__current-plan "Current Plan"]
             restricted?
             [:div.package-selector__button
              [:div.button.package-selector__button--restricted "Unavailable"]]
             :else

             [:div.package-selector__button
              (if (:job-quotas package)
                (signup-button
                  (or (:button-alt package) (:button package))
                  id billing-period (:id @job-quota))
                (contact-button
                  true (or (:button-alt package) (:button package)) id billing-period))])]

          [:div (util/smc styles/package-selector__top-perks)
           (let [live-jobs (or (:live-jobs package)
                               (:name @job-quota))]
             [:div (util/smc styles/package-selector__top-perks__item)
              [:strong (util/smc styles/package-selector__top-perks__item__title)
               "Live jobs:"]
              live-jobs])]

          [:div.package-selector__perks
           (for [perk all-package-perks]
             ^{:key perk}
             [:div.package-selector__perk
              [:span
               {:class (when (contains? (:perks package) perk) "package-selector__perk--included")}
               perk]
              [icon (if (contains? (:perks package) perk) "cutout-tick" "cross")]])]
          (when show-trials?
            (when-let [trial (:trial package)]
              [:div.package-selector__trial (str trial "-day free trial")]))
          (when show-extras?
            (when-let [extra (:extra package)]
              [:div.package-selector__trial extra]))]]))))

(defn- package-selector-render
  [selected-billing-period mobile-selected-idx mobile-view-width info-message-collapsed?]
  (fn [{:keys [signup-button contact-button mobile-fullscreen? show-billing-period-selector? show-trials? show-extras?
              exclude-packages restrict-packages billing-period current-package coupon info-message package-data]
       :or   {mobile-fullscreen?            false
              show-billing-period-selector? true
              show-trials?                  true
              show-extras?                  true
              exclude-packages              #{}
              restrict-packages             #{}
              package-data                  data/package-data}}]
    (let [billing-period    (or billing-period @selected-billing-period)
          packages          (filter data/packages (clojure.set/difference (set (keys package-data)) (set exclude-packages)))
          selected-packages (into {} (map-indexed (fn [i [k v]] (vector k [v i]))
                                                  (sort-by (comp :order second) (select-keys package-data packages))))]
      [:div
       {:class (util/merge-classes "package-selector is-centered"
                                   (when mobile-fullscreen? "package-selector--mobile-fullscreen"))}
       (when show-billing-period-selector?
         [:div.package-selector__billing-period-selector
          [selector billing-period
           (reduce-kv (fn [a k v] (if (contains? data/billing-periods k)
                                   (assoc a k (:title v))
                                   a)) {} billing-data)
           (partial reset! selected-billing-period)]])
       [:div.package-selector__container
        [:div
         {:class (util/merge-classes "package-selector__columns columns"
                                     (when mobile-fullscreen? "is-mobile")
                                     (when show-billing-period-selector? "below-selector"))}
         (doall
           (for [[k [p idx]] selected-packages]
             ^{:key k}
             [package {:id             k
                       :package        p
                       :signup-button  signup-button
                       :contact-button contact-button
                       :show-trials?   show-trials?
                       :show-extras?   show-extras?
                       :current?       (= k current-package)
                       :restricted?    (contains? (set restrict-packages) k)
                       :coupon         coupon
                       :billing-period billing-period
                       :style          (if (and (zero? idx) mobile-fullscreen? @mobile-view-width)
                                         {:margin-left (* -1 (* @mobile-selected-idx @mobile-view-width))}
                                         {:margin-left 0})}]))]
        ;;
        (when (and (-> restrict-packages count pos?) info-message)
          [info-message-display
           {:info-message        info-message
            :collapsed?          info-message-collapsed?
            :restrict-packages   restrict-packages
            :mobile-fullscreen?  mobile-fullscreen?
            :mobile-selected-idx @mobile-selected-idx}])]
       ;;
       (when mobile-fullscreen?
         #?(:cljs
            [:div.package-selector__slide-buttons.is-hidden-desktop
             [:div.package-selector__slide-buttons-inner
              [:div {:class    (when-not (pos? @mobile-selected-idx) "hidden")
                     :on-click #(do (swap! mobile-selected-idx dec))}
               [icon "circle"] [icon "arrow-left"]]
              [:div {:class    (when (>= (inc @mobile-selected-idx) (count selected-packages)) "hidden")
                     :on-click #(do (swap! mobile-selected-idx inc))}
               [icon "circle"] [icon "arrow-right"]]]]
            :clj
            [:div.package-selector__slide-buttons-disabled.is-hidden-desktop
             [:div]]))])))

(defn package-selector
  [opts]
  #?(:clj (let [selected-billing-period (atom data/default-billing-period)
                mobile-selected-idx     (atom 0)
                mobile-view-width       (atom nil)
                info-message-collapsed? (atom false)]
            ((package-selector-render selected-billing-period
                                      mobile-selected-idx
                                      mobile-view-width
                                      info-message-collapsed?) opts))
     :cljs (let [selected-billing-period (r/atom data/default-billing-period)
                 mobile-selected-idx     (r/atom 0)
                 mobile-view-width       (atom nil)
                 info-message-collapsed? (r/atom false)
                 reset-mobile-view-width!
                 (fn [_] (reset! mobile-view-width (.-offsetWidth (aget (.getElementsByClassName js/document "package-selector__columns") 0))))]
             (r/create-class
               {:component-did-mount  reset-mobile-view-width!
                :component-did-update reset-mobile-view-width!
                :reagent-render       (package-selector-render selected-billing-period
                                                               mobile-selected-idx
                                                               mobile-view-width
                                                               info-message-collapsed?)}))))
