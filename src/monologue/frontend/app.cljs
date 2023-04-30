(ns monologue.frontend.app
  (:require [reagent.core :as reagent]
            [reitit.core :as r]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]
            [reagent.dom :as rdom]
            [monologue.frontend.state :refer [s-piece s-pieces]]
            [monologue.frontend.actions :as action]))

(defonce match (reagent/atom nil))
(defonce audio (reagent/atom nil))
(def music (reagent/atom {:audio   nil
                          :path    nil
                          :title   nil
                          :playing false}))

(defn log-fn [& params]
      (fn [_]
          (apply js/console.log params)))


(defn music-box []
      (defn change-music [{:keys [path title page start]
                           :or   {start false}}]
            (if (@music :audio) (.pause (@music :audio)))
            (reset! music {:audio   (new js/Audio path)
                           :path    path
                           :title   title
                           :playing start
                           :page    page})
            (if start
              (.play (@music :audio))))

      (defn play-and-pause []
            (fn [_]
                (if (@music :playing)
                  (do (.pause (@music :audio) (swap! music assoc :playing false)))
                  (do (.play (@music :audio) (swap! music assoc :playing true))))))

      (reagent/create-class
        {:display-name        "music-box"
         :component-did-mount (fn []
                                  (change-music {:path  "/assets/worries.mp3"
                                                 :title "kira - 걱정"}))
         :reagent-render      (fn []
                                  [:div
                                   [:br] [:br]
                                   [:span.icon [:i.fas.fa-music]]
                                   [:span (str "\"" (@music :title) "\"")]
                                   [:br]
                                   [:a {:on-click (play-and-pause)}
                                    (if (@music :playing) [:span.icon [:i.fas.fa-pause]])
                                    (if-not (@music :playing) [:span.icon [:i.fas.fa-play]])]

                                   (when (@music :page)
                                         [:a {:href (rfe/href ::piece-one {:id (@music :page)})}
                                          [:span.icon [:i.fas.fa-link]]]
                                         )

                                   ])}))





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
                                         [:li [:a {:href (rfe/href ::piece-one {:id (piece :id)})}
                                               (piece :subject)]
                                          #_[:a [:span.tag.p-1.ml-2 " tag."]]])
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
                                    [:strong (@s-piece :subject)]
                                    [:h5.subtitle.mb-2 (@s-piece :summary)]
                                    #_[:small.has-text-grey (@s-piece :mtime)]
                                    (when (@s-piece :music)
                                              [:a {:on-click #(change-music {:path  (-> @s-piece :music :path)
                                                                             :title (-> @s-piece :music :title)
                                                                             :page  (@s-piece :id)
                                                                             :start true})}

                                               [:span.icon [:i.fas.fa-rotate]]
                                               ])
                                    [:div.content.mt-5
                                     {:dangerouslySetInnerHTML
                                      {:__html (@s-piece :content-parsed)}}]

                                    ])}))

(defn main-component []
      (reagent/create-class
        {:display-name        "main recent one"
         :component-did-mount (fn []
                                  (action/get-piece-recent-one))

         :reagent-render      (fn []
                                  [:div
                                   [:strong (@s-piece :subject)]
                                   [:h5.subtitle.mb-2 (@s-piece :summary)]
                                   #_[:small.has-text-grey (@s-piece :mtime)]
                                   [:div.content.mt-5
                                    {:dangerouslySetInnerHTML
                                     {:__html (@s-piece :content-parsed)}}]])}))


(def routes
  [["/pieces" {:name ::pieces
               :view pieces-component}]
   ["/main" {:name ::main
             :view main-component}]
   ["/piece/:id" {:name       ::piece-one
                  :parameters {:path {:id string?}}
                  :view       piece-one-component}]])


(defn current-page []
      [:div.container.p-3
       [:header
        [:div.columns
         [:div.column.is-one-fifth
          [:header.mb-5
           [:figure.image
            [:img {:src "assets/roomel_coffee.jpg"}]]]]
         [:div.column.has-text-right
          [:p.title "monologue"]
          [:p "snail"]
          [music-box]]]]


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