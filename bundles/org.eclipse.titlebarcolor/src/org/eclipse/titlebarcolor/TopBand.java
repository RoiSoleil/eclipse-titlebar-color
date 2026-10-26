package org.eclipse.titlebarcolor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * A band of color along the top of a window, for the systems where the title bar cannot be colored: it is drawn
 * by the plug-in itself, over the first rows of the window, so it works wherever SWT does. It covers those rows:
 * the 4 pixels are the price of working everywhere.
 */
final class TopBand {

	static final int HEIGHT = 4;

	/** The band of a window, its color, and the listener that keeps it along the top. */
	private static final class Band {
		final Composite composite;
		final Listener resize;
		Color color;
		RGB rgb;

		Band(Shell shell) {
			composite = new Composite(shell, SWT.NONE);
			resize = event -> place(shell, composite);
			shell.addListener(SWT.Resize, resize);
		}
	}

	/** Strong references to the shells are fine here: an entry goes away with its window. */
	private final Map<Shell, Band> bands = new HashMap<>();

	/** Gives the window a band of this color, or changes the color of the one it has. */
	void show(Shell shell, RGB rgb) {
		Band band = bands.get(shell);
		if (band == null) {
			band = new Band(shell);
			bands.put(shell, band);
			shell.addListener(SWT.Dispose, event -> bands.remove(shell));
		}
		if (rgb.equals(band.rgb)) {
			return;
		}
		Color previous = band.color;
		band.color = new Color(shell.getDisplay(), rgb);
		band.rgb = rgb;
		band.composite.setBackground(band.color);
		if (previous != null) {
			previous.dispose();
		}
		place(shell, band.composite);
	}

	/** Takes the band of the window away, if it has one. */
	void hide(Shell shell) {
		Band band = bands.remove(shell);
		if (band == null) {
			return;
		}
		if (!shell.isDisposed()) {
			shell.removeListener(SWT.Resize, band.resize);
		}
		band.composite.dispose();
		if (band.color != null) {
			band.color.dispose();
		}
	}

	private static void place(Shell shell, Composite band) {
		if (shell.isDisposed() || band.isDisposed()) {
			return;
		}
		Rectangle area = shell.getClientArea();
		band.setBounds(0, 0, area.width, HEIGHT);
		band.moveAbove(null);
	}
}
