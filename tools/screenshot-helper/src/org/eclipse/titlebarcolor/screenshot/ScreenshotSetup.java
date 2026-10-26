package org.eclipse.titlebarcolor.screenshot;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.ide.IDE;

/**
 * Sets the workbench up for a screenshot, for tools/screenshot.sh only: a small project with a file open in an
 * editor instead of the pages Eclipse opens at its first start, a window that fits the screen, and then a file that
 * says the window is ready and where it is, in pixels of the screen. Then, when the script asks for it, the same
 * for the preference page of the plug-in.
 */
public class ScreenshotSetup implements IStartup {

	@Override
	public void earlyStartup() {
		// An environment variable: arguments of the JVM on the command line would replace those of eclipse.ini.
		String ready = System.getenv("SCREENSHOT_READY");
		if (ready == null) {
			return;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		Display display = workbench.getDisplay();
		// earlyStartup runs out of the UI thread, where SWT does not let timers be set.
		int settle = Integer.parseInt(System.getenv().getOrDefault("SCREENSHOT_SETTLE", "30")) * 1000;
		display.asyncExec(() -> {
			// The file is written even if setting the workbench up fails, so that the script does not wait for nothing.
			display.timerExec(settle, () -> ready(workbench.getWorkbenchWindows()[0].getShell(), ready));
			String preferences = System.getenv("SCREENSHOT_PREFERENCES");
			if (preferences != null) {
				display.timerExec(settle, () -> openPreferencesWhenAsked(workbench, preferences));
			}
			// Eclipse opens some pages late at its first start (Sponsor...): they are closed again before the screenshot.
			display.timerExec(settle * 2 / 3, () -> setUp(workbench));
			setUp(workbench);
		});
	}

	/**
	 * Opens the preference page of the plug-in once the file exists, which the script creates after the screenshot of
	 * the workbench, and says where the dialog is in the file with ".ready" after its name.
	 */
	private static void openPreferencesWhenAsked(IWorkbench workbench, String asked) {
		Display display = workbench.getDisplay();
		if (!Files.exists(Path.of(asked))) {
			display.timerExec(1000, () -> openPreferencesWhenAsked(workbench, asked));
			return;
		}
		Shell window = workbench.getWorkbenchWindows()[0].getShell();
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(window, "org.eclipse.titlebarcolor.preferences",
				null, null);
		dialog.setBlockOnOpen(false);
		dialog.open();
		Rectangle bounds = window.getBounds();
		dialog.getShell().setBounds(bounds.x + 120, bounds.y + 90, 900, 600);
		display.timerExec(5000, () -> ready(dialog.getShell(), asked + ".ready"));
	}

	/** Says where the window is, in points of the screen, as "x y width height screenWidth screenHeight". */
	private static void ready(Shell shell, String ready) {
		Rectangle bounds = shell.getBounds();
		Rectangle screen = shell.getDisplay().getPrimaryMonitor().getBounds();
		String text = bounds.x + " " + bounds.y + " " + bounds.width + " " + bounds.height + " " + screen.width + " "
				+ screen.height + "\n";
		try {
			// Written aside then moved, so that the script never reads half a line.
			Path file = Path.of(ready);
			Path partial = file.resolveSibling(file.getFileName() + ".partial");
			Files.writeString(partial, text);
			Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	private static void setUp(IWorkbench workbench) {
		if (workbench.getWorkbenchWindows().length == 0) {
			return;
		}
		IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			// The notification of the packages of Eclipse (Sponsor the Eclipse IDE) opens a view with a web page,
			// titled Notification where there is no browser.
			for (IViewReference view : page.getViewReferences()) {
				String name = view.getPartName() + " " + view.getTitle();
				if (name.contains("Sponsor") || name.contains("Notification")) {
					page.hideView(view);
				}
			}
			page.closeAllEditors(false);
			try {
				IDE.openEditor(page, sample());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		// The question about Microsoft Defender comes before the preferences of the workspace are read.
		for (Shell dialog : window.getShell().getDisplay().getShells()) {
			if (!dialog.isDisposed() && dialog.getText().contains("Defender")) {
				dialog.close();
			}
		}
		Shell shell = window.getShell();
		Rectangle screen = shell.getDisplay().getPrimaryMonitor().getClientArea();
		shell.setMaximized(false);
		shell.setBounds(screen.x + 20, screen.y + 20, Math.min(1280, screen.width - 40), Math.min(800, screen.height - 40));
		shell.forceActive();
	}

	private static IFile sample() throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("hello");
		if (!project.exists()) {
			project.create(null);
		}
		project.open(null);
		IFolder folder = project.getFolder("src");
		if (!folder.exists()) {
			folder.create(true, true, null);
		}
		IFile file = folder.getFile("Hello.java");
		if (!file.exists()) {
			String source = """
					/** Each workspace has its own color, in the title bar of its windows. */
					public class Hello {

						public static void main(String[] args) {
							System.out.println("Hello from my workspace");
						}
					}
					""";
			file.create(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), true, null);
		}
		return file;
	}
}
