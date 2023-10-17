(ns monologue.knot.functions
  (:require [clojure.java.jdbc :as jdbc]
            [honey.sql :as sql]
            [hiccup2.core :as h]
            [monologue.knot.constant :refer [db-config]]))




(defn pages-in-year [year]
  (let [pieces (jdbc/query db-config
                           (sql/format {:select   [:*]
                                        :from     :knot_pieces
                                        :where    [:= :%substring.rdate.0.5 year]
                                        :order-by [[:mtime :desc]]}))]
    (str (h/html
           (if-not (empty? pieces)
             [:div
              (map #(vec [:p [:a {:href (str "/page/" (% :subject))} (% :subject)]]) pieces)]
             [:i "no pages"])))))

(defn pages-in-tag [tag-name]
  (let [pieces (jdbc/query db-config
                           (sql/format {:select   [:knot_pieces.*]
                                        :from     :knot_pieces
                                        :join     [:link_tag_piece [:= :link_tag_piece.piece_id :knot_pieces.id]]
                                        :where    [:= :link_tag_piece.tag_name tag-name]
                                        :order-by [[:mtime :desc]]}))]
    (str (h/html
           (if-not (empty? pieces)
             [:div
              (map #(vec [:p [:a {:href (str "/page/" (% :subject))} (% :subject)]]) pieces)]
             [:i "no pages"])))))


(defn pages-recent [limit]
  (let [pieces (jdbc/query db-config (sql/format {:select   [:*]
                                                  :from     :knot_pieces
                                                  :order-by [[:mtime :desc]]
                                                  :limit    limit}))]
    (str (h/html
           (if-not (empty? pieces)
             [:div
              (map #(vec [:p [:a {:href (str "/page/" (% :subject))} (% :subject)]]) pieces)
              [:p "..."]]
             [:i "no pages"])))))

(defn tags []
  (let [tags (jdbc/query db-config (sql/format {:select   :tag_name
                                                :from     :link_tag_piece
                                                :group-by [:tag_name]
                                                :order-by [[:tag_name]]}))]
    (str (h/html
           (if-not (empty? tags)
             [:div
              (map #(vec [:span "#"
                          [:a {:href (str "/page/" (% :tag_name))} (% :tag_name)]
                          " "]) tags)]
             [:i "no pages"])))))













