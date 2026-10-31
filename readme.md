## Toolforge

The bot runs daily as a job of the Toolforge tool `kanzattool1`. Every push to `main` deploys it: the workflow copies
the jar and [toolforge/jobs.yaml](toolforge/jobs.yaml) to the tool and loads the job definitions.

The bot logs in as the `BOT_USER` repository variable (`KanzatBot`) with the `BOT_PASSWORD` repository secret. The
deploy copies both to the tool's envvars, so after changing them rerun the workflow. To run the bot locally, set them
as environment variables.

## Build

[jwiki](https://github.com/Kanzat/jwiki) (a fork of the archived fastily/jwiki) is a git submodule
in `jwiki/`. Clone with submodules:

    git clone --recurse-submodules https://github.com/Kanzat/ukwikitasks.git

or, in an existing clone, `git submodule update --init`.

Build (JDK 17+):

    ./build.sh

It publishes jwiki to the local Maven repository (`~/.m2`) and then builds the bot. The deployable
jar is `target/ukwikitasks.jar`. If you build with `mvn` directly, rerun `./build.sh` after changing
jwiki, otherwise Maven keeps using the previously published jwiki.

## Changing jwiki

Work inside `jwiki/` like in any other clone of the fork (run `git checkout master` first: after
`git submodule update` it is on a detached commit). Push the fork first, then commit the new
submodule version in this repository:

    git add jwiki
    git commit -m "Update jwiki: ..."

jwiki tests against the live test.wikipedia.org are not part of the build; run them with
`cd jwiki && ./gradlew liveTest`.
