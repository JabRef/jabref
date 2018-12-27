package org.jabref.gui.externalfiletype;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonStackBuilder;

/**
 * Editor for external file types.
 */
public class ExternalFileTypeEditor extends BaseDialog<Void> {

    private List<ExternalFileType> fileTypes;
    private JTable table;
    private ExternalFileTypeEntryEditor entryEditor;
    private FileTypeTableModel tableModel;
    private final JButton add = new JButton(IconTheme.JabRefIcons.ADD_NOBOX.getIcon());
    private final JButton remove = new JButton(IconTheme.JabRefIcons.REMOVE_NOBOX.getIcon());
    private final JButton edit = new JButton(IconTheme.JabRefIcons.EDIT.getIcon());
    private final JButton toDefaults = new JButton(Localization.lang("Default"));
    private final EditListener editListener = new EditListener();

    public ExternalFileTypeEditor() {
        this.setTitle(Localization.lang("Manage external file types"));
        this.initModality(Modality.APPLICATION_MODAL);
        this.getDialogPane().setPrefSize(600, 500);

        init();
    }

    /**
     * Update the editor to show the current settings in Preferences.
     */
    private void setValues() {
        fileTypes.clear();
        Collection<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        fileTypes.addAll(types);
        fileTypes.sort(Comparator.comparing(ExternalFileType::getName));
    }

    /**
     * Store the list of external entry types to Preferences.
     */
    private void storeSettings() {
        ExternalFileTypes.getInstance().setExternalFileTypes(fileTypes);
    }

    /**
     * Get an AbstractAction for opening the external file types editor.
     *
     * @return An Action for opening the editor.
     */
    public static AbstractAction getAction() {
        return new EditExternalFileTypesAction();
    }

    private void init() {

        this.getDialogPane().getButtonTypes().setAll(
                ButtonType.CANCEL,
                ButtonType.OK
        );

        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                storeSettings();
            }
            return null;
        });

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
            fileTypes.sort(Comparator.comparing(ExternalFileType::getName));
            //Globals.prefs.resetExternalFileTypesToDefault();
            //setValues();
            tableModel.fireTableDataChanged();
            //}
        });

        add.addActionListener(e -> {
            // Generate a new file type:
            CustomExternalFileType type = new CustomExternalFileType("", "", "", "", "new",
                    IconTheme.JabRefIcons.FILE);
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
        ControlHelper.setSwingContent(getDialogPane(), upper);

        ButtonStackBuilder bs = new ButtonStackBuilder();
        bs.addButton(add);
        bs.addButton(remove);
        bs.addButton(edit);
        bs.addRelatedGap();
        bs.addButton(toDefaults);
        upper.add(bs.getPanel(), BorderLayout.EAST);
    }

    private ExternalFileTypeEntryEditor getEditor(ExternalFileType type) {
        CustomExternalFileType typeForEdit;
        if (type instanceof CustomExternalFileType) {
            typeForEdit = (CustomExternalFileType) type;
        } else {
            typeForEdit = new CustomExternalFileType(type);
        }

        if (entryEditor == null) {
            entryEditor = new ExternalFileTypeEntryEditor(typeForEdit);
        } else {
            entryEditor.setEntry(typeForEdit);
        }
        return entryEditor;
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
            lab.setIcon(((JabRefIcon) value).getIcon());
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

        private ExternalFileTypeEditor editor;

        public EditExternalFileTypesAction() {
            super();
            putValue(Action.NAME, Localization.lang("Manage external file types"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (editor == null) {
                editor = new ExternalFileTypeEditor();
            }
            editor.setValues();
            editor.show();
        }
    }

}
