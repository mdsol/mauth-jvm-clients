#!/usr/bin/env bash
function die() {
  echo $*
  exit 1
}

fullPath=`dirname $0`
jar=`find $fullPath/mauth-proxy/target/mauth-proxy*-shade.jar`
cp=`echo $jar | sed 's,./,'$fullPath'/,'`
javaArgs="-server -XX:+HeapDumpOnOutOfMemoryError -Xmx800m -jar "$cp" $*"

echo "Running using Java on path at `which java` with args $javaArgs"
java $javaArgs || die "Java process exited abnormally"
