#!/bin/bash
# This script will build the project.
# Shamelessly "borrowed" from https://github.com/spring-projects/spring-metrics/blob/master/gradle/ciBuild.sh

SWITCHES="-s --console=plain --continue"

if [ $TRAVIS_EVENT_TYPE == 'pull_request' ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch [$TRAVIS_BRANCH]"
  ./gradlew clean build $SWITCHES
elif [ -z $TRAVIS_TAG ]; then
  echo -e ?'Build Branch with Snapshot => Branch ['$TRAVIS_BRANCH']'
  ./gradlew clean build $SWITCHES
elif [ $TRAVIS_TAG ]; then
  echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
  case "$TRAVIS_TAG" in
  *-rc\.*)
    ./gradlew -Prelease.disableGitChecks=true -Prelease.useLastTag=true clean build candidate $SWITCHES
    ;;
  *)
    ./gradlew -Prelease.disableGitChecks=true -Prelease.useLastTag=true clean build final $SWITCHES
    ;;
  esac
else
  echo -e 'WARN: Should not be here => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']  Pull Request ['$TRAVIS_PULL_REQUEST']'
  ./gradlew clean build $SWITCHES
fi

EXIT=$?

exit $EXIT
