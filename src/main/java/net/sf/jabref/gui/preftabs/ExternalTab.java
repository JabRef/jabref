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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileTypeEditor;
import net.sf.jabref.external.push.PushToApplication;
import net.sf.jabref.external.push.PushToApplicationButton;
import net.sf.jabref.external.push.PushToApplications;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;

class ExternalTab extends JPanel implements PrefsTab {
    private final JabRefPreferences prefs;

    private final JabRefFrame frame;

    private final JTextField emailSubject;
    private final JTextField citeCommand;
    private final JCheckBox openFoldersOfAttachedFiles;

    public ExternalTab(JabRefFrame frame, PreferencesDialog prefsDiag, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        JButton editFileTypes = new JButton(Localization.lang("Manage external file types"));
        citeCommand = new JTextField(25);
        editFileTypes.addActionListener(ExternalFileTypeEditor.getAction(prefsDiag));

        FormLayout layout = new FormLayout(
                "1dlu, 8dlu, left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("Sending of emails"));
        builder.append(new JPanel());
        JLabel lab = new JLabel(Localization.lang("Subject for sending an email with references").concat(":"));
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
        for(PushToApplication pushToApplication : PushToApplications.getApplications()) {
            addSettingsButton(pushToApplication, butpan);
        }
        builder.append(new JPanel());
        builder.append(butpan, 3);

        builder.nextLine();
        lab = new JLabel(Localization.lang("Cite command") + ':');
        JPanel pan = new JPanel();
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
        button.addActionListener(e -> PushToApplicationButton.showSettingsDialog(frame, pt, pt.getSettingsPanel()));
        p.add(button);
    }

    @Override
    public void setValues() {

        emailSubject.setText(prefs.get(JabRefPreferences.EMAIL_SUBJECT));
        openFoldersOfAttachedFiles.setSelected(prefs.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        citeCommand.setText(prefs.get(JabRefPreferences.CITE_COMMAND));
    }

    @Override
    public void storeSettings() {
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
