package org.jabref.gui.importer.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.customentrytypes.CustomEntryTypesManager;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;

import org.controlsfx.control.CheckListView;

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
    public void performAction(BasePanel panel, ParserResult parserResult, DialogService dialogService) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);
        List<EntryType> typesToStore = determineEntryTypesToSave(panel, getListOfUnknownAndUnequalCustomizations(parserResult), mode, dialogService);

        if (!typesToStore.isEmpty()) {
            typesToStore.forEach(type -> EntryTypes.addOrModifyCustomEntryType((CustomEntryType) type, mode));
            CustomEntryTypesManager.saveCustomEntryTypes(Globals.prefs);
        }
    }

    private List<EntryType> getListOfUnknownAndUnequalCustomizations(ParserResult parserResult) {
        BibDatabaseMode mode = getBibDatabaseModeFromParserResult(parserResult);

        return parserResult.getEntryTypes().values().stream()
                           .filter(type -> (!EntryTypes.getType(type.getName(), mode).isPresent())
                                           || !EntryTypes.isEqualNameAndFieldBased(type, EntryTypes.getType(type.getName(), mode).get()))
                           .collect(Collectors.toList());
    }

    private List<EntryType> determineEntryTypesToSave(BasePanel panel, List<EntryType> allCustomizedEntryTypes, BibDatabaseMode databaseMode, DialogService dialogService) {
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

        DialogPane pane = new DialogPane();

        CheckListView<EntryType> unknownEntryTypesCheckList = new CheckListView<>(FXCollections.observableArrayList(newTypes));

        VBox vbox = new VBox();
        vbox.getChildren().add(new Label(Localization.lang("Select all customized types to be stored in local preferences") + ":"));
        vbox.getChildren().add(new Label(Localization.lang("Currently unknown")));
        vbox.getChildren().add(unknownEntryTypesCheckList);

        Optional<CheckListView<EntryType>> differentCustomizationCheckList = Optional.empty();
        if (!differentCustomizations.isEmpty()) {

            differentCustomizationCheckList = Optional.of(new CheckListView<>(FXCollections.observableArrayList(differentCustomizations)));

            vbox.getChildren().add(new Label(Localization.lang("Different customization, current settings will be overwritten") + ":"));
            vbox.getChildren().add(differentCustomizationCheckList.get());
        }

        pane.setContent(vbox);

        Optional<ButtonType> buttonPressed = dialogService.showCustomDialogAndWait(Localization.lang("Custom entry types"), pane, ButtonType.OK, ButtonType.CANCEL);
        if (buttonPressed.isPresent() && (buttonPressed.get() == ButtonType.OK)) {

            List<EntryType> differentCustomizationSelected = new ArrayList<>();
            differentCustomizationCheckList.map(view -> view.getCheckModel().getCheckedItems()).ifPresent(differentCustomizationSelected::addAll);

            List<EntryType> selectedUnknown = unknownEntryTypesCheckList.getCheckModel().getCheckedItems();

            return Stream.concat(selectedUnknown.stream(), differentCustomizationSelected.stream()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /*
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
    }*/

    private BibDatabaseMode getBibDatabaseModeFromParserResult(ParserResult parserResult) {
        return parserResult.getMetaData().getMode().orElse(Globals.prefs.getDefaultBibDatabaseMode());
    }
}
