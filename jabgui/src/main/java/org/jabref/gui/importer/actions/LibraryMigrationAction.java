package org.jabref.gui.importer.actions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javafx.geometry.HPos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
/// Every [PostOpenMigration] that would change the library is listed, so the user sees what JabRef is about
/// to rewrite. Conversions whose old format is still written get a check box to keep the old data.
@NullMarked
public class LibraryMigrationAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        return !getNecessaryMigrations(parserResult, preferences).isEmpty();
    }

    /// [impl->req~import.legacy-library-migration~1]
    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        List<PostOpenMigration> migrations = getNecessaryMigrations(parserResult, preferences);
        List<PostOpenMigration> mandatoryMigrations = migrations.stream().filter(migration -> !migration.isOptional()).toList();
        List<PostOpenMigration> optionalMigrations = migrations.stream().filter(PostOpenMigration::isOptional).toList();
        Map<PostOpenMigration, CheckBox> checkBoxes = new LinkedHashMap<>();

        VBox content = new VBox(10);
        content.setPrefWidth(600);
        content.getChildren().add(wrappingLabel(Localization.lang("This library uses data formats of older JabRef versions.")));
        if (!mandatoryMigrations.isEmpty()) {
            content.getChildren().add(headerLabel(Localization.lang("Always performed, because JabRef no longer writes the old format")));
            mandatoryMigrations.forEach(migration -> content.getChildren().add(wrappingLabel(migration.getDescription())));
        }
        if (!optionalMigrations.isEmpty()) {
            GridPane table = new GridPane(10, 10);
            ColumnConstraints descriptionColumn = new ColumnConstraints();
            descriptionColumn.setHgrow(Priority.ALWAYS);
            ColumnConstraints performColumn = new ColumnConstraints();
            performColumn.setHalignment(HPos.CENTER);
            // Otherwise the long descriptions squeeze the column and its header wraps letter by letter
            performColumn.setMinWidth(Region.USE_PREF_SIZE);
            table.getColumnConstraints().addAll(descriptionColumn, performColumn);
            table.addRow(0, headerLabel(Localization.lang("Migration")), headerLabel(Localization.lang("Perform")));
            for (PostOpenMigration migration : optionalMigrations) {
                CheckBox checkBox = new CheckBox();
                checkBox.setSelected(true);
                checkBox.setAccessibleText(migration.getDescription());
                checkBoxes.put(migration, checkBox);
                table.addRow(table.getRowCount(), wrappingLabel(migration.getDescription()), checkBox);
            }
            content.getChildren().add(table);
            content.getChildren().add(wrappingLabel(Localization.lang("Deselected conversions are remembered in the library and not offered again.")));
        }
        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(content);

        ButtonType migrate = new ButtonType(Localization.lang("Migrate"), ButtonBar.ButtonData.OK_DONE);
        ButtonType keepAsIs = new ButtonType(Localization.lang("Keep as is"), ButtonBar.ButtonData.CANCEL_CLOSE);
        // Without an optional conversion there is nothing to keep
        ButtonType[] buttons = checkBoxes.isEmpty() ? new ButtonType[] {migrate} : new ButtonType[] {migrate, keepAsIs};
        String title = Localization.lang("Migration of %0", parserResult.getPath().map(Path::toString).orElse(""));
        boolean migrateSelected = dialogService.showCustomDialogAndWait(title, dialogPane, buttons)
                                               .filter(migrate::equals)
                                               .isPresent();

        mandatoryMigrations.forEach(migration -> migration.performMigration(parserResult));
        List<String> skippedMigrations = new ArrayList<>(parserResult.getMetaData().getSkippedMigrations());
        for (Map.Entry<PostOpenMigration, CheckBox> entry : checkBoxes.entrySet()) {
            PostOpenMigration migration = entry.getKey();
            if (migrateSelected && entry.getValue().isSelected()) {
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
                             new ConvertMarkingToGroups(keywordSeparator),
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

    private static Label headerLabel(String text) {
        Label label = wrappingLabel(text);
        label.getStyleClass().add("bold");
        return label;
    }
}
