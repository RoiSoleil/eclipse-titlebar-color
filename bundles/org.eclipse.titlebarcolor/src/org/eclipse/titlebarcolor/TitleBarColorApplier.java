package org.eclipse.titlebarcolor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

/** Keeps the title bar of the windows of the workbench in the color chosen for this workspace. */
final class TitleBarColorApplier {

	private final IWorkbench workbench;
	private final TitleBarPainter painter;

	TitleBarColorApplier(IWorkbench workbench, TitleBarPainter painter) {
		this.workbench = workbench;
		this.painter = painter;
	}

	/** Colors the windows now, and the next ones and the next colors as they come. Runs in the UI thread. */
	void install() {
		applyToAll();
		workbench.addWindowListener(new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				apply(window.getShell());
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
			}
		});
		IEclipsePreferences preferences = TitleBarColors.preferences();
		Display display = workbench.getDisplay();
		preferences.addPreferenceChangeListener(event -> {
			if (TitleBarColors.COLOR.equals(event.getKey())) {
				display.asyncExec(this::applyToAll);
			}
		});
	}

	void applyToAll() {
		for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
			apply(window.getShell());
		}
	}

	void apply(Shell shell) {
		if (shell == null || shell.isDisposed()) {
			return;
		}
		RGB color = TitleBarColors.get();
		if (color == null) {
			painter.reset(shell);
		} else {
			painter.paint(shell, color);
		}
	}
}
