package org.eclipse.titlebarcolor;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Window > Preferences > General > Appearance > Title Bar Color: the color of the title bar of this workspace. */
public class TitleBarColorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final RGB SUGGESTION = new RGB(0x1F, 0x6F, 0xB5);

	private Button enabled;
	private ColorSelector selector;

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(2, false));
		Label note = new Label(page, SWT.WRAP);
		note.setText("The title bar of the windows of this workspace gets its own color, to tell the workspaces apart. "
				+ "Each workspace keeps its own choice.");
		note.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		enabled = new Button(page, SWT.CHECK);
		enabled.setText("Color the title bar");
		RGB current = TitleBarColors.get();
		enabled.setSelection(current != null);
		selector = new ColorSelector(page);
		selector.setColorValue(current != null ? current : SUGGESTION);
		selector.setEnabled(current != null);
		enabled.addListener(SWT.Selection, e -> selector.setEnabled(enabled.getSelection()));
		if (!TitleBarPainter.forThisSystem().supported()) {
			Label unsupported = new Label(page, SWT.WRAP);
			unsupported.setText("This system does not let Eclipse color the title bar yet: only Windows 11 and macOS do.");
			unsupported.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		}
		return page;
	}

	@Override
	protected void performDefaults() {
		enabled.setSelection(false);
		selector.setEnabled(false);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		TitleBarColors.set(enabled.getSelection() ? selector.getColorValue() : null);
		try {
			TitleBarColors.preferences().flush();
		} catch (org.osgi.service.prefs.BackingStoreException e) {
			setErrorMessage("The color could not be saved: " + e.getMessage());
			return false;
		}
		return true;
	}
}
