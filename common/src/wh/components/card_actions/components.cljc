(ns wh.components.card-actions.components
  (:require [wh.common.text :as text]
            [wh.components.icons :as icons]
            [wh.interop :as interop]
            [wh.styles.card-actions :as styles]
            [wh.util :as util]
            [wh.verticals :as verticals]))

;; TODO, ch5663, reuse share-urls function
(defn share-controls
  [share-opts share-id share-toggle-on-click]
  (let [social-button-opts (merge share-toggle-on-click
                                  {:class  styles/actions__share-button
                                   :rel    "noopener"
                                   :target "_blank"})]
    [:div (util/smc styles/actions__share)
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://twitter.com/intent/tweet?text=Check out %s at %s %s"
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :twitter)
                      (:url share-opts))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-twitter.svg"}]]
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://www.linkedin.com/shareArticle?mini=true&url=%s&title=%s&summary=Check+out+%s+at+%s&source=%s"
                      (:url share-opts)
                      (:content-title share-opts)
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :platform-name)
                      (verticals/config (:vertical share-opts) :platform-name))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-linkedin.svg"}]]
     [:a
      (merge social-button-opts
             {:href (text/format
                      "https://www.facebook.com/dialog/share?app_id=%s&display=popup&href=%s&quote=Check+out+%s+at+%s"
                      (:facebook-app-id share-opts)
                      (:url share-opts)
                      (:content share-opts)
                      (verticals/config (:vertical share-opts) :platform-name))})
      [:img {:class styles/actions__share-image
             :src   "/images/share-facebook.svg"}]]
     [:div (merge (interop/multiple-on-click
                    share-toggle-on-click
                    (interop/copy-str-to-clipboard-on-click (:url share-opts)))
                  (util/smc styles/actions__share-button styles/actions__share-button--center))
      [icons/icon "copy"]]
     [:a (merge share-toggle-on-click
                (util/smc styles/actions__share-button))
      [icons/icon "close"]]]))

(defn actions [{:keys [save-opts share-opts saved? class-wrapper]}]
  (let [share-id              (str (gensym "actions") "-" (:id share-opts))
        share-toggle-on-click (interop/toggle-class-on-click share-id styles/actions__inner--open)]
    [:div (util/smc styles/actions class-wrapper)
     [:div {:class (util/mc styles/actions__inner)
            :id    share-id}
      ;;
      (if share-opts
        [share-controls share-opts share-id share-toggle-on-click]
        [:div "Hidden"])
      ;;
      [:div (util/smc styles/actions__container)
       (when save-opts
         [:button (merge save-opts (util/smc styles/actions__action
                                             styles/actions__action--save))
          [icons/icon "save"
           :class (when saved? styles/actions__action--saved-icon)]])
       (when share-opts
         [:button (merge (util/smc styles/actions__action
                                   styles/actions__action--share)
                         share-toggle-on-click)
          [icons/icon "network"]])]]]))
