package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleBarColorPreferencePageTest {

	private Shell shell;
	private TitleBarColorPreferencePage page;

	@BeforeEach
	void open() {
		TitleBarColors.useForTests(new EclipsePreferences());
		TitleBarColors.set(null);
		shell = new Shell(Display.getDefault());
		page = new TitleBarColorPreferencePage();
		page.createControl(shell);
	}

	@AfterEach
	void close() {
		shell.dispose();
		TitleBarColors.set(null);
		TitleBarColors.useForTests(null);
	}

	private Button checkbox() {
		for (Control child : ((Composite) page.getControl()).getChildren()) {
			if (child instanceof Composite content) {
				for (Control c : content.getChildren()) {
					if (c instanceof Button button && (button.getStyle() & SWT.CHECK) != 0) {
						return button;
					}
				}
			}
		}
		throw new AssertionError("no check box");
	}

	@Test
	void startsWithoutColor() {
		assertTrue(!checkbox().getSelection());
	}

	@Test
	void savesNothingWhileTheBoxIsUnchecked() {
		assertTrue(page.performOk());
		assertNull(TitleBarColors.get());
	}

	@Test
	void savesTheColorOfTheSelectorOnceChecked() {
		checkbox().setSelection(true);
		assertTrue(page.performOk());
		assertEquals(new RGB(0x1F, 0x6F, 0xB5), TitleBarColors.get());
	}

	@Test
	void forgetsTheColorWhenTheBoxIsUnchecked() {
		TitleBarColors.set(new RGB(9, 9, 9));
		TitleBarColorPreferencePage other = new TitleBarColorPreferencePage();
		other.createControl(shell);
		other.performDefaults();
		assertTrue(other.performOk());
		assertNull(TitleBarColors.get());
	}

	@Test
	void tellsWhyNothingWillHappenWhereTheSystemCannotDoIt() {
		String problem = TitleBarPainter.forThisSystem().problem(shell);
		assertEquals(problem == null ? null : problem.lines().findFirst().get(), page.getMessage());
		assertEquals(problem == null ? IMessageProvider.NONE : IMessageProvider.WARNING, page.getMessageType());
	}
}
