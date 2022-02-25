(ns knot.backend.main
  (:require [environ.core :refer [env]]
            [clj-jgit.internal :refer :all]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer :all]
            [immutant.scheduling :as cron]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as sqh]
            [reitit.core :as reit]
            [reitit.http :as http]
            [reitit.ring :as ring]
            [reitit.interceptor.sieppari]
            [ring.adapter.jetty :as jetty]
            [muuntaja.interceptor]))

(def memo (atom {:git-commit-id-save? false}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))
(defn memo-set-git-commit-id-save? [val] (swap! memo assoc-in [:git-commit-id-save?] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")

(def db-config {:dbtype      "postgresql"
                :host        (env :db-host)
                :port        (env :db-port)
                :dbname      (env :db-name)
                :user        (env :db-user)
                :password    (env :db-password)
                :auto-commit true})

(def git-config {:login (System/getenv "KNOT_GIT_USER")
                 :pw    (System/getenv "KNOT_GIT_PASSWORD")
                 :repo  (System/getenv "KNOT_GIT_REPOSITORY")})

(def <sync> identity)

(defn interceptor [f x]
  {:enter (fn [ctx] (f (update-in ctx [:request :via] (fnil conj []) {:enter x})))
   :leave (fn [ctx] (f (update-in ctx [:response :body] conj {:leave x})))})

(defn handler [f]
  (fn [{:keys [via]}]
    (f {:status 200,
        :body   (conj via :handler)})))

;(def app
;  (http/ring-handler
;    (http/router
;      [
;       ["/api"
;        ["/sync" {
;                  :get {;:interceptors [(interceptor <sync> :get)]
;                        :handler (handler <sync>)}}]]
;       ["/assets/*" {:get {:handler (ring/create-resource-handler {:root "temp/files"})}}]])
;    (ring/create-default-handler)
;    {:executor reitit.interceptor.sieppari/executor}))

(def app
  (ring/ring-handler
    (ring/router
      [["/ping" (constantly {:status 200, :body "pong"})]])
       ;["/assets/*" (ring/create-resource-handler {:root "/public"})
    (ring/routes
             (ring/create-resource-handler {:path "/"})
             (ring/create-default-handler))))

;:interceptors [(muuntaja.interceptor/format-interceptor)]

(defn start []
  (jetty/run-jetty #'app {:port 1234, :join? false})
  (println "server running in port 1234"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; meta

(defn save-meta [meta-name content]
  (jdbc/execute! db-config (-> (sqh/insert-into :knot_meta)
                               (sqh/values [{:meta    meta-name
                                             :content content
                                             :mtime   [:now]}])
                               (sqh/on-conflict :meta)
                               (sqh/do-update-set :content :mtime)
                               sql/format))
  (println (format "** meta(%s, %s) upserted." meta-name content)))

(defn load-meta [meta-name]
  (let [rs (jdbc/query db-config (sql/format {:select [:*]
                                              :from   :knot_meta
                                              :where  [:= :meta meta-name]}))]
    (if (empty? rs) nil (first rs))))

(defn load-meta-content [meta-name]
  (if-let [meta-data (load-meta meta-name)]
    (meta-data :content) nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; git

(defn git-changes []
  (jgit/with-credentials git-config
                         (let [repo (jgit/load-repo "temp")
                               latest-commit (get-head-commit repo)
                               saved-commit (load-meta-content META-GIT-COMMIT-ID)
                               since-commit (if (nil? saved-commit)
                                              ((last (jgit/git-log repo :all? true)) :id)
                                              (resolve-object saved-commit repo))]
                           (if (not= (.name since-commit) (.name latest-commit))
                             (do
                               (if (@memo :git-commit-id-save?)
                                 (save-meta META-GIT-COMMIT-ID (.name latest-commit)))
                               (changed-files-between-commits repo
                                                              since-commit
                                                              latest-commit))))))

(defn git-clone []
  (jgit/with-credentials git-config
                         (jgit/git-clone (git-config :repo)
                                         :branch "main"
                                         :dir "temp")))

(defn git-pull []
  (try
    (jgit/with-credentials git-config
                           (jgit/git-pull (jgit/load-repo "temp")))
    (catch Exception e
      (git-clone))))

(defn except-md? [path]
  (every? nil? [(re-find #"^\." path)
                (re-find #"^files/" path)]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; piece

(defn load-piece [conn subject]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_pieces
                                         :where  [:= :subject subject]}))]
    (if (empty? rs) nil (first rs))))

(defn save-piece [conn subject summary content]
  (jdbc/execute! conn (-> (sqh/insert-into :knot_pieces)
                          (sqh/values [{:subject subject
                                        :summary summary
                                        :content content
                                        :ctime   [:now]
                                        :mtime   [:now]}])
                          (sqh/on-conflict :subject)
                          (sqh/do-update-set :summary :content :mtime)
                          sql/format))
  (load-piece conn subject))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; tag

(defn load-tag [conn name]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_tags
                                         :where  [:= :name name]}))]
    (if (empty? rs) nil (first rs))))

(defn save-tag [conn name content]
  (jdbc/execute! conn (-> (sqh/insert-into :knot_tags)
                          (sqh/values [{:name    name
                                        :content content
                                        :ctime   [:now]
                                        :mtime   [:now]}])
                          (sqh/on-conflict :name)
                          (sqh/do-update-set :content :mtime)
                          sql/format)))

(defn save-tag-no-content [conn name]
  (jdbc/execute! conn (-> (sqh/insert-into :knot_tags)
                          (sqh/values [{:name  name
                                        :ctime [:now]
                                        :mtime [:now]}])
                          (sqh/on-conflict :name)
                          (sqh/do-nothing)
                          (sqh/returning [:id])
                          sql/format))
  (load-tag conn name))

(defn save-link-tag-piece [conn tag-id piece-id]
  (jdbc/execute! conn (-> (sqh/insert-into :link_tag_piece)
                          (sqh/values [{:tag_id   tag-id
                                        :piece-id piece-id}])
                          (sqh/on-conflict :tag_id :piece-id)
                          sqh/do-nothing
                          sql/format)))

(defn remove-link-tag-piece [conn piece-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_tag_piece
                                   :where       [:= :piece_id piece-id]})))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; link

(defn save-link-piece [conn from-id to-id]
  (jdbc/execute! conn (-> (sqh/insert-into :link_pieces)
                          (sqh/values [{:from_piece_id from-id
                                        :to_piece_id   to-id}])
                          (sqh/on-conflict :from_piece_id :to_piece_id)
                          sqh/do-nothing
                          sql/format)))

(defn remove-link-piece-parents [conn id]
  (jdbc/execute! conn (sql/format {:delete-from :link_pieces
                                   :where       [:= :to_piece_id id]})))

(defn remove-link-piece-children [conn id]
  (jdbc/execute! conn (sql/format {:delete-from :link_pieces
                                   :where       [:= :from_piece_id id]})))

(defn remove-piece [subject]
  (jdbc/with-db-transaction [tx db-config]
                            (if-let [piece (load-piece tx subject)]
                              (do
                                (jdbc/execute! tx (sql/format {:delete-from :knot_pieces
                                                               :where       [:= :id (piece :id)]}))
                                (remove-link-tag-piece tx (piece :id))
                                (remove-link-piece-parents tx (piece :id))
                                (remove-link-piece-children tx (piece :id))))))

(defn parse-tag [piece]
  (jdbc/with-db-transaction [tx db-config]
                            (remove-link-tag-piece tx (piece :id))
                            (doseq [tag (re-seq #"(?<=^|[^\w])#([^\s]+)" (piece :content))]
                              (let [tag-name (second tag)]
                                (let [tag (save-tag-no-content tx tag-name)]
                                  (save-link-tag-piece tx (tag :id) (piece :id)))))))

(defn parse-link [piece]
  (jdbc/with-db-transaction [tx db-config]
                            (remove-link-piece-children tx (piece :id))
                            (doseq [link (re-seq #"\[\[(.*?)\]\]" (piece :content))]
                              (if-let [target-piece (load-piece tx (second link))]
                                (save-link-piece tx (piece :id) (target-piece :id))))))




(defn knot-save [path action]
  (let [content-raw (slurp (str "temp/" path))
        subject (str/replace path #".md" "")
        summary (re-find #"%%\s*summary:\s*(.*) %%" content-raw)
        content (str/replace content-raw #"%%(.*?)%%\r?\n?" "")
        piece (save-piece db-config subject (second summary) content)]
    (println "** parse ... " path action)
    (parse-tag piece)
    (parse-link piece)))



(defn knot-remove [path action]
  (remove-piece path))

(defn git-parse []
  (doseq [[path action] (git-changes)]
    (println "---> " path action)
    (if (except-md? path)
      (cond
        (or (= action :add)
            (= action :edit)) (knot-save path action)
        (= action :delete) (knot-remove path action)
        :else (throw (Exception. (str "unknown action - " action))))
      (println "** ignored - " path))))


(defn reload-md []
  (git-pull)
  (git-parse))


(defn reload-schedule []
  (cron/schedule #(reload-md) (cron/cron "0 */10 * ? * *")))

(defn -main
  [& _]
  (reload-schedule)
  (start))



(comment

  (memo-set :git-commit-id-save? true)
  (memo-set :git-commit-id-save? false)

  (let [subject "nana"
        _ (save-piece db-config subject "su" "co")
        p (load-piece subject)]
    (save-link-piece db-config (p :id) 0)
    (save-link-piece db-config 99 (p :id))
    (remove-piece subject))

  (remove-piece "lala")
  (remove-link-piece-children db-config 25))



