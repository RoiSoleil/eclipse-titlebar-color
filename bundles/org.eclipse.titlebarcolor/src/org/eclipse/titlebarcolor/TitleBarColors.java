package org.eclipse.titlebarcolor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.RGB;

/** The color chosen for the title bar of this workspace, kept in the preferences of the workspace. */
public final class TitleBarColors {

	public static final String NODE = "org.eclipse.titlebarcolor";
	/** "#RRGGBB", or absent when the title bar keeps the color of the system. */
	public static final String COLOR = "color";

	private TitleBarColors() {
	}

	/** Preferences of the tests, which run without the platform and so without the scopes of the workspace. */
	private static IEclipsePreferences forTests;

	public static IEclipsePreferences preferences() {
		return forTests != null ? forTests : InstanceScope.INSTANCE.getNode(NODE);
	}

	static void useForTests(IEclipsePreferences preferences) {
		forTests = preferences;
	}

	/** The color of this workspace, or null when there is none. */
	public static RGB get() {
		return parse(preferences().get(COLOR, ""));
	}

	public static void set(RGB color) {
		if (color == null) {
			preferences().remove(COLOR);
		} else {
			preferences().put(COLOR, format(color));
		}
	}

	/** Reads "#RRGGBB" (the # is optional); null for anything else. */
	public static RGB parse(String text) {
		if (text == null) {
			return null;
		}
		String hex = text.strip();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		if (!hex.matches("[0-9a-fA-F]{6}")) {
			return null;
		}
		return new RGB(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16),
				Integer.parseInt(hex.substring(4, 6), 16));
	}

	public static String format(RGB color) {
		return String.format("#%02X%02X%02X", color.red, color.green, color.blue);
	}

	/** Whether white text reads better than black text on this background. */
	public static boolean prefersLightText(RGB background) {
		return 0.299 * background.red + 0.587 * background.green + 0.114 * background.blue < 140;
	}
}
