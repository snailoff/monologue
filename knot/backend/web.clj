(ns knot.backend.web
  (:require [knot.backend.mapper :as mapper]
            [environ.core :refer [env]]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.interceptor.sieppari]
            [reitit.coercion.schema]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [schema.core :as s]))


(def app
  (ring/ring-handler
    (ring/router
      [;["/" (constantly {:status 200, :body (slurp "public/main.html")})]
       ["/api"
        ["/piece-recent-list" {:get {:coercion   reitit.coercion.schema/coercion
                                     :parameters {}
                                     :responses  {200 {}}
                                     :handler    (fn [{:keys []}]
                                                   {:status 200
                                                    :body   {:pieces (mapper/pieces-recent 3)}})}}]
        ["/piece-recent-one" {:get {:coercion   reitit.coercion.schema/coercion
                                    :parameters {}
                                    :responses  {200 {}}
                                    :handler    (fn [{:keys []}]
                                                  {:status 200
                                                   :body   {:piece (mapper/pieces-recent-one)}})}}]
        ["/piece/:piece-id" {:get {:coercion   reitit.coercion.schema/coercion
                                   :parameters {:path {:piece-id s/Int}}
                                   :responses  {200 {:piece {}}}
                                   :handler    (fn [{:keys [parameters]}]
                                                 {:status 200
                                                  :body   {:piece (mapper/pieces-one (-> parameters :path :piece-id))}})}}]]]


      {:data {:muuntaja   muuntaja.core/instance
              :middleware [muuntaja/format-middleware
                           rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))