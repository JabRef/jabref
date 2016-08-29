package net.sf.jabref.gui.fieldeditors;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.table.TableCellRenderer;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.external.AutoSetLinks;
import net.sf.jabref.external.DownloadExternalFile;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.MoveFileAction;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.autocompleter.AutoCompleteListener;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.GUIUtil;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileListEditor extends JTable implements FieldEditor, DownloadExternalFile.DownloadCallback {
    private static final Log LOGGER = LogFactory.getLog(FileListEditor.class);

    private final FieldNameLabel label;
    private FileListEntryEditor editor;
    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final String fieldName;
    private final EntryEditor entryEditor;
    private final JPanel panel;
    private final FileListTableModel tableModel;
    private final JButton auto;
    private final JPopupMenu menu = new JPopupMenu();

    public FileListEditor(JabRefFrame frame, BibDatabaseContext databaseContext, String fieldName, String content,
                          EntryEditor entryEditor) {
        this.frame = frame;
        this.databaseContext = databaseContext;
        this.fieldName = fieldName;
        this.entryEditor = entryEditor;
        label = new FieldNameLabel(fieldName);
        tableModel = new FileListTableModel();
        setText(content);
        setModel(tableModel);
        JScrollPane sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());

        GUIUtil.correctRowHeight(this);

        JButton add = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
        add.setToolTipText(Localization.lang("New file link (INSERT)"));
        JButton remove = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        remove.setToolTipText(Localization.lang("Remove file link (DELETE)"));
        JButton up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());

        JButton down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
        auto = new JButton(Localization.lang("Get fulltext"));
        JButton download = new JButton(Localization.lang("Download from URL"));
        add.setMargin(new Insets(0, 0, 0, 0));
        remove.setMargin(new Insets(0, 0, 0, 0));
        up.setMargin(new Insets(0, 0, 0, 0));
        down.setMargin(new Insets(0, 0, 0, 0));
        add.addActionListener(e -> addEntry());
        remove.addActionListener(e -> removeEntries());
        up.addActionListener(e -> moveEntry(-1));
        down.addActionListener(e -> moveEntry(1));
        auto.addActionListener(e -> autoSetLinks());
        download.addActionListener(e -> downloadFile());

        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout
                ("fill:pref,1dlu,fill:pref,1dlu,fill:pref", "fill:pref,fill:pref"));
        builder.add(up).xy(1, 1);
        builder.add(add).xy(3, 1);
        builder.add(auto).xy(5, 1);
        builder.add(down).xy(1, 2);
        builder.add(remove).xy(3, 2);
        builder.add(download).xy(5, 2);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sPane, BorderLayout.CENTER);
        panel.add(builder.getPanel(), BorderLayout.EAST);

        TransferHandler transferHandler = new FileListEditorTransferHandler(frame, entryEditor, null);
        setTransferHandler(transferHandler);
        panel.setTransferHandler(transferHandler);

        // Add an input/action pair for deleting entries:
        getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getActionMap().put("delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int row = getSelectedRow();
                removeEntries();
                row = Math.min(row, getRowCount() - 1);
                if (row >= 0) {
                    setRowSelectionInterval(row, row);
                }
            }
        });

        // Add an input/action pair for inserting an entry:
        getInputMap().put(KeyStroke.getKeyStroke("INSERT"), "insert");
        getActionMap().put("insert", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addEntry();
            }
        });

        // Add input/action pair for moving an entry up:
        getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.FILE_LIST_EDITOR_MOVE_ENTRY_UP), "move up");
        getActionMap().put("move up", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(-1);
            }
        });

        // Add input/action pair for moving an entry down:
        getInputMap().put(Globals.getKeyPrefs().getKey(KeyBinding.FILE_LIST_EDITOR_MOVE_ENTRY_DOWN), "move down");
        getActionMap().put("move down", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(1);
            }
        });

        JMenuItem openLink = new JMenuItem(Localization.lang("Open"));
        menu.add(openLink);
        openLink.addActionListener(e -> openSelectedFile());

        JMenuItem openFolder = new JMenuItem(Localization.lang("Open folder"));
        menu.add(openFolder);
        openFolder.addActionListener(e -> {
            int row = getSelectedRow();
            if (row >= 0) {
                FileListEntry entry = tableModel.getEntry(row);
                try {
                    String path = "";
                    // absolute path
                    if (Paths.get(entry.link).isAbsolute()) {
                        path = Paths.get(entry.link).toString();
                    } else {
                        // relative to file folder
                        for (String folder : databaseContext
                                .getFileDirectory(Globals.prefs.getFileDirectoryPreferences())) {
                            Path file = Paths.get(folder, entry.link);
                            if (Files.exists(file)) {
                                path = file.toString();
                                break;
                            }
                        }
                    }
                    if (!path.isEmpty()) {
                        JabRefDesktop.openFolderAndSelectFile(path);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                Localization.lang("File not found"),
                                Localization.lang("Error"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    LOGGER.debug("Cannot open folder", ex);
                }
            }
        });

        JMenuItem rename = new JMenuItem(Localization.lang("Move/Rename file"));
        menu.add(rename);
        rename.addActionListener(new MoveFileAction(frame, entryEditor, this, false));

        JMenuItem moveToFileDir = new JMenuItem(Localization.lang("Move file to file directory"));
        menu.add(moveToFileDir);
        moveToFileDir.addActionListener(new MoveFileAction(frame, entryEditor, this, true));

        JMenuItem deleteFile = new JMenuItem(Localization.lang("Delete local file"));
        menu.add(deleteFile);
        deleteFile.addActionListener(e -> {
            int row = getSelectedRow();
            // no selection
            if (row != -1) {

                FileListEntry entry = tableModel.getEntry(row);
                // null if file does not exist
                Optional<File> file = FileUtil.expandFilename(databaseContext, entry.link,
                        Globals.prefs.getFileDirectoryPreferences());

                // transactional delete and unlink
                try {
                    if (file.isPresent()) {
                        Files.delete(file.get().toPath());
                    }
                    removeEntries();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("File permission error"),
                            Localization.lang("Cannot delete file"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.warn("File permission error while deleting: " + entry.link, ex);
                }
            }
        });
        adjustColumnWidth();
    }

    public void adjustColumnWidth() {
        for (int column = 0; column < this.getColumnCount(); column++) {
            int width = 0;
            for (int row = 0; row < this.getRowCount(); row++) {
                TableCellRenderer renderer = this.getCellRenderer(row, column);
                Component comp = this.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            this.columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    private void openSelectedFile() {
        int row = getSelectedRow();
        if (row >= 0) {
            FileListEntry entry = tableModel.getEntry(row);
            try {
                Optional<ExternalFileType> type = ExternalFileTypes.getInstance()
                        .getExternalFileTypeByName(entry.type.get().getName());
                JabRefDesktop.openExternalFileAnyFormat(databaseContext, entry.link, type.isPresent() ? type : entry.type);
            } catch (IOException e) {
                LOGGER.warn("Cannot open selected file.", e);
            }
        }
    }

    public FileListTableModel getTableModel() {
        return tableModel;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    /*
      * Returns the component to be added to a container. Might be a JScrollPane
    * or the component itself.
    */
    @Override
    public JComponent getPane() {
        return panel;
    }

    /*
     * Returns the text component itself.
    */
    @Override
    public JComponent getTextComponent() {
        return this;
    }

    @Override
    public JLabel getLabel() {
        return label;
    }

    @Override
    public void setLabelColor(Color color) {
        label.setForeground(color);
    }

    @Override
    public String getText() {
        return tableModel.getStringRepresentation();
    }

    @Override
    public void setText(String newText) {
        tableModel.setContent(newText);
    }

    @Override
    public void append(String text) {
        // Do nothing
    }

    @Override
    public void updateFont() {
        // Do nothing
    }

    @Override
    public void paste(String textToInsert) {
        // Do nothing
    }

    @Override
    public String getSelectedText() {
        return null;
    }

    private void addEntry(String initialLink) {
        int row = getSelectedRow();
        if (row == -1) {
            row = 0;
        }
        FileListEntry entry = new FileListEntry("", initialLink);
        if (editListEntry(entry, true)) {
            tableModel.addEntry(row, entry);
        }
        entryEditor.updateField(this);
        adjustColumnWidth();
    }

    private void addEntry() {
        List<String> defaultDirectory = databaseContext.getFileDirectory(Globals.prefs.getFileDirectoryPreferences());
        if (defaultDirectory.isEmpty() || (defaultDirectory.get(0) == null)) {
            addEntry("");
        } else {
            addEntry(defaultDirectory.get(0));
        }
    }

    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null) {
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.removeEntry(rows[i]);
            }
        }
        entryEditor.updateField(this);
        adjustColumnWidth();
    }

    private void moveEntry(int i) {
        int[] sel = getSelectedRows();
        if ((sel.length != 1) || (tableModel.getRowCount() < 2)) {
            return;
        }
        int toIdx = sel[0] + i;
        if (toIdx >= tableModel.getRowCount()) {
            toIdx -= tableModel.getRowCount();
        }
        if (toIdx < 0) {
            toIdx += tableModel.getRowCount();
        }
        FileListEntry entry = tableModel.getEntry(sel[0]);
        tableModel.removeEntry(sel[0]);
        tableModel.addEntry(toIdx, entry);
        entryEditor.updateField(this);
        setRowSelectionInterval(toIdx, toIdx);
        adjustColumnWidth();
    }

    /**
     * Open an editor for this entry.
     *
     * @param entry      The entry to edit.
     * @param openBrowse True to indicate that a Browse dialog should be immediately opened.
     * @return true if the edit was accepted, false if it was canceled.
     */
    private boolean editListEntry(FileListEntry entry, boolean openBrowse) {
        if (editor == null) {
            editor = new FileListEntryEditor(frame, entry, false, true, databaseContext);
        } else {
            editor.setEntry(entry);
        }
        editor.setVisible(true, openBrowse);
        if (editor.okPressed()) {
            tableModel.fireTableDataChanged();
        }
        entryEditor.updateField(this);
        adjustColumnWidth();
        return editor.okPressed();
    }

    public void autoSetLinks() {
        auto.setEnabled(false);

        List<BibEntry> entries = new ArrayList<>(frame.getCurrentBasePanel().getSelectedEntries());

        // filesystem lookup
        JDialog dialog = new JDialog(frame, true);
        JabRefExecutorService.INSTANCE
                .execute(AutoSetLinks.autoSetLinks(entries, null, null, tableModel, databaseContext, e -> {
                    auto.setEnabled(true);

                    if (e.getID() > 0) {
                        entryEditor.updateField(this);
                        adjustColumnWidth();
                        frame.output(Localization.lang("Finished automatically setting external links."));
                    } else {
                        frame.output(Localization.lang("Finished automatically setting external links.") + " "
                                + Localization.lang("No files found."));

                        // auto download file as no file found before
                        frame.getCurrentBasePanel().runCommand(Actions.DOWNLOAD_FULL_TEXT);
                    }
                    // reset
                    auto.setEnabled(true);
                } , dialog));
    }

    /**
     * Run a file download operation.
     */
    private void downloadFile() {
        Optional<String> bibtexKey = entryEditor.getEntry().getCiteKeyOptional();
        if (!bibtexKey.isPresent()) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    Localization.lang("This entry has no BibTeX key. Generate key now?"),
                    Localization.lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                ActionListener l = entryEditor.getGenerateKeyAction();
                l.actionPerformed(null);
                bibtexKey = entryEditor.getEntry().getCiteKeyOptional();
            }
        }
        DownloadExternalFile def = new DownloadExternalFile(frame,
                frame.getCurrentBasePanel().getBibDatabaseContext(), entryEditor.getEntry());
        try {
            def.download(this);
        } catch (IOException ex) {
            LOGGER.warn("Cannot download.", ex);
        }
    }

    /**
     * This is the callback method that the DownloadExternalFile class uses to report the result
     * of a download operation. This call may never come, if the user canceled the operation.
     *
     * @param file The FileListEntry linking to the resulting local file.
     */
    @Override
    public void downloadComplete(FileListEntry file) {
        tableModel.addEntry(tableModel.getRowCount(), file);
        entryEditor.updateField(this);
        adjustColumnWidth();
    }


    class TableClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0) {
                    FileListEntry entry = tableModel.getEntry(row);
                    editListEntry(entry, false);
                }
            } else if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                processPopupTrigger(e);
            }
        }

        private void processPopupTrigger(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row >= 0) {
                setRowSelectionInterval(row, row);
                menu.show(FileListEditor.this, e.getX(), e.getY());
            }
        }
    }

    @Override
    public void undo() {
        // Do nothing
    }

    @Override
    public void redo() {
        // Do nothing
    }

    @Override
    public void setAutoCompleteListener(AutoCompleteListener listener) {
        // Do nothing
    }

    @Override
    public void clearAutoCompleteSuggestion() {
        // Do nothing
    }

    @Override
    public void setActiveBackgroundColor() {
        // Do nothing
    }

    @Override
    public void setValidBackgroundColor() {
        // Do nothing
    }

    @Override
    public void setInvalidBackgroundColor() {
        // Do nothing
    }

    @Override
    public void updateFontColor() {
        // Do nothing
    }
}
