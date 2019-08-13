(ns wh.components.menu
  (:require
    [clojure.string :as str]
    [wh.common.data :as data]
    [wh.components.common :refer [link]]
    [wh.components.icons :refer [icon]]
    [wh.interop :as interop]
    [wh.util :as util]))

(def logged-in-menu-id "logged-in-menu")

(defn probably-homepage-match?
  [target current-page]
  (let [contains-homepage? (fn [x] (and x (str/includes? (name x) "homepage")))
        homepage-target? (if (coll? target)
                           (some contains-homepage? target)
                           (contains-homepage? target))]
    (and homepage-target?
         (contains-homepage? current-page))))

(defn menu-item [current-page target icon-name & description]
  (let [[target target-link] (if (coll? target) target [target target])
        link-opts (interop/multiple-on-click (interop/set-is-open-on-click logged-in-menu-id false)
                                             (interop/disable-no-scroll-on-click))]
    [:li (merge {:key icon-name}
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

(defn render-menu
  [data current-page restricted-links]
  (let [item (partial menu-item current-page)]
    [:nav.menu.wh-menu
     (for [{:keys [section class items]} data]
       [:section
        {:class class
         :key section}
        [:header section]
        [:ul
         (for [i items]
           (if (and restricted-links (restricted-links (first i)))
             (apply item (assoc i 0 nil))
             (apply item i)))]])]))

(defn menu
  [type user current-page restricted-links]
  [:div.menu-container
   {:id logged-in-menu-id}
   (render-menu (data/menu type user) current-page restricted-links)])
