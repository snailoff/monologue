(ns knot.frontend.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [knot.frontend.state :refer [s-piece s-pieces]]
            [knot.frontend.actions :as action]))

(defonce match (reagent/atom nil))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))

(defn pieces-component []
      (reagent/create-class
        {:display-name        "pieces"
         :component-did-mount (fn []
                                  (action/get-pieces))
         :reagent-render      (fn []
                                  [:div
                                   [:p "pieces"]
                                   [:ul
                                    (for [piece @s-pieces]
                                         ^{:key piece}
                                         [:li [:a {:href (rfe/href ::piece-one {:id (piece :id)})} (piece :subject)]
                                          [:small " tag."]])]])}))

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
                             [:div
                              [:h3.title (@s-piece :subject)]
                              [:h5.subtitle (@s-piece :summary)]
                              [:div.content (@s-piece :content)]])}))

(defn main-component []
      (reagent/create-class
        {:display-name        "main recent one"
         :component-did-mount (fn []
                                  (action/get-piece-recent-one))

         :reagent-render      (fn []
                                [:div
                                 [:h3.title (@s-piece :subject)]
                                 [:h5.subtitle (@s-piece :summary)]
                                 [:div.content (@s-piece :content)]])}))


(def routes
  [["/pieces" {:name ::pieces
               :view pieces-component}]
   ["/main" {:name ::main
             :view main-component}]
   ["/piece/:id" {:name ::piece-one
                  :parameters {:path {:id int?}}
                  :view piece-one-component}]])


(defn current-page []
      [:div
       [:div.columns
        [:div.column.is-one-quarter
         [:ul
          [:li [:a.title {:href (rfe/href ::main)} "knot-md"]]]

         [:br]
         [pieces-component]]

        [:div.column
         [:section.section
          (if @match
            (let [view (:view (:data @match))]
                 [view @match])
            [main-component])]]]

       ;[:pre (with-out-str (cljs.pprint/pprint @match))]
       [:footer.footer
        [:div]]])


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