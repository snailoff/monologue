(ns knot.frontend.views
  (:require [knot.frontend.state :refer [app-state]]
            [knot.frontend.events :refer [increment decrement]]))


(defn header
      []
      [:div
       [:h1 "A template for reagent apps"]])

(defn page []
      [:div
       (get @app-state :page)])


(defn main []
      [:div
       [header]
       [page]
       [:button.btn {:on-click #(increment %)} "+"]])


