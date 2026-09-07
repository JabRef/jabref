package org.jabref.gui.importer.actions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.migrations.ConvertLegacyExplicitGroups;
import org.jabref.migrations.ConvertMarkingToGroups;
import org.jabref.migrations.PostOpenMigration;
import org.jabref.migrations.SpecialFieldsToSeparateFields;

import org.jspecify.annotations.NullMarked;

/// Offers the conversion of libraries written by older JabRef versions to the current data format.
///
/// Every [PostOpenMigration] that would change the library is listed with a check box, so the user sees
/// what JabRef is about to rewrite and can keep the old data where the old format is still written.
@NullMarked
public class LibraryMigrationAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        return !getNecessaryMigrations(parserResult, preferences).isEmpty();
    }

    /// [impl->req~import.legacy-library-migration~1]
    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        Map<PostOpenMigration, CheckBox> checkBoxes = new LinkedHashMap<>();
        VBox content = new VBox(10);
        content.setPrefWidth(600);
        content.getChildren().add(wrappingLabel(Localization.lang("This library uses data formats of older JabRef versions. Select the conversions to perform.")));
        for (PostOpenMigration migration : getNecessaryMigrations(parserResult, preferences)) {
            CheckBox checkBox = new CheckBox(migration.getDescription());
            // Descriptions name fields such as "__markedentry"; an underscore must not become a mnemonic marker
            checkBox.setMnemonicParsing(false);
            checkBox.setSelected(true);
            checkBox.setDisable(!migration.isOptional());
            checkBox.setWrapText(true);
            checkBoxes.put(migration, checkBox);
            content.getChildren().add(checkBox);
        }
        if (checkBoxes.keySet().stream().anyMatch(migration -> !migration.isOptional())) {
            content.getChildren().add(wrappingLabel(Localization.lang("Disabled conversions are always performed, because JabRef no longer writes the old format.")));
        }
        content.getChildren().add(wrappingLabel(Localization.lang("Deselected conversions are remembered in the library and not offered again.")));
        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(content);

        ButtonType migrate = new ButtonType(Localization.lang("Migrate"), ButtonBar.ButtonData.OK_DONE);
        ButtonType keepAsIs = new ButtonType(Localization.lang("Keep as is"), ButtonBar.ButtonData.CANCEL_CLOSE);
        String title = Localization.lang("Migration of %0", parserResult.getPath().map(Path::toString).orElse(""));
        boolean migrateSelected = dialogService.showCustomDialogAndWait(title, dialogPane, migrate, keepAsIs)
                                               .filter(migrate::equals)
                                               .isPresent();

        List<String> skippedMigrations = new ArrayList<>(parserResult.getMetaData().getSkippedMigrations());
        for (Map.Entry<PostOpenMigration, CheckBox> entry : checkBoxes.entrySet()) {
            PostOpenMigration migration = entry.getKey();
            if (!migration.isOptional() || (migrateSelected && entry.getValue().isSelected())) {
                migration.performMigration(parserResult);
            } else {
                skippedMigrations.add(migration.getId());
            }
        }
        // The declined conversions are part of the library, so the choice survives a reopen
        parserResult.getMetaData().setSkippedMigrations(skippedMigrations);
        parserResult.setChangedOnMigration(true);
    }

    static List<PostOpenMigration> getNecessaryMigrations(ParserResult parserResult, CliPreferences preferences) {
        Character keywordSeparator = parserResult.getDatabaseContext().getKeywordSeparator(preferences.getBibEntryPreferences().getKeywordSeparator());
        List<String> skippedMigrations = parserResult.getMetaData().getSkippedMigrations();
        return Stream.of(
                             new ConvertLegacyExplicitGroups(),
                             new ConvertMarkingToGroups(),
                             new SpecialFieldsToSeparateFields(keywordSeparator))
                     // A stored skip never silences a mandatory conversion: without it, saving would lose data
                     .filter(migration -> !migration.isOptional() || !skippedMigrations.contains(migration.getId()))
                     .filter(migration -> migration.isMigrationNecessary(parserResult))
                     .toList();
    }

    private static Label wrappingLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}
