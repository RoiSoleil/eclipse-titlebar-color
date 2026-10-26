package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Asks the window manager of Windows what it has, so that it runs on a Windows 11 only: it is skipped elsewhere. */
@EnabledOnOs(OS.WINDOWS)
class WindowsTitleBarPainterTest {

	@Test
	void theWindowManagerKeepsTheColorOfTheCaption() {
		WindowsTitleBarPainter painter = new WindowsTitleBarPainter();
		Shell shell = new Shell(Display.getDefault());
		try {
			shell.open();
			assumeTrue(painter.paint(shell, new RGB(0x1F, 0x6F, 0xB5)), "Windows 11 only");
			assertEquals(0xB56F1F, painter.captionColor(shell));
			painter.reset(shell);
			assertNotEquals(0xB56F1F, painter.captionColor(shell));
		} finally {
			shell.dispose();
		}
	}

	@Test
	void turnsRgbIntoColorRef() {
		assertEquals(0xB56F1F, WindowsTitleBarPainter.colorRef(new RGB(0x1F, 0x6F, 0xB5)));
	}
}
