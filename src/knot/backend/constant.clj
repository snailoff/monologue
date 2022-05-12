(ns knot.backend.constant)

(def db-config {:dbtype      "postgresql"
                :host        (System/getenv "KNOT_DB_HOST")
                :port        (System/getenv "KNOT_DB_PORT")
                :dbname      (System/getenv "KNOT_DB_NAME")
                :user        (System/getenv "KNOT_DB_USER")
                :password    (System/getenv "KNOT_DB_PASSWORD")
                :auto-commit true})

(def git-config {:login     (System/getenv "KNOT_GIT_USER")
                 :pw        (System/getenv "KNOT_GIT_PASSWORD")
                 :repo      (System/getenv "KNOT_GIT_REPOSITORY")
                 :workspace (System/getenv "KNOT_GIT_WORKSPACE")})

(def memo (atom {:git-commit-id-save? false}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")
