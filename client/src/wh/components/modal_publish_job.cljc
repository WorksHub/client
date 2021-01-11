(ns wh.components.modal-publish-job
  (:require [re-frame.core :refer [reg-event-db reg-sub]]
            #?(:cljs [wh.components.modal :as modal])
            [wh.re-frame.subs :refer [<sub]]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.common.data :as data]
            [wh.styles.modal-publish-job :as styles]
            [wh.util :as util]))

(def path [:wh.components.modal-publish-job :open?])

(defn toggle-modal [db]
  (update-in db path not))

(defn close-modal [db]
  (assoc-in db path false))

(defn opened? [db]
  (boolean (get-in db path)))

;; events -----------------
;; we don't want a separate namespace for them it would be too much

(reg-event-db
  ::toggle-modal
  toggle-modal)

(reg-sub
  ::open?
  (fn [db _]
    (boolean (get-in db path))))

;; perk lists -----------------

(def free-package
  (let [package (:explore data/package-data)]
    {:name (:name package)
     :perks (:perks package)
     :price "Free"}))

(def upgraded-package
  (let [package (:launch_pad data/package-data)
        price (->> package
                   :job-quotas
                   (map :cost)
                   (apply min))
        discount (get-in data/billing-data [:six :discount])
        price-with-discount (- price (* price discount))]
    {:name (:name package)
     :perks (clojure.set/difference
              (:perks package)
              (:perks free-package))
     :price (str "$" price-with-discount)}))

;; components -----------------

(defn title []
  [:span {:class styles/title}
   "Upgrade for future edits"])

(defn explanation []
  [:div {:class styles/explanation__wrapper}
   [:p {:class styles/explanation}
    "You will not be able to make further changes to this job until your package is upgraded."]])

(defn package-name [{:keys [current? name highlighted?]}]
  [:div
   [:div {:class styles/secondary-text} (if current? "current package" "package")]
   [:div (util/smc styles/package-name [highlighted? styles/package-name--highlighted])
    name]])

(defn price [{:keys [price period multiple-options?]}]
  [:div {:class styles/price__wrapper}
   (if multiple-options?
     [:div {:class styles/secondary-text} "starting at"]
     [:div])
   [:div {:class styles/price} price]
   (if period
     [:div {:class styles/secondary-text} period]
     [:div])])

(defn perk-list [{:keys [perks highlighted?]}]
  [:div
   (into
     [:ul (util/smc styles/perks [(not highlighted?) styles/perks--hide-one-perk])]
     (for [perk perks]
       [:li {:class styles/perk}
        [icon "tick"
         :class (util/mc styles/perk__icon [highlighted? styles/perk__icon--highlighted])]
        perk]))])

(defn cta [{:keys [cta cta-info highlighted? on-click button-data-test]}]
  [:div {:class styles/button__wrapper}
   [:button {:class     (util/mc styles/button [highlighted? styles/button--highlighted])
             :on-click  on-click
             :data-test button-data-test}
    cta]
   (when cta-info
     [:span (util/smc styles/secondary-text styles/button__additional-info)
      cta-info])])

(defn package [{:keys [highlighted?] :as opts}]
  [:div (util/smc styles/package [highlighted? styles/package--highlighted])
   [package-name opts]
   [price opts]
   [perk-list opts]
   [cta opts]])

(defn packages [{:keys [on-publish on-publish-and-upgrade]}]
  [:div {:class styles/packages}
   [package (merge
              free-package
              {:cta-info         "No further edits without an upgrade"
               :cta              "Publish"
               :on-click         on-publish
               :highlighted?     false
               :button-data-test "publish-without-upgrade"})]
   [package (merge
              upgraded-package
              {:period            "per month"
               :cta               "Publish & Upgrade"
               :on-click          on-publish-and-upgrade
               :highlighted?      true
               :multiple-options? true
               :button-data-test  "publish-with-upgrade"})]])

(defn paragraph [& children]
  (into [:div {:class styles/paragraph}] children))

(defn info []
  [:div {:class styles/info}
   [paragraph
    "Want to learn more about packages?"
    [link "Browse options"
     :payment-setup :step :select-package
     :class (util/mc styles/paragraph styles/paragraph--bold)]]
   [paragraph
    "Do you have any question?"
    [:a {:class (util/mc styles/paragraph styles/paragraph--bold)
         :href (str "mailto:" data/default-contact-email)}
     "Contact us"]]])

(defn modal [{:keys [on-close on-publish on-publish-and-upgrade]}]
  #?(:cljs [modal/modal {:open? (<sub [::open?])
                         :on-request-close on-close
                         :label "Editing will be blocked"
                         :class styles/modal}
            [modal/extended-body
             {:class styles/body}
             [:<>
              [modal/close-modal {:on-close on-close}]
              [title]
              [explanation]
              [packages {:on-publish on-publish
                         :on-publish-and-upgrade on-publish-and-upgrade}]
              [info]]]]
     :clj nil))

