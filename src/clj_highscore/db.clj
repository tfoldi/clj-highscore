(ns clj-highscore.db
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]

            [net-starschema.utils.inflector :as inflector]))

(def ^:private score-dbspec (env :database-url))


(defn- game-id-for
  "Returns the game_type_id for the given game-type-name or nil if such game is not
  in the allowed game types."
  [dbspec game-type-name]
  (let [game-type (jdbc/query dbspec ["SELECT id FROM game_types WHERE game_name=?" game-type-name])]
    (if (not-empty game-type)
      (-> game-type first :id)
      nil)))

(defn- event-type-id-for
  "Returns the event_type_id for the given event-type-name or nil if such event is not
  in the allowed event types."
  [dbspec event-type-name]
  (let [event-type (jdbc/query dbspec ["SELECT id FROM event_types WHERE event_name=?" event-type-name])]
    (if (not-empty event-type)
      (-> event-type first :id)
      nil)))

(defn has-game [dbspec game-id]
  (> 0
     (-> (jdbc/query dbspec ["SELECT COUNT(1) FROM games WHERE id=?" game-id])
         first
         :count)))

(defn add-highscore
  "Adds a high-score to the database"
  [dbspec {:keys [user-name game-type start-time score duration] :as game-data} events source-ip]
  (let [game-id (game-id-for dbspec game-type)]
    ;; Throw an error if the game type cannot be found
    (when (nil? game-id)
      (throw (ex-info (str "Cannot find game by name: " game-type)
                      {})))

    ;; Add the game entry
    (let [game-entry (jdbc/insert! dbspec
                                   :games
                                   {:game-type-id game-id
                                    :user-name    user-name
                                    :start-time   (tc/to-sql-time start-time)
                                    :score        score
                                    :duration     duration
                                    :source-ip    source-ip}
                                   :entities inflector/sqlify-str)
          game-id (-> game-entry first :id)
          event-type-ids (->> events
                              (map :type)
                              sort
                              distinct
                              (map (fn [et] [et (event-type-id-for dbspec et)]))
                              (into {}))]
      (apply jdbc/insert! dbspec
             :events
             (concat
               ;; Get the proper stuff
               (map (fn [{:keys [timestamp type]}]
                      {:ts            timestamp
                       :game-id       game-id
                       :event_type_id (event-type-ids type)})
                    events)

               ;; convert entities
               [:entities inflector/sqlify-str])
             ))))

(defn add-new-game [dbspec {:keys [user-name game-type start-time] :as game-data} source-ip]

  (let [game-id (game-id-for dbspec game-type)]
    ;; Throw an error if the game type cannot be found
    (when (nil? game-id)
      (throw (ex-info (str "Cannot find game by name: " game-type)
                      {})))

    ;; Add the game entry
    (let [game-entry (jdbc/insert! dbspec
                                   :games
                                   {:game-type-id game-id
                                    :user-name    user-name
                                    :start-time   (tc/to-sql-time start-time)
                                    :score        nil
                                    :duration     nil
                                    :source-ip    source-ip}
                                   :entities inflector/sqlify-str)
          game-id (-> game-entry first :id)]
      ;; Return the id of this game
      game-id
      ))
  )


(defn add-events-to-game [dbspec game-id duration score events]

  ;; Throw an error if the game type cannot be found
  (when (nil? game-id)
    (throw (ex-info (str "Cannot find game by id: " game-id)
                    {})))

  ;; Update the games attributes to the new value
  (jdbc/update! dbspec :games
                {:duration duration
                 :score    score}
                ["id = ?" game-id])

  ;; Add the game entry
  (let [event-type-ids (->> events
                            (map :type)
                            sort
                            distinct
                            (map (fn [et] [et (event-type-id-for dbspec et)]))
                            (into {}))]

    (apply jdbc/insert! dbspec
           :events
           (concat
             ;; Get the proper stuff
             (map (fn [{:keys [timestamp type]}]
                    {:ts            timestamp
                     :game-id       game-id
                     :event_type_id (event-type-ids type)})
                  events)

             ;; convert entities
             [:entities inflector/sqlify-str])
           ))
  )

(defn get-scores-for-game [dbspec game-name offset limit]
  (->> (jdbc/query dbspec
                   ["SELECT games.user_name,
                       games.score,
                       games.duration FROM games
                      INNER JOIN game_types ON (game_types.id = games.game_type_id)
                      WHERE game_name = ?
                      ORDER BY score DESC NULLS LAST, duration ASC
                      OFFSET ?
                      LIMIT ?;"
                    game-name offset limit]
                   :identifiers inflector/keywordize)
       vec))


(defn get-game-and-events [dbspec game-id]
  (let [game-data (jdbc/query dbspec
                              ["SELECT
                                 games.user_name,
                                 games.score,
                                 games.duration,
                                 game_types.game_name
                               FROM
                                 games
                               INNER JOIN game_types ON (game_types.id= games.game_type_id)
                               WHERE games.id = ?;" game-id])

        event-data (jdbc/query dbspec
                               ["SELECT
                                  events.id,
                                  events.ts,
                                  event_types.event_name
                                 FROM
                                   events
                                 INNER JOIN event_types ON (events.event_type_id = event_types.id)
                                 WHERE events.game_id=?" game-id])]
    (when-first [game game-data]
      (assoc game :events event-data))))