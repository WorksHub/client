(ns wh.components.navbar.components
  (:require [clojure.string :as str]
            [wh.components.icons :as icons :refer [icon]]
            [wh.interop :as interop]
            [wh.routes :as routes]
            [wh.styles.navbar :as styles]
            [wh.util :as util]))

(defn arrow-down []
  [icon "arrow-down" :class styles/arrow-down])

(defn dropdown-element [{:keys [route path icon-name icon-class
                                text sub-text data-pushy-ignore]}
                        {:keys [mobile?]}]
  [:li (merge (util/smc styles/dropdown__element)
              ;; Close mobile menu when you click link in the menu
              (when mobile?
                (interop/on-click-fn #?(:clj "toggleMenuDisplay()"
                                        :cljs #(js/toggleMenuDisplay)))))
   [:a (merge
         {:class (util/mc styles/dropdown__link)
          :href  (or path (routes/path route))}
         (when data-pushy-ignore {:data-pushy-ignore data-pushy-ignore}))
    [icons/icon icon-name
     :class (util/mc styles/dropdown__link__icon icon-class)]

    [:span (util/smc styles/dropdown__link__text) text]

    (when sub-text
      [:span (util/smc styles/dropdown__link__sub-text) sub-text])]])

(defn dropdown-list
  ([dropdown]
   [dropdown-list dropdown {}])

  ([dropdown opts]
   [:ul (util/smc styles/dropdown)
    (for [{:keys [path route] :as el} dropdown]
      ^{:key (or path route)}
      [dropdown-element el opts])]))

(defn link
  ([element]
   [link element {}])

  ([{:keys [children text path route page dropdown data-pushy-ignore]} opts]
   [:li (util/smc styles/link__wrapper [(= page route) styles/link__wrapper-active])
    [:a (merge
          {:href      (or path (routes/path route))
           :class     styles/link
           :data-test (name (or path route))}
          (when data-pushy-ignore {:data-pushy-ignore data-pushy-ignore}))
     (if dropdown
       [:span (util/smc styles/link--with-dropdown)
        [:span (or children text)]
        [icon "arrow-down" :class styles/arrow-down]]

       (or children text))]

    (when dropdown
      [dropdown-list dropdown opts])]))

(defn link-with-icon
  ([element]
   [link-with-icon element {}])
  ([{:keys [text route icon-name data-pushy-ignore]} {:keys [mobile?]}]
   [:li (merge
          (util/smc styles/link__wrapper)
          ;; Close mobile menu when you click link in the menu
          (when mobile?
            (interop/on-click-fn #?(:clj "toggleMenuDisplay()"
                                    :cljs #(js/toggleMenuDisplay)))))
    [:a (merge
          {:href  (routes/path route)
           :class (util/mc styles/link styles/link--with-icon)}
          (when data-pushy-ignore {:data-pushy-ignore data-pushy-ignore}))

     [icon icon-name :class styles/link__icon]
     [:span text]]]))

(defn submenu [{:keys [text dropdown icon-name]}]
  (let [label (str "submenu__" (str/lower-case text))]
    [:div {:class styles/submenu}
     [:input {:type  "checkbox" :id label
              :class styles/submenu__checkbox}]

     [:label {:class styles/submenu__title
              :for   label}
      [icon icon-name :class styles/link__icon]
      [:span (util/smc styles/link--with-dropdown) text]
      [arrow-down]]

     [dropdown-list dropdown {:mobile? true}]]))
