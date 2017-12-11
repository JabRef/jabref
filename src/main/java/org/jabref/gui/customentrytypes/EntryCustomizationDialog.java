package org.jabref.gui.customentrytypes;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.strings.StringUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class EntryCustomizationDialog extends JabRefDialog implements ListSelectionListener {

    protected GridBagLayout gbl = new GridBagLayout();
    protected GridBagConstraints con = new GridBagConstraints();
    protected JButton delete;
    private final JabRefFrame frame;
    private FieldSetComponent reqComp;
    private FieldSetComponent optComp;
    private FieldSetComponent optComp2;
    private EntryTypeList typeComp;
    private final List<String> preset = InternalBibtexFields.getAllPublicFieldNames();
    private String lastSelected;
    private final Map<String, Set<String>> reqLists = new HashMap<>();
    private final Map<String, Set<String>> optLists = new HashMap<>();
    private final Map<String, Set<String>> opt2Lists = new HashMap<>();
    private final Set<String> defaulted = new HashSet<>();
    private final Set<String> changed = new HashSet<>();

    private boolean biblatexMode;
    private BibDatabaseMode bibDatabaseMode;

    /**
     * Creates a new instance of EntryCustomizationDialog
     */
    public EntryCustomizationDialog(JabRefFrame frame) {
        super(frame, Localization.lang("Customize entry types"), false, EntryCustomizationDialog.class);

        this.frame = frame;
        initGui();
    }

    private void initGui() {
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        if (frame.getCurrentBasePanel() == null) {
            bibDatabaseMode = Globals.prefs.getDefaultBibDatabaseMode();
        } else {
            bibDatabaseMode = frame.getCurrentBasePanel().getBibDatabaseContext().getMode();
        }
        biblatexMode = BibDatabaseMode.BIBLATEX.equals(bibDatabaseMode);

        JPanel main = new JPanel();
        JPanel buttons = new JPanel();
        JPanel right = new JPanel();
        main.setLayout(new BorderLayout());
        right.setLayout(new GridLayout(biblatexMode ? 2 : 1, 2));

        List<String> entryTypes = new ArrayList<>();
        entryTypes.addAll(EntryTypes.getAllTypes(bibDatabaseMode));

        typeComp = new EntryTypeList(entryTypes, bibDatabaseMode);
        typeComp.addListSelectionListener(this);
        typeComp.addAdditionActionListener(e -> typeComp.selectField(e.getActionCommand()));
        typeComp.addDefaultActionListener(new DefaultListener());
        typeComp.setListSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //typeComp.setEnabled(false);
        reqComp = new FieldSetComponent(Localization.lang("Required fields"), new ArrayList<>(), preset, true, true);
        reqComp.setEnabled(false);
        reqComp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        ListDataListener dataListener = new DataListener();
        reqComp.addListDataListener(dataListener);
        optComp = new FieldSetComponent(Localization.lang("Optional fields"), new ArrayList<>(), preset, true, true);
        optComp.setEnabled(false);
        optComp.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        optComp.addListDataListener(dataListener);
        right.add(reqComp);
        right.add(optComp);

        if (biblatexMode) {
            optComp2 = new FieldSetComponent(Localization.lang("Optional fields") + " 2", new ArrayList<>(), preset, true, true);
            optComp2.setEnabled(false);
            optComp2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            optComp2.addListDataListener(dataListener);
            right.add(new JPanel());
            right.add(optComp2);
        }

        //right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Globals.lang("Fields")));
        right.setBorder(BorderFactory.createEtchedBorder());
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        JButton apply = new JButton(Localization.lang("Apply"));
        ok.addActionListener(e -> {
            applyChanges();
            dispose();
        });
        apply.addActionListener(e -> applyChanges());
        cancel.addActionListener(e -> dispose());
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(apply);
        bb.addButton(cancel);
        bb.addGlue();

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

        //con.fill = GridBagConstraints.BOTH;
        //con.weightx = 0.3;
        //con.weighty = 1;
        //gbl.setConstraints(typeComp, con);
        main.add(typeComp, BorderLayout.WEST);
        main.add(right, BorderLayout.CENTER);
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        pane.add(main, BorderLayout.CENTER);
        pane.add(buttons, BorderLayout.SOUTH);
        pack();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (lastSelected != null) {
            // The entry type lastSelected is now unselected, so we store the current settings
            // for that type in our two maps.
            reqLists.put(lastSelected, reqComp.getFields());
            optLists.put(lastSelected, optComp.getFields());
            if (biblatexMode) {
                opt2Lists.put(lastSelected, optComp2.getFields());
            }
        }

        String selectedTypeName = typeComp.getFirstSelected();
        if (selectedTypeName == null) {
            return;
        }
        Set<String> requiredFieldsSelectedType = reqLists.get(selectedTypeName);
        if (requiredFieldsSelectedType == null) {
            Optional<EntryType> type = EntryTypes.getType(selectedTypeName, bibDatabaseMode);
            if (type.isPresent()) {
                Set<String> req = type.get().getRequiredFields();

                Set<String> opt;
                if (biblatexMode) {
                    opt = type.get().getPrimaryOptionalFields();

                    Set<String> opt2 = type.get().getSecondaryOptionalFields();

                    optComp2.setFields(opt2);
                    optComp2.setEnabled(true);
                } else {
                    opt = type.get().getOptionalFields();
                }
                reqComp.setFields(req);
                reqComp.setEnabled(true);
                optComp.setFields(opt);
                optComp.setEnabled(true);
            } else {
                // New entry
                reqComp.setFields(new HashSet<>());
                reqComp.setEnabled(true);
                optComp.setFields(new HashSet<>());
                optComp.setEnabled(true);
                if (biblatexMode) {
                    optComp2.setFields(new HashSet<>());
                    optComp2.setEnabled(true);
                }
                reqComp.requestFocus();
            }
        } else {
            reqComp.setFields(requiredFieldsSelectedType);
            optComp.setFields(optLists.get(selectedTypeName));
            if (biblatexMode) {
                optComp2.setFields(opt2Lists.get(selectedTypeName));
            }
        }

        lastSelected = selectedTypeName;
        typeComp.enable(selectedTypeName, changed.contains(lastSelected) && !defaulted.contains(lastSelected));
    }

    private void applyChanges() {
        valueChanged(new ListSelectionEvent(new JList<>(), 0, 0, false));

        List<String> actuallyChangedTypes = new ArrayList<>();

        // Iterate over our map of required fields, and list those types if necessary:
        Set<String> types = typeComp.getFields();
        for (Map.Entry<String, Set<String>> stringListEntry : reqLists.entrySet()) {
            if (!types.contains(stringListEntry.getKey())) {
                continue;
            }

            Set<String> requiredFieldsList = stringListEntry.getValue();
            Set<String> optionalFieldsList = optLists.get(stringListEntry.getKey());
            Set<String> secondaryOptionalFieldsLists = opt2Lists.get(stringListEntry.getKey());

            if (secondaryOptionalFieldsLists == null) {
                secondaryOptionalFieldsLists = new HashSet<>(0);
            }

            // If this type is already existing, check if any changes have
            // been made
            boolean changesMade = true;

            if (defaulted.contains(stringListEntry.getKey())) {
                // This type should be reverted to its default setup.
                EntryTypes.removeType(stringListEntry.getKey(), bibDatabaseMode);

                actuallyChangedTypes.add(stringListEntry.getKey().toLowerCase(Locale.ENGLISH));
                defaulted.remove(stringListEntry.getKey());
                continue;
            }

            Optional<EntryType> oldType = EntryTypes.getType(stringListEntry.getKey(), bibDatabaseMode);
            if (oldType.isPresent()) {
                Set<String> oldRequiredFieldsList = oldType.get().getRequiredFieldsFlat();
                Set<String> oldOptionalFieldsList = oldType.get().getOptionalFields();
                if (biblatexMode) {
                    Set<String> oldPrimaryOptionalFieldsLists = oldType.get().getPrimaryOptionalFields();
                    Set<String> oldSecondaryOptionalFieldsList = oldType.get().getSecondaryOptionalFields();
                    if (oldRequiredFieldsList.equals(requiredFieldsList) && oldPrimaryOptionalFieldsLists.equals(optionalFieldsList) &&
                            oldSecondaryOptionalFieldsList.equals(secondaryOptionalFieldsLists)) {
                        changesMade = false;
                    }
                } else if (oldRequiredFieldsList.equals(requiredFieldsList) && oldOptionalFieldsList.equals(optionalFieldsList)) {
                    changesMade = false;
                }
            }

            if (changesMade) {
                CustomEntryType customType = biblatexMode ?
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), requiredFieldsList, optionalFieldsList, secondaryOptionalFieldsLists) :
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), requiredFieldsList, optionalFieldsList);

                EntryTypes.addOrModifyCustomEntryType(customType, bibDatabaseMode);
                actuallyChangedTypes.add(customType.getName().toLowerCase(Locale.ENGLISH));
            }
        }

        // update all affected entries if something has been changed
        if (!actuallyChangedTypes.isEmpty()) {
            updateEntriesForChangedTypes(actuallyChangedTypes);
        }

        Set<String> typesToRemove = new HashSet<>();
        for (String existingType : EntryTypes.getAllTypes(bibDatabaseMode)) {
            if (!types.contains(existingType)) {
                typesToRemove.add(existingType);
            }
        }

        // Remove those that should be removed:
        if (!typesToRemove.isEmpty()) {
            for (String typeToRemove : typesToRemove) {
                deleteType(typeToRemove);
            }
        }

        updateTables();
        CustomEntryTypesManager.saveCustomEntryTypes(Globals.prefs);
    }

    private void deleteType(String name) {
        Optional<EntryType> type = EntryTypes.getType(name, bibDatabaseMode);

        if (type.isPresent() && (type.get() instanceof CustomEntryType)) {
            if (!EntryTypes.getStandardType(name, bibDatabaseMode).isPresent()) {
                int reply = JOptionPane.showConfirmDialog
                        (frame, Localization.lang("All entries of this "
                                        + "type will be declared "
                                        + "typeless. Continue?"),
                                Localization.lang("Delete custom format") +
                                        " '" + StringUtil.capitalizeFirst(name) + '\'', JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                if (reply != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            EntryTypes.removeType(name, bibDatabaseMode);
            updateEntriesForChangedTypes(Collections.singletonList(name.toLowerCase(Locale.ENGLISH)));
            changed.remove(name);
            reqLists.remove(name);
            optLists.remove(name);
            if (biblatexMode) {
                opt2Lists.remove(name);
            }
        }
    }

    private void updateEntriesForChangedTypes(List<String> actuallyChangedTypes) {
        for (BasePanel bp : frame.getBasePanelList()) {
            // get all affected entries
            List<BibEntry> filtered = bp.getDatabase().getEntries().stream()
                    .filter(entry -> actuallyChangedTypes.contains(entry.getType().toLowerCase(Locale.ENGLISH))).collect(Collectors.toList());

            // update all affected entries with new type
            filtered.forEach(entry -> EntryTypes.getType(entry.getType(), bibDatabaseMode).ifPresent(entry::setType));
        }
    }

    private void updateTables() {
        for (BasePanel basePanel : frame.getBasePanelList()) {
            ((AbstractTableModel) basePanel.getMainTable().getModel()).fireTableDataChanged();
        }
    }


    // DEFAULT button pressed. Remember that this entry should be reset to default,
    // unless changes are made later.
    private class DefaultListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (lastSelected == null) {
                return;
            }
            defaulted.add(lastSelected);

            Optional<EntryType> type = EntryTypes.getStandardType(lastSelected, bibDatabaseMode);
            if (type.isPresent()) {
                Set<String> of = type.get().getOptionalFields();
                Set<String> req = type.get().getRequiredFields();
                Set<String> opt1 = new HashSet<>();
                Set<String> opt2 = new HashSet<>();

                if (!(of.isEmpty())) {
                    if (biblatexMode) {
                        opt1 = type.get().getPrimaryOptionalFields();
                        opt2 = type.get().getSecondaryOptionalFields();
                    } else {
                        opt1 = of;
                    }
                }

                reqComp.setFields(req);
                reqComp.setEnabled(true);
                optComp.setFields(opt1);
                if (biblatexMode) {
                    optComp2.setFields(opt2);
                }
            }
        }
    }

    class DataListener implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent e) {
            record();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            record();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            record();
        }

        private void record() {
            if (lastSelected == null) {
                return;
            }
            defaulted.remove(lastSelected);
            changed.add(lastSelected);
            typeComp.enable(lastSelected, true);
        }

    }
}
