(ns wh.components.tracking-popup
  (:require [wh.interop :as interop]))

(defn popup []
  [:div.tracking-popups
   {:id "tracking-popups"}
   [:div.tracking-popup.is-hidden-mobile
    [:p "We use cookies and other tracking technologies to improve your browsing experience on our site, analyze site traffic,
    and understand where our audience is coming from. To find out more, please read our "
     [:a.a--underlined {:href   "/privacy-policy"
                        :target "_blank"
                        :rel    "noopener"}
      "privacy policy."]]
    [:p "By choosing 'I Accept', you consent to our use of cookies and other tracking technologies."]
    [:button.button (interop/agree-to-tracking-on-click)
     "I Accept"]]
   [:div.tracking-popup.is-hidden-desktop
    [:div.tracking-popup--expanded
     {:id "tracking-popup-mobile--expanded"}
     [:p "We use cookies and other tracking technologies to improve your browsing experience on our site, analyze site traffic,
    and understand where our audience is coming from. To find out more, please read our "
      [:a.a--underlined {:href   "/privacy-policy"
                         :target "_blank"
                         :rel    "noopener"}
       "privacy policy."]]
     [:p
      "By choosing 'I Accept', you consent to our use of cookies and other tracking technologies. "
      [:a.a--underlined (interop/multiple-on-click
                         (interop/set-is-open-on-click "tracking-popup-mobile--expanded" false)
                         (interop/set-is-open-on-click "tracking-popup-mobile--collapsed" true)) "Less"]]
     [:button.button (interop/agree-to-tracking-on-click) "I Accept"]]
    [:div.tracking-popup--collapsed.is-open
     {:id "tracking-popup-mobile--collapsed"}
     [:p "We use cookies and other tracking technologies... "
      [:a.a--underlined (interop/multiple-on-click
                         (interop/set-is-open-on-click "tracking-popup-mobile--expanded" true)
                         (interop/set-is-open-on-click "tracking-popup-mobile--collapsed" false)) "More"]]
     [:button.button (interop/agree-to-tracking-on-click) "I Accept"]]]])
