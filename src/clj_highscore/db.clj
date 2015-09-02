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

(defn add-highscore
  "Adds a high-score to the database"
  [dbspec {:keys [user-name game-type start-time score duration] :as game-data} events]
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
                                    :duration     duration}
                                   :entities inflector/sqlify-str)
          game-id (-> game-entry first :id)
          event-type-ids (->> events
                              (map :type)
                              sort
                              distinct
                              (map (fn [et] [et (event-type-id-for dbspec et)]))
                              (into {}))]

      (clojure.pprint/pprint (map (fn [{:keys [timestamp type]}]
                      {:ts            timestamp
                       :game-id       game-id
                       :event_type_id (event-type-ids type)})
                    events))
      (clojure.pprint/pprint event-type-ids)
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


;; test code
(comment
  (add-highscore score-dbspec {:user-name "T" :game-type "clj-snake" :start-time (t/now) :score 5}
                 [{:ts 5245, :event-type "keystroke"}
                  {:ts 5741, :event-type "keystroke"}
                  {:ts 6047, :event-type "keystroke"}
                  {:ts 7113, :event-type "keystroke"}
                  {:ts 9989, :event-type "keystroke"}
                  {:ts 10365, :event-type "keystroke"}
                  {:ts 10529, :event-type "keystroke"}
                  {:ts 10814, :event-type "keystroke"}
                  {:ts 7829, :event-type "pill"}
                  {:ts 10066, :event-type "pill"}]))

(defn get-scores-for-game [dbspec game-name offset limit]
  (->> (jdbc/query dbspec
                   ["SELECT games.user_name,
                       games.score,
                       games.duration FROM games
                      INNER JOIN game_types ON (game_types.id = games.game_type_id)
                      WHERE game_name = ?
                      ORDER BY score DESC, duration DESC
                      OFFSET ?
                      LIMIT ?;"
                    game-name offset limit]
                   :identifiers inflector/keywordize)
       vec))
