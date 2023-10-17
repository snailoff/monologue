(ns me.monologue.router
  (:require [me.monologue.parser :as mpar]
            [me.monologue.constant :refer [knot-config]]
            [muuntaja.core :as muun]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as rrm-muuntaja]
            [reitit.ring.middleware.parameters :as rrm-parameter]
            [reitit.coercion.spec]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as b]))


(def app-route
  (ring/ring-handler
    (ring/router
      [["/" {:get {:parameters {}
                   :responses  {200 {}}
                   :handler    (fn [_]
                                 {:status  302
                                  :headers {"Location" (str "/page/" (knot-config :main-page))}})}}]

       ["/page/:page-name" {:get {:parameters {:path {:page-name string?}}
                                  :responses  {200 {}}
                                  :handler    (fn [{{{:keys [page-name]} :path} :parameters}]
                                                {:status  200
                                                 :headers {"Content-Type" "text/html"}
                                                 :body    (mpar/parse page-name)})}}]
       ["/external/:page-name/:action" {:post {:parameters {:path {:page-name string?
                                                                   :action    string?}}
                                               :responses  {200 {}}
                                               :handler    (fn [{{{:keys [page-name action]} :path} :parameters}]
                                                             {:status  200
                                                              :headers {"Content-Type" "text/html"}
                                                              :body    (str page-name action)})}}]]
      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   muun/instance
              :middleware [rrm-muuntaja/format-middleware
                           rrm-parameter/parameters-middleware
                           rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:root "" :path "/assets"})
      (ring/create-default-handler {:not-found (fn [_] {:status  200
                                                        :headers {"Content-Type" "text/html"}
                                                        :body    (mpar/parse (knot-config :404-page))})}))))

(defn start []
  (jetty/run-jetty app-route {:port 1234, :join? false})
  (b/debug "start!"))

(comment
  (start)
  (app-route {:request-method :get, :uri "/"})
  (app-route {:request-method :get, :uri "/index"}))

