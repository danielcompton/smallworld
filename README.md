# Small World

## local setup

install dependencies:

1. `lein install`
2. install postgres: https://postgresapp.com (database)
3. install postbird: https://github.com/Paxa/postbird (GUI)
4. create local postgres db called `smallworld-local`
### local development
1. run `bin/start-dev.sh`
   - sets the environment variables
   - starts the server
   - starts the frontend hot-reloading
   - starts the repl

2. connect your Calva repl in VSCode to the repl running in the terminal
   - command in VSCode: `Calva: Connect to a running REPL server in the project`

3. load your code into the repl
   - command in VSCode: `Calva: load current file and dependencies`

4. in the repl, stop whatever server is running and then start one:
   - in Calva: `(restart-server)`
   - this reloads the backend code as well, which does not hot-reload like the frontend

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

### start *just* the frontend hot-reloading
run this in a separate terminal to get hot-reloading to work.  you probably won't use this often, as `lein repl` starts the frontend hot-reloadingn as well as the server.

note: you'll do most of your work in the main server that you started previously, the server that _this_ step starts is just for hot-reloading.

```clojure
lein figwheel
```

### open local URL

http://localhost:3001
## deploy to Heroku

you'll generally use this combined command to deploy to Heroku: `bin/make-and-deploy.sh`

here are the steps that script follows, broken down into separate subscripts:

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

note: instead of heroku's usual `git push heroku` deployment patter, we use the [heroku java cli plugin](https://devcenter.heroku.com/articles/deploying-executable-jar-files)
## initial designs

### [🎨 Figma workspace](https://www.figma.com/file/7fJoEke9aKGNg5uGE8BMCm/Small-World-mocks?node-id=0%3A1)

| ![](dev/design%20mocks/about.png) | ![](dev/design%20mocks/main%20screen%20map.jpg) | ![](dev/design%20mocks/main%20screen.jpg) |
| -                                 | -                                                | -                                         |
|                                   |                                                  |                                           |