package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class TitleBarPainterTest {

	@Test
	@DisabledOnOs({ OS.WINDOWS, OS.MAC })
	void aSystemWithoutPainterLeavesTheTitleBarAlone() {
		TitleBarPainter painter = TitleBarPainter.forThisSystem();
		Shell shell = new Shell(Display.getDefault());
		try {
			assertFalse(painter.paint(shell, new RGB(1, 2, 3)));
			painter.reset(shell);
			assertTrue(painter.problem(shell).contains("cannot color the title bar"), painter.problem(shell));
		} finally {
			shell.dispose();
		}
	}
}
