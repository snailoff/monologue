(ns knot.frontend.app
  (:require [reagent.dom :as rdom]
            [knot.frontend.views :as views]))

(defn ^:dev/after-load start []
      (rdom/render
        [views/main-page]
        (.getElementById js/document "app")))


(defn ^:export main
      []
      (start))