(ns knot.frontend.app
  (:require [reagent.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [fipp.edn :as fedn]
            [knot.frontend.views :as views]
            [knot.frontend.state :refer [app-state]]))

(defonce match (r/atom nil))

(def routes
  [["/piece-list" {:name ::piece-list
                   :view views/piece-list-component}]
   ["/piece-one" {:name ::piece-one
                  :view views/piece-recent-one-component}]])


(defn current-page []
      [:div
       [:ul
        [:li [:a {:href (rfe/href ::piece-list)} "list"]]
        [:li [:a {:href (rfe/href ::piece-one)} "one"]]]
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