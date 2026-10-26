package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Asks the window manager of Windows to color a real window, which it accepts on Windows 11 only: skipped
 * elsewhere. The window manager cannot be asked for the color back (DwmGetWindowAttribute does not know it).
 */
@EnabledOnOs(OS.WINDOWS)
class WindowsTitleBarPainterTest {

	@Test
	void theWindowManagerAcceptsTheColorsOfTheCaption() {
		WindowsTitleBarPainter painter = new WindowsTitleBarPainter();
		Shell shell = new Shell(Display.getDefault());
		try {
			shell.open();
			// Windows 10 refuses the attributes: nothing to test there.
			assumeTrue(painter.paint(shell, new RGB(0x1F, 0x6F, 0xB5)), "Windows 11 only");
			assumeTrue(painter.paint(shell, new RGB(255, 255, 255)));
			painter.reset(shell);
		} finally {
			shell.dispose();
		}
	}

	@Test
	void refusesAWindowThatIsGone() {
		Shell shell = new Shell(Display.getDefault());
		shell.dispose();
		WindowsTitleBarPainter painter = new WindowsTitleBarPainter();
		assertFalse(painter.paint(shell, new RGB(1, 2, 3)));
		painter.reset(shell);
	}

	@Test
	void turnsRgbIntoColorRef() {
		assertEquals(0xB56F1F, WindowsTitleBarPainter.colorRef(new RGB(0x1F, 0x6F, 0xB5)));
	}
}
