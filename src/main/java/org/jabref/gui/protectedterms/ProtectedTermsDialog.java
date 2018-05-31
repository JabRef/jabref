package org.jabref.gui.protectedterms;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.WindowLocation;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.protectedterms.ProtectedTermsList;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class produces a dialog box for managing term list files.
 */
public class ProtectedTermsDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedTermsDialog.class);

    private final JabRefFrame frame;
    private JDialog diag;
    private JTable table;
    private DefaultTableModel tableModel;
    private final JPopupMenu popup = new JPopupMenu();
    private final JMenuItem edit = new JMenuItem(Localization.lang("Edit"));
    private final JMenuItem show = new JMenuItem(Localization.lang("View"));
    private final JMenuItem remove = new JMenuItem(Localization.lang("Remove"));
    private final JMenuItem reload = new JMenuItem(Localization.lang("Reload"));
    private final JMenuItem enabled = new JCheckBoxMenuItem(Localization.lang("Enabled"));
    private final JButton loadButton = new JButton(IconTheme.JabRefIcon.OPEN.getIcon());
    private final JButton removeButton = new JButton(IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
    private final JButton newButton = new JButton(IconTheme.JabRefIcon.NEW.getIcon());
    private ActionListener removeAction;

    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));

    private boolean okPressed;
    private final ProtectedTermsLoader loader;

    public ProtectedTermsDialog(JabRefFrame frame) {

        this.frame = Objects.requireNonNull(frame);
        this.loader = Globals.protectedTermsLoader;
        init();

    }

    private void init() {
        setupPopupMenu();

        loadButton.addActionListener(actionEvent -> {
            AddFileDialog addDialog = new AddFileDialog();
            addDialog.setVisible(true);
            addDialog.getFileName().ifPresent(fileName -> loader.addProtectedTermsListFromFile(fileName, true));
            tableModel.fireTableDataChanged();
        });
        loadButton.setToolTipText(Localization.lang("Add protected terms file"));

        removeButton.addActionListener(removeAction);
        removeButton.setToolTipText(Localization.lang("Remove protected terms file"));

        newButton.addActionListener(actionEvent -> {
            NewProtectedTermsFileDialog newDialog = new NewProtectedTermsFileDialog(diag, loader);
            newDialog.setVisible(true);
            tableModel.fireTableDataChanged();
        });
        newButton.setToolTipText(Localization.lang("New protected terms file"));

        setupTable();

        // Build dialog
        diag = new JDialog(frame, Localization.lang("Manage protected terms files"), true);

        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("fill:pref:grow, 4dlu, left:pref, 4dlu, left:pref, 4dlu, left:pref",
                "100dlu:grow, 4dlu, pref"));
        builder.add(new JScrollPane(table)).xyw(1, 1, 7);
        builder.add(newButton).xy(3, 3);
        builder.add(loadButton).xy(5, 3);
        builder.add(removeButton).xy(7, 3);
        builder.padding("5dlu, 5dlu, 5dlu, 5dlu");

        diag.add(builder.getPanel(), BorderLayout.CENTER);

        AbstractAction okListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                // Write changes to preferences
                storePreferences();
                diag.dispose();
            }
        };
        ok.addActionListener(okListener);

        Action cancelListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                // Restore from preferences
                loader.update(Globals.prefs.getProtectedTermsPreferences());
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelListener);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.add(bb.getPanel(), BorderLayout.SOUTH);

        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelListener);
        im.put(KeyStroke.getKeyStroke("ENTER"), "enterOk");
        am.put("enterOk", okListener);

        diag.pack();

        WindowLocation pw = new WindowLocation(diag, JabRefPreferences.TERMS_POS_X, JabRefPreferences.TERMS_POS_Y,
                JabRefPreferences.TERMS_SIZE_X, JabRefPreferences.TERMS_SIZE_Y);
        pw.displayWindowAtStoredLocation();
    }

    private void setupTable() {
        tableModel = new TermTableModel();
        table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setMinWidth((cm.getColumn(0).getPreferredWidth() * 11) / 10);
        cm.getColumn(0).setMaxWidth((cm.getColumn(0).getPreferredWidth() * 11) / 10);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(100);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    tablePopup(mouseEvent);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    tablePopup(mouseEvent);
                }
            }

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    getSelectedTermsList().ifPresent(list -> list.setEnabled(!list.isEnabled()));
                    tableModel.fireTableDataChanged();
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(new EntrySelectionListener());
    }

    private void setupPopupMenu() {
        popup.add(edit);
        popup.add(show);
        popup.add(remove);
        popup.add(reload);
        popup.addSeparator();
        popup.add(enabled);

        // Add action listener to "Edit" menu item, which is supposed to open the term file in an external editor:
        edit.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(term -> {
            Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("terms");
            String fileName = term.getLocation();
            try {
                if (type.isPresent()) {
                    JabRefDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), fileName, type);
                } else {
                    // Fall back to ".txt"
                    Optional<ExternalFileType> txtType = ExternalFileTypes.getInstance()
                            .getExternalFileTypeByExt("txt");
                    if (txtType.isPresent()) {
                        JabRefDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), fileName, txtType);
                    } else {
                        JabRefDesktop.openExternalFileUnknown(frame, new BibEntry(), new BibDatabaseContext(), fileName,
                                new UnknownExternalFileType("terms"));
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Problem open protected terms file editor", e);
            }
        }));

        // Add action listener to "Show" menu item, which is supposed to open the term file in a dialog:
        show.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(this::displayTerms));

        // Create action listener for removing a term file, also used for the remove button
        removeAction = actionEvent -> getSelectedTermsList().ifPresent(list -> {

            if (!list.isInternalList() && (JOptionPane.showConfirmDialog(diag,
                    Localization.lang("Are you sure you want to remove the protected terms file?"),
                    Localization.lang("Remove protected terms file"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                if (!loader.removeProtectedTermsList(list)) {
                    LOGGER.info("Problem removing protected terms file");
                }
                tableModel.fireTableDataChanged();
            }
        });
        // Add it to the remove menu item
        remove.addActionListener(removeAction);

        // Add action listener to the "Reload" menu item, which is supposed to reload an external term file
        reload.addActionListener(actionEvent -> {
            getSelectedTermsList().ifPresent(loader::reloadProtectedTermsList);
        });

        enabled.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(list -> {
            list.setEnabled(enabled.isSelected());
        }));

    }

    public void setVisible(boolean visible) {
        okPressed = false;
        diag.setVisible(visible);
    }

    /**
     * Get the currently selected term list.
     * @return the selected term list, or empty if no term list is selected.
     */
    private Optional<ProtectedTermsList> getSelectedTermsList() {
        if (table.getSelectedRow() != -1) {
            return Optional.of(loader.getProtectedTermsLists().get(table.getSelectedRow()));
        }
        return Optional.empty();
    }

    class TermTableModel extends DefaultTableModel {

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public int getRowCount() {
            return loader.getProtectedTermsLists().size();
        }

        @Override
        public String getColumnName(int i) {
            switch (i) {
            case 0:
                return Localization.lang("Enabled");
            case 1:
                return Localization.lang("Description");
            case 2:
                return Localization.lang("File");
            default:
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
            case 0:
                return loader.getProtectedTermsLists().get(row).isEnabled();
            case 1:
                return loader.getProtectedTermsLists().get(row).getDescription();
            case 2:
                ProtectedTermsList list = loader.getProtectedTermsLists().get(row);
                return list.isInternalList() ? Localization.lang("Internal list") + " - " + list.getLocation() : list
                        .getLocation();
            default:
                return "";
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            default:
                return String.class;
            }
        }

        @Override
        public void setValueAt(Object cell, int row, int column) {
            if (column == 0) {
                ProtectedTermsList list = loader.getProtectedTermsLists().get(row);
                list.setEnabled(!list.isEnabled());
                this.fireTableCellUpdated(row, column);
            }
        }

    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void tablePopup(MouseEvent e) {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void displayTerms(ProtectedTermsList list) {
        // Make a dialog box to display the contents:
        final JDialog dd = new JDialog(diag, list.getDescription() + " - " + list.getLocation(), true);

        JTextArea ta = new JTextArea(list.getTermListing());
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(700, 500));
        dd.getContentPane().add(sp, BorderLayout.CENTER);
        JButton okButton = new JButton(Localization.lang("OK"));
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(okButton);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dd.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        okButton.addActionListener(actionEvent -> dd.dispose());
        dd.pack();
        dd.setLocationRelativeTo(diag);
        dd.setVisible(true);
    }

    /**
     * The listener for the table monitoring the current selection.
     */
    private class EntrySelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent listEvent) {
            getSelectedTermsList().ifPresent(list -> {
                // Enable/disable popup menu items and buttons
                if (list.isInternalList()) {
                    remove.setEnabled(false);
                    edit.setEnabled(false);
                    reload.setEnabled(false);
                    removeButton.setEnabled(false);
                } else {
                    remove.setEnabled(true);
                    edit.setEnabled(true);
                    reload.setEnabled(true);
                    removeButton.setEnabled(true);
                }
                enabled.setSelected(list.isEnabled());
            });
        }
    }

    private class AddFileDialog extends JabRefDialog {

        private final JTextField newFile = new JTextField();
        private boolean addOKPressed;

        public AddFileDialog() {
            super(diag, Localization.lang("Add protected terms file"), true, AddFileDialog.class);

            JButton browse = new JButton(Localization.lang("Browse"));
            FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                    .addExtensionFilter(FileType.TERMS)
                    .withDefaultExtension(FileType.TERMS)
                    .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
            DialogService ds = new FXDialogService();

            browse.addActionListener(e -> {
                Optional<Path> file = DefaultTaskExecutor
                        .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));
                file.ifPresent(f -> newFile.setText(f.toAbsolutePath().toString()));
            });

            // Build content panel
            FormBuilder builder = FormBuilder.create();
            builder.layout(new FormLayout("left:pref, 4dlu, fill:100dlu:grow, 4dlu, pref", "p"));
            builder.add(Localization.lang("File")).xy(1, 1);
            builder.add(newFile).xy(3, 1);
            builder.add(browse).xy(5, 1);
            builder.padding("10dlu, 10dlu, 10dlu, 10dlu");
            getContentPane().add(builder.build(), BorderLayout.CENTER);

            // Buttons
            ButtonBarBuilder bb = new ButtonBarBuilder();
            JButton addOKButton = new JButton(Localization.lang("OK"));
            JButton addCancelButton = new JButton(Localization.lang("Cancel"));
            bb.addGlue();
            bb.addButton(addOKButton);
            bb.addButton(addCancelButton);
            bb.addGlue();
            bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
            addOKButton.addActionListener(e -> {
                addOKPressed = true;
                dispose();
            });

            Action cancelAction = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addOKPressed = false;
                    dispose();
                }
            };
            addCancelButton.addActionListener(cancelAction);

            // Key bindings:
            bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
            bb.getPanel().getActionMap().put("close", cancelAction);
            pack();
            setLocationRelativeTo(diag);
        }

        public Optional<String> getFileName() {
            if (addOKPressed && (newFile.getText() != null) && !newFile.getText().isEmpty()) {
                return Optional.of(newFile.getText());
            }
            return Optional.empty();
        }

    }

    private void storePreferences() {
        Globals.prefs.setProtectedTermsPreferences(loader);
    }
}
