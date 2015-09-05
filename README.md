# clj-highscore

*clj-highscore* is a generic REST API web service to store/retrieve high score and event information from javascript games. Originnaly used with [Tableau Cnake](http://tfoldi.github.io/cljs-tableau-cnake/).

# What's this?

Clojure has a beautiful library to build REST web services: [liberator](http://clojure-liberator.github.io/liberator/). If you familiar with Erlang’s web machine then you will find liberator very straightforward: it uses the same decision tree based approach.

After I told my colleague [Gyula](https://github.com/gyulalaszlo) that I started to build a high score server he immediately stepped in and took over the project. His first move was to add Ruby/ActiveRecord like migrations with [ragtime](https://github.com/weavejester/ragtime). (Once I was a big ruby fan but rails ruined everything).

Also, his version stores all key presses and score event with relative time stamps. This will be useful to visualize how guys are playing. If you are new to Ring/Liberator I would suggest to look into core.clj.

More Details on [Databoss](http://databoss.starschema.net/tableau-cnake-playing-with-the-js-api-from-clojurescript/)
