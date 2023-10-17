(ns monologue.knot.router
  (:require [monologue.knot.parser :as mpar]
            [monologue.knot.constant :refer [knot-config]]
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
                                  :headers {"Location" (str "/page/" (knot-config :start-page))}})}}]

       ["/page/:req-name" {:get {:parameters {:path {:req-name string?}}
                                  :responses  {200 {}}
                                  :handler    (fn [{{{:keys [req-name]} :path} :parameters}]
                                                {:status  200
                                                 :headers {"Content-Type" "text/html"}
                                                 :body    (mpar/parse req-name)})}}]
       ["/external/:page-subject/:action" {:post {:parameters {:path {:page-subject string?
                                                                   :action    string?}}
                                               :responses  {200 {}}
                                               :handler    (fn [{{{:keys [page-subject action]} :path} :parameters}]
                                                             {:status  200
                                                              :headers {"Content-Type" "text/html"}
                                                              :body    (str page-subject action)})}}]]
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

