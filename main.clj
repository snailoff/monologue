(ns main
  (:require [environ.core :refer [env]]
            [clj-jgit.internal :refer :all]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer :all]
            [immutant.scheduling :as cron]
            [clojure.java.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as sqh]
            [reitit.core :as reit]
            [reitit.http :as http]
            [reitit.ring :as ring]
            [reitit.interceptor.sieppari]
            [ring.adapter.jetty :as jetty]
            [muuntaja.interceptor]))

(defn interceptor [f x]
  {:enter (fn [ctx] (f (update-in ctx [:request :via] (fnil conj []) {:enter x})))
   :leave (fn [ctx] (f (update-in ctx [:response :body] conj {:leave x})))})

(defn handler [f]
  (fn [{:keys [via]}]
    (f {:status 200,
        :body (conj via :handler)})))

(def <sync> identity)

(def app
  (http/ring-handler
    (http/router
      [
       ["/api"
        ["/sync" {
                  :get {;:interceptors [(interceptor <sync> :get)]
                        :handler (handler <sync>)}}]]
       ["/assets/*" (ring/create-resource-handler {:root "temp/files"})]])
    (ring/create-default-handler)
    {:executor reitit.interceptor.sieppari/executor
     ;:interceptors [(muuntaja.interceptor/format-interceptor)]
     }))

(defn start []
  (jetty/run-jetty #'app {:port 1234, :join? false, :async? true})
  (println "server running in port 1234"))

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
  (jgit/with-credentials client-config
                         (jgit/git-clone (client-config :repo)
                                         :branch "main"
                                         :dir "temp")))
(defn git-pull []
  (println "** git pulling ...")
  (jgit/with-credentials client-config
                         (jgit/git-pull (jgit/load-repo "temp")))
  (println "** git pulled."))

(defn save-meta [meta-name content]
  (jdbc/execute! db-config (-> (sqh/insert-into :knot_meta)
                               (sqh/values [{:meta meta-name
                                             :content content
                                             :mtime [:now]}])
                               (sqh/on-conflict :meta)
                               (sqh/do-update-set :content)
                               sql/format))
  (println (format "** meta(%s, %s) upserted." meta-name content)))

(defn load-meta [meta-name]
  (jdbc/query db-config (sql/format {:select [:*]
                                     :from :knot_meta
                                     :where [:= :meta "git"]})))

(defn git-changes []
  (jgit/with-credentials client-config
                         (let [repo (jgit/load-repo "temp")
                               logs (jgit/git-log repo)
                               last-commit (.getName ((first logs) :id))
                               first "5a765e753e5e67474254d4c56fd8dfc8c7666e16"
                               ]

                           (println first)
                           (println last-commit)

                           (changed-files-between-commits repo
                                                          (resolve-object repo last-commit)
                                                          (resolve-object repo first))
                           (save-meta-git-commit-id last-commit)

                           ))

  )

(defn git-schedule []
  (cron/schedule #(git-pull) (cron/cron "0 */10 * ? * *")))



(comment
  (jdbc/with-db-connection [tx db-config]
                        (jdbc/execute! tx
  (->
    (insert-into :knot_meta)
    (columns :meta :content)
    (values [["aaa" "123"]])
    sql/format)))

  (-> (sqh/insert-into :knot_meta)
      (sqh/columns :meta :content)
      (sqh/values [{:meta "GIT_COMMIT_ID" :content "1"}])
      (sqh/on-conflict :meta)
      ;sqh/do-nothing
      (sqh/do-update-set :content)
      sql/format)

  )

(defn test-knot-pieces []
  (let [rows (jdbc/query db-config (sql/format {:select [:id :subject :summary :content]
                                                :from :knot_pieces
                                                :order-by [:ctime]}))]
    (println rows)))

(defn -main
  [& args]
  (git-schedule)
  (start))

(comment
  (-> app (ring/get-router) (reit/compiled-routes))

  (jgit/with-credentials client-config
                         (let [repo (jgit/load-repo "temp")
                               logs (jgit/git-log repo)
                               last (.name ((first logs) :id))
                               since "5a765e753e5e67474254d4c56fd8dfc8c7666e16"]

                           (println first)
                           (println last)

                           (changed-files-between-commits repo
                                                          (resolve-object repo last)
                                                          (resolve-object repo first))
                           (doseq [log logs]
                             (println log))





                           ;(find-rev-commit repo rev-commit)
                           ;(changed-files repo "124b7e92e0902f40a1863d37c5c552b1ceb7789d")

                           ;(println logs)
                           ))
  )


