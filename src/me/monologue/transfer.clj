(ns me.monologue.transfer
  (:require [clj-jgit.internal :refer [get-head-commit resolve-object]]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer [changed-files-between-commits]]
            [clojure.string :as str]
            [me.raynes.fs :as fs]
            [immutant.scheduling :as cron]
            [me.monologue.mapper :as data]
            [me.monologue.constant :refer :all]
            [me.monologue.parser :as mpar]
            [taoensso.timbre :as b]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; git

(defn git-changes []
  (jgit/with-credentials git-config
                         (let [repo (jgit/load-repo (knot-config :workspace))
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
                                         :dir (knot-config :workspace))))

(defn git-pull []
  (try
    (jgit/with-credentials git-config
                           (jgit/git-pull (jgit/load-repo (knot-config :workspace))))
    (catch Exception _
      (git-clone))))

(defn parse-target? [path]
  (let [file (str/replace path #".*/" "")]
    (and (every? nil? [(re-find #"^\." path)
                       (re-find #"^/?files/" path)
                       (re-find #"^-" file)])
         (every? some? [(re-find #"\.md$" path)]))))


(defn slurp-file [path]
  {:path    path
   :content (slurp (str (knot-config :workspace) "/" path))})

(defn resource-task [action path]
  (cond
    (or (= action :add)
        (= action :edit)) (fs/copy+ (str (knot-config :workspace) "/" path)
                                    (str (knot-config :resource) "/" path))
    (= action :delete) (fs/delete (str (knot-config :resource) "/" path))
    :else (throw (Exception. (str "unknown action - " action))))

  (if (= path (knot-config :template-file))
    (mpar/load-template)))

(defn git-parse []
  (doseq [[path action] (git-changes)]
    (println "parse path : " path)
    (if (parse-target? path)
      (cond
        (or (= action :add)
            (= action :edit)) (data/save-piece (slurp-file path))
        (= action :delete) (data/remove-piece path)
        :else (throw (Exception. (str "unknown action - " action))))

      (resource-task action path)
      )))

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
