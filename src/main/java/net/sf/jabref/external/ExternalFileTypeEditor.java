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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 * Editor for external file types.
 */
public class ExternalFileTypeEditor extends JDialog {
    private JFrame frame;
    private JDialog dialog;
    private List<ExternalFileType> fileTypes;
    private JTable table;
    private ExternalFileTypeEntryEditor entryEditor;
    private FileTypeTableModel tableModel;
    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JButton add = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getIcon());
    private final JButton remove = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getIcon());
    private final JButton edit = new JButton(IconTheme.JabRefIcon.EDIT.getIcon());
    private final JButton toDefaults = new JButton(Localization.lang("Default"));
    private final EditListener editListener = new EditListener();


    private ExternalFileTypeEditor(JFrame frame) {
        super(frame, Localization.lang("Manage external file types"), true);
        this.frame = frame;
        init();
    }

    private ExternalFileTypeEditor(JDialog dialog) {
        super(dialog, Localization.lang("Manage external file types"), true);
        this.dialog = dialog;
        init();
    }

    /**
     * Update the editor to show the current settings in Preferences.
     */
    private void setValues() {
        fileTypes.clear();
        Collection<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        for (ExternalFileType type : types) {
            fileTypes.add(type.copy());
        }
        Collections.sort(fileTypes);
    }

    /**
     * Store the list of external entry types to Preferences.
     */
    private void storeSettings() {
        ExternalFileTypes.getInstance().setExternalFileTypes(fileTypes);
    }

    private void init() {

        ok.addActionListener(e -> {
            storeSettings();
            dispose();
        });
        Action cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        cancel.addActionListener(cancelAction);
        // The toDefaults resets the entire list to its default values.
        toDefaults.addActionListener(e -> {
            /*int reply = JOptionPane.showConfirmDialog(ExternalFileTypeEditor.this,
                    Globals.lang("All custom file types will be lost. Proceed?"),
                    Globals.lang("Reset file type definitions"), JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);*/
            //if (reply == JOptionPane.YES_OPTION) {
            List<ExternalFileType> list = ExternalFileTypes.getDefaultExternalFileTypes();
            fileTypes.clear();
            fileTypes.addAll(list);
            Collections.sort(fileTypes);
            //Globals.prefs.resetExternalFileTypesToDefault();
            //setValues();
            tableModel.fireTableDataChanged();
            //}
        });

        add.addActionListener(e ->  {
            // Generate a new file type:
            ExternalFileType type = new ExternalFileType("", "", "", "", "new", IconTheme.JabRefIcon.FILE.getSmallIcon());
            // Show the file type editor:
            getEditor(type).setVisible(true);
            if (entryEditor.okPressed()) {
                // Ok was pressed. Add the new file type and update the table:
                fileTypes.add(type);
                tableModel.fireTableDataChanged();
            }
        });

        remove.addActionListener(e -> {
            int[] rows = table.getSelectedRows();
            if (rows.length == 0) {
                return;
            }
            for (int i = rows.length - 1; i >= 0; i--) {
                fileTypes.remove(rows[i]);
            }
            tableModel.fireTableDataChanged();
            if (!fileTypes.isEmpty()) {
                int row = Math.min(rows[0], fileTypes.size() - 1);
                table.setRowSelectionInterval(row, row);
            }
        });

        edit.addActionListener(editListener);
        fileTypes = new ArrayList<>();
        setValues();

        tableModel = new FileTypeTableModel();
        table = new JTable(tableModel);
        table.setDefaultRenderer(ImageIcon.class, new IconRenderer());
        table.addMouseListener(new TableClickListener());

        table.getColumnModel().getColumn(0).setMaxWidth(24);
        table.getColumnModel().getColumn(0).setMinWidth(24);
        table.getColumnModel().getColumn(1).setMinWidth(170);
        table.getColumnModel().getColumn(2).setMinWidth(60);
        table.getColumnModel().getColumn(3).setMinWidth(100);
        table.getColumnModel().getColumn(0).setResizable(false);

        JScrollPane sp = new JScrollPane(table);

        JPanel upper = new JPanel();
        upper.setLayout(new BorderLayout());
        upper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        upper.add(sp, BorderLayout.CENTER);
        getContentPane().add(upper, BorderLayout.CENTER);

        ButtonStackBuilder bs = new ButtonStackBuilder();
        bs.addButton(add);
        bs.addButton(remove);
        bs.addButton(edit);
        bs.addRelatedGap();
        bs.addButton(toDefaults);
        upper.add(bs.getPanel(), BorderLayout.EAST);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        // Key bindings:
        ActionMap am = upper.getActionMap();
        InputMap im = upper.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);
        am = bb.getPanel().getActionMap();
        im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        if (frame == null) {
            setLocationRelativeTo(dialog);
        } else {
            setLocationRelativeTo(frame);
        }
    }

    private ExternalFileTypeEntryEditor getEditor(ExternalFileType type) {
        if (entryEditor == null) {
            entryEditor = new ExternalFileTypeEntryEditor(ExternalFileTypeEditor.this, type);
        } else {
            entryEditor.setEntry(type);
        }
        return entryEditor;
    }

    /**
     * Get an AbstractAction for opening the external file types editor.
     * @param frame The JFrame used as parent window for the dialog.
     * @return An Action for opening the editor.
     */
    public static AbstractAction getAction(JabRefFrame frame) {
        return new EditExternalFileTypesAction(frame);
    }

    /**
     * Get an AbstractAction for opening the external file types editor.
     * @param dialog The JDialog used as parent window for the dialog.
     * @return An Action for opening the editor.
     */
    public static AbstractAction getAction(JDialog dialog) {
        return new EditExternalFileTypesAction(dialog);
    }

    class EditListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            int[] rows = table.getSelectedRows();
            if (rows.length != 1) {
                return;
            }
            getEditor(fileTypes.get(rows[0])).setVisible(true);
            if (entryEditor.okPressed()) {
                tableModel.fireTableDataChanged();
            }
        }
    }

    static class IconRenderer implements TableCellRenderer {

        private final JLabel lab = new JLabel();


        @Override
        public Component getTableCellRendererComponent(JTable tab, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            lab.setText(null);
            lab.setIcon((Icon) value);
            return lab;
        }
    }

    private class FileTypeTableModel extends AbstractTableModel {
        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public int getRowCount() {
            return fileTypes.size();
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return " ";
            case 1:
                return Localization.lang("Name");
            case 2:
                return Localization.lang("Extension");
            case 3:
                return Localization.lang("MIME type");
            default: // Five columns
                return Localization.lang("Application");
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return ImageIcon.class;
            } else {
                return String.class;
            }
        }

        @Override
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
                return type.getMimeType();
            default:
                return type.getOpenWithApplication();
            }
        }
    }

    class TableClickListener extends MouseAdapter {

        private void handleClick(MouseEvent e) {
            if (e.getClickCount() == 2) {
                editListener.actionPerformed(null);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            handleClick(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            handleClick(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            handleClick(e);
        }
    }

    public static class EditExternalFileTypesAction extends MnemonicAwareAction {
        private JabRefFrame frame;
        private JDialog dialog;
        private ExternalFileTypeEditor editor;


        public EditExternalFileTypesAction(JabRefFrame frame) {
            super();
            putValue(Action.NAME, Localization.menuTitle("Manage external file types"));
            this.frame = frame;
        }

        public EditExternalFileTypesAction(JDialog dialog) {
            super();
            putValue(Action.NAME, Localization.menuTitle("Manage external file types"));
            this.dialog = dialog;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (editor == null) {
                if (frame == null) {
                    editor = new ExternalFileTypeEditor(dialog);
                } else {
                    editor = new ExternalFileTypeEditor(frame);
                }
            }
            editor.setValues();
            editor.setVisible(true);
            if ((frame != null) && (frame.getCurrentBasePanel() != null)) {
                frame.getCurrentBasePanel().mainTable.repaint();
            }
        }
    }

}
