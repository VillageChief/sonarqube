#!/bin/sh

./stop.sh

TZ=UTC ./gradlew build $*
