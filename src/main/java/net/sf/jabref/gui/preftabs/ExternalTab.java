/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.preftabs;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.push.*;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpDialog;
import net.sf.jabref.gui.util.GUIGlobals;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

class ExternalTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JabRefFrame frame;

    private final JTextField regExpTextField;
    private final JTextField fileDir;
    private final JTextField emailSubject;
    private final JTextField citeCommand;

    private final JCheckBox bibLocationAsFileDir;
    private final JCheckBox bibLocAsPrimaryDir;
    private final JCheckBox runAutoFileSearch;
    private final JCheckBox allowFileAutoOpenBrowse;
    private final JCheckBox openFoldersOfAttachedFiles;

    private final JRadioButton useRegExpComboBox;
    private final JRadioButton matchExactKeyOnly = new JRadioButton(Localization.lang("Autolink only files that match the BibTeX key"));
    private final JRadioButton matchStartsWithKey = new JRadioButton(Localization.lang("Autolink files with names starting with the BibTeX key"));


    public ExternalTab(JabRefFrame frame, PreferencesDialog prefsDiag, JabRefPreferences prefs,
            HelpDialog helpDialog) {
        this.prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        fileDir = new JTextField(25);
        bibLocationAsFileDir = new JCheckBox(Localization.lang("Allow file links relative to each bib file's location"));
        bibLocAsPrimaryDir = new JCheckBox(Localization.lang("Use the bib file location as primary file directory"));
        bibLocAsPrimaryDir.setToolTipText(Localization.lang("When downloading files, or moving linked files to the "
                + "file directory, prefer the bib file location rather than the file directory set above"));
        bibLocationAsFileDir.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected());
            }
        });
        JButton editFileTypes = new JButton(Localization.lang("Manage external file types"));
        runAutoFileSearch = new JCheckBox(Localization.lang("When opening file link, search for matching file if no link is defined"));
        allowFileAutoOpenBrowse = new JCheckBox(Localization.lang("Automatically open browse dialog when creating new file link"));
        citeCommand = new JTextField(25);
        regExpTextField = new JTextField(25);
        useRegExpComboBox = new JRadioButton(Localization.lang("Use Regular Expression Search"));
        ItemListener regExpListener = new ItemListener() {


            @Override
            public void itemStateChanged(ItemEvent e) {
                regExpTextField.setEditable(useRegExpComboBox.isSelected());
            }
        };
        useRegExpComboBox.addItemListener(regExpListener);

        editFileTypes.addActionListener(ExternalFileTypeEditor.getAction(prefsDiag));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(matchExactKeyOnly);
        buttonGroup.add(matchStartsWithKey);
        buttonGroup.add(useRegExpComboBox);

        BrowseAction browse;

        FormLayout layout = new FormLayout(
                "1dlu, 8dlu, left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("External file links"));
        JPanel pan = new JPanel();
        builder.append(pan);
        /**
         * Fix for [ 1749613 ] About translation
         *
         * https://sourceforge.net/tracker/index.php?func=detail&aid=1749613&group_id=92314&atid=600306
         *
         * Cannot really use %0 to refer to the file type, since this ruins translation.
         */
        JLabel lab = new JLabel(Localization.lang("Main file directory") + ':');
        builder.append(lab);
        builder.append(fileDir);
        browse = BrowseAction.buildForDir(this.frame, fileDir);
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

        builder.append(new HelpAction(helpDialog, GUIGlobals.regularExpressionSearchHelp,
                Localization.lang("Help on Regular Expression Search")).getIconButton());
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(runAutoFileSearch, 3);
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(allowFileAutoOpenBrowse);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Sending of emails"));
        builder.append(new JPanel());
        lab = new JLabel(Localization.lang("Subject for sending an email with references").concat(":"));
        builder.append(lab);
        emailSubject = new JTextField(25);
        builder.append(emailSubject);
        builder.nextLine();
        builder.append(new JPanel());
        openFoldersOfAttachedFiles = new JCheckBox(Localization.lang("Automatically open folders of attached files"));
        builder.append(openFoldersOfAttachedFiles);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("External programs"));
        builder.nextLine();

        JPanel butpan = new JPanel();
        butpan.setLayout(new GridLayout(3, 3));
        for(PushToApplication pushToApplication : PushToApplications.applications) {
            addSettingsButton(pushToApplication, butpan);
        }
        builder.append(new JPanel());
        builder.append(butpan, 3);

        builder.nextLine();
        lab = new JLabel(Localization.lang("Cite command") + ':');
        builder.append(pan);
        builder.append(lab);
        builder.append(citeCommand);

        builder.nextLine();
        builder.append(pan);
        builder.append(editFileTypes);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    private void addSettingsButton(final PushToApplication pt, JPanel p) {
        JButton button = new JButton(Localization.lang("Settings for %0", pt.getApplicationName()),
                pt.getIcon());
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                PushToApplicationButton.showSettingsDialog(frame, pt, pt.getSettingsPanel());
            }
        });
        p.add(button);
    }

    @Override
    public void setValues() {
        fileDir.setText(prefs.get(Globals.FILE_FIELD + "Directory"));
        bibLocationAsFileDir.setSelected(prefs.getBoolean(JabRefPreferences.BIB_LOCATION_AS_FILE_DIR));
        bibLocAsPrimaryDir.setSelected(prefs.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR));
        bibLocAsPrimaryDir.setEnabled(bibLocationAsFileDir.isSelected());
        runAutoFileSearch.setSelected(prefs.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH));
        regExpTextField.setText(prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY));
        allowFileAutoOpenBrowse.setSelected(prefs.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE));

        emailSubject.setText(prefs.get(JabRefPreferences.EMAIL_SUBJECT));
        openFoldersOfAttachedFiles.setSelected(prefs.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        citeCommand.setText(prefs.get(JabRefPreferences.CITE_COMMAND));

        if (prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
            useRegExpComboBox.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY)) {
            matchExactKeyOnly.setSelected(true);
        } else {
            matchStartsWithKey.setSelected(true);
        }
    }

    @Override
    public void storeSettings() {

        prefs.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, useRegExpComboBox.isSelected());
        if (useRegExpComboBox.isSelected()) {
            prefs.put(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY, regExpTextField.getText());
        }

        // We should maybe do some checking on the validity of the contents?
        prefs.put(Globals.FILE_FIELD + "Directory", fileDir.getText());
        prefs.putBoolean(JabRefPreferences.BIB_LOCATION_AS_FILE_DIR, bibLocationAsFileDir.isSelected());
        prefs.putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, bibLocAsPrimaryDir.isSelected());
        prefs.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, matchExactKeyOnly.isSelected());
        prefs.putBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH, runAutoFileSearch.isSelected());
        prefs.putBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE, allowFileAutoOpenBrowse.isSelected());
        prefs.put(JabRefPreferences.EMAIL_SUBJECT, emailSubject.getText());
        prefs.putBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, openFoldersOfAttachedFiles.isSelected());
        prefs.put(JabRefPreferences.CITE_COMMAND, citeCommand.getText());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }
}
