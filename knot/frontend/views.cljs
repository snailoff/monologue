(ns knot.frontend.views
  (:require [knot.frontend.state :refer [app-state]]
            [knot.frontend.actions :as action]
            [reagent.core :as reagent]))



(defn pieces-component []
      (reagent/create-class
        {:display-name "pieces"
         :component-did-mount
         (fn [this]
             (action/get-pieces))

         :reagent-render
         (fn [this]
             [:div
              [:h3 "pieces"]
              [:ul
               (for [piece (get @app-state :pieces)]
                    ^{:key piece} [:li "piece - " (piece :subject)])]])}))


(defn main-page []
      [pieces-component])



