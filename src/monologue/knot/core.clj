(ns monologue.knot.core
  (:require [monologue.knot.transfer :as mtra]
            [monologue.knot.router :as mrou]
            [taoensso.timbre :as timbre])

  (:gen-class))

(timbre/set-config!
  {:min-level [["taoensso.*" :error]
               ;["org.eclipse.jgit.*" :info]
               ["org.eclipse.*" :error]
               ]})

(defn -main
  [& _]
  (mtra/reload-schedule)
  (mrou/start))


(comment
  (mrou/start))

