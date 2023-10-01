(ns monologue.backend.router
  (:require [monologue.backend.mapper :as mapper]
            [muuntaja.core :as muun]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as rrm-muuntaja]
            [reitit.ring.middleware.parameters :as rrm-parameter]
            [reitit.coercion.spec]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.cors :refer [wrap-cors]]
            [taoensso.timbre :as b]))


(def app-route
  (ring/ring-handler
    (ring/router
      [["/api"
        ["/piece-recent-list" {:get {:parameters {}
                                     :responses  {200 {}}
                                     :handler    (fn [{:keys []}]
                                                   {:status 200
                                                    :body   {:pieces (mapper/pieces-recent 10)}})}}]
        ["/piece-recent-one" {:get {:parameters {}
                                    :responses  {200 {}}
                                    :handler    (fn [{:keys []}]
                                                  {:status 200
                                                   :body   {:piece (mapper/pieces-recent-one)}})}}]
        ["/piece/:piece-id" {:get {:parameters {:path {:piece-id string?}}
                                   :responses  {200 {:piece {}}}
                                   :handler    (fn [{:keys [parameters]}]
                                                 {:status 200
                                                  :body   {:piece (mapper/pieces-one (-> parameters :path :piece-id))}})}}]]
       ["/blog"
        ["/tistory/auth-code-callback" {:get {:parameters {:query {:code  string?
                                                                   :state string?}}
                                              :responses  {200 {}}
                                              :handler    (fn [{{{:keys [code state]} :query} :parameters}]
                                                            {:status 200
                                                             :body   {:authorization-code code
                                                                      :state              state}})}}]]]
      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   muun/instance
              :middleware [rrm-muuntaja/format-middleware
                           rrm-parameter/parameters-middleware
                           rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(def app
  (wrap-cors app-route
             :access-control-allow-origin [#".*"]
             :access-control-allow-methods [:get :options]))

(defn start []
  (jetty/run-jetty app {:port 1234, :join? false})
  (b/debug "start!"))
