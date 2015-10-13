(ns clj-highscore.core
  (:gen-class)
  (:require [liberator.core :refer [resource defresource]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]

            [clj-highscore.db :as db]

            [ring.middleware.cors :as ring-cors]
            [ring.middleware.json :as ring-json]

            [environ.core :refer [env]]
            [cheshire.core :refer [generate-string]]
            [compojure.handler :refer [site]]
            [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))

;; The DBSpec for the score database
(def ^:private score-dbspec (env :database-url))

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
  (db/get-scores-for-game score-dbspec
                          game
                          (integerify offset)
                          (integerify limit 10))

  )

(defn- POST-param
  "Returns a POST parameter"
  [ctx param]
  (get-in ctx [:request :form-params param]))

(defn- GET-param
  "Returns a GET parameter"
  [ctx param] (get-in ctx [:request :params param]))

(defn- debug-val [v] (clojure.pprint/pprint v) v)

(defn- parse-json-body [context] (-> context :request :body slurp cheshire.core/parse-string))

(defresource get-scores
             [game]
             :allowed-methods [:get]
             :handle-ok (fn [ctx]
                          (debug-val game)
                          (->>
                            (all-scores game (GET-param ctx "offset") (GET-param ctx "limit"))
                            (vector game)
                            ;(generate-string)
                            ))
             :available-media-types ["application/json"])

(defresource add-score
             :allowed-methods [:post]
             :malformed? (fn [context]
                           (let [params (-> context :request :body)]
                             (or (empty? (params :game-type))
                                 (empty? (params :user-name))
                                 (not (number? (params :score)))
                                 (not (number? (params :duration))))))
             :handle-malformed "user-name, game-type, score, duration cannot be empty!"
             :post!
             (fn [context]
               (let [params (-> context :request :body)
                     ;; Add the IP address from the request.
                     ;; TODO: maybe use (or (get-in request [:headers "x-forwarded-for"]) (:remote-addr request))

                     ;; If the request comes from behind a proxy, this will
                     ;; be the address of the proxy.

                     ;; If we are behind a load balancer, we should be able to
                     ;; get the ip from the 'x-forwarded-for' header, but if the
                     ;; request comes from behind a proxy, it will be a list of
                     ;; IP addresses not a single one.
                     source-ip (-> context :request :remote-addr)]
                 (db/add-highscore score-dbspec params (params :events) source-ip)))
             :handle-created (fn [ctx]
                               (let [{:keys [game-type]} (-> ctx :request :body)]
                                 (db/get-scores-for-game score-dbspec game-type 0 10)))
             :available-media-types ["application/json"])


(defresource new-game
             [game-type]
             :allowed-methods [:post]
             :malformed? (fn [context]
                           (let [params (-> context :request :body)]
                             (or (empty? game-type)
                                 (empty? (params :user-name))
                                 (empty? (params :start-time)))))
             :handle-malformed "game-type, user-name and start-time cannot be empty"

             :post!
             (fn [context]
               (let [params (-> context :request :body)
                     source-ip (-> context :request :remote-addr)]
                 {"game-id" (db/add-new-game score-dbspec params source-ip)}))

             :handle-created (fn [ctx] {"game-id" (get ctx "game-id")})
             :available-media-types ["application/json"]
             )


(defresource add-events
             [game-id]
             :allowed-methods [:post]
             :malformed? (fn [context]
                           (let [params (-> context :request :body)]
                             (or (nil? game-id)
                                 (nil? (params :score))
                                 (nil? (params :duration))
                                 (empty? (params :events))
                                 )))

             :handle-malformed "score, duration and events cannot be emtpy"

             :post!
             (fn [context]
               (let [{:keys [score duration events]} (-> context :request :body)]
                 (db/add-events-to-game score-dbspec game-id duration score events)))


             :handle-created (fn [ctx] nil)
             :available-media-types ["application/json"]
             )

(defresource get-events
             [game-id]
             :allowed-methods [:get]
             :available-media-types ["application/json"]
             :malformed? (fn [context]
                           (or (nil? game-id)
                               (db/has-game score-dbspec game-id)))

             :handle-malformed "The game by the specified game id cannot be found."

             :handle-ok
             (fn [context]
               (db/get-game-and-events score-dbspec game-id)
               )
             )




(defroutes app
           (ANY "/" resource)
           (GET "/get-scores/:game" [game] (get-scores game))
           (GET "/get-events/:game-id" [game-id] (get-events (Integer. game-id)))

           (POST "/new-score" [] add-score)
           (POST "/new-game/:game" [game] (new-game game))

           (POST "/add-events/:game-id" [game-id] (add-events (Integer. game-id)))

           )

(def handler
  (-> app
      ;wrap-params
      (ring-cors/wrap-cors
        :access-control-allow-origin [#".*"]
        :access-control-allow-methods [:get :put :post :delete])
      (ring-json/wrap-json-body {:keywords? true :bigdecimals? true})
      (ring-json/wrap-json-response))

  )

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 80))]
    (jetty/run-jetty (site #'handler) {:port port :join? false})))
