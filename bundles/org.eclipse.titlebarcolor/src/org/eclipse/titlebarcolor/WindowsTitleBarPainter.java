package org.eclipse.titlebarcolor;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Windows 11 (build 22000 or later): the caption and text colors that the Desktop Window Manager
 * accepts for each window. Older versions of Windows refuse them, and keep their title bar.
 */
final class WindowsTitleBarPainter implements TitleBarPainter {

	private static final int DWMWA_CAPTION_COLOR = 35;
	private static final int DWMWA_TEXT_COLOR = 36;
	/** Tells the window manager to use its own color again. */
	private static final int DWMWA_COLOR_DEFAULT = 0xFFFFFFFF;

	public interface Dwmapi extends Library {
		int DwmSetWindowAttribute(Pointer window, int attribute, IntByReference value, int size);
	}

	private Dwmapi dwmapi;

	private Dwmapi dwmapi() {
		if (dwmapi == null) {
			dwmapi = Native.load("dwmapi", Dwmapi.class);
		}
		return dwmapi;
	}

	@Override
	public boolean paint(Shell shell, RGB color) {
		Pointer window = window(shell);
		if (window == null) {
			return false;
		}
		RGB text = TitleBarColors.prefersLightText(color) ? new RGB(255, 255, 255) : new RGB(0, 0, 0);
		return set(window, DWMWA_CAPTION_COLOR, colorRef(color)) && set(window, DWMWA_TEXT_COLOR, colorRef(text));
	}

	@Override
	public void reset(Shell shell) {
		Pointer window = window(shell);
		if (window != null) {
			set(window, DWMWA_CAPTION_COLOR, DWMWA_COLOR_DEFAULT);
			set(window, DWMWA_TEXT_COLOR, DWMWA_COLOR_DEFAULT);
		}
	}

	private boolean set(Pointer window, int attribute, int colorRef) {
		try {
			// A negative result (E_INVALIDARG...) is a Windows that does not know the attribute.
			return dwmapi().DwmSetWindowAttribute(window, attribute, new IntByReference(colorRef), 4) == 0;
		} catch (LinkageError | RuntimeException e) {
			// JNA, or the library of Windows, is not there: the title bar keeps its color.
			return false;
		}
	}

	/** COLORREF is 0x00BBGGRR. */
	static int colorRef(RGB color) {
		return color.blue << 16 | color.green << 8 | color.red;
	}

	/** The HWND of the shell: the handle field exists in the SWT of Windows only, hence the reflection. */
	private static Pointer window(Shell shell) {
		try {
			long handle = shell.isDisposed() ? 0 : Control.class.getField("handle").getLong(shell);
			return handle == 0 ? null : new Pointer(handle);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
