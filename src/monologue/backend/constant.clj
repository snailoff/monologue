(ns monologue.backend.constant
  (:require [environ.core :refer [env]]))

(def db-config {:dbtype      "postgresql"
                :host        (env :monologue-db-host)
                :port        (env :monologue-db-port)
                :dbname      (env :monologue-db-name)
                :user        (env :monologue-db-user)
                :password    (env :monologue-db-password)
                :auto-commit true})

(def git-config {:login     (env :monologue-git-user)
                 :pw        (env :monologue-git-token)
                 :repo      (env :monologue-git-repository)
                 :workspace (env :monologue-git-workspace)})

(def memo (atom {:git-commit-id-save? true}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")
