#!/bin/bash

if [ $TRAVIS_OS_NAME = 'linux' ]; then
  ./gradlew check build allTests --stacktrace --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
else
  ./gradlew check build -x lint $LINT allTests --stacktrace --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
fi