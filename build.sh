#!/bin/bash

if [[ -z "${ANDROID_HOME}" ]]; then
  LINT="lint"
else
  LINT=""
fi

if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew check build allTests $LINT --build-cache --scan
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew check build allTests $LINT -x jsBrowserTest --build-cache  --scan
else
  ./gradlew check build allTests $LINT --build-cache --scan
fi
