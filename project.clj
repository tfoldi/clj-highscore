(defproject clj-highscore "0.1.0-SNAPSHOT"
  :description "Generic High Score Server"
  :url "http://github.com/tfoldi/clj-highscore"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [liberator "0.13"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [compojure "1.4.0"]
                 [cheshire "5.5.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [environ "1.0.0"]
                 [ring/ring-core "1.4.0"]

                 ;; JSON support
                 [ring/ring-json "0.4.0"]

                 ;; SQL migration support
                 [ragtime "0.5.1"]

                 ;; Time & money
                 [clj-time "0.11.0"]

                 ;; CORS support
                 [ring-cors "0.1.7"]]

  :exclusions [org.clojure/clojure ]

  ;; Add SQL migration lein targets
  ;;
  ;; Running lein migrate will now migrate the database
  ;; to the latest version, and lein rollback will roll
  ;; the database back one version.

  ;; Until a version 0.5.2 is available with the windows path serparator
  ;; fix, its not recommended to run this version, or at least apply
  ;; the changes in this patch:
  ;;
  ;; "Escape file separator in regex"
  ;; https://github.com/cnatoli/ragtime/commit/636775a5ec9593c3b511cded27f345d1e509df60

  :aliases {"migrate"  ["run" "-m" "clj-highscore.migrate/migrate"]
            "rollback" ["run" "-m" "clj-highscore.migrate/rollback"]}


  ;; Add the environment plugin
  :plugins [[lein-environ "1.0.0"]
            [lein-ring "0.8.11"]]

  :ring {:handler clj-highscore.core/handler}

  :main ^:skip-aot clj-highscore.core

  :target-path "target/%s"
  :profiles {:dev        {:env {:database-url "jdbc:postgresql://localhost:5432/clj_highscore?user=test&password=test"
                                :production   false}}
             :production {:env {:database-url "jdbc:postgresql://localhost:5432/clj_highscore"
                                :production   true}}})
