#!/bin/bash

if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew check build allTests --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew check build allTests -x jsBrowserTest --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
else
  ./gradlew --info :client:testDebugUnitTest --console=plain --max-workers=1 --build-cache -Dkotlin.colors.enabled=false
fi
