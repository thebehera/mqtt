#!/bin/bash

if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew check build allTests --stacktrace --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false --scan
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew check build allTests -x jsBrowserTest --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false --scan
else
  ./gradlew check build -x allTests --stacktrace --console=plain --max-workers=1 --build-cache -Dkotlin.colors.enabled=false --scan
fi
