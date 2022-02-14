(ns main
  (:require [clj-jgit.internal :refer :all]
            [clj-jgit.porcelain :as jgp]
            [clj-jgit.querying :refer :all]
            [immutant.scheduling :as ims]))

(def client-config {:login (System/getenv "KNOT_GIT_USER")
                    :pw (System/getenv "KNOT_GIT_PASSWORD")
                    :repo (System/getenv "KNOT_GIT_REPOSITORY")})

(defn git-clone []
  (jgp/with-credentials client-config
                        (jgp/git-clone (client-config :repo)
                                       :branch "main"
                                       :dir "temp")))
(defn git-pull []
  (jgp/with-credentials client-config
                        (jgp/git-pull (jgp/load-repo "temp")))
  (println "pull?"))

;(defn git-changes []
;  (jgp/with-credentials client-config
;                        (let [repo (jgp/load-repo "temp")
;                              rev-walk (new-rev-walk repo)]
;                          ;(find-rev-commit repo rev-commit)
;                          (changed-files-between-commits repo
;                                                         (resolve-object repo "124b7e92e0902f40a1863d37c5c552b1ceb7789d")
;                                                         (resolve-object repo "af94d3067583a3101dc9844ed8fb753e9b8ccb72"))
;                          )))

(defn git-schedule []
  (ims/schedule #(git-pull) (ims/cron "0 * * ? * *")))

(defn -main
  [& args]
  (println client-config))
