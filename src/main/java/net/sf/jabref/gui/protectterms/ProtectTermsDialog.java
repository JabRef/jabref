/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.protectterms;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
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
import javax.swing.table.TableColumnModel;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.protectterms.ProtectTermsList;
import net.sf.jabref.logic.protectterms.ProtectTermsLoader;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class produces a dialog box for choosing a style file.
 */
public class ProtectTermsDialog {

    private static final Log LOGGER = LogFactory.getLog(ProtectTermsDialog.class);

    private final JabRefFrame frame;
    private EventList<ProtectTermsList> termList;
    private JDialog diag;
    private JTable table;
    private DefaultEventTableModel<ProtectTermsList> tableModel;
    private DefaultEventSelectionModel<ProtectTermsList> selectionModel;
    private final JPopupMenu popup = new JPopupMenu();
    private final JMenuItem edit = new JMenuItem(Localization.lang("Edit"));
    private final JMenuItem show = new JMenuItem(Localization.lang("View"));
    private final JMenuItem remove = new JMenuItem(Localization.lang("Remove"));
    private final JMenuItem reload = new JMenuItem(Localization.lang("Reload"));
    private final JButton addButton = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getIcon());
    private final JButton removeButton = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getIcon());
    private ActionListener removeAction;

    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));

    private boolean okPressed;
    private final ProtectTermsLoader loader;


    public ProtectTermsDialog(JabRefFrame frame, ProtectTermsLoader loader) {

        this.frame = Objects.requireNonNull(frame);
        this.loader = Objects.requireNonNull(loader);
        init();

    }

    private void init() {
        setupPopupMenu();

        addButton.addActionListener(actionEvent -> {
            AddFileDialog addDialog = new AddFileDialog();
            addDialog.setVisible(true);
            addDialog.getFileName().ifPresent(loader::addFromFile);
            updateStyles();
        });
        addButton.setToolTipText(Localization.lang("Add term file"));

        removeButton.addActionListener(removeAction);
        removeButton.setToolTipText(Localization.lang("Remove term file"));


        setupTable();
        updateStyles();

        // Build dialog
        diag = new JDialog(frame, Localization.lang("Manage term files"), true);

        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("fill:pref:grow, 4dlu, left:pref, 4dlu, left:pref",
                "pref, 4dlu, 100dlu:grow, 4dlu, pref, 4dlu, fill:100dlu"));
        builder.add(Localization.lang("Select one of the available styles or add a style file from disk.")).xyw(1, 1,
                5);
        builder.add(new JScrollPane(table)).xyw(1, 3, 5);
        builder.add(addButton).xy(3, 5);
        builder.add(removeButton).xy(5, 5);
        builder.padding("5dlu, 5dlu, 5dlu, 5dlu");

        diag.add(builder.getPanel(), BorderLayout.CENTER);

        AbstractAction okListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                if ((table.getRowCount() == 0) || (table.getSelectedRowCount() == 0)) {
                    JOptionPane.showMessageDialog(diag, Localization.lang("You must select a valid style file."),
                            Localization.lang("Style selection"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                okPressed = true;
                diag.dispose();
            }
        };
        ok.addActionListener(okListener);

        Action cancelListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
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

        PositionWindow pw = new PositionWindow(diag, JabRefPreferences.STYLES_POS_X, JabRefPreferences.STYLES_POS_Y,
                JabRefPreferences.STYLES_SIZE_X, JabRefPreferences.STYLES_SIZE_Y);
        pw.setWindowPosition();
    }

    private void setupTable() {
        termList = new BasicEventList<>();
        EventList<ProtectTermsList> sortedStyles = new SortedList<>(termList);

        tableModel = (DefaultEventTableModel<ProtectTermsList>) GlazedListsSwing
                .eventTableModelWithThreadProxyList(sortedStyles, new TermTableFormat());
        table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(0).setPreferredWidth(100);
        selectionModel = (DefaultEventSelectionModel<ProtectTermsList>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(sortedStyles);
        table.setSelectionModel(selectionModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        });

        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
    }

    private void setupPopupMenu() {
        popup.add(edit);
        popup.add(show);
        popup.add(remove);
        popup.add(reload);

        // Add action listener to "Edit" menu item, which is supposed to open the style file in an external editor:
        edit.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(term -> {
            Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("txt");
            String link = term.getLocation();
            try {
                if (type.isPresent()) {
                    JabRefDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), link, type);
                } else {
                    JabRefDesktop.openExternalFileUnknown(frame, new BibEntry(), new BibDatabaseContext(), link,
                            new UnknownExternalFileType("txt"));
                }
            } catch (IOException e) {
                LOGGER.warn("Problem open term file editor", e);
            }
        }));

        // Add action listener to "Show" menu item, which is supposed to open the style file in a dialog:
        show.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(this::displayStyle));

        // Create action listener for removing a style, also used for the remove button
        removeAction = actionEvent -> getSelectedTermsList().ifPresent(list -> {

            if (!list.isInternalList() && (JOptionPane.showConfirmDialog(diag,
                    Localization.lang("Are you sure you want to remove the term list?"),
                    Localization.lang("Remove term list"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
                if (!loader.removeTermList(list)) {
                    LOGGER.info("Problem removing term list");
                }
                updateStyles();
            }
        });
        // Add it to the remove menu item
        remove.addActionListener(removeAction);

        // Add action listener to the "Reload" menu item, which is supposed to reload an external style file
        reload.addActionListener(actionEvent -> getSelectedTermsList().ifPresent(list -> {
            try {
                list.ensureUpToDate();
            } catch (IOException e) {
                LOGGER.warn("Problem with terms file '" + list.getLocation() + "'", e);
            }
        }));

    }

    public void setVisible(boolean visible) {
        okPressed = false;
        diag.setVisible(visible);
    }

    /**
     * Read all style files or directories of style files indicated by the current
     * settings, and add the styles to the list of styles.
     */
    private void updateStyles() {

        table.clearSelection();
        termList.getReadWriteLock().writeLock().lock();
        termList.clear();
        termList.addAll(loader.getTermsLists());
        termList.getReadWriteLock().writeLock().unlock();
    }


    /**
     * Get the currently selected style.
     * @return the selected style, or empty if no style is selected.
     */
    private Optional<ProtectTermsList> getSelectedTermsList() {
        if (!selectionModel.getSelected().isEmpty()) {
            return Optional.of(selectionModel.getSelected().get(0));
        }
        return Optional.empty();
    }

    static class TermTableFormat implements TableFormat<ProtectTermsList> {

        @Override
        public int getColumnCount() {
            return 3;
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
        public Object getColumnValue(ProtectTermsList termList, int i) {
            switch (i) {
            case 0:
                return termList.isEnabled() ? Localization.lang("Yes") : Localization.lang("No");
            case 1:
                return termList.getDescription();
            case 2:
                return termList.getLocation();
            default:
                return "";
            }
        }
    }


    public boolean isOkPressed() {
        return okPressed;
    }

    private void tablePopup(MouseEvent e) {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void displayStyle(ProtectTermsList style) {
        // Make a dialog box to display the contents:
        final JDialog dd = new JDialog(diag, style.getDescription(), true);

        JTextArea ta = new JTextArea(style.getTermListing());
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
     * The listener for the Glazed list monitoring the current selection.
     */
    private class EntrySelectionListener implements ListEventListener<ProtectTermsList> {

        @Override
        public void listChanged(ListEvent<ProtectTermsList> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                ProtectTermsList list = listEvent.getSourceList().get(0);
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
            }
        }
    }

    private class AddFileDialog extends JDialog {

        private final JTextField newFile = new JTextField();
        private boolean addOKPressed;


        public AddFileDialog() {
            super(diag, Localization.lang("Add protected term file"), true);

            JButton browse = new JButton(Localization.lang("Browse"));
            browse.addActionListener(BrowseAction.buildForFile(newFile, null, Collections.singletonList(".txt")));

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
}
