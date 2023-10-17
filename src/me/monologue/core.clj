(ns me.monologue.core
  (:require [me.monologue.transfer :as mtra]
            [me.monologue.router :as mrou]
            [me.monologue.parser :as mpar]
            [taoensso.timbre :as timbre])

  (:gen-class))

(timbre/set-config!
  {:min-level [["taoensso.*" :error]
               ;["org.eclipse.jgit.*" :info]
               ["org.eclipse.*" :error]
               ]})

(defn -main
  [& _]
  (mpar/load-template)
  (mtra/reload-schedule)
  (mrou/start))


(comment
  (mrou/start))

