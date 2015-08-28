(ns clj-highscore.core
  (:gen-class)
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.java.jdbc :as db]
            [environ.core :refer [env]]
            [cheshire.core :refer [generate-string]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))

;; You are not needed...
#_ (defn- parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))

(defn- integerify
  "Tries to convert a string to an integer or returns default-value/0 otherwise."
 ; main arity
 ([a-string default-value]
  (try
   (if (or (nil? a-string) (= "" a-string))
       default-value
       (Integer/valueOf a-string))
   (catch Exception e default-value)))
 ; helper arity for 0
 ([a-string] (integerify a-string 0)))

(defn all-scores
  "Returns all scores for the game named game, starting from offset, max
   limit results. If either offset or limit cannot be converted to an integer.
   they will be set to 0 and 100 respectively."
  [game offset limit]

  (db/query (env :database-url "postgres:highscore")
            ["select *
            from scores w
            where game = ?
            order by score desc, gametime asc
            limit ? offset ?" game
	    (integerify limit 100)
	    (integerify offset)]))

(defn- POST-param
  "Returns a POST parameter"
  [ctx param]
  (get-in ctx [:request :form-params param]))

(defn- GET-param
  "Returns a GET parameter"
  [ctx param] (get-in ctx [:request :params param]))

(defresource get-scores
             [game]
             :allowed-methods [:get]
             :handle-ok (fn [ctx]
                          (generate-string
                            (vector game
                                    (all-scores game (GET-param ctx "offset") (GET-param ctx "limit")))))
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
        ;         (db/insert (env :database-url "postgres:highscore")
         ;          :scores
 	
))
             :handle-created (fn [_] (generate-string (all-scores "tableau-cnake")))
             :available-media-types ["application/json"])

(defroutes app
           (ANY "/" resource)
           (GET "/get-scores/:game" [game] (get-scores game)))

(def handler (-> app wrap-params))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 80))]
    (jetty/run-jetty (site #'handler) {:port port :join? false})))
