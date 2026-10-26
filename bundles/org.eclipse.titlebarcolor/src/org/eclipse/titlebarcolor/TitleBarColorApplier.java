package org.eclipse.titlebarcolor;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Keeps the title bar of every window of the display in the color chosen for this workspace: the windows of the
 * workbench, and also the dialogs and the detached views and editors.
 */
final class TitleBarColorApplier {

	private final Display display;
	private final TitleBarPainter painter;
	/** The windows this plug-in painted, with their color: the others keep the color they have, whoever gave it. */
	private final Map<Shell, RGB> painted = new WeakHashMap<>();
	private final Listener showFilter = this::shown;
	private final IPreferenceChangeListener colorListener = this::colorChanged;

	TitleBarColorApplier(Display display, TitleBarPainter painter) {
		this.display = display;
		this.painter = painter;
	}

	private void shown(Event event) {
		if (event.widget instanceof Shell shell) {
			apply(shell);
		}
	}

	private void colorChanged(PreferenceChangeEvent event) {
		if (TitleBarColors.COLOR.equals(event.getKey()) && !display.isDisposed()) {
			display.asyncExec(this::applyToAll);
		}
	}

	/** Colors the windows now, and the next ones and the next colors as they come. Runs in the UI thread. */
	void install() {
		applyToAll();
		display.addFilter(SWT.Show, showFilter);
		TitleBarColors.preferences().addPreferenceChangeListener(colorListener);
	}

	void dispose() {
		if (!display.isDisposed()) {
			display.removeFilter(SWT.Show, showFilter);
		}
		IEclipsePreferences preferences = TitleBarColors.preferences();
		preferences.removePreferenceChangeListener(colorListener);
	}

	void applyToAll() {
		if (display.isDisposed()) {
			return;
		}
		for (Shell shell : display.getShells()) {
			apply(shell);
		}
	}

	void apply(Shell shell) {
		if (shell == null || shell.isDisposed()) {
			return;
		}
		RGB color = TitleBarColors.get();
		if (color == null) {
			// Whatever became of its title bar, a window this plug-in painted gets the color of the system back.
			if (painted.remove(shell) != null) {
				painter.reset(shell);
			}
		} else if ((shell.getStyle() & SWT.TITLE) != 0 && !color.equals(painted.get(shell))
				&& painter.paint(shell, color)) {
			// No title bar, no color: tool tips and pop-ups are shells too.
			painted.put(shell, color);
		}
	}
}
