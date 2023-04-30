(ns monologue.frontend.state
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [reagent.core :refer [atom]]))


(defonce s-piece (atom {}))
(defonce s-pieces (atom []))


(def custom-formatter (timef/formatter "yyyy-MM-dd'T'hh:mm:ss'Z"))
(def knot-time-format (timef/formatter "yyyy.MM.dd hh:mm:ss"))

(defn o2o [subject]
      (-> subject
          (str/replace #"0" "o")
          (str/replace #"^.*?/" "")))

(defn set-pieces [res-data]
      (reset! s-pieces
              (map #(assoc % :subject (-> (% :subject) o2o)) res-data)))
(defn set-piece [res-data]
      (let [{:keys [content]} res-data
            content_parsed (-> content
                               (str/replace #"#[^\s]+" "")
                               (str/replace #"\n" "<br />")
                               (str/replace #"!\[\[(.*?)\]\]" "<figure class=\"image\"><img src=\"https://b.monologue.me/public/$1\" /></figure>")
                               (str/replace #"\[\[(.*?)\]\]" "<a href=\"/#/piece/$1\">$1</a>")
                               (str/replace #"\[(.*?)\]\((.*?)\)" "<a href=\"/#/piece/$2\">$1</a>")
                               (str/replace #"%%.*?%%" ""))
            ctime (timef/parse custom-formatter (res-data :ctime))
            mtime (timef/parse custom-formatter (res-data :mtime))]
           (reset! s-piece (conj res-data
                                 {:subject        (-> (res-data :subject) o2o)
                                  :content-parsed content_parsed
                                  :ctime          (timef/unparse knot-time-format ctime)
                                  :mtime          (timef/unparse knot-time-format mtime)
                                  :music          (edn/read-string (last (re-find #"%%\s*music:\s*(.*)\s*%%" content)))
                                  }))
           #_(prn @s-piece)))

