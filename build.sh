#!/bin/bash
# Quick local build without Maven. Usage: ECLIPSE_HOME=/path/to/eclipse ./build.sh [--install]
# The official build is "mvn clean install" (Tycho), which also produces the update site.
set -euo pipefail
cd "$(dirname "$0")"
BUNDLE=bundles/org.eclipse.titlebarcolor
: "${ECLIPSE_HOME:?Set ECLIPSE_HOME to your Eclipse installation directory}"
INSTALL=
for option in ${@+"$@"}; do
	case "$option" in
	--install) INSTALL=yes ;;
	*)
		echo "Unknown option $option"
		exit 2
		;;
	esac
done
SEP=:
case "$(uname -s)" in
MINGW* | MSYS* | CYGWIN*)
	SEP=';'
	ECLIPSE_HOME=$(cygpath -m "$ECLIPSE_HOME")
	;;
esac
# JNA is a bundle folder in some installations.
CP="$ECLIPSE_HOME/plugins/*$(find "$ECLIPSE_HOME/plugins" -maxdepth 1 -type d -name 'com.sun.jna*' | sed "s/^/$SEP/" | tr -d '\n')"
# A new qualifier for every build, so that Eclipse does not keep serving the previous jar.
QUALIFIER=$(date +%Y%m%d%H%M%S)
VERSION=$(sed -n 's/^Bundle-Version: //p' $BUNDLE/META-INF/MANIFEST.MF | sed "s/qualifier/$QUALIFIER/")
rm -rf bin dist && mkdir -p bin/classes dist
JAR="dist/org.eclipse.titlebarcolor_${VERSION}.jar"
javac --release 21 -nowarn -encoding UTF-8 -cp "$CP" -d bin/classes $(find $BUNDLE/src -name '*.java')
sed "s/^Bundle-Version: .*/Bundle-Version: ${VERSION}/" $BUNDLE/META-INF/MANIFEST.MF > bin/MANIFEST.MF
jar --create --file "$JAR" --manifest bin/MANIFEST.MF -C bin/classes org -C "$BUNDLE" plugin.xml
echo "Built $JAR"
if [ -n "$INSTALL" ]; then
	mkdir -p "$ECLIPSE_HOME/dropins"
	rm -f "$ECLIPSE_HOME"/dropins/org.eclipse.titlebarcolor_*.jar
	cp "$JAR" "$ECLIPSE_HOME/dropins/"
	echo "Installed into $ECLIPSE_HOME/dropins (restart Eclipse with -clean)"
fi
