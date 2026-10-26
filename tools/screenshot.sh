#!/bin/bash
# Takes the screenshot of the plug-in on the system it runs on: a real Eclipse, with the plug-in in its dropins and
# a workspace whose title bar has a color, saved as screenshots/<system>.png.
# Usage: ECLIPSE_HOME=/path/to/eclipse ./tools/screenshot.sh <system> <color as RRGGBB>
# Run by .github/workflows/screenshots.yml on Windows (Git Bash), Linux (Xvfb) and macOS.
set -euo pipefail
cd "$(dirname "$0")/.."
SYSTEM=$1
COLOR=$2
: "${ECLIPSE_HOME:?Set ECLIPSE_HOME to your Eclipse installation directory}"
# How long Eclipse may take to start and set its window up, and how long the helper lets the workbench settle.
WAIT=${WAIT:-400}
export SCREENSHOT_SETTLE=${SCREENSHOT_SETTLE:-30}

./build.sh --install
# The helper that sets the workbench up for the screenshot (tools/screenshot-helper), in the dropins too.
HELPER=tools/screenshot-helper
SEP=:
case "$(uname -s)" in
MINGW* | MSYS* | CYGWIN*) SEP=';' ;;
esac
CP="$ECLIPSE_HOME/plugins/*"
rm -rf build/screenshot-helper && mkdir -p build/screenshot-helper
javac --release 21 -nowarn -encoding UTF-8 -cp "$(case $SEP in ';') cygpath -m "$CP" ;; *) echo "$CP" ;; esac)" \
	-d build/screenshot-helper $(find $HELPER/src -name '*.java')
HELPER_JAR="$ECLIPSE_HOME/dropins/org.eclipse.titlebarcolor.screenshot_1.0.0.jar"
jar --create --file "$HELPER_JAR" \
	--manifest $HELPER/META-INF/MANIFEST.MF -C build/screenshot-helper org -C $HELPER plugin.xml

# The name of the workspace is what the title bar shows.
WORKSPACE="$PWD/build/screenshots/my-workspace"
SETTINGS="$WORKSPACE/.metadata/.plugins/org.eclipse.core.runtime/.settings"
rm -rf "$WORKSPACE" && mkdir -p "$SETTINGS"
printf 'color=\\#%s\neclipse.preferences.version=1\n' "$COLOR" > "$SETTINGS/org.eclipse.titlebarcolor.prefs"
# No welcome page: the workbench itself is what the screenshot shows.
printf 'showIntro=false\neclipse.preferences.version=1\n' > "$SETTINGS/org.eclipse.ui.prefs"
mkdir -p screenshots
OUT="$PWD/screenshots/$SYSTEM.png"
READY="$PWD/build/screenshots/ready"
rm -f "$READY"
FULL="$PWD/build/screenshots/full.png"

# Eclipse goes when the script ends, whether the screenshot was taken or not, and so does the helper, which must
# not stay in the installation.
stop() {
	rm -f "$HELPER_JAR"
	case "$SYSTEM" in
	windows) taskkill //F //IM eclipse.exe //T > /dev/null 2>&1 || true ;;
	*) [ -n "${ECLIPSE_PID:-}" ] && kill "$ECLIPSE_PID" 2> /dev/null || true ;;
	esac
}
trap stop EXIT

# Starts Eclipse, and waits for the helper to say that the window is ready: "x y width height screenWidth screenHeight".
start() {
	local launcher=$1 ready=$READY workspace=$WORKSPACE
	if [ "$SYSTEM" = windows ]; then
		ready=$(cygpath -w "$READY")
		workspace=$(cygpath -w "$WORKSPACE")
	fi
	SCREENSHOT_READY=$ready "$launcher" -clean -nosplash -data "$workspace" &
	ECLIPSE_PID=$!
	local waited=0
	until [ -s "$READY" ]; do
		if [ $waited -ge "$WAIT" ]; then
			echo "Eclipse did not get ready in $WAIT seconds"
			exit 1
		fi
		sleep 5
		waited=$((waited + 5))
	done
	read -r X Y WIDTH HEIGHT SCREEN_WIDTH SCREEN_HEIGHT < "$READY"
	echo "Window at $X,$Y, $WIDTH x $HEIGHT, on a screen of $SCREEN_WIDTH x $SCREEN_HEIGHT"
}

case "$SYSTEM" in
windows)
	# Nothing in front of Eclipse (Edge opens its first run page on the runners).
	taskkill //F //IM msedge.exe //T > /dev/null 2>&1 || true
	start "$ECLIPSE_HOME/eclipse.exe"
	powershell -NoProfile -ExecutionPolicy Bypass -File tools/screenshot-windows.ps1 -Out "$(cygpath -w "$OUT")"
	;;
linux)
	# Xvfb has no window manager: GTK draws the title bar itself (GTK_CSD=1), as on GNOME under Wayland.
	export GTK_CSD=1
	start "$ECLIPSE_HOME/eclipse"
	# The window of GTK, title bar included, as the X server has it. The whole screen is taken, then cut: the
	# window alone comes out black without a compositing manager.
	if ! timeout 60 xdotool search --sync --onlyvisible --name ' - Eclipse IDE' > /dev/null; then
		echo "No visible window of the workbench"
		exit 1
	fi
	WINDOW=$(xdotool search --onlyvisible --name ' - Eclipse IDE' | while read -r id; do
		eval "$(xdotool getwindowgeometry --shell "$id")"
		echo "$((WIDTH * HEIGHT)) $id"
	done | sort -n | tail -1 | cut -d' ' -f2)
	eval "$(xdotool getwindowgeometry --shell "$WINDOW")"
	import -window root "$FULL"
	convert "$FULL" -crop "${WIDTH}x${HEIGHT}+${X}+${Y}" +repage "$OUT"
	;;
macos)
	start "$ECLIPSE_HOME/../MacOS/eclipse"
	osascript -e 'tell application "Eclipse" to activate' || true
	sleep 3
	screencapture -x "$FULL"
	# The bounds are in points: the screen may have more pixels than points.
	PIXELS=$(sips -g pixelWidth "$FULL" | awk '/pixelWidth/ { print $2 }')
	read -r CROP_H CROP_W CROP_Y CROP_X < <(awk -v p="$PIXELS" -v s="$SCREEN_WIDTH" -v x="$X" -v y="$Y" -v w="$WIDTH" \
		-v h="$HEIGHT" 'BEGIN { k = p / s; printf "%d %d %d %d\n", h * k, w * k, y * k, x * k }')
	sips --cropToHeightWidth "$CROP_H" "$CROP_W" --cropOffset "$CROP_Y" "$CROP_X" "$FULL" --out "$OUT" > /dev/null
	;;
*)
	echo "Unknown system $SYSTEM"
	exit 2
	;;
esac
ls -l "$OUT"
