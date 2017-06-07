package org.jabref.gui.fieldeditors;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.table.TableCellRenderer;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.autocompleter.AutoCompleteListener;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.externalfiles.DownloadExternalFile;
import org.jabref.gui.externalfiles.MoveFileAction;
import org.jabref.gui.externalfiles.RenameFileAction;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileListEditor extends JTable implements FieldEditor, DownloadExternalFile.DownloadCallback {
    private static final Log LOGGER = LogFactory.getLog(FileListEditor.class);

    private final JabRefFrame frame;
    private final BibDatabaseContext databaseContext;
    private final String fieldName;
    private final EntryEditor entryEditor;
    private final JPanel panel;
    private final FileListTableModel tableModel;
    private final JPopupMenu menu = new JPopupMenu();
    private FileListEntryEditor editor;

    public FileListEditor(JabRefFrame frame, BibDatabaseContext databaseContext, String fieldName, String content,
            EntryEditor entryEditor) {
        this.frame = frame;
        this.databaseContext = databaseContext;
        this.fieldName = fieldName;
        this.entryEditor = entryEditor;
        tableModel = new FileListTableModel();
        setText(content);
        setModel(tableModel);
        JScrollPane sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());
        initKeyBindings();

        JButton remove = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        remove.setToolTipText(Localization.lang("Remove file link (DELETE)"));
        JButton up = new JButton(IconTheme.JabRefIcon.UP.getSmallIcon());
        JButton down = new JButton(IconTheme.JabRefIcon.DOWN.getSmallIcon());
        remove.setMargin(new Insets(0, 0, 0, 0));
        up.setMargin(new Insets(0, 0, 0, 0));
        down.setMargin(new Insets(0, 0, 0, 0));
        remove.addActionListener(e -> removeEntries());
        up.addActionListener(e -> moveEntry(-1));
        down.addActionListener(e -> moveEntry(1));

        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("fill:pref,1dlu,fill:pref,1dlu,fill:pref", "fill:pref,fill:pref"));
        builder.add(up).xy(1, 1);
        builder.add(down).xy(1, 2);
        builder.add(remove).xy(3, 2);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sPane, BorderLayout.CENTER);
        panel.add(builder.getPanel(), BorderLayout.EAST);

        TransferHandler transferHandler = new FileListEditorTransferHandler(frame, entryEditor, null);
        setTransferHandler(transferHandler);
        panel.setTransferHandler(transferHandler);

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
                    Path path = null;
                    // absolute path
                    if (Paths.get(entry.getLink()).isAbsolute()) {
                        path = Paths.get(entry.getLink());
                    } else {
                        // relative to file folder
                        for (String folder : databaseContext
                                .getFileDirectories(Globals.prefs.getFileDirectoryPreferences())) {
                            Path file = Paths.get(folder, entry.getLink());
                            if (Files.exists(file)) {
                                path = file;
                                break;
                            }
                        }
                    }
                    if (path != null) {
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

        JMenuItem rename = new JMenuItem(Localization.lang("Rename file"));
        menu.add(rename);
        rename.addActionListener(new RenameFileAction(frame, entryEditor, this));

        JMenuItem moveToFileDir = new JMenuItem(Localization.lang("Move file to file directory"));
        menu.add(moveToFileDir);
        moveToFileDir.addActionListener(new MoveFileAction(frame, entryEditor, this));

        JMenuItem deleteFile = new JMenuItem(Localization.lang("Permanently delete local file"));
        menu.add(deleteFile);
        deleteFile.addActionListener(e -> {
            int row = getSelectedRow();

            // no selection
            if (row == -1) {
                return;
            }

            FileListEntry entry = tableModel.getEntry(row);
            Optional<Path> file = entry.toParsedFileField().findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());
            if (file.isPresent()) {
                String[] options = {Localization.lang("Delete"), Localization.lang("Cancel")};
                int userConfirm = JOptionPane.showOptionDialog(frame,
                        Localization.lang("Delete '%0'?", file.get().getFileName().toString()),
                        Localization.lang("Delete file"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (userConfirm == JOptionPane.YES_OPTION) {
                    try {
                        Files.delete(file.get());
                        removeEntries();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("File permission error"),
                                Localization.lang("Cannot delete file"), JOptionPane.ERROR_MESSAGE);
                        LOGGER.warn("File permission error while deleting: " + entry.getLink(), ex);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(frame, Localization.lang("File not found"),
                        Localization.lang("Cannot delete file"), JOptionPane.ERROR_MESSAGE);
            }
        });
        adjustColumnWidth();
    }

    private void initKeyBindings() {
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

        getInputMap().put(KeyStroke.getKeyStroke("F4"),"open file");
        getActionMap().put("open file", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openSelectedFile();
            }
        });
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
                        .getExternalFileTypeByName(entry.getType().get().getName());
                JabRefDesktop.openExternalFileAnyFormat(databaseContext, entry.getLink(),
                        type.isPresent() ? type : entry.getType());
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
    public void paste(String textToInsert) {
        // Do nothing
    }

    @Override
    public String getSelectedText() {
        return null;
    }

    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null) {
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.removeEntry(rows[i]);
            }
        }
        //entryEditor.updateField(this);
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
        //entryEditor.updateField(this);
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
        //entryEditor.updateField(this);
        adjustColumnWidth();
        return editor.okPressed();
    }

    /**
     * This is the callback method that the DownloadExternalFile class uses to report the result
     * of a download operation. This call may never come, if the user canceled the operation.
     *
     * @param file The FileListEntry linking to the resulting local file.
     */
    @Override
    public void downloadComplete(FileListEntry file) {
        tableModel.addEntry(0, file);
        //entryEditor.updateField(this);
        adjustColumnWidth();
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
}
