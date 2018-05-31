package org.jabref.gui.importer.actions;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.customentrytypes.CustomEntryTypesManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;

/**
 * This action checks whether any new custom entry types were loaded from this
 * BIB file. If so, an offer to remember these entry types is given.
 */
public class CheckForNewEntryTypesAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult) {
        return !getListOfUnknownAndUnequalCustomizations(parserResult).isEmpty();
    }

    @Override
    public void performAction(BasePanel panel, ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        List<EntryType> typesToStore = determineEntryTypesToSave(panel, getListOfUnknownAndUnequalCustomizations(parserResult), mode);

        if (!typesToStore.isEmpty()) {
            typesToStore.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            CustomEntryTypesManager.saveCustomEntryTypes(Globals.prefs);
        }
    }

    private List<EntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        return parserResult.getEntryTypes().values().stream()
                .filter(type ->
                    (!EntryTypes.getType(type.getName(), mode).isPresent())
                        || !EntryTypes.isEqualNameAndFieldBased(type, EntryTypes.getType(type.getName(), mode).get()))
                .collect(Collectors.toList());
    }

    private List<EntryType> determineEntryTypesToSave(BasePanel panel, List<EntryType> allCustomizedEntryTypes, BibDatabaseMode databaseMode) {
        List<EntryType> newTypes = new ArrayList<>();
        List<EntryType> differentCustomizations = new ArrayList<>();

        for (EntryType customType : allCustomizedEntryTypes) {
            if (!EntryTypes.getType(customType.getName(), databaseMode).isPresent()) {
                newTypes.add(customType);
            } else {
                EntryType currentlyStoredType = EntryTypes.getType(customType.getName(), databaseMode).get();
                if (!EntryTypes.isEqualNameAndFieldBased(customType, currentlyStoredType)) {
                    differentCustomizations.add(customType);
                }
            }
        }

        Map<EntryType, JCheckBox> typeCheckBoxMap = new HashMap<>();

        JPanel checkboxPanel = createCheckBoxPanel(newTypes, differentCustomizations, typeCheckBoxMap);

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                checkboxPanel,
                Localization.lang("Custom entry types"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (answer == JOptionPane.YES_OPTION) {
            return typeCheckBoxMap.entrySet().stream().filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }

    }

    private JPanel createCheckBoxPanel(List<EntryType> newTypes, List<EntryType> differentCustomizations,
            Map<EntryType, JCheckBox> typeCheckBoxMap) {
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.PAGE_AXIS));

        JLabel customFoundLabel = new JLabel(Localization.lang("Custom entry types found in file") + ".");
        Font boldStandardFont = new Font(customFoundLabel.getFont().getFontName(), Font.BOLD, customFoundLabel.getFont().getSize());
        customFoundLabel.setFont(boldStandardFont);
        checkboxPanel.add(customFoundLabel);

        JLabel selectLabel = new JLabel(Localization.lang("Select all customized types to be stored in local preferences") + ":");
        selectLabel.setFont(boldStandardFont);
        checkboxPanel.add(selectLabel);

        checkboxPanel.add(new JLabel(" "));

        // add all unknown types:
        if (!newTypes.isEmpty()) {
            checkboxPanel.add(new JLabel(Localization.lang("Currently unknown") + ":"));
            for (EntryType type : newTypes) {
                JCheckBox box = new JCheckBox(type.getName(), true);
                checkboxPanel.add(box);
                typeCheckBoxMap.put(type, box);
            }
        }

        // add all different customizations
        if (!differentCustomizations.isEmpty()) {
            checkboxPanel.add(new JLabel(Localization.lang("Different customization, current settings will be overwritten") + ":"));
            for (EntryType type : differentCustomizations) {
                JCheckBox box = new JCheckBox(type.getName(), true);
                checkboxPanel.add(box);
                typeCheckBoxMap.put(type, box);
            }
        }
        return checkboxPanel;
    }

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult) {
        return parserResult.getMetaData().getMode().orElse(Globals.prefs.getDefaultBibDatabaseMode());
    }
}
