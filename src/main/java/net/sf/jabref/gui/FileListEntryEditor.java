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
package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.util.io.JabRefDesktop;
import net.sf.jabref.util.Util;
import net.sf.jabref.external.ConfirmCloseFileListEntryEditor;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class produces a dialog box for editing a single file link from a Bibtex entry.
 *
 * The information to be edited includes the file description, the link itself and the
 * file type. The dialog also includes convenience buttons for quick linking.
 *
 * For use when downloading files, this class also offers a progress bar and a "Downloading..."
 * label that can be hidden when the download is complete.
 */
public class FileListEntryEditor {

    private JDialog diag;
    private final JTextField link = new JTextField();
    private final JTextField description = new JTextField();
    private final JButton ok = new JButton(Localization.lang("Ok"));

    private final JComboBox types;
    private final JProgressBar prog = new JProgressBar(SwingConstants.HORIZONTAL);
    private final JLabel downloadLabel = new JLabel(Localization.lang("Downloading..."));
    private ConfirmCloseFileListEntryEditor externalConfirm;

    private FileListEntry entry;
    private final MetaData metaData;
    private boolean okPressed;
    private boolean okDisabledExternally;
    private boolean openBrowseWhenShown;
    private boolean dontOpenBrowseUntilDisposed;

    private static final Pattern remoteLinkPattern = Pattern.compile("[a-z]+://.*");


