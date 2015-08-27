(ns clj-highscore.core
  (:gen-class)
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [environ.core :refer [env]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))

(defroutes app
           (ANY "/" [] (resource))
           (ANY "/foo" [] (resource :available-media-types ["text/html"]
                                    :handle-ok (fn [ctx]
                                                 (format "<html>It's %d milliseconds since the beginning of the epoch."
                                                         (System/currentTimeMillis))))))

(def handler
  (-> app
      wrap-params))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 80))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))
