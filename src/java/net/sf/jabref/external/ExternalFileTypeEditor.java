package net.sf.jabref.external;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 * Editor for external file types.
 */
public class ExternalFileTypeEditor extends JDialog {

    private JabRefFrame frame;
    private ArrayList<ExternalFileType> fileTypes;
    private JTable table;
    private ExternalFileTypeEntryEditor entryEditor = null;
    private FileTypeTableModel tableModel;
    private JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    private JButton add = new JButton(GUIGlobals.getImage("add")),
        remove = new JButton(GUIGlobals.getImage("remove")),
        edit = new JButton(GUIGlobals.getImage("edit"));

    public ExternalFileTypeEditor(JabRefFrame frame) {
        this.frame = frame;
        init();
    }

    /**
     * Update the editor to show the current settings in Preferences.
     */
    public void setValues() {
        fileTypes.clear();
        ExternalFileType[] types = Globals.prefs.getExternalFileTypeSelection();
        for (int i = 0; i < types.length; i++) {
            fileTypes.add(types[i]);
        }
        Collections.sort(fileTypes);
    }

    /**
     * Store the list of external entry types to Preferences.
     */
    public void storeSettings() {
        Globals.prefs.setExternalFileTypes(fileTypes);
    }

    private void init() {

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                storeSettings();
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        add.addActionListener(new AddListener());
        remove.addActionListener(new RemoveListener());
        edit.addActionListener(new EditListener());
        fileTypes = new ArrayList<ExternalFileType>();
        setValues();
        

        tableModel = new FileTypeTableModel();
        table = new JTable(tableModel);
        table.setDefaultRenderer(ImageIcon.class, new IconRenderer());

        table.getColumnModel().getColumn(0).setMaxWidth(24);
        table.getColumnModel().getColumn(0).setMinWidth(24);
        table.getColumnModel().getColumn(1).setMinWidth(170);
        table.getColumnModel().getColumn(2).setMinWidth(60);
        table.getColumnModel().getColumn(3).setMinWidth(100);
        table.getColumnModel().getColumn(0).setResizable(false);
        
        JScrollPane sp = new JScrollPane(table);

        JPanel upper = new JPanel();
        upper.setLayout(new BorderLayout());
        upper.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        upper.add(sp, BorderLayout.CENTER);
        getContentPane().add(upper, BorderLayout.CENTER);

        ButtonStackBuilder bs = new ButtonStackBuilder();
        bs.addGridded(add);
        bs.addGridded(remove);
        bs.addGridded(edit);
        upper.add(bs.getPanel(), BorderLayout.EAST);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        setLocationRelativeTo(frame);
    }

    private ExternalFileTypeEntryEditor getEditor(ExternalFileType type) {
        if (entryEditor == null)
            entryEditor = new ExternalFileTypeEntryEditor(ExternalFileTypeEditor.this,  type);
        else
            entryEditor.setEntry(type);
        return entryEditor;
    }

    class AddListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            // Generate a new file type:
            ExternalFileType type = new ExternalFileType("", "", "", "new");
            // Show the file type editor:
            getEditor(type).setVisible(true);
            if (entryEditor.okPressed()) {
                // Ok was pressed. Add the new file type and update the table:
                fileTypes.add(type);
                tableModel.fireTableDataChanged();
            }
        }
    }

    class RemoveListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0)
                return;
            for (int i=rows.length-1; i>=0; i--) {
                fileTypes.remove(rows[i]);
            }
            tableModel.fireTableDataChanged();
            if (fileTypes.size() > 0) {
                int row = Math.min(rows[0], fileTypes.size()-1);
                table.setRowSelectionInterval(row, row);
            }
        }
    }

    class EditListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            if (rows.length != 1)
                return;
            getEditor(fileTypes.get(rows[0])).setVisible(true);
            if (entryEditor.okPressed())
                tableModel.fireTableDataChanged();
        }
    }

    class IconRenderer implements TableCellRenderer {
        JLabel lab = new JLabel();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            lab.setText(null);
            lab.setIcon((ImageIcon)value);
            return lab;
        }
    }

    class FileTypeTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return 4;
        }

        public int getRowCount() {
            return fileTypes.size();
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return Globals.lang("Name");
                case 2:
                    return Globals.lang("Extension");
                case 3:
                    return Globals.lang("Application");
                default:
                    return null;
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return ImageIcon.class;
            else return String.class;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ExternalFileType type = fileTypes.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return type.getIcon();
                case 1:
                    return type.getName();
                case 2:
                    return type.getExtension();
                case 3:
                    return type.getOpenWith();
                default:
                    return null;
            }
        }
    }

}
