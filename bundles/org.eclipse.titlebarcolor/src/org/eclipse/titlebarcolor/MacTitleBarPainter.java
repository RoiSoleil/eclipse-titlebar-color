package org.eclipse.titlebarcolor;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * macOS: the title bar of an NSWindow is made transparent, so that the background color of the window shows
 * through it, and the window gets the light or dark appearance whose title reads on that color. Cocoa is
 * called through the Objective-C runtime, on the UI thread, which is the main thread of macOS.
 */
final class MacTitleBarPainter implements TitleBarPainter {

	/**
	 * objc_msgSend takes a different set of arguments for each message: one declaration for each shape, as a
	 * variable argument list is not passed in the same way on Apple silicon.
	 */
	public interface ObjC extends Library {
		Pointer objc_getClass(String name);

		Pointer sel_registerName(String name);

		Pointer objc_msgSend(Pointer receiver, Pointer selector);

		Pointer objc_msgSend(Pointer receiver, Pointer selector, Pointer argument);

		Pointer objc_msgSend(Pointer receiver, Pointer selector, String argument);

		Pointer objc_msgSend(Pointer receiver, Pointer selector, byte flag);

		Pointer objc_msgSend(Pointer receiver, Pointer selector, double red, double green, double blue,
				double alpha);
	}

	private ObjC objc;

	private ObjC objc() {
		if (objc == null) {
			objc = Native.load("objc", ObjC.class);
		}
		return objc;
	}

	@Override
	public boolean paint(Shell shell, RGB color) {
		try {
			Pointer window = window(shell);
			if (window == null) {
				return false;
			}
			Pointer nsColor = send(cls("NSColor"), "colorWithSRGBRed:green:blue:alpha:", color.red / 255.0,
					color.green / 255.0, color.blue / 255.0, 1.0);
			send(window, "setTitlebarAppearsTransparent:", (byte) 1);
			send(window, "setBackgroundColor:", nsColor);
			// The dark appearance has a light title.
			String name = TitleBarColors.prefersLightText(color) ? "NSAppearanceNameDarkAqua"
					: "NSAppearanceNameAqua";
			Pointer appearance = send(cls("NSAppearance"), "appearanceNamed:",
					send(cls("NSString"), "stringWithUTF8String:", name));
			send(window, "setAppearance:", appearance);
			return true;
		} catch (LinkageError | RuntimeException e) {
			// JNA, or the Objective-C runtime, is not there or does not answer: the title bar keeps its color.
			return false;
		}
	}

	@Override
	public void reset(Shell shell) {
		try {
			Pointer window = window(shell);
			if (window != null) {
				send(window, "setTitlebarAppearsTransparent:", (byte) 0);
				send(window, "setBackgroundColor:", send(cls("NSColor"), "windowBackgroundColor"));
				send(window, "setAppearance:", (Pointer) null);
			}
		} catch (LinkageError | RuntimeException e) {
			// Nothing was painted without JNA.
		}
	}

	/** What Cocoa has for the transparency of the title bar of the window; false if it cannot say. */
	boolean isTitlebarTransparent(Shell shell) {
		try {
			Pointer window = window(shell);
			// A BOOL is the low byte of the result.
			return window != null && (Pointer.nativeValue(send(window, "titlebarAppearsTransparent")) & 0xFF) != 0;
		} catch (LinkageError | RuntimeException e) {
			return false;
		}
	}

	private Pointer cls(String name) {
		return objc().objc_getClass(name);
	}

	private Pointer send(Pointer receiver, String message) {
		return objc().objc_msgSend(receiver, objc().sel_registerName(message));
	}

	private Pointer send(Pointer receiver, String message, Pointer argument) {
		return objc().objc_msgSend(receiver, objc().sel_registerName(message), argument);
	}

	private Pointer send(Pointer receiver, String message, String argument) {
		return objc().objc_msgSend(receiver, objc().sel_registerName(message), argument);
	}

	private Pointer send(Pointer receiver, String message, byte flag) {
		return objc().objc_msgSend(receiver, objc().sel_registerName(message), flag);
	}

	private Pointer send(Pointer receiver, String message, double red, double green, double blue, double alpha) {
		return objc().objc_msgSend(receiver, objc().sel_registerName(message), red, green, blue, alpha);
	}

	/** The NSWindow of the shell: its NSView is the view field of the SWT of macOS, whose id is the pointer. */
	private Pointer window(Shell shell) {
		try {
			if (shell.isDisposed()) {
				return null;
			}
			Object view = Control.class.getField("view").get(shell);
			long id = view == null ? 0 : view.getClass().getField("id").getLong(view);
			return id == 0 ? null : send(new Pointer(id), "window");
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
