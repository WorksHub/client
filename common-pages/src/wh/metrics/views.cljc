(ns wh.metrics.views
  (:require
    #?(:clj [cheshire.core :as json])
    #?(:clj [wh.url :as url])
    [wh.metrics.subs :as subs]
    [wh.re-frame.subs :refer [<sub]]
    [wh.verticals :as verticals]
    [clojure.string :as str]))

(defn time-period-button [vertical selected-time-period time-period]
  #?(:clj (let [url (url/get-url (or vertical "www" ) :metrics :query-params {:time-period (name time-period)})
        label (str/capitalize (name time-period))]
    [:a {:class (str "button button--small button--inline" (when (not= selected-time-period time-period) " button--inverted"))
         :href  url} label])))

(defn generate-tabs [selected-vertical vertical]
  #?(:clj (let [url (url/get-url vertical :metrics)]
            [:li {:class (str "tab " (when (= selected-vertical vertical) "tab--selected"))}
             [:a {:href url} (str/capitalize (if (= vertical "www") "All" vertical))]])))

(defn chart-config [data]
  {:type    "line"

   :data    {:labels   (mapv :month data)
             :datasets [{:label           "Registrations",
                         :backgroundColor "rgb(153, 102, 255)"
                         :borderColor     "rgb(153, 102, 255)"
                         :data            (mapv :registrations data)
                         :fill            false}
                        {:label           "Approvals",
                         :backgroundColor "rgb(54, 162, 235)"
                         :borderColor     "rgb(54, 162, 235)"
                         :data            (mapv :approvals data)
                         :fill            false}
                        {:label           "Outstanding",
                         :backgroundColor "rgb(255, 99, 132)"
                         :borderColor     "rgb(255, 99, 132)"
                         :data            (mapv :outstanding data)
                         :fill            false}
                        {:label           "Applied",
                         :backgroundColor "rgb(75, 192, 192)"
                         :borderColor     "rgb(75, 192, 192)"
                         :data            (mapv :users-applied data)
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
                                                :labelString "Users"}}]}}})

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
  [{:keys [month registrations approvals outstanding users-applied
           total-applications apps-per-applicant apps-per-job]}]
  [:tr
   [:th {:scope "row"} month]
   [:td registrations]
   [:td approvals]
   [:td outstanding]
   [:td users-applied]
   [:td total-applications]
   [:td apps-per-applicant]
   [:td apps-per-job]])

(defn candidate-table []
  [:table.table.is-bordered.is-hoverable
   [:thead
    [:tr
     [:th "Month"]
     [:th "Registrations"]
     [:th "Approvals"]
     [:th "Outstanding"]
     [:th "Users applied"]
     [:th "Total # of applications"]
     [:th "# of applications per applicant"]
     [:th "# of applications per job"]]]
   [:tbody
    (for [{:keys [month] :as row-data} (reverse (<sub [::subs/candidate-data]))]
      ^{:key month}
      (generate-row row-data))]])

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

(defn init-charts []
  #?(:clj
     [:div
      [:script#chart-data
       (str "var chartConfig='" (json/generate-string (chart-config (take 12 (<sub [::subs/candidate-data])))) "'")]
      [:script#growth-chart-data
       (str "var growthChartConfig='" (json/generate-string (growth-chart-config (take 12 (<sub [::subs/candidate-data])))) "'")]]))

(defn page []
  (let [platform-name (<sub [:wh/platform-name])
        selected-vertical (<sub [:wh/vertical])]
    [:div.main
     [:div.data-page
      [:div.spread-or-stack
       [:h1 (str platform-name " Metrics")]
       (into [:div.has-bottom-margin]
             (mapv (partial time-period-button selected-vertical (<sub [::subs/time-period])) [:month :week]))]
      [:section
       (into [:ul.tabs]
             (mapv (partial generate-tabs selected-vertical) (concat ["www"] verticals/ordered-job-verticals)))]

      [:section
       [:p "Please be aware that data might be cached for up to 1 hour."]]
      [:h2 "Candidates"]
      [:h3 "Total number of approved candidates: " (<sub [::subs/total-registrations])]
      [:canvas#growthChart]
      [:canvas#myChart]
      [candidate-table]
      [:h2 "Companies"]
      [company-table]
      [init-charts]]]))