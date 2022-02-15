(ns main
  (:require [environ.core :refer [env]]
            [clj-jgit.internal :refer :all]
            [clj-jgit.porcelain :as jgp]
            [clj-jgit.querying :refer :all]
            [immutant.scheduling :as ims]
            [clojure.java.jdbc :as j]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [reitit.core :as r]
            [reitit.http :as http]
            [reitit.ring :as ring]
            [reitit.interceptor.sieppari]
            ;[sieppari.async.core-async] ;; needed for core.async
            ;[sieppari.async.manifold]   ;; needed for manifold
            [ring.adapter.jetty :as jetty]
            [muuntaja.interceptor]
            ;[clojure.core.async :as a]
            ;[manifold.deferred :as d]
            ;[promesa.core :as p]
            ))

(defn interceptor [f x]
  {:enter (fn [ctx] (f (update-in ctx [:request :via] (fnil conj []) {:enter x})))
   :leave (fn [ctx] (f (update-in ctx [:response :body] conj {:leave x})))})

(defn handler [f]
  (fn [{:keys [via]}]
    (f {:status 200,
        :body (conj via :handler)})))

(def <sync> identity)
;(def <future> #(future %))
;(def <async> #(a/go %))


(def app
  (http/ring-handler
    (http/router
      [
       ["/api" ;{:interceptors [(interceptor <sync> :api)]}
        ["/sync" {;:interceptors [(interceptor <sync> :sync)]
                  :get {;:interceptors [(interceptor <sync> :get)]
                        :handler (handler <sync>)}}]
        ;["/async"
        ; {:interceptors [(interceptor <async> :async)]
        ;  :get {:interceptors [(interceptor <async> :get)]
        ;        :handler (handler <async>)}}]

        ]
       ["/assets/*" (ring/create-resource-handler {:root "temp/files"})]]



      ;["/temp/files/*" (ring/create-resource-handler)]
      )
    (ring/create-default-handler)
    {:executor reitit.interceptor.sieppari/executor
     ;:interceptors [(muuntaja.interceptor/format-interceptor)]
     }))

(defn start []
  (jetty/run-jetty #'app {:port 1234, :join? false, :async? true})
  (println "server running in port 1234"))

(comment
  (-> app (ring/get-router) (r/compiled-routes))
  )

(def db-config {:dbtype      "postgresql"
             :host        (env :db-host)
             :port        (env :db-port)
             :dbname      (env :db-name)
             :user        (env :db-user)
             :password    (env :db-password)
             :auto-commit true})

(def client-config {:login (System/getenv "KNOT_GIT_USER")
                    :pw (System/getenv "KNOT_GIT_PASSWORD")
                    :repo (System/getenv "KNOT_GIT_REPOSITORY")})

(defn git-clone []
  (jgp/with-credentials client-config
                        (jgp/git-clone (client-config :repo)
                                       :branch "main"
                                       :dir "temp")))
(defn git-pull []
  (println "** git pulling ...")
  (jgp/with-credentials client-config
                        (jgp/git-pull (jgp/load-repo "temp")))
  (println "** git pulled."))

;(defn git-changes []
;  (jgp/with-credentials client-config
;                        (let [repo (jgp/load-repo "temp")
;                              rev-walk (new-rev-walk repo)]
;                          ;(find-rev-commit repo rev-commit)
;                          (changed-files-between-commits repo
;                                                         (resolve-object repo "124b7e92e0902f40a1863d37c5c552b1ceb7789d")
;                                                         (resolve-object repo "af94d3067583a3101dc9844ed8fb753e9b8ccb72"))
;                          )))

(defn git-schedule []
  (ims/schedule #(git-pull) (ims/cron "0 */10 * ? * *")))

(defn test-knot-pieces []
  (let [rows (j/query db-config (->
                                  (select :id :subject :summary :content)
                                  (from :knot_pieces)
                                  (order-by [:ctime])
                                  sql/format))]
    (println rows)))

(defn -main
  [& args]
  (git-schedule)
  (start))
