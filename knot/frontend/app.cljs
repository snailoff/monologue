(ns knot.frontend.app
  (:require [reagent.core :as reagent]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [fipp.edn :as fedn]
            [knot.frontend.state :refer [app-state]]
            [knot.frontend.actions :as action]))

(defonce match (reagent/atom nil))


(defn piece-list-component []
      (reagent/create-class
        {:display-name "piece list"
         :component-did-mount (fn [this]
                                  (action/get-pieces))
         :reagent-render (fn [this]
                             [:div.content
                              [:h3.title "piece list"]
                              [:ul
                               (for [piece (@app-state :pieces)]
                                    ^{:key piece}
                                    [:li [:a {:href (rfe/href ::piece-one {:piece-id (piece :id)})} (piece :subject)]])]])}))


(defn piece-one-component [match]
      (let [{:keys [path]} (:parameters match)
            {:keys [piece-id]} path]
           (reagent/create-class
             {:display-name "piece one"
              :component-did-mount (fn [this]
                                       (action/get-piece piece-id))
              :reagent-render (fn [this]
                                  (let [piece (@app-state :piece)]
                                       [:div
                                        [:h3.title (piece :subject)]
                                        [:h5.subtitle (piece :summary)]
                                        [:div.content (piece :content)]]))})))


(defn piece-recent-one-component []
      (reagent/create-class
        {:display-name "piece recent one"
         :component-did-mount (fn [this]
                                  (action/get-piece-recent-one))

         :reagent-render (fn [this]
                             [:div.content
                              [:h3.title "piece recent one"]
                              [:ul
                               (let [piece (get @app-state :piece)]
                                    [:li (piece :subject)])]])}))


(def routes
  [["/piece-list" {:name ::piece-list
                   :view piece-list-component}]
   ["/piece-recent" {:name ::piece-recent
                           :view piece-recent-one-component}]
   ["/piece-one/:piece-id" {:name ::piece-one
                            :path {:piece-id int?}
                            :view piece-one-component}]])

(defn current-page []
      [:div
       [:ul
        [:li [:a {:href (rfe/href ::piece-list)} "list"]]
        [:li [:a {:href (rfe/href ::piece-recent)} "recent"]]
        [:li [:a {:href (rfe/href ::piece-one {:piece-id 85})} "piece"]]]
       (if @match
         (let [view (:view (:data @match))]
              [view @match]))
       (prn "** match" @match)])


(defn ^:dev/after-load start []
      (rfe/start!
        (rf/router routes {:data {:coercion rss/coercion}})
        (fn [m] (reset! match m))
        {:use-fragment true})

      (rdom/render
        [current-page]
        (.getElementById js/document "app")))


(defn ^:export main
      []
      (start))