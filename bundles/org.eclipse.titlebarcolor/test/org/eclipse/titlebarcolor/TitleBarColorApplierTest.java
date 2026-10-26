package org.eclipse.titlebarcolor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
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
	};
	private Shell shell;

	@BeforeEach
	void open() {
		TitleBarColors.useForTests(new EclipsePreferences());
		shell = new Shell(Display.getDefault());
		TitleBarColors.set(null);
	}

	@AfterEach
	void close() {
		shell.dispose();
		TitleBarColors.set(null);
	}

	@Test
	void paintsTheWindowWithTheColorOfTheWorkspace() {
		TitleBarColors.set(new RGB(1, 2, 3));
		new TitleBarColorApplier(null, painter).apply(shell);
		assertEquals(List.of("paint #010203"), calls);
	}

	@Test
	void givesTheSystemColorBackWhenThereIsNoColor() {
		new TitleBarColorApplier(null, painter).apply(shell);
		assertEquals(List.of("reset"), calls);
	}

	@Test
	void ignoresAWindowThatIsGone() {
		shell.dispose();
		new TitleBarColorApplier(null, painter).apply(shell);
		new TitleBarColorApplier(null, painter).apply(null);
		assertEquals(List.of(), calls);
	}

	/** A workbench with one window, which hands the window listener it is given to the test. */
	private IWorkbench workbench(IWorkbenchWindow window, IWindowListener[] listener) {
		return (IWorkbench) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IWorkbench.class },
				(proxy, method, args) -> switch (method.getName()) {
				case "getDisplay" -> shell.getDisplay();
				case "getWorkbenchWindows" -> new IWorkbenchWindow[] { window };
				case "addWindowListener" -> {
					listener[0] = (IWindowListener) args[0];
					yield null;
				}
				default -> throw new UnsupportedOperationException(method.getName());
				});
	}

	private IWorkbenchWindow window() {
		return (IWorkbenchWindow) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { IWorkbenchWindow.class }, (proxy, method, args) -> {
					if (method.getName().equals("getShell")) {
						return shell;
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}

	@Test
	void installColorsTheOpenWindowsAndTheNextOnes() {
		IWindowListener[] listener = new IWindowListener[1];
		IWorkbenchWindow window = window();
		TitleBarColors.set(new RGB(1, 2, 3));
		new TitleBarColorApplier(workbench(window, listener), painter).install();
		assertEquals(List.of("paint #010203"), calls);
		listener[0].windowOpened(window);
		listener[0].windowActivated(window);
		listener[0].windowDeactivated(window);
		listener[0].windowClosed(window);
		assertEquals(List.of("paint #010203", "paint #010203"), calls);
	}

	@Test
	void installFollowsTheColorOfThePreferences() {
		IWindowListener[] listener = new IWindowListener[1];
		new TitleBarColorApplier(workbench(window(), listener), painter).install();
		calls.clear();
		TitleBarColors.set(new RGB(4, 5, 6));
		TitleBarColors.preferences().put("another key", "x");
		while (shell.getDisplay().readAndDispatch()) {
			// runs what the change asked of the UI thread
		}
		assertEquals(List.of("paint #040506"), calls);
	}
}
