package net.sf.jabref.gui;

import net.sf.jabref.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by Morten O. Alver 2007.02.22
 */
public class FileListEditor extends JTable implements FieldEditor {

    FieldNameLabel label;
    FileListEntryEditor editor = null;
    private JabRefFrame frame;
    private String fieldName;
    private JPanel panel;
    private FileListTableModel tableModel;
    private JScrollPane sPane;
    private JButton add, remove;

    public FileListEditor(JabRefFrame frame, String fieldName, String content) {
        this.frame = frame;
        this.fieldName = fieldName;
        label = new FieldNameLabel(" " + Util.nCase(fieldName) + " ");
        tableModel = new FileListTableModel();
        setText(content);
        setModel(tableModel);
        sPane = new JScrollPane(this);
        setTableHeader(null);
        addMouseListener(new TableClickListener());

        add = new JButton(GUIGlobals.getImage("add"));
        remove = new JButton(GUIGlobals.getImage("remove"));
        add.setMargin(new Insets(0,0,0,0));
        remove.setMargin(new Insets(0,0,0,0));
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

        JToolBar tlb = new JToolBar(JToolBar.VERTICAL);
        tlb.add(add);
        tlb.add(remove);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(sPane, BorderLayout.CENTER);
        panel.add(tlb, BorderLayout.EAST);
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
    }

    private void removeEntries() {
        int[] rows = getSelectedRows();
        if (rows != null)
            for (int i = rows.length-1; i>=0; i--) {
                tableModel.removeEntry(rows[i]);
            }
    }

    private boolean editListEntry(FileListEntry entry) {
        if (editor == null) {
            editor = new FileListEntryEditor(frame, entry);
        }
        else
            editor.setEntry(entry);
        editor.setVisible(true);
        if (editor.okPressed())
            tableModel.fireTableDataChanged();
        return editor.okPressed();
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
