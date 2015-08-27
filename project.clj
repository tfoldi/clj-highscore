(defproject clj-highscore "0.1.0-SNAPSHOT"
  :description "Generic High Score Server"
  :url "http://github.com/tfoldi/clj-highscore"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler clj-highscore.core/handler}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.13"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.4.0"]]
;  :main ^:skip-aot clj-highscore.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
