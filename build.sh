#!/bin/sh
# Builds jwiki (git submodule) into the local Maven repository, then the bot.
# Extra arguments are passed to Maven, e.g. ./build.sh -DskipTests
set -e
cd "$(dirname "$0")"

if [ ! -f jwiki/gradlew ]; then
    echo "jwiki submodule is not checked out. Run: git submodule update --init" >&2
    exit 1
fi

(cd jwiki && ./gradlew publishToMavenLocal)
mvn -B package "$@"
