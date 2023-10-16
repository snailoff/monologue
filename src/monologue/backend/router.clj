(ns monologue.backend.router
  (:require [monologue.backend.service :as mser]
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
                                  :headers {"Location" "/page/@main"}})}}]
       ["/tags" {:get {:parameters {}
                       :responses  {200 {}}
                       :handler    (fn [_]
                                     {:status  200
                                      :headers {"Content-Type" "text/html"}
                                      :body    "tags"})}}]
       ["/tag/:page-name" {:get {:parameters {}
                                 :responses  {200 {}}
                                 :handler    (fn [_]
                                               {:status  200
                                                :headers {"Content-Type" "text/html"}
                                                :body    "tag - page"})}}]
       ["/pages" {:get {:parameters {}
                        :responses  {200 {}}
                        :handler    (fn [_]
                                      {:status  200
                                       :headers {"Content-Type" "text/html"}
                                       :body    (str (mser/load-pages))})}}]
       ["/pages/:year" {:get {:parameters {:path {:year string?}}
                              :responses  {200 {}}
                              :handler    (fn [{{{:keys [year]} :path} :parameters}]
                                            {:status  200
                                             :headers {"Content-Type" "text/html"}
                                             :body    (str (mser/load-pages-year year))})}}]
       ["/page/:page-name" {:get {:parameters {:path {:page-name string?}}
                                  :responses  {200 {}}
                                  :handler    (fn [{{{:keys [page-name]} :path} :parameters}]
                                                {:status  200
                                                 :headers {"Content-Type" "text/html"}
                                                 :body    (mser/load-page-one page-name)})}}]
       ["/comments" {:get {:parameters {}
                           :responses  {200 {}}
                           :handler    (fn [_]
                                         {:status  200
                                          :headers {"Content-Type" "text/html"}
                                          :body    "comments"})}}]
       ["/comments/:page-name" {:get {:parameters {}
                                      :responses  {200 {}}
                                      :handler    (fn [_]
                                                    {:status  200
                                                     :headers {"Content-Type" "text/html"}
                                                     :body    "comments - page"})}}]
       ["api"
        [["/comment/:page-name/create" {:post {:parameters {}
                                        :responses  {200 {}}
                                        :handler    (fn [_]
                                                      {:status  200
                                                       :headers {"Content-Type" "text/html"}
                                                       :body    "comment api - create"})}}]
         ["/comment/:page-name/remote" {:delete {:parameters {}
                                          :responses  {200 {}}
                                          :handler    (fn [_]
                                                        {:status  200
                                                         :headers {"Content-Type" "text/html"}
                                                         :body "comment api - delete"
                                                         })}}]]]]

      {:data {:coercion   reitit.coercion.spec/coercion
              :muuntaja   muun/instance
              :middleware [rrm-muuntaja/format-middleware
                           rrm-parameter/parameters-middleware
                           rrc/coerce-exceptions-middleware
                           rrc/coerce-request-middleware
                           rrc/coerce-response-middleware
                           ]}})
    (ring/routes
      (ring/create-resource-handler {:root "public" :path "/"})
      (ring/create-default-handler))))

(defn start []
  (jetty/run-jetty app-route {:port 1234, :join? false})
  (b/debug "start!"))

(start)
(comment
  (start)
  (app-route {:request-method :get, :uri "/"})
  (app-route {:request-method :get, :uri "/index"}))

