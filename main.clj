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

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")

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

(def <sync> identity)

(defn interceptor [f x]
  {:enter (fn [ctx] (f (update-in ctx [:request :via] (fnil conj []) {:enter x})))
   :leave (fn [ctx] (f (update-in ctx [:response :body] conj {:leave x})))})

(defn handler [f]
  (fn [{:keys [via]}]
    (f {:status 200,
        :body (conj via :handler)})))

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
  (let [rs (jdbc/query db-config (sql/format {:select [:*]
                                              :from :knot_meta
                                              :where [:= :meta meta-name]}))]
    (if (empty? rs) nil (first rs))))

(defn load-meta-content [meta-name]
  (if-let [meta-data (load-meta meta-name)]
    (meta-data :content) nil))


(defn start []
  (jetty/run-jetty #'app {:port 1234, :join? false, :async? true})
  (println "server running in port 1234"))



(defn git-changes []
  (jgit/with-credentials client-config
                         (let [repo (jgit/load-repo "temp")
                               latest-commit (get-head-commit repo)
                               saved-commit (load-meta-content META-GIT-COMMIT-ID)
                               since-commit (if (nil? saved-commit)
                                              ((last (jgit/git-log repo :all? true)) :id)
                                              (resolve-object saved-commit repo))]
                           (if (not= (.name latest-commit) (.name since-commit))
                             (do
                               (changed-files-between-commits repo
                                                              latest-commit
                                                              since-commit)
                               (save-meta META-GIT-COMMIT-ID (.name latest-commit)))))))

(defn git-clone []
  (jgit/with-credentials client-config
                         (jgit/git-clone (client-config :repo)
                                         :branch "main"
                                         :dir "temp")))

(defn git-pull []
  (try
    (jgit/with-credentials client-config
                           (jgit/git-pull (jgit/load-repo "temp")))
    (catch Exception e
      (git-clone))))


(defn knot-parse []
  (if-let [files (git-changes)]
    (doseq [file files]
      (println file))))



(defn reload-md []
  (git-pull)
  (knot-parse))


(defn reload-schedule []
  (cron/schedule #(reload-md) (cron/cron "0 */10 * ? * *")))

(defn -main
  [& _]
  (reload-schedule)
  (start))




