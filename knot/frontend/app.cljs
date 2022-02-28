(ns knot.frontend.app
  (:require [reagent.core :as r]
            [knot.frontend.views :as views]))

(defn ^:dev/after-load start []
      (r/render-component [views/main]
                          (.getElementById js/document "app")))

(defn ^:export main
      []
      (start))