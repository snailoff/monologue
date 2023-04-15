(ns monologue.backend.constant)

(def db-config {:dbtype      "postgresql"
                :host        (System/getenv "MONOLOGUE_DB_HOST")
                :port        (System/getenv "MONOLOGUE_DB_PORT")
                :dbname      (System/getenv "MONOLOGUE_DB_NAME")
                :user        (System/getenv "MONOLOGUE_DB_USER")
                :password    (System/getenv "MONOLOGUE_DB_PASSWORD")
                :auto-commit true})

(def git-config {:login     (System/getenv "MONOLOGUE_GIT_USER")
                 :pw        (System/getenv "MONOLOGUE_GIT_TOKEN")
                 :repo      (System/getenv "MONOLOGUE_GIT_REPOSITORY")
                 :workspace (System/getenv "MONOLOGUE_GIT_WORKSPACE")})

(def memo (atom {:git-commit-id-save? false}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")
