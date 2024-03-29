(ns monologue.backend.mapper
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [honey.sql :as sql]
            [honey.sql.helpers :as sqh]
            [taoensso.timbre :as b]
            [nano-id.core :refer [custom]]
            [monologue.backend.constant :refer [db-config]]))

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

(defn select-meta-one [meta-name]
  (let [rs (jdbc/query db-config (sql/format {:select [:*]
                                              :from   :knot_meta
                                              :where  [:= :meta meta-name]}))]
    (if (empty? rs) nil (first rs))))

(defn select-meta-content [meta-name]
  (if-let [meta-data (select-meta-one meta-name)]
    (meta-data :content) nil))

(defn delete-meta-content [meta-name]
  (if-let [_ (select-meta-one meta-name)]
    (jdbc/execute! db-config (sql/format {:delete-from :knot_meta
                                          :where       [:= :meta meta-name]}))
    (b/spy :warn (str "no meta - " meta-name))))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; piece
(defn select-piece-by-id [conn id]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_pieces
                                         :where  [:= :id id]}))]
    (if (empty? rs) nil (first rs))))

(defn select-piece-by-subject [conn subject]
  (let [rs (jdbc/query conn (sql/format {:select [:*]
                                         :from   :knot_pieces
                                         :where  [:= :subject subject]}))]
    (if (empty? rs) nil (first rs))))


(defn upsert-piece [conn data]
  (jdbc/execute! conn (-> (sqh/insert-into :knot_pieces)
                          (sqh/values [{:id      (data :id)
                                        :subject (data :subject)
                                        :summary (data :summary)
                                        :content (data :content)
                                        :ctime   [:now]
                                        :mtime   [:now]}])
                          (sqh/on-conflict :subject)
                          (sqh/do-update-set :summary :content :mtime)
                          sql/format)))

(defn delete-piece [conn id]
  (jdbc/execute! conn (sql/format {:delete-from :knot_pieces
                                   :where       [:= :id id]})))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; link (knot to piece)

;TODO tag => knot table. column

(defn upsert-link-knot-piece [conn tag-id piece-id]
  (jdbc/execute! conn (-> (sqh/insert-into :link_tag_piece)
                          (sqh/values [{:tag_id   tag-id
                                        :piece-id piece-id}])
                          (sqh/on-conflict :tag_id :piece-id)
                          sqh/do-nothing
                          sql/format)))

(defn select-link-knot-piece-by-knot-id [conn knot-id]
  (jdbc/query conn (sql/format {:select :*
                                :from   :link_tag_piece
                                :where  [:= :tag_id knot-id]})))
(defn select-link-knot-piece-by-piece-id [conn piece-id]
  (jdbc/query conn (sql/format {:select :*
                                :from   :link_tag_piece
                                :where  [:= :piece_id piece-id]})))

(defn delete-link-knot-piece-by-knot-id [conn knot-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_tag_piece
                                   :where       [:= :tag_id knot-id]})))

(defn delete-link-knot-piece-by-piece-id [conn piece-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_tag_piece
                                   :where       [:= :piece_id piece-id]})))











;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; link (piece to piece)

(defn insert-link-piece-piece [conn from-id to-id]
  (jdbc/execute! conn (-> (sqh/insert-into :link_pieces)
                          (sqh/values [{:from_piece_id from-id
                                        :to_piece_id   to-id}])
                          (sqh/on-conflict :from_piece_id :to_piece_id)
                          sqh/do-nothing
                          sql/format)))

(defn select-link-piece-piece-by-from-id [conn from-id]
  (jdbc/query conn (sql/format {:select :*
                                :from   :link_pieces
                                :where  [:= :from_piece_id from-id]})))

(defn select-link-piece-piece-by-to-id [conn to-id]
  (jdbc/query conn (sql/format {:select :*
                                :from   :link_pieces
                                :where  [:= :to_piece_id to-id]})))

(defn delete-link-piece-piece [conn from-id to-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_pieces
                                   :where       [:and [:= :from_piece_id from-id]
                                                 [:= :to_piece_id to-id]]})))

(defn delete-link-piece-piece-by-from-id [conn from-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_pieces
                                   :where       [:= :from_piece_id from-id]})))

(defn delete-link-piece-piece-by-to-id [conn to-id]
  (jdbc/execute! conn (sql/format {:delete-from :link_pieces
                                   :where       [:= :to_piece_id to-id]})))








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; action

(defn load-or-save-new-piece [tx piece-subject]
  (if-let [piece (select-piece-by-subject tx piece-subject)]
    (piece :id)
    (let [piece-id (nano-pid)]
      (upsert-piece tx {:id      piece-id
                        :subject piece-subject})
      piece-id)))

(defn parse-tag [piece-id piece-content]
  (jdbc/with-db-transaction [tx db-config]
                            (delete-link-knot-piece-by-piece-id tx piece-id)
                            (doseq [knot (re-seq #"(?<=^|[^\w])#([^\s#]+)" piece-content)]
                              (let [knot-subject (second knot)
                                    knot-id (load-or-save-new-piece tx knot-subject)] ; TODO 없으면 생성
                                (upsert-link-knot-piece tx knot-id piece-id)))))










(defn parse-link [piece-from-id piece-content]
  (jdbc/with-db-transaction [tx db-config]
                            (delete-link-knot-piece-by-knot-id tx piece-from-id)
                            (doseq [link (re-seq #"\[\[(.*?)\]\]" piece-content)]
                              (let [piece-subject (second link)
                                    piece-to-id (load-or-save-new-piece tx piece-subject)]
                                (insert-link-piece-piece tx piece-from-id piece-to-id)))))








(defn save-piece [{:keys [path content]}]
  (println "** knot-save : " path)
  (let [piece-id (nano-pid)
        subject (str/replace path #".md" "")
        summary (re-find #"%%\s*summary:\s*(.*) %%" content)
        ;content (str/replace content-raw #"%%(.*?)%%\r?\n?" "")
        ]
    (upsert-piece db-config {:id      piece-id
                             :subject subject
                             :summary (second summary)
                             :content content})
    (parse-tag piece-id content)
    (parse-link piece-id content)))





(comment
  (re-matches #"^.*[0-9]{12}.*$" "2024/202404142323.md")

  (let [str "index.md" #_"2023/202304151432.md"]
    (println (if (re-matches #"^.*[0-9]{12}.*$" str)
               (str/replace str #"^.*20[0-9]{2}(....)[0-9]{4}.*$" "$1") "no"))
    #_(println (str/replace str #"^.*20[0-9]{4}(..)[0-9]{4}.*$" "$1"))
    )
  )


(defn remove-piece [path]
  (println "** knot-remove : " path)
  (let [subject (str/replace path #".md" "")]
    (jdbc/with-db-transaction [tx db-config]
                              (if-let [piece (select-piece-by-subject tx subject)]
                                (do
                                  (delete-piece tx (piece :id))
                                  (delete-link-knot-piece-by-piece-id tx (piece :id))
                                  (delete-link-piece-piece-by-from-id tx (piece :id))
                                  (delete-link-piece-piece-by-to-id tx (piece :id)))))))







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
  (select-piece-by-id db-config id))

#_(defn tags-recent [limit]
    (let [rs (jdbc/query db-config (sql/format {:select   [:*]
                                                :from     :knot_tags
                                                :order-by [[:mtime :desc]]
                                                :limit    limit}))]
      (if (empty? rs) nil rs)))


