package org.eclipse.titlebarcolor;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Linux, when Eclipse runs on Wayland itself (GDK_BACKEND=wayland): GTK draws the title bar of its windows
 * (client side decoration), so a style sheet colors it. Under X11, XWayland included, the window manager draws the
 * title bar and no application can color it.
 * <p>
 * The style sheet is given to the whole screen of the application: one given to a window only styles the window,
 * not the title bar inside it. A process of Eclipse has a single workspace, so all its windows have the same color,
 * the dialogs of GTK (file chooser...) included, and a window gets the color of the system back only with the last
 * one: {@link #reset(Shell)} takes the style sheet away when no painted window is left.
 */
final class LinuxTitleBarPainter implements TitleBarPainter {

	private static final int GTK_STYLE_PROVIDER_PRIORITY_USER = 800;

	public interface Gtk extends Library {
		Pointer gtk_css_provider_new();

		boolean gtk_css_provider_load_from_data(Pointer provider, String css, long length, Pointer error);

		Pointer gtk_widget_get_toplevel(Pointer widget);

		Pointer gdk_screen_get_default();

		void gtk_style_context_add_provider_for_screen(Pointer screen, Pointer provider, int priority);

		void gtk_style_context_remove_provider_for_screen(Pointer screen, Pointer provider);

		Pointer gdk_display_get_default();

		String g_type_name_from_instance(Pointer instance);

		void g_object_unref(Pointer object);
	}

	private Gtk gtk;
	/** The style sheet of the screen while a window is painted, and what it holds. */
	private Pointer provider;
	/** The screen the style sheet was given to, to take it back from the same one. */
	private Pointer screen;
	private String css;
	/** What keeps GTK from drawing the title bars, the same for the whole life of the process; "" for nothing. */
	private String systemProblem;
	/** The windows painted: the style sheet goes when the last one is reset or closed. */
	private final Set<Shell> painted = Collections.newSetFromMap(new WeakHashMap<>());
	/** The windows whose closing is followed, once each. */
	private final Set<Shell> followed = Collections.newSetFromMap(new WeakHashMap<>());

	private Gtk gtk() {
		if (gtk == null) {
			gtk = Native.load("libgtk-3.so.0", Gtk.class);
		}
		return gtk;
	}

	@Override
	public String problem(Shell window) {
		try {
			if (window(window) == null) {
				return "Eclipse did not give the GTK window of its window (the SWT of this Eclipse is not the one of GTK 3).";
			}
			if (systemProblem == null) {
				systemProblem = systemProblem();
			}
			return systemProblem.isEmpty() ? null : systemProblem;
		} catch (LinkageError | RuntimeException e) {
			return "Eclipse cannot call GTK: the JNA bundle (com.sun.jna) or libgtk-3 is not available (" + e + ").";
		}
	}

	private String systemProblem() {
		Pointer gdkDisplay = gtk().gdk_display_get_default();
		String display = gdkDisplay == null ? "no display" : gtk().g_type_name_from_instance(gdkDisplay);
		if (!"GdkWaylandDisplay".equals(display)) {
			return "Eclipse runs on X11 (" + display + "), where the window manager draws the title bar: start it "
					+ "on Wayland itself (environment variable GDK_BACKEND=wayland) to color it.";
		}
		String desktop = System.getenv("XDG_CURRENT_DESKTOP");
		if (desktop != null && desktop.toUpperCase(Locale.ROOT).contains("KDE")) {
			return "KDE draws the title bar of the windows itself, whatever the application asks.";
		}
		return "";
	}

	@Override
	public boolean paint(Shell shell, RGB color) {
		try {
			Pointer window = window(shell);
			if (window == null || problem(shell) != null) {
				return false;
			}
			String text = TitleBarColors.prefersLightText(color) ? "#FFFFFF" : "#000000";
			String hex = TitleBarColors.format(color);
			// The window that lost the focus has its own style (:backdrop), which the theme gives a color too.
			String sheet = ".titlebar, headerbar, .titlebar:backdrop, headerbar:backdrop { background: " + hex
					+ "; background-image: none; color: " + text + "; text-shadow: none; } "
					+ ".titlebar label, headerbar label, .titlebar button, headerbar button { color: " + text + "; }";
			if (provider == null) {
				Pointer created = gtk().gtk_css_provider_new();
				if (!gtk().gtk_css_provider_load_from_data(created, sheet, -1, null)) {
					gtk().g_object_unref(created);
					return false;
				}
				try {
					screen = gtk().gdk_screen_get_default();
					gtk().gtk_style_context_add_provider_for_screen(screen, created, GTK_STYLE_PROVIDER_PRIORITY_USER);
				} catch (LinkageError | RuntimeException e) {
					gtk().g_object_unref(created);
					throw e;
				}
				provider = created;
				css = sheet;
			} else if (!sheet.equals(css)) {
				// Loading the style sheet again replaces what it held, and GTK restyles the windows.
				if (!gtk().gtk_css_provider_load_from_data(provider, sheet, -1, null)) {
					return false;
				}
				css = sheet;
			}
			painted.add(shell);
			if (followed.add(shell)) {
				// A closed window counts as reset: the style sheet goes with the last one.
				shell.addListener(SWT.Dispose, event -> reset(shell));
			}
			return true;
		} catch (LinkageError | RuntimeException e) {
			return false;
		}
	}

	@Override
	public void reset(Shell shell) {
		if (!painted.remove(shell) || !painted.isEmpty() || provider == null) {
			return;
		}
		try {
			gtk().gtk_style_context_remove_provider_for_screen(screen, provider);
			gtk().g_object_unref(provider);
		} catch (LinkageError | RuntimeException e) {
			// GTK is gone with the application.
		}
		provider = null;
		screen = null;
		css = null;
	}

	/**
	 * The GtkWindow of the shell. The handle of an SWT shell is the widget of its client area, whose top level is the
	 * window that GTK decorates.
	 */
	private Pointer window(Shell shell) {
		try {
			if (shell.isDisposed()) {
				return null;
			}
			long handle = Control.class.getField("handle").getLong(shell);
			return handle == 0 ? null : gtk().gtk_widget_get_toplevel(new Pointer(handle));
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
