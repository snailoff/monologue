(ns monologue.backend.mapper
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [honey.sql :as sql]
            [honey.sql.helpers :as sqh]
            [taoensso.timbre :as b]
            [nano-id.core :refer [custom]]
            [monologue.backend.constant :refer [db-config git-config]]))

(def nano-pid (custom "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" 15))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; meta


(defn save-meta [meta-name content]
  (jdbc/execute! db-config (-> (sqh/insert-into :knot_meta)
                               (sqh/values [{:meta    meta-name
                                             :content content
                                             :mtime   [:now]}])
                               (sqh/on-conflict :meta)
                               (sqh/do-update-set :content :mtime)
                               sql/format))
  (b/spy :info meta-name content))

(defn load-meta [meta-name]
  (let [rs (jdbc/query db-config (sql/format {:select [:*]
                                              :from   :knot_meta
                                              :where  [:= :meta meta-name]}))]
    (if (empty? rs) nil (first rs))))

(defn load-meta-content [meta-name]
  (if-let [meta-data (load-meta meta-name)]
    (meta-data :content) nil))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; piece
(defn load-piece-by-id [conn id]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_pieces
                                         :where  [:= :id id]}))]
    (if (empty? rs) nil (first rs))))

(defn load-piece-by-subject [conn subject]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_pieces
                                         :where  [:= :subject subject]}))]
    (if (empty? rs) nil (first rs))))


(defn save-piece [conn data]
  (jdbc/execute! conn (-> (sqh/insert-into :knot_pieces)
                          (sqh/values [{:id (nano-pid)
                                        :subject (data :subject)
                                        :summary (data :summary)
                                        :content (data :content)
                                        :ctime   [:now]
                                        :mtime   [:now]}])
                          (sqh/on-conflict :subject)
                          (sqh/do-update-set :summary :content :mtime)
                          sql/format))
  (load-piece-by-subject conn (data :subject)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; tag

(defn load-tag [conn name]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_tags
                                         :where  [:= :name name]}))]
    (if (empty? rs) nil (first rs))))

#_(defn save-tag [conn name content]
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
                            (if-let [piece (load-piece-by-subject tx subject)]
                              (do
                                (jdbc/execute! tx (sql/format {:delete-from :knot_pieces
                                                               :where       [:= :id (piece :id)]}))
                                (remove-link-tag-piece tx (piece :id))
                                (remove-link-piece-parents tx (piece :id))
                                (remove-link-piece-children tx (piece :id)))
                              ())))

(defn parse-tag [piece]
  (jdbc/with-db-transaction [tx db-config]
                            (remove-link-tag-piece tx (piece :id))
                            (doseq [tag (re-seq #"(?<=^|[^\w])#([^\s]+)" (piece :content))]
                              (let [tag-name (second tag)
                                    tag (save-tag-no-content tx tag-name)]
                                (save-link-tag-piece tx (tag :id) (piece :id))))))

(defn parse-link [piece]
  (jdbc/with-db-transaction [tx db-config]
                            (remove-link-piece-children tx (piece :id))
                            (doseq [link (re-seq #"\[\[(.*?)\]\]" (piece :content))]
                              (if-let [target-piece (load-piece-by-subject tx (second link))]
                                (save-link-piece tx (piece :id) (target-piece :id))
                                ()))))


(defn knot-save [path action]
  (println "** knot-save : " path action)
  (b/info (slurp (str (git-config :workspace) "/" path)))
  (let [content-raw (slurp (str (git-config :workspace) "/" path))
        subject (str/replace path #".md" "")
        summary (re-find #"%%\s*summary:\s*(.*) %%" content-raw)
        content (str/replace content-raw #"%%(.*?)%%\r?\n?" "")
        piece (save-piece db-config {:subject subject
                                     :summary (second summary)
                                     :content content })]
    (parse-tag piece)
    (parse-link piece)))

(comment
  (re-matches #"^.*[0-9]{12}.*$" "2024/202404142323.md")

  (let [str "index.md" #_"2023/202304151432.md"]
    (println (if (re-matches #"^.*[0-9]{12}.*$" str)
               (str/replace str #"^.*20[0-9]{2}(....)[0-9]{4}.*$" "$1") "no"))
    #_(println (str/replace str #"^.*20[0-9]{4}(..)[0-9]{4}.*$" "$1"))
    )
  )



(defn knot-remove [path _]
  (remove-piece path))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; page

(defn pieces-recent-one []
  (let [rs (jdbc/query db-config (sql/format {:select   [:*]
                                              :from     :knot_pieces
                                              :order-by [[:mtime :desc]]
                                              :limit    1}))]
    (if (empty? rs) nil (first rs))))

(defn pieces-recent [limit]
  (let [rs (jdbc/query db-config (sql/format {:select   [:*]
                                              :from     :knot_pieces
                                              :order-by [[:mtime :desc]]
                                              :limit    limit}))]
    (if (empty? rs) nil rs)))

(defn pieces-one [id]
  (load-piece-by-id db-config id))

#_(defn tags-recent [limit]
    (let [rs (jdbc/query db-config (sql/format {:select   [:*]
                                                :from     :knot_tags
                                                :order-by [[:mtime :desc]]
                                                :limit    limit}))]
      (if (empty? rs) nil rs)))


