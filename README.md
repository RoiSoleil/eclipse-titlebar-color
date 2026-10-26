# Title Bar Color: a color of its own for the title bar of each workspace

[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/RoiSoleil/eclipse-titlebar-color/build.yml)](https://github.com/RoiSoleil/eclipse-titlebar-color/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/RoiSoleil/eclipse-titlebar-color/branch/main/graph/badge.svg)](https://codecov.io/gh/RoiSoleil/eclipse-titlebar-color)
[![GitHub](https://img.shields.io/github/license/RoiSoleil/eclipse-titlebar-color)](LICENSE)

With several Eclipse windows open on several workspaces, the title bars all look alike. This plug-in
colors the title bar of the windows of a workspace with the color you chose for it, so that you see
at a glance which workspace you are in.

*Window > Preferences > General > Appearance > Title Bar Color*

The color is a preference of the workspace: every workspace keeps its own. The change is applied
at once to the open windows, without restarting Eclipse, and again at each start. The text of the title
bar turns black or white, whichever reads better on the color.

# Update Site

You can find the latest build of Title Bar Color here:

https://github.com/RoiSoleil/eclipse-titlebar-color/raw/update-site/latest/

# Platforms

The system draws the title bar, so each system needs its own way to color it:

| System | State |
|---|---|
| Windows 11 (build 22000 or later) | Supported, through the caption color of the Desktop Window Manager |
| Windows 10 | Not possible: the system ignores the color, the title bar stays as it is |
| Linux | Planned |
| macOS | Planned |

On a system that is not supported yet the plug-in does nothing, and the preference page says so.
A system is added by implementing `TitleBarPainter` (`bundles/org.eclipse.titlebarcolor/src`).

# Build

Requires JDK 21 and Maven 3.9 (the Apache distribution: the Maven packaged by some Linux
distributions does not work with Tycho).

```bash
mvn clean install   # update site in update-site/org.eclipse.titlebarcolor/target/repository
```

Every push to `main` is built by GitHub Actions, which publishes the update site to the
`update-site` branch and a zipped copy to the `latest` release.

Two scripts help during development; they compile against an Eclipse installation (2024-06 or
later) and need bash:

```bash
export ECLIPSE_HOME=/path/to/eclipse   # macOS: /Applications/Eclipse.app/Contents/Eclipse
./build.sh --install   # quick build, copied to $ECLIPSE_HOME/dropins
./test.sh              # tests with coverage
```

The tests open real windows, so they need a display (`xvfb-run ./test.sh` on a Linux without one).
GitHub Actions runs them on Linux, macOS and Windows, sends the coverage of Linux to Codecov, and the
test of the Windows painter runs on Windows only.

# License

[Eclipse Public License, v2.0](http://www.eclipse.org/legal/epl-v20.html).
