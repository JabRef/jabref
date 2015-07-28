/*  Copyright (C) 2003-2012 JabRef contributors.
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
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.Logger;

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
import javax.swing.event.UndoableEditListener;

import net.sf.jabref.*;
import net.sf.jabref.external.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.util.StringUtil;
import net.sf.jabref.util.Util;

/**
 * Created by Morten O. Alver 2007.02.22
 */
public class FileListEditor extends JTable implements FieldEditor,
        DownloadExternalFile.DownloadCallback {

    private static final Logger logger = Logger.getLogger(FileListEditor.class.getName());

    private final FieldNameLabel label;
    private FileListEntryEditor editor = null;
    private final JabRefFrame frame;
    private final MetaData metaData;
    private final String fieldName;
    private final EntryEditor entryEditor;
    private final JPanel panel;
    private final FileListTableModel tableModel;
    private final JButton auto;
    private final JPopupMenu menu = new JPopupMenu();


    public FileListEditor(JabRefFrame frame, MetaData metaData, String fieldName, String content,
            EntryEditor entryEditor) {
        this.frame = frame;
        this.metaData = metaData;
        this.fieldName = fieldName;
        this.entryEditor = entryEditor;
        label = new FieldNameLabel(" " + StringUtil.nCase(fieldName) + " ");
        tableModel = new FileListTableModel();
        setText(content);
        setModel(tableModel);
        JScrollPane sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());

        JButton add = new JButton(GUIGlobals.getImage("add"));
        add.setToolTipText(Globals.lang("New file link (INSERT)"));
        JButton remove = new JButton(GUIGlobals.getImage("remove"));
        remove.setToolTipText(Globals.lang("Remove file link (DELETE)"));
        JButton up = new JButton(GUIGlobals.getImage("up"));

        JButton down = new JButton(GUIGlobals.getImage("down"));
        auto = new JButton(Globals.lang("Auto"));
        JButton download = new JButton(Globals.lang("Download"));
        add.setMargin(new Insets(0, 0, 0, 0));
        remove.setMargin(new Insets(0, 0, 0, 0));
        up.setMargin(new Insets(0, 0, 0, 0));
        down.setMargin(new Insets(0, 0, 0, 0));
        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addEntry();
            }
        });
        remove.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeEntries();
            }
        });
        up.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveEntry(-1);
            }
        });
        down.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveEntry(1);
            }
        });
        auto.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                autoSetLinks();
            }
        });
        download.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                downloadFile();
            }
        });
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout
                ("fill:pref,1dlu,fill:pref,1dlu,fill:pref", "fill:pref,fill:pref"));
        builder.append(up);
        builder.append(add);
        builder.append(auto);
        builder.append(down);
        builder.append(remove);
        builder.append(download);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sPane, BorderLayout.CENTER);
        panel.add(builder.getPanel(), BorderLayout.EAST);

        TransferHandler th = new FileListEditorTransferHandler(frame, entryEditor, null);
        setTransferHandler(th);
        panel.setTransferHandler(th);

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
        getInputMap().put(Globals.prefs.getKey("File list editor, move entry up"), "move up");
        getActionMap().put("move up", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(-1);
            }
        });

        // Add input/action pair for moving an entry down:
        getInputMap().put(Globals.prefs.getKey("File list editor, move entry down"), "move down");
        getActionMap().put("move down", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveEntry(1);
            }
        });

        JMenuItem openLink = new JMenuItem(Globals.lang("Open"));
        menu.add(openLink);
        openLink.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openSelectedFile();
            }
        });

        JMenuItem openFolder = new JMenuItem(Globals.lang("Open folder"));
        menu.add(openFolder);
        openFolder.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int row = getSelectedRow();
                if (row >= 0) {
                    FileListEntry entry = tableModel.getEntry(row);
                    try {
                        Util.openFolderAndSelectFile(entry.getLink());
                    } catch (IOException ex) {
                        FileListEditor.logger.fine(ex.getMessage());
                    }
                }
            }
        });

        JMenuItem rename = new JMenuItem(Globals.lang("Move/Rename file"));
        menu.add(rename);
        rename.addActionListener(new MoveFileAction(frame, entryEditor, this, false));

        JMenuItem moveToFileDir = new JMenuItem(Globals.lang("Move to file directory"));
        menu.add(moveToFileDir);
        moveToFileDir.addActionListener(new MoveFileAction(frame, entryEditor, this, true));
    }

    private void openSelectedFile() {
        int row = getSelectedRow();
        if (row >= 0) {
            FileListEntry entry = tableModel.getEntry(row);
            try {
                ExternalFileType type = Globals.prefs.getExternalFileTypeByName(entry.getType().getName());
                Util.openExternalFileAnyFormat(metaData, entry.getLink(),
                        type != null ? type : entry.getType());
            } catch (IOException e) {
                e.printStackTrace();
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
    public void setLabelColor(Color c) {
        label.setForeground(c);
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

    }

    @Override
    public void updateFont() {

    }

    @Override
    public void paste(String textToInsert) {

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
        FileListEntry entry = new FileListEntry("", initialLink, null);
        if (editListEntry(entry, true)) {
            tableModel.addEntry(row, entry);
        }
        entryEditor.updateField(this);
    }

    private void addEntry() {
        addEntry("");
    }

    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null) {
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.removeEntry(rows[i]);
            }
        }
        entryEditor.updateField(this);
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
    }

    /**
     * Open an editor for this entry.
     * @param entry The entry to edit.
     * @param openBrowse True to indicate that a Browse dialog should be immediately opened.
     * @return true if the edit was accepted, false if it was cancelled.
     */
    private boolean editListEntry(FileListEntry entry, boolean openBrowse) {
        if (editor == null) {
            editor = new FileListEntryEditor(frame, entry, false, true, metaData);
        } else {
            editor.setEntry(entry);
        }
        editor.setVisible(true, openBrowse);
        if (editor.okPressed()) {
            tableModel.fireTableDataChanged();
        }
        entryEditor.updateField(this);
        return editor.okPressed();
    }

    public void autoSetLinks() {
        auto.setEnabled(false);
        BibtexEntry entry = entryEditor.getEntry();
        JDialog diag = new JDialog(frame, true);
        JabRefExecutorService.INSTANCE.execute(Util.autoSetLinks(entry, tableModel, metaData, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                auto.setEnabled(true);
                if (e.getID() > 0) {
                    entryEditor.updateField(FileListEditor.this);
                    frame.output(Globals.lang("Finished autosetting external links."));
                } else {
                    frame.output(Globals.lang("Finished autosetting external links.")
                            + " " + Globals.lang("No files found."));
                }
            }
        }, diag));

    }

    /**
     * Run a file download operation.
     */
    private void downloadFile() {
        String bibtexKey = entryEditor.getEntry().getCiteKey();
        if (bibtexKey == null) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    Globals.lang("This entry has no BibTeX key. Generate key now?"),
                    Globals.lang("Download file"), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.OK_OPTION) {
                ActionListener l = entryEditor.generateKeyAction;
                l.actionPerformed(null);
                bibtexKey = entryEditor.getEntry().getCiteKey();
            }
        }
        DownloadExternalFile def = new DownloadExternalFile(frame,
                frame.basePanel().metaData(), bibtexKey);
        try {
            def.download(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This is the callback method that the DownloadExternalFile class uses to report the result
     * of a download operation. This call may never come, if the user cancelled the operation.
     * @param file The FileListEntry linking to the resulting local file.
     */
    @Override
    public void downloadComplete(FileListEntry file) {
        tableModel.addEntry(tableModel.getRowCount(), file);
        entryEditor.updateField(this);
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
            }
            else if (e.isPopupTrigger()) {
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
    public boolean hasUndoInformation() {
        return false;
    }

    @Override
    public void undo() {
    }

    @Override
    public boolean hasRedoInformation() {
        return false;
    }

    @Override
    public void redo() {
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
    }

    @Override
    public void setAutoCompleteListener(AutoCompleteListener listener) {
    }

    @Override
    public void clearAutoCompleteSuggestion() {
    }

    @Override
    public void setActiveBackgroundColor() {
    }

    @Override
    public void setValidBackgroundColor() {
    }

    @Override
    public void setInvalidBackgroundColor() {
    }

    @Override
    public void updateFontColor() {
    }
}
