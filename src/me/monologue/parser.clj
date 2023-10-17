(ns me.monologue.parser
  (:require [me.monologue.mapper :as mmap]
            [me.monologue.constant :refer [db-config knot-config]]
            [me.raynes.fs :as fs]
            [clojure.string :as str]))


(defn eval-string [text]
  (try
    #_(binding [*ns* 'me.monologue.parser])
    (eval (read-string text))
    (catch Exception _
      "<i>뭔가 잘못 되었다</i>")))

(def mem-template (atom ""))

(defn load-template []
  (let [path (str (knot-config :resource) "/" (knot-config :template-file))]
    (reset! mem-template (if (fs/exists? path) (slurp path) ""))))

(defn html-wrap [piece content]
  (-> @mem-template
      (str/replace #"::page-name::" (piece :subject))
      (str/replace #"::page-subject::" (piece :subject))
      ;(str/replace #"::page-summary::" (piece :summary))
      (str/replace #"::page-content::" content)))

(defn parse [page-name]
  (if-let [piece (mmap/select-piece-by-subject db-config page-name)]
    (html-wrap
      piece
      (-> (piece :content)
          (str/replace #"::page-name::" page-name)
          (str/replace #"::page-subject::" (piece :subject))
          ;(str/replace #"::page-summary::" (piece :summary))
          (str/replace #"@(\(.*?\))" (fn [[_ k]] (eval-string k)))
          (str/replace #"\n" "<br />")))
    (html-wrap
      {:subject "404" :summary ""}
      "<i>no pages</i>")))








