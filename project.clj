(defproject title-sketch "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.apache.commons/commons-lang3 "3.4"]
                 [info.debatty/java-string-similarity "0.18"]
                 [cheshire "5.6.3"]
                 [clj-time "0.13.0"]]
  ;;:resource-paths ["resources"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :jvm-opts ["-Xmx6g"]
                   :source-paths ["dev"]}})
