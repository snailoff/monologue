(ns knot.frontend.views
  (:require [knot.frontend.state :refer [app-state]]
            [knot.frontend.events :refer [increment decrement]]))


(defn header
      []
      [:div
       [:h1 "A template for reagent apps"]])

(defn counter
      []
      [:div
       [:button.btn {:on-click #(decrement %)} "-"]
       [:button {:disabled true} (get @app-state :count)]])


(defn main []
      [:div
       [header]
       [:button.btn {:on-click #(increment %)} "+"]])


(defn app []
      [:div
       [header]
       [counter]])