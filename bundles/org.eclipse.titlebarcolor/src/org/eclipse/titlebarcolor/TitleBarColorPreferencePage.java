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
		// The page tells why nothing will happen, instead of letting the user pick a color for nothing.
		String problem = TitleBarPainter.forThisSystem().problem(parent.getShell());
		if (problem != null) {
			// The first line says what is wrong, the next ones the ways around it in this configuration.
			int end = problem.indexOf('\n');
			setMessage(end < 0 ? problem : problem.substring(0, end), WARNING);
			if (end >= 0) {
				Label solutions = new Label(page, SWT.WRAP);
				solutions.setText("To color the title bar:\n" + problem.substring(end + 1));
				GridData data = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
				data.widthHint = 400;
				solutions.setLayoutData(data);
			}
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
