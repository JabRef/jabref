package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.*;

import net.sf.jabref.external.ExternalFileTypeEditor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ExternalTab extends JPanel implements PrefsTab {

	JabRefPreferences _prefs;

	JabRefFrame _frame;

	JTextField pdfDir, regExpTextField, fileDir, psDir, pdf, ps, html, lyx, winEdt, led,
        citeCommand, vim, vimServer;

    JButton editFileTypes;
    ItemListener regExpListener;

	JCheckBox useRegExpComboBox;

	public ExternalTab(JabRefFrame frame, PrefsDialog3 prefsDiag, JabRefPreferences prefs, 
                       HelpDialog helpDialog) {
		_prefs = prefs;
		_frame = frame;
		setLayout(new BorderLayout());

		psDir = new JTextField(30);
		pdfDir = new JTextField(30);
        fileDir = new JTextField(30);
        pdf = new JTextField(30);
		ps = new JTextField(30);
		html = new JTextField(30);
		lyx = new JTextField(30);
		winEdt = new JTextField(30);
		vim = new JTextField(30);
		vimServer = new JTextField(30);
        citeCommand = new JTextField(30);
        led = new JTextField(30);
        editFileTypes = new JButton(Globals.lang("Manage external file types"));

        regExpTextField = new JTextField(30);

		useRegExpComboBox = new JCheckBox(Globals.lang("Use Regular Expression Search"));
		regExpListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				regExpTextField.setEditable(useRegExpComboBox.isSelected());
				if (useRegExpComboBox.isSelected()) {
					regExpTextField.setText(Globals.prefs
						.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY));
				} else {
					Globals.prefs.put(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY,
						regExpTextField.getText());
					regExpTextField.setText(Globals.prefs
						.get(JabRefPreferences.DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY));
				}
			}
		};
		useRegExpComboBox.addItemListener(regExpListener);

        editFileTypes.addActionListener(ExternalFileTypeEditor.getAction(prefsDiag));

        BrowseAction browse;

		FormLayout layout = new FormLayout(
			"1dlu, 8dlu, left:pref, 4dlu, fill:200dlu, 4dlu, fill:pref",// 4dlu,
																		// left:pref,
																		// 4dlu",
			"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);

		builder.appendSeparator(Globals.lang("External file links"));
		JPanel pan = new JPanel();
		builder.append(pan);
		JLabel lab = new JLabel(Globals.lang("Main %0 directory", GUIGlobals.FILE_FIELD) + ":");
		builder.append(lab);
		builder.append(fileDir);
		browse = new BrowseAction(_frame, fileDir, true);
		builder.append(new JButton(browse));
		builder.nextLine();

        pan = new JPanel();
		builder.append(pan);
		lab = new JLabel(Globals.lang("Main PDF directory") + ":");
		builder.append(lab);
		builder.append(pdfDir);
		browse = new BrowseAction(_frame, pdfDir, true);
		builder.append(new JButton(browse));
		builder.nextLine();

        pan = new JPanel();
		builder.append(pan);
		lab = new JLabel(Globals.lang("Main PS directory") + ":");
		builder.append(lab);
		builder.append(psDir);
		browse = new BrowseAction(_frame, psDir, true);
		builder.append(new JButton(browse));
		builder.nextLine();

		builder.append(new JPanel());
		builder.append(useRegExpComboBox);
		builder.append(regExpTextField);
		HelpAction helpAction = new HelpAction(helpDialog, GUIGlobals.regularExpressionSearchHelp,
			Globals.lang("Help on Regular Expression Search"), GUIGlobals.getIconUrl("helpSmall"));
		builder.append(helpAction.getIconButton());
		builder.nextLine();

		builder.appendSeparator(Globals.lang("External programs"));

		/*builder.nextLine();
		lab = new JLabel(Globals.lang("Path to PDF viewer") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(pdf);
		browse = new BrowseAction(_frame, pdf, false);
		if (Globals.ON_WIN)
			browse.setEnabled(false);
		builder.append(new JButton(browse));
		builder.nextLine();
		lab = new JLabel(Globals.lang("Path to PS viewer") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(ps);
		browse = new BrowseAction(_frame, ps, false);
		if (Globals.ON_WIN)
			browse.setEnabled(false);
		builder.append(new JButton(browse));
		*/
		builder.nextLine();
		lab = new JLabel(Globals.lang("Path to HTML viewer") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(html);
		browse = new BrowseAction(_frame, html, false);
		if (Globals.ON_WIN)
			browse.setEnabled(false);
		builder.append(new JButton(browse));
		builder.nextLine();
		lab = new JLabel(Globals.lang("Path to LyX pipe") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(lyx);
		browse = new BrowseAction(_frame, lyx, false);
		builder.append(new JButton(browse));
		builder.nextLine();
		lab = new JLabel(Globals.lang("Path to WinEdt.exe") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(winEdt);
		browse = new BrowseAction(_frame, winEdt, false);
		builder.append(new JButton(browse));
		builder.nextLine();
        lab = new JLabel(Globals.lang("Path to LatexEditor (LEd.exe)") + ":");
        builder.append(pan);
        builder.append(lab);
        builder.append(led);
        browse = new BrowseAction(_frame, led, false);
        builder.append(new JButton(browse));
        builder.nextLine();
        builder.append(pan);
        lab = new JLabel(Globals.lang("Path to Vim") + ":");
		builder.append(lab);
		builder.append(vim);
		browse = new BrowseAction(_frame, vim, false);
		builder.append(new JButton(browse));
		builder.nextLine();
		lab = new JLabel(Globals.lang("Vim Server Name") + ":");
		builder.append(pan);
		builder.append(lab);
		builder.append(vimServer);
		browse = new BrowseAction(_frame, vimServer, false);
		builder.append(new JButton(browse));
		builder.nextLine();
        builder.append(pan);

		builder.append(Globals.lang("Cite command (for Emacs/WinEdt)") + ":");
		builder.append(citeCommand);
		// builder.appendSeparator();

        builder.nextLine();
        builder.append(pan);
        builder.append(editFileTypes);
        
        pan = builder.getPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pan, BorderLayout.CENTER);

    }

	public void setValues() {
		pdfDir.setText(_prefs.get("pdfDirectory"));
		psDir.setText(_prefs.get("psDirectory"));
        fileDir.setText(_prefs.get(GUIGlobals.FILE_FIELD+"Directory"));
        if (!Globals.ON_WIN) {
			pdf.setText(_prefs.get("pdfviewer"));
			ps.setText(_prefs.get("psviewer"));
			html.setText(_prefs.get("htmlviewer"));
		} else {
			pdf.setText(Globals.lang("Uses default application"));
			ps.setText(Globals.lang("Uses default application"));
			html.setText(Globals.lang("Uses default application"));
			pdf.setEnabled(false);
			ps.setEnabled(false);
			html.setEnabled(false);
		}

		lyx.setText(_prefs.get("lyxpipe"));
		winEdt.setText(_prefs.get("winEdtPath"));
        vim.setText(_prefs.get("vim"));
		vimServer.setText(_prefs.get("vimServer"));
        led.setText(_prefs.get("latexEditorPath"));
        citeCommand.setText(_prefs.get("citeCommand"));

		regExpTextField.setText(_prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY));
		useRegExpComboBox.setSelected(_prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY));
		regExpListener.itemStateChanged(null);
	}

	public void storeSettings() {

		_prefs.putBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY, useRegExpComboBox.isSelected());
		if (useRegExpComboBox.isSelected()) {
			_prefs.put(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY, regExpTextField.getText());
		}

		// We should maybe do some checking on the validity of the contents?
		_prefs.put("pdfDirectory", pdfDir.getText());
		_prefs.put("psDirectory", psDir.getText());
        _prefs.put(GUIGlobals.FILE_FIELD+"Directory", fileDir.getText());
        _prefs.put("pdfviewer", pdf.getText());
		_prefs.put("psviewer", ps.getText());
		_prefs.put("htmlviewer", html.getText());
		_prefs.put("lyxpipe", lyx.getText());
		_prefs.put("winEdtPath", winEdt.getText());
        _prefs.put("vim", vim.getText());
        _prefs.put("vimServer", vimServer.getText());
        _prefs.put("latexEditorPath", led.getText());
        _prefs.put("citeCommand", citeCommand.getText());
	}

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("External programs");
	}
}
