package org.eclipse.titlebarcolor;

import java.util.Arrays;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/** Starts the plug-in with the workbench: the title bar must be colored without anyone opening the preferences. */
public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		var workbench = PlatformUI.getWorkbench();
		var display = workbench.getDisplay();
		display.asyncExec(() -> {
			var applier = new TitleBarColorApplier(display, TitleBarPainter.forThisSystem(),
					shell -> Arrays.stream(workbench.getWorkbenchWindows()).anyMatch(window -> window.getShell() == shell));
			applier.install();
			// A window can be shown before the workbench lists it: it is colored again once it is open.
			workbench.addWindowListener(new IWindowListener() {
				@Override
				public void windowOpened(IWorkbenchWindow window) {
					applier.windowOpened(window.getShell());
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
		});
	}
}
