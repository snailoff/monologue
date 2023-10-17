(ns monologue.knot.parser
  (:require [monologue.knot.mapper :as mmap]
            [monologue.knot.constant :refer [db-config knot-config]]
            [monologue.knot.functions :refer :all]
            [me.raynes.fs :as fs]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [clojure.string :as str]))

(def date-formatter (f/with-zone (f/formatter "yyyy년 MM월 dd일 HH시 mm분") (t/time-zone-for-id "Asia/Seoul")))
(defn sqltime2str [date]
  (println "*** " date)
  (if-not (nil? date)
    (f/unparse date-formatter (c/from-sql-time date))
    "no date"))


(defn eval-string [text]
  (try
    #_(binding [*ns* 'monologue.knot.parser])
    (eval (read-string text))
    (catch Exception _
      (str "<i>functions 에러 - " text "</i>"))))

(defn -read-template []
  (let [path (str (knot-config :resource) "/" (knot-config :template-file))]
    (if (fs/exists? path) (slurp path) "")))

(def mem-template (atom (-read-template)))

(defn reload-template []
  (reset! mem-template (-read-template)))

(defn parse-functions [text req-name]
  (-> text
      (str/replace #"::req-name::" req-name)
      (str/replace #"@(\(.*?\))" (fn [[_ k]] (eval-string k)))))

(defn parse-content [text req-name]
  (-> text
      (parse-functions req-name)
      (str/replace #"!\[\[(.*?)\]\]" "<img src=\"/assets/$1\" alt=\"\"/>")
      (str/replace #"\$" "\\$")
      (str/replace #"\n" "<br />")))



(defn html-wrap [req-name {:keys [subject summary ctime mtime content]}]
  (-> (parse-functions @mem-template req-name)
      (str/replace #"::req-name::" req-name)
      (str/replace #"@(\(.*?\))" (fn [[_ k]] (eval-string k)))
      (str/replace #"::page-subject::" (str subject))
      (str/replace #"::page-summary::" (str summary))
      (str/replace #"::page-ctime::" (sqltime2str ctime))
      ;(str/replace #"::page-mtime::" (sqltime2str mtime))
      (str/replace #"::page-content::" (str/re-quote-replacement (parse-content content req-name)))))


(defn parse [req-name]
  (if-let [piece (mmap/select-piece-by-subject db-config req-name)]
    (html-wrap req-name piece)
    (if-let [default-p (mmap/select-piece-by-subject db-config (knot-config :default-page))]
      (html-wrap req-name default-p)
      (html-wrap req-name {:subject "404" :summary "" :content (str "<i>(knot-config :default-page) 없음 - " (knot-config :default-page))}))))


(let [req-name"202307301226"
      content ((mmap/select-piece-by-subject db-config req-name) :content)]
  ;(html-wrap req-name {:content content})

  ;(parse-content content req-name)
  (str/replace @mem-template
               #"::page-content::"
               (str/re-quote-replacement "<br />$ ystemctl --user enable caffeine <br />```<br /><br />")
               ;"caffeine 자동 시작.<br /><br />~/.config/systemd/user/caffeine.service<br />```<br />[Unit]<br />Description=caffeine<br /><br />[Service]<br />Type=simple<br />ExecStart=/usr/bin/caffeine<br />Restart=on-failure<br />StartLimitIntervalSec=10 # 재시도시 딜레이<br />StartLimitBurst=5 # 재시도 횟수<br /><br />[Install]<br />WantedBy=default.target<br /><br />```<br /><br /><br />```<br />$ ystemctl --user enable caffeine <br />```<br /><br />"
               )

  )
(parse-content ((mmap/select-piece-by-subject db-config "202307301226") :content)
               "202307301226")


;(reload-template)