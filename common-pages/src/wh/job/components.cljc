(ns wh.job.components
  (:require #?(:cljs [wh.components.modal :as modal])
            [wh.routes :as routes]
            [wh.re-frame.events :refer [dispatch dispatch-sync]]
            [wh.re-frame.subs :refer [<sub]]))

(defn publish-button [{:keys [on-click publishing?]}]
  [:button.button.button--medium
   (merge {:id        "job-view__publish-button"
           :class     (when publishing? "button--inverted button--loading")
           :data-test "publish-job"}
          #?(:cljs {:on-click #(dispatch on-click)}))
   "Publish"])

(defn view-applications-button [{:keys [href]}]
  [:a.button.button--medium
   {:href      href
    :data-test "view-applications"
    :id        "job-view__view-applications-button"}
   "View Applications"])

(defn apply-button-options [{:keys [applied? _id job]}]
  (let [company?           (<sub [:user/company?])
        applied-jobs       (<sub [:user/applied-jobs])
        options            (merge {:on-click #(dispatch [:apply/try-apply job :jobpage-apply])}
                                  (when (or applied? company?) {:disabled (or applied? company?)}))

        text (cond applied?             "Applied"
                   (some? applied-jobs) "Instant Apply"
                   :else                "Apply")]
    {:text text
     :options options}))

(defn apply-button [{:keys [_applied? id job] :as args}]
  (let [{:keys [text options]} (apply-button-options args)]
    [:a.button.button--medium 
     (merge
       {:id        (cond-> "job-view__apply-button" id (str "__" id))
        :data-test "job-apply"
        :href      (routes/path :job :params {:slug (:slug job)}
                                 :query-params {:interaction 1 :apply true})}
       options)
     text]))

(defn more-jobs-link [{:keys [href condensed? company-name]}]
  [:a.button.button--medium.button--inverted.button--ellipsis {:href href}
   (if condensed?
     "See more"
     (str "More jobs from " company-name))])

(defn about-company-link [{:keys [href company-name]}]
  [:a.button.button--medium.button--inverted.button--ellipsis {:href href}
   "About " company-name])

