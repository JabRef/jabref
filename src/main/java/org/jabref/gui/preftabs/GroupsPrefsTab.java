package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class GroupsPrefsTab extends JPanel implements PrefsTab {

    private final JCheckBox hideNonHits = new JCheckBox(Localization.lang("Hide non-hits"));
    private final JCheckBox grayOut = new JCheckBox(Localization.lang("Gray out non-hits"));
    private final JCheckBox autoAssignGroup = new JCheckBox(
            Localization.lang("Automatically assign new entry to selected groups"));
    private final JRadioButton multiSelectionModeIntersection = new JRadioButton(Localization.lang("Intersection"));
    private final JRadioButton multiSelectionModeUnion = new JRadioButton(Localization.lang("Union"));

    private final JTextField groupingField = new JTextField(20);
    private final JTextField keywordSeparator = new JTextField(2);

    private final JabRefPreferences prefs;


    public GroupsPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;

        keywordSeparator.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                keywordSeparator.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                // deselection is automatic
            }
        });

        ButtonGroup hideMode = new ButtonGroup();
        hideMode.add(grayOut);
        hideMode.add(hideNonHits);

        ButtonGroup multiSelectionMode = new ButtonGroup();
        multiSelectionMode.add(multiSelectionModeIntersection);
        multiSelectionMode.add(multiSelectionModeUnion);
        multiSelectionModeIntersection.setToolTipText(Localization.lang("Display only entries belonging to all selected groups."));
        multiSelectionModeUnion.setToolTipText(Localization.lang("Display all entries belonging to one or more of the selected groups."));


        FormLayout layout = new FormLayout("9dlu, pref", //500px",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.appendSeparator(Localization.lang("View"));
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        builder.append(hideNonHits);
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        builder.append(grayOut);
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        builder.append(multiSelectionModeIntersection);
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        builder.append(multiSelectionModeUnion);
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        builder.append(autoAssignGroup);
        builder.nextLine();
        builder.nextLine();
        builder.appendSeparator(Localization.lang("Dynamic groups"));
        builder.nextLine();
        builder.nextLine();
        builder.nextColumn();
        // build subcomponent
        FormLayout layout2 = new FormLayout("left:pref, 2dlu, left:pref",
                "p, 3dlu, p");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        builder2.append(new JLabel(Localization.lang("Default grouping field") + ":"));
        builder2.append(groupingField);
        builder2.nextLine();
        builder2.nextLine();
        builder2.append(new JLabel(Localization.lang("When adding/removing keywords, separate them by") + ":"));
        builder2.append(keywordSeparator);
        builder.append(builder2.getPanel());

        setLayout(new BorderLayout());
        JPanel panel = builder.getPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        grayOut.setSelected(prefs.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS));
        groupingField.setText(prefs.get(JabRefPreferences.GROUPS_DEFAULT_FIELD));
        keywordSeparator.setText(prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
        autoAssignGroup.setSelected(prefs.getBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP));
        multiSelectionModeIntersection.setSelected(prefs.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS));
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.GRAY_OUT_NON_HITS, grayOut.isSelected());
        prefs.put(JabRefPreferences.GROUPS_DEFAULT_FIELD, groupingField.getText().trim());
        prefs.putBoolean(JabRefPreferences.AUTO_ASSIGN_GROUP, autoAssignGroup.isSelected());
        prefs.put(JabRefPreferences.KEYWORD_SEPARATOR, keywordSeparator.getText());
        prefs.putBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS, multiSelectionModeIntersection.isSelected());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Groups");
    }

}