    public FileListEntryEditor(JabRefFrame frame, FileListEntry entry, boolean showProgressBar,
            boolean showOpenButton, MetaData metaData) {
        this.entry = entry;
        this.metaData = metaData;

        AbstractAction okAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // If OK button is disabled, ignore this event:
                if (!ok.isEnabled()) {
                    return;
                }
                // If necessary, ask the external confirm object whether we are ready to close.
                if (externalConfirm != null) {
                    // Construct an updated FileListEntry:
                    FileListEntry testEntry = new FileListEntry("", "", null);
                    storeSettings(testEntry);
                    if (!externalConfirm.confirmClose(testEntry)) {
                        return;
                    }
                }
                diag.dispose();
                storeSettings(FileListEntryEditor.this.entry);
                okPressed = true;
            }
        };
        types = new JComboBox();
        types.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (!okDisabledExternally) {
                    ok.setEnabled(types.getSelectedItem() != null);
                }
            }
        });

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref, 4dlu, fill:pref", ""));
        builder.append(Localization.lang("Link"));
        builder.append(link);
        final BrowseListener browse = new BrowseListener(frame, link);
        final JButton browseBut = new JButton(Localization.lang("Browse"));
        browseBut.addActionListener(browse);
        builder.append(browseBut);
        JButton open = new JButton(Localization.lang("Open"));
        if (showOpenButton) {
            builder.append(open);
        }
        builder.nextLine();
        builder.append(Localization.lang("Description"));
        builder.append(description, 3);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.nextLine();
        builder.append(Localization.lang("File type"));
        builder.append(types, 3);
        if (showProgressBar) {
            builder.nextLine();
            builder.append(downloadLabel);
            builder.append(prog, 3);
        }

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        //bb.addButton(open);
        //bb.addRelatedGap();
        bb.addRelatedGap();
        bb.addButton(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addGlue();

        ok.addActionListener(okAction);
        // Add OK action to the two text fields to simplify entering:
        link.addActionListener(okAction);
        description.addActionListener(okAction);

        open.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openFile();
            }
        });

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        link.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }

        });

        diag = new JDialog(frame, Localization.lang("Edit file link"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        Util.placeDialog(diag, frame);
        diag.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent event) {
                if (openBrowseWhenShown && !dontOpenBrowseUntilDisposed) {
                    dontOpenBrowseUntilDisposed = true;
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            browse.actionPerformed(new ActionEvent(browseBut, 0, ""));
                        }
                    });
                }
            }

            @Override
            public void windowClosed(WindowEvent event) {
                dontOpenBrowseUntilDisposed = false;
            }
        });
        setValues(entry);
    }

    private void checkExtension() {
        if ((types.getSelectedIndex() == -1) &&
                (!link.getText().trim().isEmpty())) {

            // Check if this looks like a remote link:
            if (FileListEntryEditor.remoteLinkPattern.matcher(link.getText()).matches()) {
                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("html");
                if (type != null) {
                    types.setSelectedItem(type);
                    return;
                }
            }

            // Try to guess the file type:
            String theLink = link.getText().trim();
            ExternalFileType type = Globals.prefs.getExternalFileTypeForName(theLink);
            if (type != null) {
                types.setSelectedItem(type);
            }
        }
    }

    private void openFile() {
        ExternalFileType type = (ExternalFileType) types.getSelectedItem();
        if (type != null) {
            try {
                JabRefDesktop.openExternalFileAnyFormat(metaData, link.getText(), type);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setExternalConfirm(ConfirmCloseFileListEntryEditor eC) {
        this.externalConfirm = eC;
    }

    public void setOkEnabled(boolean enabled) {
        okDisabledExternally = !enabled;
        ok.setEnabled(enabled);
    }

    public JProgressBar getProgressBar() {
        return prog;
    }

    public JLabel getProgressBarLabel() {
        return downloadLabel;
    }

    public void setEntry(FileListEntry entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible, boolean openBrowse) {
        openBrowseWhenShown = openBrowse && Globals.prefs.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE);
        if (visible) {
            okPressed = false;
        }
        diag.setVisible(visible);
    }

    public boolean isVisible() {
        return (diag != null) && diag.isVisible();
    }

    private void setValues(FileListEntry entry) {
        description.setText(entry.getDescription());
        link.setText(entry.getLink());
        //if (link.getText().length() > 0)
        //    checkExtension();
        types.setModel(new DefaultComboBoxModel(Globals.prefs.getExternalFileTypeSelection()));
        types.setSelectedIndex(-1);
        // See what is a reasonable selection for the type combobox:
        if ((entry.getType() != null) && !(entry.getType() instanceof UnknownExternalFileType)) {
            types.setSelectedItem(entry.getType());
        } else if ((entry.getLink() != null) && (!entry.getLink().isEmpty())) {
            checkExtension();
        }

    }

    private void storeSettings(FileListEntry entry) {
        entry.setDescription(description.getText().trim());
        // See if we should trim the file link to be relative to the file directory:
        try {
            String[] dirs = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
            if (dirs.length == 0) {
                entry.setLink(link.getText().trim());
            } else {
                boolean found = false;
                for (String dir : dirs) {
                    String canPath = (new File(dir)).getCanonicalPath();
                    File fl = new File(link.getText().trim());
                    if (fl.isAbsolute()) {
                        String flPath = fl.getCanonicalPath();
                        if ((flPath.length() > canPath.length()) && (flPath.startsWith(canPath))) {
                            String relFileName = fl.getCanonicalPath().substring(canPath.length() + 1);
                            entry.setLink(relFileName);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    entry.setLink(link.getText().trim());
                }
            }
        } catch (java.io.IOException ex)
        {
            ex.printStackTrace();
            // Don't think this should happen, but set the file link directly as a fallback:
            entry.setLink(link.getText().trim());
        }

        entry.setType((ExternalFileType) types.getSelectedItem());
    }

    public boolean okPressed() {
        return okPressed;
    }


    class BrowseListener implements ActionListener {

        private final JFrame parent;
        private final JTextField comp;


        public BrowseListener(JFrame parent, JTextField comp) {
            this.parent = parent;
            this.comp = comp;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File initial = new File(comp.getText().trim());
            if (comp.getText().trim().isEmpty()) {
                // Nothing in the field. Go to the last file dir used:
                initial = new File(Globals.prefs.get(JabRefPreferences.FILE_WORKING_DIRECTORY));
            }
            String chosen = FileDialogs.getNewFile(parent, initial, Globals.NONE,
                    JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                // Store the directory for next time:
                Globals.prefs.put(JabRefPreferences.FILE_WORKING_DIRECTORY, newFile.getParent());

                // If the file is below the file directory, make the path relative:
                String[] dirsS = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
                newFile = FileUtil.shortenFileName(newFile, dirsS);

                comp.setText(newFile.getPath());
                comp.requestFocus();
            }
        }
    }

}
