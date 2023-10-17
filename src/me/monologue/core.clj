(ns me.monologue.core
  (:require [me.monologue.transfer :as gitter]
            [me.monologue.router :as router]
            [taoensso.timbre :as timbre])

  (:gen-class))

(timbre/set-config!
  {:min-level [["taoensso.*" :error]
               ;["org.eclipse.jgit.*" :info]
               ["org.eclipse.*" :error]
               ]})

(defn -main
  [& _]
  (gitter/reload-schedule)
  (router/start))


(comment
  (router/start))

