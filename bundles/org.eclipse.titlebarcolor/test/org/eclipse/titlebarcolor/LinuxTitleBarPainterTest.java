package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * GTK draws the title bar only under Wayland: elsewhere (X11, or the Xvfb of the CI) the painter refuses, and says
 * why. Run with GDK_BACKEND=wayland to see it paint.
 */
@EnabledOnOs(OS.LINUX)
class LinuxTitleBarPainterTest {

	@Test
	void paintsWhereGtkDrawsTheTitleBarAndSaysWhyNotElsewhere() {
		LinuxTitleBarPainter painter = new LinuxTitleBarPainter();
		Shell shell = new Shell(Display.getDefault());
		try {
			shell.open();
			String problem = painter.problem(shell);
			assertEquals(problem == null, painter.paint(shell, new RGB(0x1F, 0x6F, 0xB5)), String.valueOf(problem));
			if (problem != null) {
				assertFalse(problem.isBlank());
			}
			// A second color replaces the first one, and the system color comes back.
			painter.paint(shell, new RGB(255, 255, 255));
			painter.reset(shell);
			painter.reset(shell);
		} finally {
			shell.dispose();
		}
	}

	@Test
	void refusesAWindowThatIsGone() {
		Shell shell = new Shell(Display.getDefault());
		shell.dispose();
		LinuxTitleBarPainter painter = new LinuxTitleBarPainter();
		assertFalse(painter.paint(shell, new RGB(1, 2, 3)));
		painter.reset(shell);
	}
}
