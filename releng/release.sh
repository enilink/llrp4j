#!/bin/bash
PUSHCHANGES="false"
while [[ $# > 1 ]]
do
key="$1"

case $key in
    -r|--releaseVersion)
    RELEASEVERSION="$2"
    shift # past argument
    ;;
    -d|--developmentVersion)
    DEVELOPMENTVERSION="$2"
    shift # past argument
    ;;
    -p|--pushChanges)
    PUSHCHANGES="true"
    ;;
    *)
    # unknown option
    ;;
esac
shift # past argument or value
done

mvn release:prepare -Dtag=v${RELEASEVERSION} -DreleaseVersion=${RELEASEVERSION} -DdevelopmentVersion=${DEVELOPMENTVERSION}-SNAPSHOT -DpushChanges=${PUSHCHANGES}
