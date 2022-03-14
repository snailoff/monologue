(ns knot.frontend.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [fipp.edn :as fedn]
            [knot.frontend.state :refer [app-state]]
            [knot.frontend.actions :as action]))

(defonce match (reagent/atom nil))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))

(defn piece-list-component []
      (reagent/create-class
        {:display-name        "pieces"
         :component-did-mount (fn [this]
                                  (action/get-pieces))
         :reagent-render      (fn [this]
                                  [:div
                                   [:p "pieces"]
                                   [:ul
                                    (for [piece (@app-state :pieces)]
                                         ^{:key piece}
                                         [:li [:a {:href (rfe/href ::piece-one {:id (piece :id)})} (piece :subject)]
                                          #_[:small (piece :mtime)]])]])}))


(defn piece-one-component []
  (reagent/create-class
    {:display-name         "piece one"
     :component-did-update (fn [this [_ prev-argv]]
                             (let [[_ new-argv] (reagent/argv this)
                                   nid (-> new-argv :parameters :path :id)
                                   prev-id (-> prev-argv :parameters :path :id)]
                               (if (not= nid prev-id)
                                 (action/get-piece nid))))

     :component-did-mount  (fn [this]
                             (let [[_ new-argv] (reagent/argv this)
                                   nid (-> new-argv :parameters :path :id)]
                               (action/get-piece nid)))

     :reagent-render       (fn []
                             (let [piece (@app-state :piece)]
                               [:div
                                [:h3.title (piece :subject)]
                                [:h5.subtitle (piece :summary)]
                                [:div.content (piece :content)]]))}))


(defn main-component []
      (reagent/create-class
        {:display-name        "main recent one"
         :component-did-mount (fn []
                                  (action/get-piece-recent-one))

         :reagent-render      (fn []
                                  (let [piece (@app-state :piece)]
                                       [:div
                                        [:h3.title (piece :subject)]
                                        [:section (piece :content)]]))}))

(def routes
  [["/piece-list" {:name ::piece-list
                   :view piece-list-component}]
   ["/main" {:name ::main
             :view main-component}]
   ["/piece/:id" {:name ::piece-one
                  :parameters {:path {:id int?}}
                  :view piece-one-component}]])


(defn current-page []
      [:div
       [:div.columns
        [:div.column.is-one-quarter
         [:h1.title "knot-md"]
         [:ul
          ;;[:li [:a {:href (rfe/href ::piece-list)} "list"]]
          [:li [:a {:href (rfe/href ::main)} "main"]]]

         [:br]
         [piece-list-component]]

        [:div.column
         [:section.section
          (if @match
            (let [view (:view (:data @match))]
                 [view @match])
            [main-component])]]]

       [:pre (with-out-str (cljs.pprint/pprint @match))]
       [:footer.footer
        [:div
         "powered by knot-md"]]])

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