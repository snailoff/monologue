(defproject monologue-backend "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [environ "1.2.0"]
                 [clj-jgit "1.0.2"]
                 [com.taoensso/timbre "6.0.4"]
                 [org.immutant/scheduling "2.1.10"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.github.seancorfield/honeysql "2.4.980"]
                 [org.postgresql/postgresql "42.5.4"]
                 [metosin/reitit "0.5.18"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring-cors "0.1.13"]]

  :plugins [[lein-environ "1.2.0"]]
  :profiles {:uberjar {:aot :all}}

  :source-paths ["src"]
  :clean-targets ^{:protect false} ["target"]
  :main monologue.backend.core)

