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
 * Linux, when GTK draws the title bar of the windows (client side decoration, the style class "csd" of the window):
 * a style sheet colors it. GTK does so when Eclipse runs on Wayland itself under GNOME, or anywhere when it is started
 * with GTK_CSD=1. Otherwise the window manager draws the title bar (X11, XWayland, KDE) and no application can color
 * it: the preference page then says what to start Eclipse with.
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

		Pointer gtk_widget_get_style_context(Pointer widget);

		boolean gtk_style_context_has_class(Pointer context, String name);

		boolean gtk_widget_get_realized(Pointer widget);

		String g_type_name_from_instance(Pointer instance);

		void g_object_unref(Pointer object);
	}

	private Gtk gtk;
	/** The style sheet of the screen while a window is painted, and what it holds. */
	private Pointer provider;
	/** The screen the style sheet was given to, to take it back from the same one. */
	private Pointer screen;
	private String css;
	/** What keeps GTK from drawing the title bars in this configuration, with the ways around it; computed once. */
	private String configurationProblem;
	/** Whether the configuration lets GTK draw the title bars, for a window not on the screen yet; computed once. */
	private Boolean configurationDecorates;
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
			Pointer gtkWindow = window(window);
			if (gtkWindow == null) {
				return "Eclipse did not give the GTK window of its window (the SWT of this Eclipse is not the one of GTK 3).";
			}
			if (configurationProblem == null) {
				configurationProblem = configurationProblem();
			}
			// GTK says whether it decorates a window once the window is on the screen, and decides for each window
			// (it does not decorate a pop-up): before, the configuration tells.
			boolean decorated = gtk().gtk_widget_get_realized(gtkWindow)
					? gtk().gtk_style_context_has_class(gtk().gtk_widget_get_style_context(gtkWindow), "csd")
					: configurationDecorates;
			return decorated ? null : configurationProblem;
		} catch (LinkageError | RuntimeException e) {
			return "Eclipse cannot call GTK: the JNA bundle (com.sun.jna) or libgtk-3 is not available (" + e + ").";
		}
	}

	/**
	 * What keeps GTK from drawing the title bars in this configuration, with the ways around it, one per line after
	 * the first. Also tells whether the configuration should let GTK draw them.
	 */
	private String configurationProblem() {
		Pointer gdkDisplay = gtk().gdk_display_get_default();
		String display = gdkDisplay == null ? "no display" : gtk().g_type_name_from_instance(gdkDisplay);
		boolean wayland = "GdkWaylandDisplay".equals(display);
		String desktop = System.getenv("XDG_CURRENT_DESKTOP");
		boolean kde = desktop != null && desktop.toUpperCase(Locale.ROOT).contains("KDE");
		boolean csdForced = "1".equals(System.getenv("GTK_CSD"));
		configurationDecorates = csdForced || wayland && !kde;
		String eclipse = quoted(System.getProperty("eclipse.launcher", "/path/to/eclipse"));
		StringBuilder problem = new StringBuilder(kde ? "KDE draws the title bar of Eclipse, not GTK: Eclipse cannot color it."
				: "The window manager draws the title bar of Eclipse, not GTK: Eclipse cannot color it.");
		if (csdForced) {
			// The usual way around did not work: nothing else is known to.
			return problem.append(" GTK_CSD=1 did not make GTK draw it in this configuration (").append(display)
					.append(kde ? ", KDE" : "").append(").").toString();
		}
		boolean waylandSession = System.getenv("WAYLAND_DISPLAY") != null
				|| "wayland".equals(System.getenv("XDG_SESSION_TYPE"));
		String variable = "GTK_CSD=1";
		if (!wayland && waylandSession && !kde) {
			variable = "GDK_BACKEND=wayland";
			problem.append("\nStart Eclipse on Wayland itself, where GTK draws the title bar: GDK_BACKEND=wayland ")
					.append(eclipse);
			problem.append("\nOr have GTK draw the title bar: GTK_CSD=1 ").append(eclipse);
		} else {
			problem.append("\nHave GTK draw the title bar: GTK_CSD=1 ").append(eclipse);
		}
		if (kde) {
			problem.append(" (the title bar then has the look of GTK, not that of Breeze)");
		}
		problem.append("\nSet the variable in the command or the .desktop file that starts Eclipse (for example Exec=env ")
				.append(variable).append(' ').append(eclipse).append("), then restart Eclipse.");
		return problem.toString();
	}

	/** The path as a shell and a .desktop file read it. */
	private static String quoted(String path) {
		return path.matches("[\\w./+-]+") ? path : "\"" + path.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
