package org.jabref.gui.filelist;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private static final Logger LOGGER = LoggerFactory.getLogger(FileListEntryEditor.class);
    private final JTextField link = new JTextField();
    private final JTextField description = new JTextField();
    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JComboBox<ExternalFileType> types;
    private final JProgressBar prog = new JProgressBar(SwingConstants.HORIZONTAL);
    private final JLabel downloadLabel = new JLabel(Localization.lang("Downloading..."));
    private JDialog diag;
    //Do not make this variable final, as then the lambda action listener will fail on compile
    private JabRefFrame frame;
    private boolean showSaveDialog;
    private ConfirmCloseFileListEntryEditor externalConfirm;
    private LinkedFile entry;
    //Do not make this variable final, as then the lambda action listener will fail on compiöe
    private BibDatabaseContext databaseContext;
    private final ActionListener browsePressed = e -> {
        String fileText = link.getText().trim();
        Optional<Path> file = FileHelper.expandFilename(this.databaseContext, fileText,
                Globals.prefs.getFileDirectoryPreferences());

        Path workingDir = file.orElse(Paths.get(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)));
        String fileName = Paths.get(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDir)
                .withInitialFileName(fileName).build();
        DialogService ds = new FXDialogService();

        Optional<Path> path;
        if (showSaveDialog) {
            path = DefaultTaskExecutor.runInJavaFXThread(() -> ds.showFileSaveDialog(fileDialogConfiguration));
        } else {
            path = DefaultTaskExecutor.runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));
        }

        path.ifPresent(newFile -> {
            // Store the directory for next time:
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, newFile.toString());

            // If the file is below the file directory, make the path relative:
            List<Path> fileDirectories = this.databaseContext
                    .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
            newFile = FileUtil.shortenFileName(newFile, fileDirectories);

            link.setText(newFile.toString());
            link.requestFocus();
        });
    };
    private boolean okPressed;

    private boolean okDisabledExternally;
    private boolean openBrowseWhenShown;

    private boolean dontOpenBrowseUntilDisposed;

    public FileListEntryEditor(LinkedFile entry, boolean showProgressBar, boolean showOpenButton, BibDatabaseContext databaseContext, boolean showSaveDialog) {
        this(entry, showProgressBar, showOpenButton, databaseContext);
        this.showSaveDialog = showSaveDialog;
    }

    public FileListEntryEditor(LinkedFile entry, boolean showProgressBar, boolean showOpenButton, BibDatabaseContext databaseContext) {
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

        FormLayout fileDialog = new FormLayout(
                "left:pref, 4dlu, fill:400dlu, 4dlu, fill:pref, 4dlu, fill:pref",
                "p, 8dlu, p, 8dlu, p");
        FormBuilder builder = FormBuilder.create().layout(fileDialog);
        builder.add(Localization.lang("Link")).xy(1, 1);
        builder.add(link).xy(3, 1);

        final JButton browseBut = new JButton(Localization.lang("Browse"));
        browseBut.addActionListener(browsePressed);
        builder.add(browseBut).xy(5, 1);
        JButton open = new JButton(Localization.lang("Open"));
        if (showOpenButton) {
            builder.add(open).xy(7, 1);
        }
        builder.add(Localization.lang("Description")).xy(1, 3);
        builder.add(description).xyw(3, 3, 5);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.add(Localization.lang("File type")).xy(1, 5);
        builder.add(types).xyw(3, 5, 5);
        if (showProgressBar) {
            builder.appendRows("2dlu, p");
            builder.add(downloadLabel).xy(1, 7);
            builder.add(prog).xyw(3, 7, 3);
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

        diag = new JDialog();
        diag.setTitle(Localization.lang("Select files"));
        diag.setModal(true);
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent event) {
                if (openBrowseWhenShown && !dontOpenBrowseUntilDisposed) {
                    dontOpenBrowseUntilDisposed = true;
                    SwingUtilities.invokeLater(() -> browsePressed.actionPerformed(new ActionEvent(browseBut, 0, "")));
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

    public void setEntry(LinkedFile entry) {
        this.entry = entry;
        setValues(entry);
    }

    public void setVisible(boolean visible, boolean openBrowse) {
        openBrowseWhenShown = openBrowse && Globals.prefs.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE);
        if (visible) {
            okPressed = false;
        }
        String title;
        if (showSaveDialog) {
            title = Localization.lang("Save file");
        } else {
            title = Localization.lang("Select files");
        }
        diag.setTitle(title);
        diag.setVisible(visible);
    }

    public boolean isVisible() {
        return (diag != null) && diag.isVisible();
    }

    private void setValues(LinkedFile entry) {
        description.setText(entry.getDescription());
        link.setText(entry.getLink());

        Collection<ExternalFileType> list = ExternalFileTypes.getInstance().getExternalFileTypeSelection();

        types.setModel(new DefaultComboBoxModel<>(list.toArray(new ExternalFileType[list.size()])));
        types.setSelectedIndex(-1);
        // See what is a reasonable selection for the type combobox:
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().fromLinkedFile(entry, false);
        if (fileType.isPresent() && !(fileType.get() instanceof UnknownExternalFileType)) {
            types.setSelectedItem(fileType.get());
        } else if ((entry.getLink() != null) && (!entry.getLink().isEmpty())) {
            checkExtension();
        }
    }

    private void storeSettings(LinkedFile listEntry) {
        String descriptionText = this.description.getText().trim();
        String fileLink = "";
        // See if we should trim the file link to be relative to the file directory:
        try {
            List<String> dirs = databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
            if (dirs.isEmpty()) {
                fileLink = this.link.getText().trim();
            } else {
                boolean found = false;
                for (String dir : dirs) {
                    String canPath = (new File(dir)).getCanonicalPath();
                    File fl = new File(this.link.getText().trim());
                    if (fl.isAbsolute()) {
                        String flPath = fl.getCanonicalPath();
                        if ((flPath.length() > canPath.length()) && (flPath.startsWith(canPath))) {
                            fileLink = fl.getCanonicalPath().substring(canPath.length() + 1);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    fileLink = this.link.getText().trim();
                }
            }
        } catch (IOException ex) {
            // Don't think this should happen, but set the file link directly as a fallback:
            fileLink = this.link.getText().trim();
        }

        ExternalFileType type = (ExternalFileType) types.getSelectedItem();

        listEntry.setDescription(descriptionText);
        listEntry.setFileType(type.getName());
        listEntry.setLink(fileLink);
    }

    public boolean okPressed() {
        return okPressed;
    }

}
