(ns wh.job.components
  (:require #?(:cljs [wh.components.modal :as modal])
            [wh.interop :as interop]
            [wh.re-frame.events :refer [dispatch dispatch-sync]]
            [wh.re-frame.subs :refer [<sub]]))

(defn publish-button [{:keys [on-click publishing?]}]
  [:button.button.button--medium
   (merge {:id    "job-view__publish-button"
           :class (when publishing? "button--inverted button--loading")}
          #?(:cljs {:on-click #(dispatch on-click)}))
   "Publish"])

(defn view-applications-button [{:keys [href]}]
  [:a.button.button--medium
   {:href href
    :id   "job-view__view-applications-button"}
   "View Applications"])

(defn apply-button [{:keys [applied? id job]}]
  (let [company?           (<sub [:user/company?])
        logged-in?         (<sub [:user/logged-in?])
        applied-jobs       (<sub [:user/applied-jobs])
        show-auth-popup-fn (interop/show-auth-popup :jobpage-apply
                                                    [:job
                                                     :params {:slug (:slug job)}
                                                     :query-params {:apply "true"}])]
    [:button.button.button--medium
     (if logged-in?
       {:id       (cond-> "job-view__apply-button" id (str "__" id))
        :disabled (or applied? company?)
        :on-click #(dispatch [:apply/try-apply job :jobpage-apply])}
       (merge {:id (cond-> "job-view__logged-out-apply-button" id (str "__" id))}
              (interop/on-click-fn show-auth-popup-fn)))
     (cond applied?             "Applied"
           (some? applied-jobs) "Instant Apply"
           :else                "Apply")]))

(defn more-jobs-link [{:keys [href condensed? company-name]}]
  [:a.button.button--medium.button--inverted.button--ellipsis {:href href}
   (if condensed?
     "See more"
     (str "More jobs from " company-name))])

(defn about-company-link [{:keys [href company-name]}]
  [:a.button.button--medium.button--inverted.button--ellipsis {:href href}
   "About " company-name])

