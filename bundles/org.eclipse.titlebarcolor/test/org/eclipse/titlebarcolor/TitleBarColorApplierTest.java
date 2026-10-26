package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TitleBarColorApplierTest {

	private final List<String> calls = new ArrayList<>();
	private final TitleBarPainter painter = new TitleBarPainter() {
		@Override
		public boolean paint(Shell shell, RGB color) {
			calls.add("paint " + TitleBarColors.format(color));
			return true;
		}

		@Override
		public void reset(Shell shell) {
			calls.add("reset");
		}

		@Override
		public String problem(Shell window) {
			return null;
		}
	};
	private Shell shell;
	private TitleBarColorApplier installed;

	@BeforeEach
	void open() {
		TitleBarColors.useForTests(new EclipsePreferences());
		shell = new Shell(Display.getDefault());
		TitleBarColors.set(null);
	}

	@AfterEach
	void close() {
		if (installed != null) {
			installed.dispose();
		}
		shell.dispose();
		TitleBarColors.set(null);
		TitleBarColors.useForTests(null);
	}

	@Test
	void paintsTheWindowWithTheColorOfTheWorkspace() {
		TitleBarColors.set(new RGB(1, 2, 3));
		new TitleBarColorApplier(shell.getDisplay(), painter).apply(shell);
		assertEquals(List.of("paint #010203"), calls);
	}

	@Test
	void leavesTheWindowAloneWhenThereIsNoColorAndNothingWasPainted() {
		new TitleBarColorApplier(shell.getDisplay(), painter).apply(shell);
		assertEquals(List.of(), calls);
	}

	@Test
	void givesTheSystemColorBackWhenTheColorIsRemoved() {
		TitleBarColorApplier applier = new TitleBarColorApplier(shell.getDisplay(), painter);
		TitleBarColors.set(new RGB(1, 2, 3));
		applier.apply(shell);
		TitleBarColors.set(null);
		applier.apply(shell);
		applier.apply(shell);
		assertEquals(List.of("paint #010203", "reset"), calls);
	}

	@Test
	void doesNotPaintAgainAWindowOfTheSameColor() {
		TitleBarColorApplier applier = new TitleBarColorApplier(shell.getDisplay(), painter);
		TitleBarColors.set(new RGB(1, 2, 3));
		applier.apply(shell);
		applier.apply(shell);
		TitleBarColors.set(new RGB(4, 5, 6));
		applier.apply(shell);
		assertEquals(List.of("paint #010203", "paint #040506"), calls);
	}

	@Test
	void ignoresAWindowThatIsGone() {
		shell.dispose();
		new TitleBarColorApplier(Display.getDefault(), painter).apply(shell);
		new TitleBarColorApplier(Display.getDefault(), painter).apply(null);
		assertEquals(List.of(), calls);
	}

	@Test
	void installColorsTheOpenWindowsAndTheNextOnes() {
		TitleBarColors.set(new RGB(1, 2, 3));
		installed = new TitleBarColorApplier(shell.getDisplay(), painter);
		installed.install();
		assertEquals(List.of("paint #010203"), calls);
		// A dialog or a detached view is a shell of its own.
		Shell dialog = new Shell(shell, SWT.DIALOG_TRIM);
		try {
			calls.clear();
			dialog.open();
			assertEquals(List.of("paint #010203"), calls);
			// A tool tip has no title bar.
			calls.clear();
			Shell toolTip = new Shell(shell, SWT.NO_TRIM | SWT.ON_TOP);
			toolTip.open();
			toolTip.dispose();
			assertEquals(List.of(), calls);
		} finally {
			dialog.dispose();
		}
	}

	@Test
	void installFollowsTheColorOfThePreferences() {
		installed = new TitleBarColorApplier(shell.getDisplay(), painter);
		installed.install();
		calls.clear();
		TitleBarColors.set(new RGB(4, 5, 6));
		TitleBarColors.preferences().put("another key", "x");
		while (shell.getDisplay().readAndDispatch()) {
			// runs what the change asked of the UI thread
		}
		assertEquals(List.of("paint #040506"), calls);
	}
}
