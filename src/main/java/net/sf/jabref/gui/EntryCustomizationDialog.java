package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.strings.StringUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class EntryCustomizationDialog extends JDialog implements ListSelectionListener, ActionListener {

    private final JabRefFrame frame;
    protected GridBagLayout gbl = new GridBagLayout();
    protected GridBagConstraints con = new GridBagConstraints();
    private FieldSetComponent reqComp;
    private FieldSetComponent optComp;
    private FieldSetComponent optComp2;
    private EntryTypeList typeComp;
    private JButton ok;
    private JButton cancel;
    private JButton apply;
    protected JButton helpButton;
    protected JButton delete;
    protected JButton importTypes;
    protected JButton exportTypes;
    private final List<String> preset = InternalBibtexFields.getAllPublicFieldNames();
    private String lastSelected;
    private final Map<String, List<String>> reqLists = new HashMap<>();
    private final Map<String, List<String>> optLists = new HashMap<>();
    private final Map<String, List<String>> opt2Lists = new HashMap<>();
    private final Set<String> defaulted = new HashSet<>();
    private final Set<String> changed = new HashSet<>();

    private boolean biblatexMode;
    private BibDatabaseMode bibDatabaseMode;

    /**
     * Creates a new instance of EntryCustomizationDialog
     */
    public EntryCustomizationDialog(JabRefFrame frame) {
        super(frame, Localization.lang("Customize entry types"), false);

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
        for (String s : EntryTypes.getAllTypes(bibDatabaseMode)) {
            entryTypes.add(s);
        }

        typeComp = new EntryTypeList(entryTypes, bibDatabaseMode);
        typeComp.addListSelectionListener(this);
        typeComp.addAdditionActionListener(this);
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
        ok = new JButton(Localization.lang("OK"));
        cancel = new JButton(Localization.lang("Cancel"));
        apply = new JButton(Localization.lang("Apply"));
        ok.addActionListener(this);
        apply.addActionListener(this);
        cancel.addActionListener(this);
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

        String s = typeComp.getFirstSelected();
        if (s == null) {
            return;
        }
        List<String> rl = reqLists.get(s);
        if (rl == null) {
            Optional<EntryType> type = EntryTypes.getType(s, bibDatabaseMode);
            if (type.isPresent()) {
                List<String> req = type.get().getRequiredFields();

                List<String> opt;
                if (biblatexMode) {
                    opt = type.get().getPrimaryOptionalFields();

                    List<String> opt2 = type.get().getSecondaryOptionalFields();

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
                reqComp.setFields(new ArrayList<>());
                reqComp.setEnabled(true);
                optComp.setFields(new ArrayList<>());
                optComp.setEnabled(true);
                if (biblatexMode) {
                    optComp2.setFields(new ArrayList<>());
                    optComp2.setEnabled(true);
                }
                reqComp.requestFocus();
            }
        } else {
            reqComp.setFields(rl);
            optComp.setFields(optLists.get(s));
            if (biblatexMode) {
                optComp2.setFields(opt2Lists.get(s));
            }
        }

        lastSelected = s;
        typeComp.enable(s, changed.contains(lastSelected) && !defaulted.contains(lastSelected));
    }

    private void applyChanges() {
        valueChanged(new ListSelectionEvent(new JList<>(), 0, 0, false));
        // Iterate over our map of required fields, and list those types if necessary:

        List<String> types = typeComp.getFields();
        for (Map.Entry<String, List<String>> stringListEntry : reqLists.entrySet()) {
            if (!types.contains(stringListEntry.getKey())) {
                continue;
            }

            List<String> reqStr = stringListEntry.getValue();
            List<String> optStr = optLists.get(stringListEntry.getKey());
            List<String> opt2Str = opt2Lists.get(stringListEntry.getKey());

            if (opt2Str == null) {
                opt2Str = new ArrayList<>(0);
            }

            // If this type is already existing, check if any changes have
            // been made
            boolean changesMade = true;

            if (defaulted.contains(stringListEntry.getKey())) {
                // This type should be reverted to its default setup.
                String nm = StringUtil.capitalizeFirst(stringListEntry.getKey());
                EntryTypes.removeType(nm, bibDatabaseMode);

                updateTypesForEntries();
                continue;
            }

            Optional<EntryType> oldType = EntryTypes.getType(stringListEntry.getKey(), bibDatabaseMode);
            if (oldType.isPresent()) {
                List<String> oldReq = oldType.get().getRequiredFieldsFlat();
                List<String> oldOpt = oldType.get().getOptionalFields();
                if (biblatexMode) {
                    List<String> oldPriOpt = oldType.get().getPrimaryOptionalFields();
                    List<String> oldSecOpt = oldType.get().getSecondaryOptionalFields();
                    if (equalLists(oldReq, reqStr) && equalLists(oldPriOpt, optStr) &&
                            equalLists(oldSecOpt, opt2Str)) {
                        changesMade = false;
                    }
                } else if (equalLists(oldReq, reqStr) && equalLists(oldOpt, optStr)) {
                    changesMade = false;
                }
            }

            if (changesMade) {
                CustomEntryType typ = biblatexMode ?
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), reqStr, optStr, opt2Str) :
                        new CustomEntryType(StringUtil.capitalizeFirst(stringListEntry.getKey()), reqStr, optStr);

                EntryTypes.addOrModifyCustomEntryType(typ);
                updateTypesForEntries();
            }
        }

        Set<Object> toRemove = new HashSet<>();
        for (String o : EntryTypes.getAllTypes(bibDatabaseMode)) {
            if (!types.contains(o)) {
                toRemove.add(o);
            }
        }

        // Remove those that should be removed:
        if (!toRemove.isEmpty()) {
            for (Object aToRemove : toRemove) {
                typeDeletion((String) aToRemove);
            }
        }

        updateTables();
    }

    private void typeDeletion(String name) {
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
            updateTypesForEntries();
            changed.remove(name);
            reqLists.remove(name);
            optLists.remove(name);
            if (biblatexMode) {
                opt2Lists.remove(name);
            }
        }
    }

    private static boolean equalLists(List<String> one, List<String> two) {
        if ((one == null) && (two == null)) {
            return true; // Both null.
        }
        if ((one == null) || (two == null)) {
            return false; // One of them null, the other not.
        }
        if (one.size() != two.size()) {
            return false; // Different length.
        }
        // If we get here, we know that both are non-null, and that they have the same length.
        for (int i = 0; i < one.size(); i++) {
            if (!one.get(i).equals(two.get(i))) {
                return false;
            }
        }
        // If we get here, all entries have matched.
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            applyChanges();
            dispose();
        } else if (e.getSource() == cancel) {
            dispose();
        } else if (e.getSource() == apply) {
            applyChanges();
        } else if (e.getSource() == typeComp) {
            typeComp.selectField(e.getActionCommand());
        }
    }

    /**
     * Cycle through all databases, and make sure everything is updated with
     * the new type customization. This includes making sure all entries have
     * a valid type, that no obsolete entry editors are around, and that
     * the right-click menus' change type menu is up-to-date.
     */
    private void updateTypesForEntries() {
        for (BasePanel bp : frame.getBasePanelList()) {
            for (BibEntry entry : bp.getDatabase().getEntries()) {
                EntryTypes.getType(entry.getType(), bibDatabaseMode).ifPresent(entry::setType);
            }
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
                List<String> of = type.get().getOptionalFields();
                List<String> req = type.get().getRequiredFields();
                List<String> opt1 = new ArrayList<>();
                List<String> opt2 = new ArrayList<>();

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
