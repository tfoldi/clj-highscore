(ns clj-highscore.core
  (:gen-class)
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
    ;[korma.db]
    ;[korma.core]
            [clojure.java.jdbc :as db]
            [environ.core :refer [env]]
            [cheshire.core :refer [generate-string]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))


; TODO: from DB
(def all-scores (atom [{:id 1 :game :tableu-cnake :score 1 :date "2015-08-27T21:23" :game-time 30}
                       {:id 2 :game :tableu-cnake :score 2 :date "2015-08-27T21:24" :game-time 22}]))

(defresource get-scores
             :allowed-methods [:get]
             :handle-ok (fn [context]
                          (let [game (get-in context [:request :params :game])]
                            (generate-string (vector game
                                                     (db/query (env :database-url)
                                                               ["select * from scores where game = ? " (second game)]
                                                               )
                                                     ))))
             :available-media-types ["application/json"])

(defresource add-score
             :allowed-methods [:post]
             :malformed? (fn [context]
                           (let [params (get-in context [:request :form-params])]
                             (empty? (get params "game"))))
             :handle-malformed "user name cannot be empty!"
             :post!
             (fn [context]
               (let [params (get-in context [:request :form-params])]
                 (swap! users conj (get params "game"))))
             :handle-created (fn [_] (generate-string @all-scores))
             :available-media-types ["application/json"])

(defroutes app
           (ANY "/" resource)
           (ANY "/get-scores" resource get-scores))

(def handler
  (-> app
      wrap-params))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 80))]
    (jetty/run-jetty (site #'handler) {:port port :join? false})))
