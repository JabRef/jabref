/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.external.*;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.help.HelpDialog;
import net.sf.jabref.plugin.core.JabRefPlugin;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ExternalTab extends JPanel implements PrefsTab {

	JabRefPreferences _prefs;

	JabRefFrame _frame;

	JTextField pdfDir, regExpTextField, fileDir, psDir, emailSubject;
            
    JCheckBox bibLocationAsFileDir, bibLocAsPrimaryDir, runAutoFileSearch,
            allowFileAutoOpenBrowse, openFoldersOfAttachedFiles;
    JButton editFileTypes;
    ItemListener regExpListener;

	JRadioButton useRegExpComboBox;
    JRadioButton matchExactKeyOnly = new JRadioButton(Globals.lang("Autolink only files that match the BibTeX key")),
        matchStartsWithKey = new JRadioButton(Globals.lang("Autolink files with names starting with the BibTeX key"));

    public ExternalTab(JabRefFrame frame, PrefsDialog3 prefsDiag, JabRefPreferences prefs,
                       HelpDialog helpDialog) {
		_prefs = prefs;
		_frame = frame;
		setLayout(new BorderLayout());

		psDir = new JTextField(25);
		pdfDir = new JTextField(25);
        fileDir = new JTextField(25);
        bibLocationAsFileDir = new JCheckBox(Globals.lang("Allow file links relative to each bib file's location"));
        bibLocAsPrimaryDir = new JCheckBox(Globals.lang("Use the bib file location as primary file directory"));
        bibLocAsPrimaryDir.setToolTipText(Globals.lang("When downloading files, or moving linked files to the "
            +"file directory, prefer the bib file location rather than the file directory set above"));
        bibLocationAsFileDir.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected());
            }
        });
        editFileTypes = new JButton(Globals.lang("Manage external file types"));
        runAutoFileSearch = new JCheckBox(Globals.lang("When opening file link, search for matching file if no link is defined"));
        allowFileAutoOpenBrowse = new JCheckBox(Globals.lang("Automatically open browse dialog when creating new file link"));
        regExpTextField = new JTextField(25);
        useRegExpComboBox = new JRadioButton(Globals.lang("Use Regular Expression Search"));
		regExpListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				regExpTextField.setEditable(useRegExpComboBox.isSelected());
			}
		};
		useRegExpComboBox.addItemListener(regExpListener);

        editFileTypes.addActionListener(ExternalFileTypeEditor.getAction(prefsDiag));

        ButtonGroup bg = new ButtonGroup();
        bg.add(matchExactKeyOnly);
        bg.add(matchStartsWithKey);
        bg.add(useRegExpComboBox);

        BrowseAction browse;

		FormLayout layout = new FormLayout(
			"1dlu, 8dlu, left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref","");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);

		builder.appendSeparator(Globals.lang("External file links"));
		JPanel pan = new JPanel();
		builder.append(pan);
		/**
		 * Fix for [ 1749613 ] About translation
		 * 
		 * https://sourceforge.net/tracker/index.php?func=detail&aid=1749613&group_id=92314&atid=600306
		 * 
		 * Cannot really use %0 to refer to the file type, since this ruins translation.
		 */
		JLabel lab = new JLabel(Globals.lang("Main file directory") + ":");
		builder.append(lab);
		builder.append(fileDir);
		browse = new BrowseAction(_frame, fileDir, true);
		builder.append(new JButton(browse));
		builder.nextLine();
        builder.append(new JPanel());
        builder.append(bibLocationAsFileDir, 3);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(bibLocAsPrimaryDir, 3);
        builder.nextLine();
		builder.append(new JPanel());
        builder.append(matchStartsWithKey, 3);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(matchExactKeyOnly, 3);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(useRegExpComboBox);
		builder.append(regExpTextField);

		HelpAction helpAction = new HelpAction(helpDialog, GUIGlobals.regularExpressionSearchHelp,
			Globals.lang("Help on Regular Expression Search"), GUIGlobals.getIconUrl("helpSmall"));
		builder.append(helpAction.getIconButton());
		builder.nextLine();
        builder.append(new JPanel());
        builder.append(runAutoFileSearch, 3);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(allowFileAutoOpenBrowse);
        builder.nextLine();

		builder.appendSeparator(Globals.lang("Sending of emails"));
		builder.append(new JPanel());
		lab = new JLabel(Globals.lang("Subject for sending an email with references").concat(":"));
		builder.append(lab);
		emailSubject = new JTextField(25);
		builder.append(emailSubject);
		builder.nextLine();
		builder.append(new JPanel());
		openFoldersOfAttachedFiles = new JCheckBox(Globals.lang("Automatically open folders of attached files"));
		builder.append(openFoldersOfAttachedFiles);
		builder.nextLine();

        builder.appendSeparator(Globals.lang("Legacy file fields"));
		pan = new JPanel();
		builder.append(pan);		
		builder.append(new JLabel("<html>"+Globals.lang("Note that these settings are used for the legacy "
			+"<b>pdf</b> and <b>ps</b> fields only.<br>For most users, setting the <b>Main file directory</b> "
			+"above should be sufficient.")+"</html>"), 5);
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
		builder.appendSeparator(Globals.lang("External programs"));

		builder.nextLine();

        JPanel butpan = new JPanel();
        butpan.setLayout(new GridLayout(2,3));
        addSettingsButton(new PushToLyx(), butpan);
        addSettingsButton(new PushToEmacs(), butpan);
        addSettingsButton(new PushToWinEdt(), butpan);
        addSettingsButton(new PushToVim(), butpan);
        addSettingsButton(new PushToLatexEditor(), butpan);
        addSettingsButton(new PushToTeXstudio(), butpan);
        builder.append(new JPanel());
        builder.append(butpan, 3);

        builder.nextLine();
        builder.append(pan);
        builder.append(editFileTypes);
        
        pan = builder.getPanel();
		pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(pan, BorderLayout.CENTER);

    }

    private void addSettingsButton(final PushToApplication pt, JPanel p) {
        //if (b.getColumn() < 2)
        //    b.append(new JPanel());
        //JPanel pan = new JPanel();
        //pan.setLayout(new BorderLayout());
        //pan.add(new JLabel(Globals.lang("Settings for %0", pt.getApplicationName())), BorderLayout.CENTER);
        JButton button = new JButton(Globals.lang("Settings for %0", pt.getApplicationName()),
                pt.getIcon());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                PushToApplicationButton.showSettingsDialog(_frame, pt, pt.getSettingsPanel());
            }
        });
        p.add(button);//, BorderLayout.CENTER);
        //b.append(button);
        //b.nextLine();
        //b.append(pan);
        //if (b.getColumn() >= 6)
        //    b.nextLine();
    }

	public void setValues() {
		pdfDir.setText(_prefs.get("pdfDirectory"));
		psDir.setText(_prefs.get("psDirectory"));
        fileDir.setText(_prefs.get(GUIGlobals.FILE_FIELD+"Directory"));
        bibLocationAsFileDir.setSelected(_prefs.getBoolean("bibLocationAsFileDir"));
        bibLocAsPrimaryDir.setSelected(_prefs.getBoolean("bibLocAsPrimaryDir"));
        bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected());
        runAutoFileSearch.setSelected(_prefs.getBoolean("runAutomaticFileSearch"));
		regExpTextField.setText(_prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY));
        allowFileAutoOpenBrowse.setSelected(_prefs.getBoolean("allowFileAutoOpenBrowse"));
        
        emailSubject.setText(_prefs.get(JabRefPreferences.EMAIL_SUBJECT));
        openFoldersOfAttachedFiles.setSelected(_prefs.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        if (_prefs.getBoolean(JabRefPreferences.USE_REG_EXP_SEARCH_KEY))
            useRegExpComboBox.setSelected(true);
        else if (_prefs.getBoolean("autolinkExactKeyOnly"))
            matchExactKeyOnly.setSelected(true);
        else
            matchStartsWithKey.setSelected(true);
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
        _prefs.putBoolean("bibLocationAsFileDir", bibLocationAsFileDir.isSelected());
        _prefs.putBoolean("bibLocAsPrimaryDir", bibLocAsPrimaryDir.isSelected());
		_prefs.putBoolean("autolinkExactKeyOnly", matchExactKeyOnly.isSelected());
        _prefs.putBoolean("runAutomaticFileSearch", runAutoFileSearch.isSelected());
        _prefs.putBoolean("allowFileAutoOpenBrowse", allowFileAutoOpenBrowse.isSelected());
        _prefs.put(JabRefPreferences.EMAIL_SUBJECT, emailSubject.getText());
        _prefs.putBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, openFoldersOfAttachedFiles.isSelected());
    }

	public boolean readyToClose() {
		return true;
	}

	public String getTabName() {
		return Globals.lang("External programs");
	}
}
