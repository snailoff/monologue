(ns knot.frontend.views
  (:require [knot.frontend.state :refer [app-state]]
            [knot.frontend.actions :as action]
            [reagent.core :as reagent]))

(defn piece-list-component []
      (reagent/create-class
        {:display-name "piece list"
         :component-did-mount (fn [this]
                                  (action/get-pieces))
         :reagent-render (fn [this]
                             [:div.content
                              [:h3.title "piece list"]
                              [:ul
                               (for [piece (get @app-state :pieces)]
                                    ^{:key piece} [:li (piece :subject)])]])}))

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






