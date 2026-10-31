You need to manually set up file with credentials for you tool. 

    become toolname
    install -m 600 <(echo -e 'user=BotUsername\npassword=BotPassword') /data/project/toolname/credentials.properties

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
