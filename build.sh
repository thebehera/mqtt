#!/bin/bash
if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew check build allTests --build-cache --scan --info
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew check build allTests -x jsBrowserTest --build-cache  --scan
else
  ./gradlew check build allTests --build-cache --scan
fi
