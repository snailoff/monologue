(ns knot.frontend.state
  (:require [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.format :as timef]
            [reagent.core :refer [atom]]
            [taoensso.timbre :as b]))


(defonce s-piece (atom {}))
(defonce s-pieces (atom []))


(def custom-formatter (timef/formatter "yyyy-MM-dd'T'hh:mm:ss'Z"))
(def knot-time-format (timef/formatter "yyyy.MM.dd hh:mm:ss"))

(defn set-piece [piece-data]
  (let [{:keys [content]} piece-data

        content_parsed (-> content
                           (str/replace #"#[^\s]+" "")
                           (str/replace #"\n" "<br />")
                           (str/replace #"!\[\[(.*?)\]\]" "<figure class=\"image\"><img src=\"files/$1\" /></figure>")
                           (str/replace #"\[(.*?)\]\((.*?)\)" "<a href=\"$2\">$1</a>"))
        ctime (timef/parse custom-formatter (piece-data :ctime))
        mtime (timef/parse custom-formatter (piece-data :mtime))]
    (prn (timef/unparse knot-time-format ctime))
    (reset! s-piece (conj piece-data
                          {:content-parsed content_parsed
                           :ctime (timef/unparse knot-time-format ctime)
                           :mtime (timef/unparse knot-time-format mtime)}))
    (prn @s-piece)))

