(ns me.monologue.parser
  (:require [me.monologue.mapper :as mmap]
            [me.monologue.generator :as mgen]
            [me.monologue.constant :refer [db-config knot-config]]
            [me.raynes.fs :as fs]))


(defn eval-string [text]
  (try
    #_(binding [*ns* 'me.monologue.parser])
    (eval (read-string text))
    (catch Exception _
      "<i>뭔가 잘못 되었다</i>")))


(def template (if (fs/exists? (str (knot-config :resource) "/knot.html"))
                (slurp (str (knot-config :resource) "/knot.html"))
                ""))

(defn html-wrap [piece content]
  (-> template
      (clojure.string/replace #"::page-name::" (piece :subject))
      (clojure.string/replace #"::page-subject::" (piece :subject))
      (clojure.string/replace #"::page-summary::" (piece :summary))
      (clojure.string/replace #"::page-content::" content)))

(defn parse [page-name]
  (if-let [piece (mmap/select-piece-by-subject db-config page-name)]
    (html-wrap
      piece
      (-> (piece :content)
          (clojure.string/replace #"::page-name::" page-name)
          (clojure.string/replace #"::page-subject::" (piece :subject))
          (clojure.string/replace #"::page-summary::" (piece :summary))
          (clojure.string/replace #"@(\(.*?\))" (fn [[_ k]] (eval-string k)))
          (clojure.string/replace #"\n" "<br />")))
    (html-wrap
      {:subject "404" :summary ""}
      "<i>no pages</i>")))









