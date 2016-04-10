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
package net.sf.jabref.gui;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ConfirmCloseFileListEntryEditor;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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

    private static final Log LOGGER = LogFactory.getLog(FileListEntryEditor.class);

    private JDialog diag;
    private final JTextField link = new JTextField();
    private final JTextField description = new JTextField();
    private final JButton ok = new JButton(Localization.lang("OK"));

    private final JComboBox<ExternalFileType> types;
    private final JProgressBar prog = new JProgressBar(SwingConstants.HORIZONTAL);
    private final JLabel downloadLabel = new JLabel(Localization.lang("Downloading..."));
    private ConfirmCloseFileListEntryEditor externalConfirm;

    private FileListEntry entry;
    private final BibDatabaseContext databaseContext;
    private boolean okPressed;
    private boolean okDisabledExternally;
    private boolean openBrowseWhenShown;
    private boolean dontOpenBrowseUntilDisposed;

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");


    public FileListEntryEditor(JabRefFrame frame, FileListEntry entry, boolean showProgressBar,
            boolean showOpenButton, BibDatabaseContext databaseContext) {
        this.entry = entry;
        this.databaseContext = databaseContext;

        ActionListener okAction = e -> {
            // If OK button is disabled, ignore this event:
            if (!ok.isEnabled()) {
                return;
            }
            // If necessary, ask the external confirm object whether we are ready to close.
            if (externalConfirm != null) {
                // Construct an updated FileListEntry:
                storeSettings(entry);
                if (!externalConfirm.confirmClose(entry)) {
                    return;
                }
            }
            diag.dispose();
            storeSettings(FileListEntryEditor.this.entry);
            okPressed = true;
        };
        types = new JComboBox<>();
        types.addItemListener(itemEvent -> {
            if (!okDisabledExternally) {
                ok.setEnabled(types.getSelectedItem() != null);
            }
        });

        FormBuilder builder = FormBuilder.create().layout(new FormLayout
                ("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref, 4dlu, fill:pref", "p, 2dlu, p, 2dlu, p"));
        builder.add(Localization.lang("Link")).xy(1, 1);
        builder.add(link).xy(3, 1);
        final BrowseListener browse = new BrowseListener(frame, link);
        final JButton browseBut = new JButton(Localization.lang("Browse"));
        browseBut.addActionListener(browse);
        builder.add(browseBut).xy(5, 1);
        JButton open = new JButton(Localization.lang("Open"));
        if (showOpenButton) {
            builder.add(open).xy(7, 1);
        }
        builder.add(Localization.lang("Description")).xy(1, 3);
        builder.add(description).xyw(3,3,3);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.add(Localization.lang("File type")).xy(1, 5);
        builder.add(types).xyw(3, 5, 3);
        if (showProgressBar) {
            builder.appendRows("2dlu, p");
            builder.add(downloadLabel).xy(1,7);
            builder.add(prog).xyw(3,7,3);
        }

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addRelatedGap();
        bb.addButton(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ok.addActionListener(okAction);
        // Add OK action to the two text fields to simplify entering:
        link.addActionListener(okAction);
        description.addActionListener(okAction);

        open.addActionListener(e -> openFile());

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
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        link.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                // Do nothing
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                checkExtension();
            }

        });

        diag = new JDialog(frame, Localization.lang("Save file"), true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(frame);
        diag.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent event) {
                if (openBrowseWhenShown && !dontOpenBrowseUntilDisposed) {
                    dontOpenBrowseUntilDisposed = true;
                    SwingUtilities.invokeLater(() -> browse.actionPerformed(new ActionEvent(browseBut, 0, "")));
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
        if ((types.getSelectedIndex() == -1) && (!link.getText().trim().isEmpty())) {

            // Check if this looks like a remote link:
            if (FileListEntryEditor.REMOTE_LINK_PATTERN.matcher(link.getText()).matches()) {
                Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
                if (type.isPresent()) {
                    types.setSelectedItem(type.get());
                    return;
                }
            }

            // Try to guess the file type:
            String theLink = link.getText().trim();
            ExternalFileTypes.getInstance().getExternalFileTypeForName(theLink).ifPresent(types::setSelectedItem);
        }
    }

    private void openFile() {
        ExternalFileType type = (ExternalFileType) types.getSelectedItem();
        if (type != null) {
            try {
                JabRefDesktop.openExternalFileAnyFormat(databaseContext, link.getText(), Optional.of(type));
            } catch (IOException e) {
                LOGGER.error("File could not be opened", e);
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
        description.setText(entry.description);
        link.setText(entry.link);
        //if (link.getText().length() > 0)
        //    checkExtension();
        Collection<ExternalFileType> list = ExternalFileTypes.getInstance().getExternalFileTypeSelection();

        types.setModel(new DefaultComboBoxModel<>(list.toArray(new ExternalFileType[list.size()])));
        types.setSelectedIndex(-1);
        // See what is a reasonable selection for the type combobox:
        if ((entry.type.isPresent()) && !(entry.type.get() instanceof UnknownExternalFileType)) {
            types.setSelectedItem(entry.type.get());
        } else if ((entry.link != null) && (!entry.link.isEmpty())) {
            checkExtension();
        }
    }

    private void storeSettings(FileListEntry entry) {
        String descriptionText = this.description.getText().trim();
        String link = "";
        // See if we should trim the file link to be relative to the file directory:
        try {
            List<String> dirs = databaseContext.getFileDirectory();
            if (dirs.isEmpty()) {
                link = this.link.getText().trim();
            } else {
                boolean found = false;
                for (String dir : dirs) {
                    String canPath = (new File(dir)).getCanonicalPath();
                    File fl = new File(this.link.getText().trim());
                    if (fl.isAbsolute()) {
                        String flPath = fl.getCanonicalPath();
                        if ((flPath.length() > canPath.length()) && (flPath.startsWith(canPath))) {
                            String relFileName = fl.getCanonicalPath().substring(canPath.length() + 1);
                            link = relFileName;
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    link = this.link.getText().trim();
                }
            }
        } catch (IOException ex) {
            // Don't think this should happen, but set the file link directly as a fallback:
            link = this.link.getText().trim();
        }

        ExternalFileType type = (ExternalFileType) types.getSelectedItem();

        entry.description = descriptionText;
        entry.type = Optional.ofNullable(type);
        entry.link = link;
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
                List<String> dirsS = databaseContext.getFileDirectory();
                newFile = FileUtil.shortenFileName(newFile, dirsS);

                comp.setText(newFile.getPath());
                comp.requestFocus();
            }
        }
    }

}
