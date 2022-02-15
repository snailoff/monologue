(defproject knot-md-backend "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [environ "1.2.0"]
                 [clj-jgit "1.0.2"]
                 [org.immutant/scheduling "2.1.10"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [com.github.seancorfield/honeysql "2.0.0-rc2"]
                 [org.postgresql/postgresql "42.2.14"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [metosin/reitit "0.5.15"]]
  :plugins [[lein-environ "1.2.0"]]
  :profiles {:dev [:local-db]}
  :source-paths ["."]
  :main main)
