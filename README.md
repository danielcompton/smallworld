# Small World

you can find the live version of Small World at:
https://small-world-friends.herokuapp.com

### local setup

1. `lein install`
2. install postgres: https://postgresapp.com (database)
3. create local postgres db called `smallworld-local`
### local development
1. run `bin/start-dev.sh`
   - sets the environment variables
   - starts the server: http://localhost:3001
   - starts the frontend hot-reloading*
   - starts the repl

2. connect Calva repl in VSCode to the repl running in the terminal &nbsp; *(optional – the previous step starts a repl in your terminal, so this step is just for people who prefer to use the Calva repl instead of the terminal repl)*
   - command in VSCode: `Calva: Connect to a running REPL server in the project`
   - how to reload your code into the repl: `Calva: Load Current File and Dependencies`
   - how to reload the backend code in the running server: `(restart-server)`

> \* if you want to start <i>just</i> the frontend hot-reloading, without the server: `lein figwheel`.  you probably won't use this often, as `lein repl` starts the frontend hot-reloading as well as the server.

### update code running in the repl
you have two options:

1. reload the entire file into the repl
   - command in VSCode: `Calva: load current file and dependencies`
   - pros: simpler because it just reloads everything that file needs, so you don't need to worry about it
   - cons: slower
2. evaluate just the form that you want to update in the code
   - e.g. you can evaluate just the `(defroutes app ...) form if you updated code within
   - pros: faster
   - cons: more likely that you forget to evaluate a dependency that's needed and the whole thing doesn't actually update as you expect

### deploy to Heroku

```
bin/make-and-deploy.sh
```

<details><summary>here are the steps that script follows, broken down into separate subscripts:</summary>


1. build a production version
   ```sh
   bin/make-jar.sh
   ```

2. optional: run the jar locally to make sure it works, and open it at http://localhost:8080
   ```sh
   bin/run-jar.sh
   ```

3. deploy the jar to heroku
   ```sh
   bin/deploy
   ```

4. view heroku logs to check if deployment succeeded
   ```sh
   bin/heroku-logs.sh
   ```

</details>

## initial designs

[🎨 Figma workspace](https://www.figma.com/file/7fJoEke9aKGNg5uGE8BMCm/Small-World-mocks?node-id=0%3A1)

| ![](dev/design%20mocks/about.png) | ![](dev/design%20mocks/main%20screen%20map.jpg) | ![](dev/design%20mocks/main%20screen.jpg) |
| -                                 | -                                                | -                                         |
|                                   |                                                  |                                           |

### sql cheatsheet

- open Heroku Postgres instance in terminal:

   ```bash
   heroku pg:psql postgresql-rigid-43177 --app small-world-friends
   ```

- view all tables:

   ```sql
   select table_name from information_schema.tables
   where  table_schema = 'public';
   ```

- make a user go through welcome flow again:

   ```sql
   update settings
   set    welcome_flow_complete = false
   where  screen_name           = 'devon_dos';
   ```

- get column names of a table:

   ```sql
   select column_name, data_type
   from   information_schema.columns
   where  table_name = 'friends';
   ```

- reset a user: (BE CAREFUL, THIS IS VERY DESTRUCTIVE!)

   ```sql
   delete from profiles      where request_key = 'devon_dos';
   delete from friends       where request_key = 'devon_dos';
   delete from settings      where screen_name = 'devon_dos';
   delete from access_tokens where request_key = 'devon_dos';
   ```