(ns wh.components.navbar.tasks
  (:require #?(:cljs [reagent.core :as r])
            [wh.common.data :as data]
            [wh.common.data.company-profile :as company-data]
            [wh.styles.navbar :as navbar-styles]
            [wh.styles.tasks :as styles]
            [wh.components.common :refer [link]]
            [wh.components.icons :as icon]
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

(defn task-description [{:keys [icon title subtitle] :as _task} state]
  (let [icon (if (#{:read :complete} state) "cutout-tick" icon)]
    [:div {:class styles/task}
     [icon/icon icon :class styles/task__icon]
     [:div {:class styles/title} title]
     [:p {:class styles/subtitle} subtitle]]))

(defn task-wrapper
  [id task tasks-open?]
  (let [state (<sub [:user/company-onboarding-task-state id])
        [handler & {:as link-options}] (task->path id)]
    [link {:text [task-description task state]
           :handler handler
           :options (assoc link-options
                           :on-click #(do (reset! tasks-open? false)
                                          #?(:cljs (js/setClass data/logged-in-menu-id "is-open" false))
                                          #?(:cljs (js/disableNoScroll))
                                          (dispatch [:company/set-task-as-read id])))}]))

(defn task-notifications-content
  [tasks-open?]
  [:div {:class styles/tasks__wrapper}
   [:div {:class styles/header}
    "Explore WorksHub"]
   [:ul {:class styles/tasks}
    (doall
      (for [[id t] company-data/company-onboarding-tasks]
        ^{:key id}
        [:li [task-wrapper id t tasks-open?]]))]])

(defn tasks [tasks-open?]
  [:div {:class styles/content}
   [task-notifications-content tasks-open?]])


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
       [:div {:class (util/mc navbar-styles/notification class)
              :ref   #(reset! !ref %)}

        [icon/icon "bell"
         :class navbar-styles/bell-icon
         :on-click #(swap! open? not)]

        (when unfinished?
          [:div (util/smc navbar-styles/unfinished-marker)])

        (when @open? [tasks open?])]

       (finally
         (.removeEventListener js/document "click" handler)))))
