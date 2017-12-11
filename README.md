# Clojure Dependency Leiningen

clojure-deps-lein -- TODO?

Clojure dependency leiningen is an [Atomist](https://atomist.com) automation using our [experimental Clojure client](https://github.com/atomisthq/automation-client-clj). It listens for commit events and checks the leiningen project.clj for changes to dependencies. If it notices that a library can be updated it asks the user if they would like to update and opens a PR with the change. As it runs in Clojure it is well suited to editing Clojure code, we use [clj-rewrite](https://github.com/xsc/rewrite-clj) for this.

The basic strategy is:

* Listen for commits. When a commit is received read the project.clj and grab the list of project dependencies. Send these along with a sha of all the dependencies back to atomist.
* Atomist ingests this list and stores them in the wider graph for the project. We call pairs of sha and data a fingerprint.
* Updates to those fingerprints where the sha has changed generate what's called an impact event. An impact event will be fired for this change if the dependencies update
* We have a second listener that listens for impact events. When we receive an impact event we get passed the list of dependencies that changed. We also request the last set of project dependencies from the graph to compare against.
* We compare these two sets of dependencies and look for updated versions. If we find one then we offer the user a chance to update the "goal" version for the library. We store this set of goal versions as a preference for the team.
* We also compare our current list of dependencies against the goal versions. On finding a divergence we offer to create a PR to update the library to the goal state.

This is just an example strategy. There are many other ways of choosing how to handle library updates e.g. pushing updates out upon a library update. This does provide us with a good example of many of the tasks needed to build the right automation for your team.

## Setup and development

See https://github.com/atomisthq/automation-client-clj for more detailed instructions on the clojure automation client.

In short: update `/env/dev/resources/config.edn` with your team-id and make sure you have `$GITHUB_TOKEN` set in your env with a [github token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) for that team.

You can find your team-id by logging into https://app.atomist.com/ If you are new to atomist visit: https://docs.atomist.com/user/ to follow the setup instructions first.

You can then start the automation by evaling `(mount/start)` in `main.clj`, similarly you can stop it by evaling `(mount/stop)`

To get started try editing the `hello-clojure-world` automation in `src/fingerprints/helloworld_example.clj`

## Building an running

The automation can be built using `lein uberjar` and run using `java -cp target/fingerprint.jar clojure.main -m main`

Note that this version will use the `config.edn` version from `resources/config.edn` rather than the dev version.

## License

TODO! eclipse?
