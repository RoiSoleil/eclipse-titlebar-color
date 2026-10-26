package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Calls Cocoa on a real window: it only checks that the calls go through, not what the title bar looks like. */
@EnabledOnOs(OS.MAC)
class MacTitleBarPainterTest {

	@Test
	void cocoaAcceptsTheColorOfTheTitleBar() {
		MacTitleBarPainter painter = new MacTitleBarPainter();
		Shell shell = new Shell(Display.getDefault());
		try {
			shell.open();
			assertTrue(painter.paint(shell, new RGB(0x1F, 0x6F, 0xB5)));
			assertTrue(painter.isTitlebarTransparent(shell));
			assertTrue(painter.paint(shell, new RGB(255, 255, 255)));
			painter.reset(shell);
			assertFalse(painter.isTitlebarTransparent(shell));
		} finally {
			shell.dispose();
		}
	}

	@Test
	void refusesAWindowThatIsGone() {
		Shell shell = new Shell(Display.getDefault());
		shell.dispose();
		MacTitleBarPainter painter = new MacTitleBarPainter();
		assertFalse(painter.paint(shell, new RGB(1, 2, 3)));
		painter.reset(shell);
	}
}
