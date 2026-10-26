package org.eclipse.titlebarcolor;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

/** Colors the title bar of a window, which the system draws: one implementation for each system. */
public interface TitleBarPainter {

	/** The painter of the system Eclipse runs on. */
	static TitleBarPainter forThisSystem() {
		String system = System.getProperty("os.name", "");
		if (system.startsWith("Windows")) {
			return new WindowsTitleBarPainter();
		}
		if (system.startsWith("Mac")) {
			return new MacTitleBarPainter();
		}
		if (system.startsWith("Linux")) {
			return new LinuxTitleBarPainter();
		}
		return unsupported();
	}

	/** The painter of a system that has none: it paints nothing and says so. */
	static TitleBarPainter unsupported() {
		return new TitleBarPainter() {
			@Override
			public boolean paint(Shell shell, RGB color) {
				return false;
			}

			@Override
			public String problem(Shell window) {
				// Say what Eclipse runs on, which is what decides who draws the title bar.
				String session = System.getenv("XDG_SESSION_TYPE");
				String backend = System.getenv("GDK_BACKEND");
				return "Eclipse cannot color the title bar on " + System.getProperty("os.name", "this system")
						+ " yet" + (session == null && backend == null ? ""
								: " (session: " + (session == null ? "unknown" : session) + ", GDK_BACKEND: "
										+ (backend == null ? "not set" : backend) + ")")
						+ ": the window manager draws it.";
			}

			@Override
			public void reset(Shell shell) {
			}
		};
	}

	/**
	 * What keeps the title bars from being colored on this system, in a sentence for the user; null when
	 * nothing does. Asks the system, with a window that is open, instead of guessing from its version.
	 */
	String problem(Shell window);

	/**
	 * Gives the title bar of the window this color, and a text color that reads on it.
	 *
	 * @return false when the system cannot do it (an old version of it, for instance)
	 */
	boolean paint(Shell shell, RGB color);

	/** Gives the title bar back the color of the system. */
	void reset(Shell shell);
}
