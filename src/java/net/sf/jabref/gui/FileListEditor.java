package net.sf.jabref.gui;

import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.DownloadExternalFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.beans.PropertyChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by Morten O. Alver 2007.02.22
 */
public class FileListEditor extends JTable implements FieldEditor,
        DownloadExternalFile.DownloadCallback {

    FieldNameLabel label;
    FileListEntryEditor editor = null;
    private JabRefFrame frame;
    private String fieldName;
    private EntryEditor entryEditor;
    private JPanel panel;
    private FileListTableModel tableModel;
    private JScrollPane sPane;
    private JButton add, remove, up, down, auto, download;

    public FileListEditor(JabRefFrame frame, String fieldName, String content,
                          EntryEditor entryEditor) {
        this.frame = frame;
        this.fieldName = fieldName;
        this.entryEditor = entryEditor;
        label = new FieldNameLabel(" " + Util.nCase(fieldName) + " ");
        tableModel = new FileListTableModel();
        setText(content);
        setModel(tableModel);
        sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());

        add = new JButton(GUIGlobals.getImage("add"));
        remove = new JButton(GUIGlobals.getImage("remove"));
        up = new JButton(GUIGlobals.getImage("up"));
        down = new JButton(GUIGlobals.getImage("down"));
        auto = new JButton(Globals.lang("Auto"));
        download = new JButton(Globals.lang("Download"));
        add.setMargin(new Insets(0,0,0,0));
        remove.setMargin(new Insets(0,0,0,0));
        up.setMargin(new Insets(0,0,0,0));
        down.setMargin(new Insets(0,0,0,0));
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addEntry();
            }
        });
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeEntries();
            }
        });
        up.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveEntry(-1);
            }
        });
        down.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                moveEntry(1);
            }
        });
        auto.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSetLinks();
            }
        });
        download.addActionListener(new ActionListener() {
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

        // Add an input/action pair for deleting entries:
        getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
        getActionMap().put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent actionEvent) {
                int row = getSelectedRow();
                removeEntries();
                row = Math.min(row, getRowCount()-1);
                if (row >= 0)
                    setRowSelectionInterval(row, row);
            }
        });

        // Add an input/action pair for inserting an entry:
        getInputMap().put(KeyStroke.getKeyStroke("INSERT"), "insert");
        getActionMap().put("insert", new AbstractAction() {

            public void actionPerformed(ActionEvent actionEvent) {
                addEntry();
            }
        });
    }



    public String getFieldName() {
        return fieldName;
    }

    /*
      * Returns the component to be added to a container. Might be a JScrollPane
    * or the component itself.
    */
    public JComponent getPane() {
        return panel;
    }

    /*
     * Returns the text component itself.
    */
    public JComponent getTextComponent() {
        return this;
    }

    public JLabel getLabel() {
        return label;
    }

    public void setLabelColor(Color c) {
        label.setForeground(c);
    }

    public String getText() {
        return tableModel.getStringRepresentation();
    }

    public void setText(String newText) {
        tableModel.setContent(newText);
    }


    public void append(String text) {

    }

    public void updateFont() {

    }

    public void paste(String textToInsert) {

    }

    public String getSelectedText() {
        return null;
    }

    private void addEntry() {
        int row = getSelectedRow();
        if (row == -1)
            row = 0;
        FileListEntry entry = new FileListEntry("", "", null);
        if (editListEntry(entry))
            tableModel.addEntry(row, entry);
        entryEditor.updateField(this);
    }

    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null)
            for (int i = rows.length-1; i>=0; i--) {
                tableModel.removeEntry(rows[i]);
            }
        entryEditor.updateField(this);
    }

    private void moveEntry(int i) {
        int[] sel = getSelectedRows();
        if ((sel.length != 1) || (tableModel.getRowCount() < 2))
            return;
        int toIdx = sel[0]+i;
        if (toIdx >= tableModel.getRowCount())
            toIdx -= tableModel.getRowCount();
        if (toIdx < 0)
            toIdx += tableModel.getRowCount();
        FileListEntry entry = tableModel.getEntry(sel[0]);
        tableModel.removeEntry(sel[0]);
        tableModel.addEntry(toIdx, entry);
        entryEditor.updateField(this);
        setRowSelectionInterval(toIdx, toIdx);
    }

    private boolean editListEntry(FileListEntry entry) {
        if (editor == null) {
            editor = new FileListEntryEditor(frame, entry, false);
        }
        else
            editor.setEntry(entry);
        editor.setVisible(true);
        if (editor.okPressed())
            tableModel.fireTableDataChanged();
        entryEditor.updateField(this);
        return editor.okPressed();
    }

    private void autoSetLinks() {
        BibtexEntry entry = entryEditor.getEntry();
        if (autoSetLinks(entry, tableModel))
            entryEditor.updateField(this);
    }

    /**
     * Automatically add links for this entry to the table model given as an argument, based on
     * the globally stored list of external file types. The entry itself is not modified. The entry's
     * bibtex key must have been set.
     *
     * @param entry The BibtexEntry to find links for.
     * @param tableModel The table model to insert links into. Already existing links are not duplicated or removed.
     * @return true if any new links were found, false otherwise.
     */
    public static boolean autoSetLinks(BibtexEntry entry, FileListTableModel tableModel) {

        String field = null;
        boolean foundAny = false;
        ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
        ArrayList dirs = new ArrayList();
        if (Globals.prefs.hasKey(GUIGlobals.FILE_FIELD+"Directory"))
            dirs.add(Globals.prefs.get(GUIGlobals.FILE_FIELD+"Directory"));
        for (int i = 0; i < types.length; i++) {
            ExternalFileType type = types[i];
            //System.out.println("Looking for "+type.getName());
            String found = Util.findFile(entry, type, dirs);
            if (found != null) {
                //System.out.println("Found: "+found);
                File f= new File(found);
                boolean alreadyHas = false;
                for (int j=0; j<tableModel.getRowCount(); j++) {
                    FileListEntry existingEntry = tableModel.getEntry(j);
                    if (new File(existingEntry.getLink()).equals(f)) {
                        alreadyHas = true;
                        break;
                    }
                }
                if (!alreadyHas) {
                    FileListEntry flEntry = new FileListEntry(f.getName(), found, type);
                    tableModel.addEntry(tableModel.getRowCount(), flEntry);
                    foundAny = true;
                }
            }
        }
        return foundAny;
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
    public void downloadComplete(FileListEntry file) {
        tableModel.addEntry(tableModel.getRowCount(), file);
        entryEditor.updateField(this);
    }

    class TableClickListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                int row = rowAtPoint(e.getPoint());
                if (row >= 0) {
                    FileListEntry entry = tableModel.getEntry(row);
                    editListEntry(entry);
                }
            }
        }
    }


}
