package org.jabref.gui.cleanup;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.MetaData;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class FieldFormatterCleanupsPanel extends JPanel {

    private static final String DESCRIPTION = Localization.lang("Description") + ": ";
    private final JCheckBox cleanupEnabled;
    private FieldFormatterCleanups fieldFormatterCleanups;
    private JList<?> actionsList;
    private JComboBox<?> formattersCombobox;
    private JComboBox<String> selectFieldCombobox;
    private JButton addButton;
    private JTextArea descriptionAreaText;
    private JButton removeButton;
    private JButton resetButton;
    private JButton recommendButton;

    private final FieldFormatterCleanups defaultFormatters;
    private List<Formatter> availableFormatters;


    public FieldFormatterCleanupsPanel(String description, FieldFormatterCleanups defaultFormatters) {
        this.defaultFormatters = Objects.requireNonNull(defaultFormatters);
        cleanupEnabled = new JCheckBox(description);
        availableFormatters = Cleanups.getBuiltInFormatters();
        availableFormatters.add(new ProtectTermsFormatter(Globals.protectedTermsLoader));
    }

    public void setValues(MetaData metaData) {
        Objects.requireNonNull(metaData);
        Optional<FieldFormatterCleanups> saveActions = metaData.getSaveActions();
        setValues(saveActions.orElse(Cleanups.DEFAULT_SAVE_ACTIONS));
    }

    public void setValues(FieldFormatterCleanups formatterCleanups) {
        fieldFormatterCleanups = formatterCleanups;

        // first clear existing content
        this.removeAll();

        List<FieldFormatterCleanup> configuredActions = fieldFormatterCleanups.getConfiguredActions();
        //The copy is necessary because the original List is unmodifiable
        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions);
        buildLayout(actionsToDisplay);

    }

    private void buildLayout(List<FieldFormatterCleanup> actionsToDisplay) {
        FormBuilder builder = FormBuilder.create().layout(new FormLayout(
                "left:pref, 13dlu, left:pref:grow, 4dlu, pref, 4dlu, pref",
                "pref, 2dlu, pref, 2dlu, pref, 4dlu, pref, 2dlu, fill:pref:grow, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu"));
        builder.add(cleanupEnabled).xyw(1, 1, 7);

        actionsList = new JList<>(new CleanupActionsListModel(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionsList.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                CleanupActionsListModel m = (CleanupActionsListModel) actionsList.getModel();
                int index = actionsList.locationToIndex(e.getPoint());
                if (index > -1) {
                    actionsList.setToolTipText(m.getElementAt(index).getFormatter().getDescription());
                }
            }
        });

        actionsList.getModel().addListDataListener(new ListDataListener() {

            @Override
            public void intervalRemoved(ListDataEvent e) {
                //index0 is sufficient, because of SingleSelection
                if (e.getIndex0() == 0) {
                    //when an item gets deleted, the next one becomes the new 0
                    actionsList.setSelectedIndex(e.getIndex0());
                }
                if (e.getIndex0() > 0) {
                    actionsList.setSelectedIndex(e.getIndex0() - 1);
                }

            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                //empty, not needed

            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                //empty, not needed

            }
        });

        builder.add(actionsList).xyw(3, 5, 5);

        resetButton = new JButton(Localization.lang("Reset"));
        resetButton.addActionListener(e -> ((CleanupActionsListModel) actionsList.getModel()).reset(defaultFormatters));

        BibDatabaseContext databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext();

        recommendButton = new JButton(Localization.lang("Recommended for %0", databaseContext.getMode().getFormattedName()));
        boolean isBiblatex = databaseContext.isBiblatexMode();

        recommendButton.addActionListener(e -> {
            if (isBiblatex) {
                ((CleanupActionsListModel) actionsList.getModel()).reset(Cleanups.RECOMMEND_BIBLATEX_ACTIONS);
            } else {
                ((CleanupActionsListModel) actionsList.getModel()).reset(Cleanups.RECOMMEND_BIBTEX_ACTIONS);
            }
        });

        removeButton = new JButton(Localization.lang("Remove selected"));
        removeButton.addActionListener(
                e -> ((CleanupActionsListModel) actionsList.getModel()).removeAtIndex(actionsList.getSelectedIndex()));

        builder.add(removeButton).xy(7, 11);
        builder.add(resetButton).xy(3, 11);
        builder.add(recommendButton).xy(5, 11);
        builder.add(getSelectorPanel()).xyw(3, 15, 5);

        makeDescriptionTextAreaLikeJLabel();
        builder.add(descriptionAreaText).xyw(3, 17, 5);
        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);

        updateDescription();

        // make sure the layout is set according to the checkbox
        cleanupEnabled.addActionListener(new EnablementStatusListener(fieldFormatterCleanups.isEnabled()));
        cleanupEnabled.setSelected(fieldFormatterCleanups.isEnabled());

    }

    /**
     * Create a TextArea that looks and behaves like a JLabel. Has the advantage of supporting multine and wordwrap
     */
    private void makeDescriptionTextAreaLikeJLabel() {

        descriptionAreaText = new JTextArea(DESCRIPTION);
        descriptionAreaText.setLineWrap(true);
        descriptionAreaText.setWrapStyleWord(true);
        descriptionAreaText.setColumns(6);
        descriptionAreaText.setEditable(false);
        descriptionAreaText.setOpaque(false);
        descriptionAreaText.setFocusable(false);
        descriptionAreaText.setCursor(null);
        descriptionAreaText.setFont(UIManager.getFont("Label.font"));

    }

    private void updateDescription() {
        FieldFormatterCleanup formatterCleanup = getFieldFormatterCleanup();
        if (formatterCleanup != null) {
            descriptionAreaText.setText(DESCRIPTION + formatterCleanup.getFormatter().getDescription());
        } else {
            Formatter selectedFormatter = getFieldFormatter();
            if (selectedFormatter != null) {
                descriptionAreaText.setText(DESCRIPTION + selectedFormatter.getDescription());
            } else {
                descriptionAreaText.setText(DESCRIPTION);
            }
        }
    }

    /**
     * This panel contains the two comboboxes and the Add button
     * @return Returns the created JPanel
     */
    private JPanel getSelectorPanel() {
        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("left:pref:grow, 4dlu, left:pref:grow, 4dlu, fill:pref:grow, 4dlu, right:pref",
                        "fill:pref:grow, 2dlu, pref, 2dlu"));

        List<String> fieldNames = InternalBibtexFields.getAllPublicAndInternalFieldNames();
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);

        selectFieldCombobox = new JComboBox<>(allPlusKey);
        selectFieldCombobox.setEditable(true);
        builder.add(selectFieldCombobox).xy(1, 1);

        List<String> formatterNames = availableFormatters.stream()
                .map(Formatter::getName).collect(Collectors.toList());
        List<String> formatterDescriptions = availableFormatters.stream()
                .map(Formatter::getDescription).collect(Collectors.toList());
        formattersCombobox = new JComboBox<>(formatterNames.toArray());
        formattersCombobox.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                if ((-1 < index) && (index < formatterDescriptions.size()) && (value != null)) {
                    setToolTipText(formatterDescriptions.get(index));

                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        formattersCombobox.addItemListener(e -> updateDescription());
        builder.add(formattersCombobox).xy(3, 1);

        addButton = new JButton(Localization.lang("Add"));
        addButton.addActionListener(e -> {
            FieldFormatterCleanup newAction = getFieldFormatterCleanup();
            if (newAction == null) {
                return;
            }

            ((CleanupActionsListModel) actionsList.getModel()).addCleanupAction(newAction);

        });
        builder.add(addButton).xy(5, 1);

        return builder.getPanel();
    }

    public void storeSettings(MetaData metaData) {
        Objects.requireNonNull(metaData);

        FieldFormatterCleanups formatterCleanups = getFormatterCleanups();

        // if all actions have been removed, remove the save actions from the MetaData
        if (formatterCleanups.getConfiguredActions().isEmpty()) {
            metaData.clearSaveActions();
            return;
        }

        metaData.setSaveActions(formatterCleanups);
    }

    public FieldFormatterCleanups getFormatterCleanups() {
        List<FieldFormatterCleanup> actions = ((CleanupActionsListModel) actionsList.getModel()).getAllActions();
        return new FieldFormatterCleanups(cleanupEnabled.isSelected(), actions);
    }

    public boolean hasChanged() {
        return !fieldFormatterCleanups.equals(getFormatterCleanups());
    }

    public boolean isDefaultSaveActions() {
        return Cleanups.DEFAULT_SAVE_ACTIONS.equals(getFormatterCleanups());
    }

    private FieldFormatterCleanup getFieldFormatterCleanup() {
        Formatter selectedFormatter = getFieldFormatter();

        String fieldKey = selectFieldCombobox.getSelectedItem().toString();
        return new FieldFormatterCleanup(fieldKey, selectedFormatter);

    }

    private Formatter getFieldFormatter() {
        Formatter selectedFormatter = null;
        String selectedFormatterName = formattersCombobox.getSelectedItem().toString();
        for (Formatter formatter : availableFormatters) {
            if (formatter.getName().equals(selectedFormatterName)) {
                selectedFormatter = formatter;
                break;
            }
        }
        return selectedFormatter;
    }

    class EnablementStatusListener implements ActionListener {

        public EnablementStatusListener(boolean initialStatus) {
            setStatus(initialStatus);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean enablementStatus = cleanupEnabled.isSelected();
            setStatus(enablementStatus);

        }

        private void setStatus(boolean status) {
            actionsList.setEnabled(status);
            selectFieldCombobox.setEnabled(status);
            formattersCombobox.setEnabled(status);
            addButton.setEnabled(status);
            removeButton.setEnabled(status);
            resetButton.setEnabled(status);
            recommendButton.setEnabled(status);

        }
    }

}
