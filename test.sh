#!/bin/bash
# Runs the tests with coverage. Usage: ECLIPSE_HOME=/path/to/eclipse ./test.sh
# The tests open real windows on the current display (xvfb-run on a Linux without one).
# Runs on Linux, macOS and Windows (Git Bash).
set -euo pipefail
cd "$(dirname "$0")"
BUNDLE=bundles/org.eclipse.titlebarcolor
: "${ECLIPSE_HOME:?Set ECLIPSE_HOME to your Eclipse installation directory}"
# The painters of Windows and macOS are only run (and tested) on their own system: they are left out of the measure.
MIN_COVERAGE=85
JACOCO=0.8.15
JUNIT=6.1.3
MAVEN=https://repo1.maven.org/maven2

SEP=:
JAVA_OPTS=()
case "$(uname -s)" in
MINGW* | MSYS* | CYGWIN*)
	SEP=';'
	# Java does not understand the /c/... paths of Git Bash.
	ECLIPSE_HOME=$(cygpath -m "$ECLIPSE_HOME")
	;;
Darwin)
	# SWT runs on the main thread of macOS.
	JAVA_OPTS=(-XstartOnFirstThread)
	;;
esac

mkdir -p .cache
fetch() {
	[ -f ".cache/$(basename "$1")" ] || curl -sfL -o ".cache/$(basename "$1")" "$MAVEN/$1"
}
fetch "org/jacoco/org.jacoco.agent/$JACOCO/org.jacoco.agent-$JACOCO-runtime.jar"
fetch "org/jacoco/org.jacoco.cli/$JACOCO/org.jacoco.cli-$JACOCO-nodeps.jar"
fetch "org/junit/platform/junit-platform-console-standalone/$JUNIT/junit-platform-console-standalone-$JUNIT.jar"
CONSOLE=".cache/junit-platform-console-standalone-$JUNIT.jar"

rm -rf build && mkdir -p build/classes build/test-classes
# Eclipse ships its own JUnit bundles: keep them off the class path, the console brings JUnit.
# JNA is a bundle folder in some installations.
ECLIPSE_CP=$( (find "$ECLIPSE_HOME/plugins" -maxdepth 1 -name '*.jar'; find "$ECLIPSE_HOME/plugins" -maxdepth 1 -type d -name 'com.sun.jna*') |
	grep -v -E 'junit|opentest4j|apiguardian|\.source_' | tr '\n' "$SEP")

# In argument files: the class path is longer than a command line of Windows.
echo "-cp \"$ECLIPSE_CP\"" > build/main.args
find $BUNDLE/src -name '*.java' >> build/main.args
echo "-cp \"build/classes$SEP$CONSOLE$SEP$ECLIPSE_CP\"" > build/test.args
find $BUNDLE/test -name '*.java' >> build/test.args
echo "-cp \"build/classes${SEP}build/test-classes$SEP$CONSOLE$SEP$ECLIPSE_CP\"" > build/run.args

javac --release 21 -nowarn -encoding UTF-8 -g -d build/classes @build/main.args
javac --release 21 -nowarn -encoding UTF-8 -d build/test-classes @build/test.args

STATUS=0
# ${@+"$@"}: no arguments is not an error for the bash 3 of macOS.
java ${JAVA_OPTS[@]+"${JAVA_OPTS[@]}"} -Dfile.encoding=UTF-8 \
	-javaagent:".cache/org.jacoco.agent-$JACOCO-runtime.jar=destfile=build/jacoco.exec,includes=org.eclipse.titlebarcolor.*" \
	"-Dosgi.instance.area=file:build/workspace/" \
	"@build/run.args" \
	org.junit.platform.console.ConsoleLauncher execute --scan-classpath build/test-classes \
	--details=tree --disable-banner ${@+"$@"} || STATUS=$?

java -jar ".cache/org.jacoco.cli-$JACOCO-nodeps.jar" report build/jacoco.exec \
	--classfiles build/classes --sourcefiles $BUNDLE/src \
	--csv build/coverage.csv --xml build/coverage.xml --html build/coverage >/dev/null
awk -F, -v min="$MIN_COVERAGE" 'NR > 1 && $3 !~ /^(Windows|Mac)TitleBarPainter/ { missed += $8; covered += $9; printf "  %-34s %5.1f%%\n", $3, 100 * $9 / ($8 + $9) }
	END { total = 100 * covered / (missed + covered); printf "Line coverage: %.1f%% (minimum %d%%), report in build/coverage/index.html\n", total, min; exit total < min }' \
	build/coverage.csv || STATUS=1
exit $STATUS
