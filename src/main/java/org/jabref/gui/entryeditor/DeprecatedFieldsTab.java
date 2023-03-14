package org.jabref.gui.entryeditor;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class DeprecatedFieldsTab extends FieldsEditorTab {

    private final BibEntryTypesManager entryTypesManager;

    public DeprecatedFieldsTab(BibDatabaseContext databaseContext,
                               SuggestionProviders suggestionProviders,
                               UndoManager undoManager,
                               DialogService dialogService,
                               PreferencesService preferences,
                               StateManager stateManager,
                               ThemeManager themeManager,
                               IndexingTaskManager indexingTaskManager,
                               BibEntryTypesManager entryTypesManager,
                               TaskExecutor taskExecutor,
                               JournalAbbreviationRepository journalAbbreviationRepository) {
        super(false, databaseContext, suggestionProviders, undoManager, dialogService, preferences, stateManager, themeManager, taskExecutor, journalAbbreviationRepository, indexingTaskManager);
        this.entryTypesManager = entryTypesManager;

        setText(Localization.lang("Deprecated fields"));
        EasyBind.subscribe(preferences.getGeneralPreferences().showAdvancedHintsProperty(), advancedHints -> {
            if (advancedHints) {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.\nFor instance, the publication month should be part of the date field.\nUse the Cleanup Entries functionality to convert the entry to biblatex.")));
            } else {
                setTooltip(new Tooltip(Localization.lang("Shows fields having a successor in biblatex.")));
            }
        });
        setGraphic(IconTheme.JabRefIcons.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Set<Field> determineFieldsToShow(BibEntry entry) {
        BibDatabaseMode mode = databaseContext.getMode();
        Optional<BibEntryType> entryType = entryTypesManager.enrich(entry.getType(), mode);
        if (entryType.isPresent()) {
            return entryType.get().getDeprecatedFields(mode).stream().filter(field -> !entry.getField(field).isEmpty()).collect(Collectors.toSet());
        } else {
            // Entry type unknown -> treat all fields as required
            return Collections.emptySet();
        }
    }
}
