(ns wh.components.menu
  (:require [clojure.string :as str]
            [wh.common.data :as data]
            [wh.common.data.company-profile :as company-data]
            [wh.common.user :as user-common]
            [wh.components.common :refer [link]]
            [wh.components.icons :refer [icon]]
            [wh.interop :as interop]
            [wh.re-frame.subs :refer [<sub]]
            [wh.util :as util]))

(defn probably-homepage-match?
  [target current-page]
  (let [contains-homepage? (fn [x] (and x (str/includes? (some-> x name) "homepage")))
        homepage-target?   (if (coll? target)
                             (some contains-homepage? target)
                             (contains-homepage? target))]
    (and homepage-target?
         (contains-homepage? current-page))))

(defn menu-item [current-page target icon-name & description]
  (let [[target target-link] (if (coll? target) target [target target])
        link-opts            (interop/multiple-on-click (interop/set-is-open-on-click data/logged-in-menu-id false)
                                                        (interop/disable-no-scroll-on-click))]
    [:li (merge {:key (str (some-> target name) "_" icon-name)}
                (when (or (= target current-page)
                          (and (probably-homepage-match? target current-page)))
                  {:class "current"}))
     (cond
       (string? target)
       [:a (merge link-opts {:href target})
        (into [:span (icon icon-name)] description)]
       (and target (keyword target-link))
       (link
         (into [:span (icon icon-name)] description)
         target-link
         :on-click (first (vals link-opts)))
       (and target (coll? target-link))
       (apply link
              (into [:span (icon icon-name)] description)
              target
              :on-click (first (vals link-opts))
              target-link)
       :else
       [:a.disabled (into [:span (icon icon-name)] description)])]))

(defn notifications-header
  [tasks-header-id tab-opts]
  (let [utc (->> company-data/company-onboarding-tasks
                 (keys)
                 (remove #(= :complete (<sub [:user/company-onboarding-task-state %])))
                 (count))]
    [:header.menu__notifications-header.is-hidden-desktop
     (merge {:id tasks-header-id}
            tab-opts)
     (str "Notifications" (when (pos? utc)
                            (str " (" utc ")")))]))

(defn render-menu
  [data current-page restricted-links]
  (let [item (partial menu-item current-page)]
    [:nav.menu.wh-menu
     (doall
       (for [{:keys [section class items]} data]
         (let [list-id              (str "menu__list__" section)
               tasks-header-id      "menu__list__tasks-header"
               tasks-id             "menu__list__tasks"
               toggle-section-class "menu__section--no-notifications"
               tab-opts-fn          (fn [show?] (interop/multiple-on-click
                                                 (interop/set-is-open-class-on-click toggle-section-class (not show?))
                                                 (interop/set-is-open-on-click list-id (not show?))
                                                 (interop/set-is-open-on-click tasks-id show?)
                                                 (interop/set-is-open-on-click tasks-header-id show?)))]
           [:section
            {:class (util/merge-classes class
                                        "menu__section"
                                        "is-open")
             :key   section}
            [:ul.menu__list.is-open
             {:id list-id}
             (for [i items]
               (if (and restricted-links (restricted-links (first i)))
                 (apply item (assoc i 0 nil))
                 (apply item i)))]])))]))

(defn menu [type user current-page restricted-links]
  (when-not (or
              (user-common/candidate-type? type)
              (user-common/company-type? type))
    [:div.menu-container
     {:id data/logged-in-menu-id}
     [render-menu (data/menu type user) current-page restricted-links]]))
