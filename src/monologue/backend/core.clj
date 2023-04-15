(ns monologue.backend.core
  (:require [monologue.backend.gitter :as gitter]
            [monologue.backend.router :as router]
            [taoensso.timbre :as timbre])

  (:gen-class))

(timbre/set-config!
  {:min-level [["taoensso.*" :error]
               ["org.eclipse.jgit.*" :info]
               ["*" :debug]]})

(defn -main
  [& _]
  (gitter/reload-schedule)
  (router/start))


(comment
  (router/start))

