(ns wh.components.modal
  (:require ["react-modal" :as ReactModal]
            [reagent.core :as reagent]
            [wh.components.icons :as icons]
            [wh.styles.modal :as styles]
            [wh.util :as util]))

(def react-modal (reagent/adapt-react-class ReactModal))
(.setAppElement ReactModal "#app")

(defn close-modal [{:keys [on-close]}]
  [:button
   {:on-click on-close
    :class styles/close__wrapper}
   [icons/icon "close" :class styles/close]])

(defn header [{:keys [on-close title]}]
  [:div
   {:class styles/header}
   title
   [close-modal {:on-close on-close}]])

(defn footer [& children]
  (into [:div {:class styles/footer}] children))

(defn body [& children]
  (into [:div {:class styles/body}] children))

(defn button [{:keys [text on-click type]}]
  [:button {:class (util/mc styles/button [(= type :secondary) styles/button--secondary])
            :on-click on-click} text])

(def modal-wrapper-id "modal-wrapper")

(defn modal
  [{:keys [open? on-request-close id label]
    :or {id "modal"}
    :as _opts}
   & children]
  [:div {:id modal-wrapper-id}
   (into [react-modal {:is-open open?
                       :class-name styles/modal
                       :overlay-class-name styles/overlay
                       :content-label label
                       :id id
                       :on-after-open #(js/window.toggleNoScroll modal-wrapper-id)
                       :on-after-close #(js/window.toggleNoScroll modal-wrapper-id)
                       :on-request-close on-request-close}]
         children)])
