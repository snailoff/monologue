(defproject knot-backend "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [environ "1.2.0"]
                 [clj-jgit "1.0.2"]
                 [com.taoensso/timbre "5.1.2"]
                 [org.immutant/scheduling "2.1.10"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.github.seancorfield/honeysql "2.2.861"]
                 [org.postgresql/postgresql "42.3.2"]
                 [metosin/reitit "0.5.15"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring-cors "0.1.13"]]

  :plugins [[lein-environ "1.2.0"]]
  :profiles {:uberjar {:aot :all}}

  :source-paths ["src"]
  :clean-targets ^{:protect false} ["target"]
  :main knot.backend.core)

