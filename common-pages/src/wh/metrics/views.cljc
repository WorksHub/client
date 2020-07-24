(ns wh.metrics.views
  (:require
    #?(:clj [cheshire.core :as json])
    #?(:clj [wh.url :as url])
    [clojure.string :as str]
    [wh.components.forms :as forms]
    [wh.components.www-homepage :as www]
    [wh.interop :refer [unparse-arg]]
    [wh.metrics.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]))

(defn time-period-button [vertical time-period]
  #?(:clj
     (let [url (url/get-url (or vertical "www") :metrics :query-params {:time-period (name time-period)})
           label (str (str/capitalize (name time-period)) "ly metrics")]
       {:id    time-period
        :label [:span label]
        :href  url})))

(defn chart-config [admin? data]
  {:type    "line"

   :data    {:labels   (mapv :month data)
             :datasets [{:label           "Registrations",
                         :backgroundColor "#ff7c6e"
                         :borderColor     "#ff7c6e"
                         :data            (mapv :registrations data)
                         :fill            false}
                        {:label           "Approvals",
                         :backgroundColor "#ffb1a8"
                         :borderColor     "#ffb1a8"
                         :data            (mapv :approvals data)
                         :fill            false}
                        {:label           "Outstanding",
                         :backgroundColor "#ffdedb"
                         :borderColor     "#ffdedb"
                         :data            (mapv :outstanding data)
                         :fill            false}
                        {:label           (if admin? "Applied" "Applications"),
                         :backgroundColor "#7eb5bd"
                         :borderColor     "#7eb5bd"
                         :data            (mapv (if admin? :users-applied :total-applications) data)
                         :fill            false}]}
   :options {:responsive true
             :tooltips   {:mode      "index"
                          :intersect true}
             :hover      {:mode      "nearest"
                          :intersect true}
             :scales     {:xAxes [{:display    true}]
                          :yAxes [{:display    true}]}}})

(defn growth-chart-config [data]
  {:type    "line"
   :data    {:labels   (mapv :month data)
             :datasets [{:label           "Monthly growth of approved candidates",
                         :backgroundColor "rgb(54, 162, 235)"
                         :borderColor     "rgb(54, 162, 235)"
                         :data            (mapv :growth data)
                         :fill            false}]}
   :options {:responsive true
             :tooltips   {:mode      "index"
                          :intersect true}
             :hover      {:mode      "nearest"
                          :intersect true}
             :scales     {:xAxes [{:display    true
                                   :scaleLabel {:display     true
                                                :labelString "Month"}}]
                          :yAxes [{:display    true
                                   :scaleLabel {:display     true
                                                :labelString "Growth %"}}]}}})


(defn generate-row
  [admin?
   {:keys [month registrations approvals outstanding users-applied
           total-applications apps-per-applicant apps-per-job]}]
  [:tr
   [:th {:scope "row"} month]
   [:td registrations]
   [:td approvals]
   [:td outstanding]
   (when admin? [:td users-applied])
   [:td total-applications]
   (when admin? [:td apps-per-applicant])
   [:td apps-per-job]])

(defn candidate-table [admin?]
  [:div.table-container
   [:table.table.is-bordered.is-hoverable.is-fullwidth
    [:thead
     [:tr
      [:th (str/capitalize (name (<sub [::subs/time-period])))]
      [:th "Registrations"]
      [:th "Approvals"]
      [:th "Outstanding"]
      (when admin? [:th "Users applied"])
      [:th "Applications"]
      (when admin? [:th "# of applications per applicant"])
      [:th "Applications per job"]]]
    [:tbody
     (for [{:keys [month] :as row-data} (reverse (<sub [::subs/candidate-data]))]
       ^{:key month}
       (generate-row admin? row-data))]]])

(defn generate-company-row
  [{:keys [period registrations-count launchpad-count take-off-count]}]
  [:tr
   [:th {:scope "row"} period]
   [:td registrations-count]
   [:td launchpad-count]
   [:td take-off-count]])

(defn company-table []
  [:table.table.is-bordered.is-hoverable
   [:thead
    [:tr
     [:th "Period"]
     [:th "Registrations"]
     [:th "Launchpad"]
     [:th "Take-off"]]]
   [:tbody
    (for [{:keys [period] :as row-data} (<sub [::subs/company-data])]
      ^{:key period}
      (generate-company-row row-data))]])

(defn init-charts [admin?]
  #?(:clj
     [:div
      [:script#candidate-chart-data
       (str "var candidateChartConfig='" (json/generate-string (chart-config admin? (take 12 (<sub [::subs/candidate-data])))) "';")]
      (when admin?
        [:script#growth-chart-data
         (str "var growthChartConfig='" (json/generate-string (growth-chart-config (take 12 (<sub [::subs/candidate-data])))) "';")])]))

(defn change-to-url
  [options]
  #?(:clj (str "let v=" (str (unparse-arg (map :url options)) "[this.value];")
               "window.location.href = " 'v)))

(defn vertical-selector []
  [forms/select-field
   {:solo?     true
    :value     (keyword (<sub [:wh/vertical]))
    :class     "companies__sorting__dropdown"
    :options   (<sub [::subs/vertical-options])
    :on-change (change-to-url (<sub [::subs/vertical-options]))}])

(defn page []
  (let [selected-vertical (<sub [:wh/vertical])
        admin? (<sub [:user/admin?])]
    [:div
     [:div.public-content
      [:h2.public__subtitle "WORKSHUB METRICS"]
      [:div.public__header-selector-wrapper
       [:h1.public__title "See how we are growing"]
       [:div.public__header-selector
        [forms/fake-radio-buttons
         (<sub [::subs/time-period])
         (map (partial time-period-button selected-vertical) [:week :month])]]]]
     [init-charts admin?]
     (when admin?
       [:div.public-content
        [vertical-selector]
        [:h3 "Total number of approved candidates: " (<sub [::subs/total-registrations])]
        [:canvas#growthChart]])
     [:div.public-content
      [:canvas#candidateChart]
      [:h2.public__title "Software Developers on WorkHubs"]
      [candidate-table admin?]]
     (when admin?
       [:div.public-content
        [:h2.public__title "Companies"]
        [company-table]])
     [www/animated-hr "/images/homepage/globe.svg" "homepage__animated-hr__globe"]
     [:div.public-content.has-text-centered
      [www/testimonials]]]))