#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
GRADLE_OPTS=""
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" "$@" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
set -e
DIRNAME="$(dirname "$0")"
cd "$DIRNAME"

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

warn () {
    echo "$*"
} >&2

APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || die "Couldn't determine application home directory"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
