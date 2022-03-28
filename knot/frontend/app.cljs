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
                                 [:span.icon [:i.fas.fa-clock]]
                                 [:ul
                                  (for [piece @s-pieces]
                                    ^{:key piece}
                                    [:li [:a {:href (rfe/href ::piece-one {:id (piece :id)})} (piece :subject)]
                                     [:a [:span.tag.p-1.ml-2 " tag."]]])
                                  [:li "..."]]])}))


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
                              [:h5.subtitle.mb-2 (@s-piece :summary)]
                              [:small.has-text-grey (@s-piece :mtime)]
                              [:div.content.mt-5
                               {:dangerouslySetInnerHTML
                                {:__html (@s-piece :content-parsed)}}]])}))

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
      [:div.container.p-3
       [:header
        [:div.columns
         [:div.column.is-one-fifth
          [:header.mb-5
           [:figure.image
            [:img {:src "assets/roomel_coffee.jpg"}]]]]
         [:div.column.has-text-right
          [:p.title "knot-md"]
          [:p "snailoff"]]]]


       [:div.columns
        [:div.column.is-one-fifth
         [pieces-component]]
        [:div.column
         [:section.section.mt-0.pt-0
          (if @match
            (let [view (:view (:data @match))]
                 [view @match])
            [main-component])]]]

       ;[:pre (with-out-str (cljs.pprint/pprint @match))]
       [:footer
        [:p [:span.icon-text
             [:span.icon [:i.fas.fa-copyright]]
             [:span "monologue.me"]]]]])



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