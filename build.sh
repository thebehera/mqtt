#!/bin/bash

if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew check build allTests --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew check build allTests -x jsBrowserTest --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
else
  # the mac osx travis machine doesn't seem to keep up with the tests so we disable the ones that dont work for now
  ./gradlew check build -x lint allTests --console=plain --max-workers=1 --build-cache -Dkotlin.colors.enabled=false
fi
