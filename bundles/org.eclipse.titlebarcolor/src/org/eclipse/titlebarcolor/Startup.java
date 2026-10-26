package org.eclipse.titlebarcolor;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

/** Starts the plug-in with the workbench: the title bar must be colored without anyone opening the preferences. */
public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		var display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> new TitleBarColorApplier(display, TitleBarPainter.forThisSystem()).install());
	}
}
