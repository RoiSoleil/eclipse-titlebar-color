package org.eclipse.titlebarcolor;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;

/** Colors the title bar of a window, which the system draws: one implementation for each system. */
public interface TitleBarPainter {

	/** The painter of the system Eclipse runs on. */
	static TitleBarPainter forThisSystem() {
		if (System.getProperty("os.name", "").startsWith("Windows")) {
			return new WindowsTitleBarPainter();
		}
		return new TitleBarPainter() {
			@Override
			public boolean paint(Shell shell, RGB color) {
				return false;
			}

			@Override
			public void reset(Shell shell) {
			}
		};
	}

	/**
	 * Gives the title bar of the window this color, and a text color that reads on it.
	 *
	 * @return false when the system cannot do it (an old version of it, for instance)
	 */
	boolean paint(Shell shell, RGB color);

	/** Gives the title bar back the color of the system. */
	void reset(Shell shell);
}
