(ns monologue.backend.gitter
  (:require [clj-jgit.internal :refer [get-head-commit resolve-object]]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer [changed-files-between-commits]]
            [immutant.scheduling :as cron]
            [monologue.backend.mapper :as data]
            [monologue.backend.constant :refer :all]
            [taoensso.timbre :as b]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; git

(defn git-changes []
  (jgit/with-credentials git-config
                         (let [repo (jgit/load-repo (git-config :workspace))
                               latest-commit (get-head-commit repo)
                               saved-commit (data/load-meta-content META-GIT-COMMIT-ID)
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
  (and (every? nil? [(re-find #"^\." path)
                     (re-find #"^/?files/" path)])
       (every? some? [(re-find #"\.md$" path)])))


(defn git-parse []
  (doseq [[path action] (git-changes)]
    (if (parse-target? path)
      (cond
        (or (= action :add)
            (= action :edit)) (data/knot-save path action)
        (= action :delete) (data/knot-remove path action)
        :else (throw (Exception. (str "unknown action - " action))))
      (b/info "** ignored - " path))))
(comment
  (doseq [[path action] (git-changes)]
    (println "** change : " path )
    )

  (git-changes)
  (git-parse)
  )



(defn reload-md []
  (git-pull)
  (git-parse))


(defn reload-schedule []
  (b/info "** reload")
  (cron/schedule #(reload-md) (cron/cron "0 */1 * ? * *")))

(comment
  (git-parse)
  (git-changes)
  (git-pull)

  (reload-md)


  (memo-set :git-commit-id-save? true)
  (memo-set :git-commit-id-save? false)

  #_(let [subject "nana"
          _ (save-piece db-config subject "su" "co")
          p (load-piece subject)]
      (save-link-piece db-config (p :id) 0)
      (save-link-piece db-config 99 (p :id))
      (remove-piece subject))

  #_(remove-piece "lala")
  #_(remove-link-piece-children db-config 25))
