(ns monologue.backend.gitter
  (:require [clj-jgit.internal :refer [get-head-commit resolve-object]]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer [changed-files-between-commits]]
            [clojure.string :as str]
            [immutant.scheduling :as cron]
            [monologue.backend.mapper :as data]
            [monologue.backend.constant :refer :all]
            [taoensso.timbre :as b]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; git

(defn git-changes []
  (jgit/with-credentials git-config
                         (let [repo (jgit/load-repo (git-config :workspace))
                               latest-commit (get-head-commit repo)
                               saved-commit (data/select-meta-content META-GIT-COMMIT-ID)
                               since-commit (if (nil? saved-commit)
                                              ((last (jgit/git-log repo :all? true)) :id)
                                              (resolve-object saved-commit repo))]
                           (if (not= (.name since-commit) (.name latest-commit))
                             (do
                               (if (@memo :git-commit-id-save?)
                                 (data/save-meta META-GIT-COMMIT-ID (.name latest-commit))
                                 ())
                               (changed-files-between-commits repo
                                                              since-commit
                                                              latest-commit))
                             ()))))

(defn git-clone []
  (jgit/with-credentials git-config
                         (jgit/git-clone (git-config :repo)
                                         :branch "main"
                                         :dir (git-config :workspace))))

(defn git-pull []
  (try
    (jgit/with-credentials git-config
                           (jgit/git-pull (jgit/load-repo (git-config :workspace))))
    (catch Exception _
      (git-clone))))

(defn parse-target? [path]
  (let [file (clojure.string/replace path #".*/" "")]
    (and (every? nil? [(re-find #"^\." path)
                       (re-find #"^/?files/" path)
                       (re-find #"^-" file)])
         (every? some? [(re-find #"\.md$" path)]))))


(parse-target? "2023/hehe.md")
(parse-target? "2023/-hehe.md")
(clojure.string/replace "2023/hehe.md" #".*/" "")


(defn slurp-file [path]
  {:path    path
   :content (slurp (str (git-config :workspace) "/" path))})

(defn git-parse []
  (doseq [[path action] (git-changes)]
    (println "path : " path)
    (if (parse-target? path)
      (cond
        (or (= action :add)
            (= action :edit)) (data/save-piece (slurp-file path))
        (= action :delete) (data/remove-piece path)
        :else (throw (Exception. (str "unknown action - " action))))
      (b/info "** ignored - " path))))

(defn reload-md []
  (git-pull)
  (git-parse))


(defn reload-schedule []
  (b/info "** reload")
  (cron/schedule #(reload-md) (cron/cron "0 */1 * ? * *")))

(comment
  (git-clone)
  (git-parse)
  (git-changes)
  (git-pull)
  (memo-set :git-commit-id-save? true)
  (memo-set :git-commit-id-save? false)
  )
