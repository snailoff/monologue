(ns knot.backend.gitter
  (:require [clj-jgit.internal :refer [get-head-commit resolve-object]]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying :refer [changed-files-between-commits]]
            [immutant.scheduling :as cron]
            [knot.backend.mapper :as data]
            [taoensso.timbre :as b]))


(def memo (atom {:git-commit-id-save? false}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")

(def git-config {:login (System/getenv "KNOT_GIT_USER")
                 :pw    (System/getenv "KNOT_GIT_PASSWORD")
                 :repo  (System/getenv "KNOT_GIT_REPOSITORY")})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; git

(defn git-changes []
  (jgit/with-credentials git-config
                         (let [repo (jgit/load-repo "temp")
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
                                         :dir "temp")))

(defn git-pull []
  (try
    (jgit/with-credentials git-config
                           (jgit/git-pull (jgit/load-repo "temp")))
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



(defn reload-md []
  (git-pull)
  (git-parse))


(defn reload-schedule []
  (cron/schedule #(reload-md) (cron/cron "0 */1 * ? * *")))

(comment

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
