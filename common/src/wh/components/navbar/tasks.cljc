(ns wh.components.navbar.tasks
  (:require #?(:cljs [reagent.core :as r])
            [wh.common.data :as data]
            [wh.common.data.company-profile :as company-data]
            [wh.styles.navbar :as styles]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.re-frame.events :refer [dispatch]]
            [wh.re-frame.subs :refer [<sub]]
            [wh.util :as util]))

(defn unfinished-task-count
  []
  (->> company-data/company-onboarding-tasks
       (keys)
       (remove #(= :complete (<sub [:user/company-onboarding-task-state %])))
       (count)))


(defn task->path
  [task]
  (case task
    :complete_profile [:company :slug (some-> (<sub [:user/company]) :slug)]
    :add_job          [:create-job]
    :add_integration  [:edit-company]
    :add_issue        [:company-issues]))

(defn task
  [id {:keys [title subtitle] :as task} tasks-open?]
  (let [state (<sub [:user/company-onboarding-task-state id])
        [handler & {:as link-options}] (task->path id)]
    [:div
     {:key id
      :class (util/merge-classes "navbar__task"
                                 (when state (str "navbar__task--" (name state))))}
     [link {:text [:div
                   [:div.is-flex
                    [icon (:icon task)]
                    [:div.title title
                     [icon "cutout-tick"]]]
                   [:p subtitle]]
            :handler handler
            :options (assoc link-options
                            :on-click #(do (reset! tasks-open? false)
                                           #?(:cljs (js/setClass data/logged-in-menu-id "is-open" false))
                                           #?(:cljs (js/disableNoScroll))
                                           (dispatch [:company/set-task-as-read id])))}]]))

(defn task-notifications-content
  [tasks-open?]
  [:div.navbar-overlay__content
   [:div.navbar__tasks-header
    "Get more out of WorksHub"]
   (doall
     (for [[id t] company-data/company-onboarding-tasks]
       [:div.navbar__task-container
        {:key id}
        [task id t tasks-open?]]))])

(defn tasks [tasks-open?]
  [:div.navbar__tasks__inner
   [:div.navbar-overlay__inner
    [:div.navbar-overlay__bg]
    [task-notifications-content tasks-open?]
    [:div.navbar-overlay__triangle]]])

(defn tasks-notifications [{:keys [class]}]
  #?(:cljs
     (r/with-let [open?       (r/atom false)
                  !ref        (r/atom nil)
                  ;; Implements closing on click outside element
                  handler     (fn [e]
                                (when-not (.contains @!ref (.-target e))
                                  (reset! open? false)))
                  unfinished? (pos? (unfinished-task-count))
                  _           (.addEventListener js/document "click" handler)]
       [:div {:class (util/mc styles/notification class)
              :ref   #(reset! !ref %)}

        [icon "bell"
         :class styles/bell-icon
         :on-click #(swap! open? not)]

        (when unfinished?
          [:div (util/smc styles/unfinished-marker)])

        (when @open? [tasks open?])]

       (finally
         (.removeEventListener js/document "click" handler)))))
