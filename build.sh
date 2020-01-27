#!/bin/bash

if [ $TRAVIS_OS_NAME = "linux" ]; then
  ./gradlew :transport:jvmTest --info --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
elif [ $TRAVIS_OS_NAME = "windows" ]; then
  ./gradlew :transport:jvmTest --info -x jsBrowserTest -x :transport:jvmTest --console=plain --max-workers=1 --no-daemon --build-cache -Dkotlin.colors.enabled=false
else
  ./gradlew check build -x lint allTests --console=plain --max-workers=1 --build-cache -Dkotlin.colors.enabled=false
fi
