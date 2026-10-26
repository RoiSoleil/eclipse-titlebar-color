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
# How long Eclipse may take to show its window, then to fill it.
WAIT=${WAIT:-300}
SETTLE=${SETTLE:-40}

./build.sh --install
# The name of the workspace is what the title bar shows.
WORKSPACE="$PWD/build/screenshots/my-workspace"
SETTINGS="$WORKSPACE/.metadata/.plugins/org.eclipse.core.runtime/.settings"
rm -rf "$WORKSPACE" && mkdir -p "$SETTINGS"
printf 'color=\\#%s\neclipse.preferences.version=1\n' "$COLOR" > "$SETTINGS/org.eclipse.titlebarcolor.prefs"
# No welcome page: the workbench itself is what the screenshot shows.
printf 'showIntro=false\neclipse.preferences.version=1\n' > "$SETTINGS/org.eclipse.ui.prefs"
mkdir -p screenshots
OUT="$PWD/screenshots/$SYSTEM.png"
# Eclipse goes when the script ends, whether the screenshot was taken or not.
stop() {
	case "$SYSTEM" in
	windows) taskkill //F //IM eclipse.exe //T > /dev/null 2>&1 || true ;;
	*) [ -n "${ECLIPSE_PID:-}" ] && kill "$ECLIPSE_PID" 2> /dev/null || true ;;
	esac
}
trap stop EXIT

case "$SYSTEM" in
windows)
	"$ECLIPSE_HOME/eclipse.exe" -clean -nosplash -data "$(cygpath -w "$WORKSPACE")" &
	powershell -NoProfile -ExecutionPolicy Bypass -File tools/screenshot-windows.ps1 -Out "$(cygpath -w "$OUT")" \
		-Wait "$WAIT" -Settle "$SETTLE"
	;;
linux)
	# Xvfb has no window manager: GTK draws the title bar itself (GTK_CSD=1), as on GNOME under Wayland.
	GTK_CSD=1 "$ECLIPSE_HOME/eclipse" -clean -nosplash -data "$WORKSPACE" &
	ECLIPSE_PID=$!
	timeout "$WAIT" xdotool search --sync --onlyvisible --name ' - Eclipse IDE' > /dev/null
	sleep "$SETTLE"
	# The workbench is the largest of the visible windows of that name.
	WINDOW=$(for id in $(xdotool search --onlyvisible --name ' - Eclipse IDE'); do
		eval "$(xdotool getwindowgeometry --shell "$id")"
		echo "$((WIDTH * HEIGHT)) $id"
	done | sort -n | tail -1 | cut -d' ' -f2)
	xdotool windowsize --sync "$WINDOW" 1280 800 windowmove --sync "$WINDOW" 20 20
	sleep 5
	import -window "$WINDOW" "$OUT"
	;;
macos)
	"$ECLIPSE_HOME/../MacOS/eclipse" -clean -nosplash -data "$WORKSPACE" &
	ECLIPSE_PID=$!
	# The window of the workbench cannot be listed without permissions a runner may lack: Eclipse gets its time.
	sleep $((SETTLE * 3))
	# Brings Eclipse to the front, then the whole screen: the bounds of a window need permissions a runner may lack.
	osascript -e 'tell application "Eclipse" to activate' || true
	sleep 5
	screencapture -x "$OUT"
	;;
*)
	echo "Unknown system $SYSTEM"
	exit 2
	;;
esac
ls -l "$OUT"
