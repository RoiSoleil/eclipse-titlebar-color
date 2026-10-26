package org.eclipse.titlebarcolor;

import java.util.Locale;
import java.util.Map;
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
 * (client side decoration), so a style sheet added to the window colors it. Under X11, XWayland included, the
 * window manager draws the title bar and no application can color it.
 */
final class LinuxTitleBarPainter implements TitleBarPainter {

	private static final int GTK_STYLE_PROVIDER_PRIORITY_USER = 800;

	public interface Gtk extends Library {
		Pointer gtk_css_provider_new();

		boolean gtk_css_provider_load_from_data(Pointer provider, String css, long length, Pointer error);

		Pointer gtk_widget_get_toplevel(Pointer widget);

		Pointer gtk_widget_get_style_context(Pointer widget);

		void gtk_style_context_add_provider(Pointer context, Pointer provider, int priority);

		void gtk_style_context_remove_provider(Pointer context, Pointer provider);

		Pointer gdk_display_get_default();

		String g_type_name_from_instance(Pointer instance);

		void g_object_unref(Pointer object);
	}

	private Gtk gtk;
	/** The style sheet this painter gave to each window, to take it away again. */
	private final Map<Shell, Pointer> providers = new WeakHashMap<>();

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
			return null;
		} catch (LinkageError | RuntimeException e) {
			return "Eclipse cannot call GTK: the JNA bundle (com.sun.jna) or libgtk-3 is not available (" + e + ").";
		}
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
			Pointer provider = gtk().gtk_css_provider_new();
			// The window that lost the focus has its own style (:backdrop), which the theme gives a color too.
			String css = ".titlebar, headerbar, .titlebar:backdrop, headerbar:backdrop { background: " + hex
					+ "; background-image: none; color: " + text + "; text-shadow: none; } "
					+ ".titlebar label, headerbar label, .titlebar button, headerbar button { color: " + text + "; }";
			if (!gtk().gtk_css_provider_load_from_data(provider, css, -1, null)) {
				gtk().g_object_unref(provider);
				return false;
			}
			try {
				gtk().gtk_style_context_add_provider(gtk().gtk_widget_get_style_context(window), provider,
						GTK_STYLE_PROVIDER_PRIORITY_USER);
			} catch (LinkageError | RuntimeException e) {
				gtk().g_object_unref(provider);
				throw e;
			}
			// The new style sheet is on the window: now the previous one can go.
			Pointer previous = providers.put(shell, provider);
			if (previous == null) {
				// Each window frees its style sheet when it is closed, whether it was reset or not.
				shell.addListener(SWT.Dispose, event -> reset(shell));
			} else {
				release(window, previous);
			}
			return true;
		} catch (LinkageError | RuntimeException e) {
			return false;
		}
	}

	@Override
	public void reset(Shell shell) {
		Pointer provider = providers.remove(shell);
		if (provider == null) {
			return;
		}
		try {
			release(shell.isDisposed() ? null : window(shell), provider);
		} catch (LinkageError | RuntimeException e) {
			// The window is gone with its style sheet.
		}
	}

	private void release(Pointer window, Pointer provider) {
		if (window != null) {
			gtk().gtk_style_context_remove_provider(gtk().gtk_widget_get_style_context(window), provider);
		}
		gtk().g_object_unref(provider);
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
