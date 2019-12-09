#!/bin/bash

if [ $TRAVIS_OS_NAME = 'linux' ]; then
  ./gradlew check build allTests --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
else
  ./gradlew check build -x lint allTests --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
fi