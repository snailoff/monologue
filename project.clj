(defproject knot-monologue "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [environ "1.2.0"]
                 [clj-jgit "1.0.2"]
                 [org.immutant/scheduling "2.1.10"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [com.github.seancorfield/honeysql "2.4.980"]
                 [org.postgresql/postgresql "42.5.4"]
                 [metosin/reitit "0.5.18"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [clj-time "0.15.2"]
                 [ring-cors "0.1.13"]
                 [nano-id "1.0.0"]
                 [org.slf4j/slf4j-api "1.7.14"]
                 [com.taoensso/timbre "6.1.0"]
                 [clj-commons/fs "1.6.310"]
                 [hiccup "2.0.0-RC2"]]

  :plugins [[lein-environ "1.2.0"]
            [lein-pprint "1.3.2"]]
  :profiles {:uberjar {:aot :all}
             :dev     [:test]}
  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["/tmp/knot-resource"]
  :clean-targets ^{:protect false} ["target"]
  :main monologue.knot.core)

