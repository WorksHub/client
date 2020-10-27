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

(defn extended-body [{:keys [class] :as _opts} & children]
  (into [:div (util/smc styles/body class)] children))

(defn button [{:keys [text on-click type data-test]}]
  [:button {:class (util/mc styles/button [(= type :secondary) styles/button--secondary])
            :on-click on-click
            :data-test data-test} text])

(def modal-wrapper-id "modal-wrapper")

(defn modal
  []
  (let [;; without randomised id setNoScroll doesn't work correctly
        ;; if there are several modals on one page
        modal-wrapper-id' (str modal-wrapper-id (random-uuid))
        on-after-close #(js/window.setNoScroll modal-wrapper-id' false)
        on-after-open #(js/window.setNoScroll modal-wrapper-id' true)]
    (fn [{:keys [open? on-request-close id label class]
          :or {id "modal"}
          :as _opts}
         & children]
      [:div {:id modal-wrapper-id'}
       (into [react-modal {:is-open open?
                           :class-name (util/merge-classes styles/modal class)
                           :overlay-class-name styles/overlay
                           :content-label label
                           :id id
                           :on-after-open on-after-open
                           :on-after-close on-after-close
                           :on-request-close on-request-close}]
             children)])))
