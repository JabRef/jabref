package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.bibtexhighlighter.BibTeXHighlighter;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.Range;

import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.ObservableRuleBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import jfx.incubator.scene.control.richtext.CodeArea;

import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceTab.class);
    private final FieldPreferences fieldPreferences;
    private final UndoManager undoManager;
    private final ObjectProperty<ValidationMessage> validationMessage = new SimpleObjectProperty<>();
    private final ObservableRuleBasedValidator sourceValidator = new ObservableRuleBasedValidator();
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;
    private final DialogService dialogService;
    private final BibEntryTypesManager entryTypesManager;
    private final KeyBindingRepository keyBindingRepository;
    private final StateManager stateManager;
    private Map<Field, Range> fieldPositions;
    private CodeArea codeArea;
    private BibEntry previousEntry;
    private final BibTeXHighlighter bibTeXHighlighter;

    public SourceTab(CountingUndoManager undoManager,
                     FieldPreferences fieldPreferences,
                     ImportFormatPreferences importFormatPreferences,
                     FileUpdateMonitor fileMonitor,
                     DialogService dialogService,
                     BibEntryTypesManager entryTypesManager,
                     KeyBindingRepository keyBindingRepository,
                     StateManager stateManager,
                     BibTeXHighlighter bibTeXHighlighter) {
        this.stateManager = stateManager;
        this.bibTeXHighlighter = bibTeXHighlighter;
        this.setGraphic(IconTheme.JabRefIcons.SOURCE.getGraphicNode());
        this.undoManager = undoManager;
        this.fieldPreferences = fieldPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
        this.dialogService = dialogService;
        this.entryTypesManager = entryTypesManager;
        this.keyBindingRepository = keyBindingRepository;

        EasyBind.subscribe(stateManager.activeTabProperty(), library -> {
            if (library.isEmpty()) {
                this.setText(Localization.lang("Source"));
                this.setTooltip(new Tooltip(Localization.lang("Show/edit source")));
            } else {
                BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                                   .orElse(BibDatabaseMode.BIBLATEX);
                this.setText(Localization.lang("%0 source", mode.getFormattedName()));
                this.setTooltip(new Tooltip(Localization.lang("Show/edit %0 source", mode.getFormattedName())));
            }
        });
        EasyBind.subscribe(stateManager.searchQueryProperty(), _ -> Platform.runLater(this::refreshCodeAreaDecorator));
    }

    private void refreshCodeAreaDecorator() {
        if (codeArea == null) {
            return;
        }
        SyntaxDecorator currentDecorator = codeArea.getSyntaxDecorator();
        codeArea.setSyntaxDecorator(null);
        codeArea.setSyntaxDecorator(currentDecorator);
    }

    /// Method similar to [BibEntry#getStringRepresentation(BibEntry, BibDatabaseMode, BibEntryTypesManager, FieldPreferences)]. This method additionally updates [#fieldPositions].
    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldPreferences fieldPreferences) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            BibWriter bibWriter = new BibWriter(writer, "\n"); // JavaFX works with LF only
            FieldWriter fieldWriter = new FieldWriter(fieldPreferences);
            BibEntryWriter bibEntryWriter = new BibEntryWriter(fieldWriter, entryTypesManager);
            bibEntryWriter.write(entry, bibWriter, type, true);
            fieldPositions = bibEntryWriter.getFieldPositions();
            return writer.toString();
        }
    }

    private void setupSourceEditor() {
        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setOnInputMethodTextChanged(event -> {
            String committed = event.getCommitted();
            if (!committed.isEmpty()) {
                TextPos caretPos = codeArea.getCaretPosition();
                codeArea.getModel().replace(null, caretPos, caretPos, committed);
            }
        });
        codeArea.getStyleClass().add("bibtex-code-area");

        //codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> CodeAreaKeyBindings.call(codeArea, event, keyBindingRepository));
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::listenForSaveKeybinding);

        codeArea.setSyntaxDecorator(bibTeXHighlighter);

        ActionFactory factory = new ActionFactory();
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.CUT, new EditAction(StandardActions.CUT)),
                factory.createMenuItem(StandardActions.COPY, new EditAction(StandardActions.COPY)),
                factory.createMenuItem(StandardActions.PASTE, new EditAction(StandardActions.PASTE)),
                factory.createMenuItem(StandardActions.SELECT_ALL, new EditAction(StandardActions.SELECT_ALL))
        );

        contextMenu.getStyleClass().add("context-menu");
        codeArea.setContextMenu(contextMenu);

        sourceValidator.addRule(validationMessage);

        codeArea.focusedProperty().addListener((_, _, onFocus) -> {
            if (!onFocus && (getCurrentEntry() != null)) {
                storeSource(getCurrentEntry(), codeArea.getText());
            }
        });

        this.setContent(codeArea);
    }

    private void updateCodeArea() {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            if (codeArea == null) {
                setupSourceEditor();
            }

            BibDatabaseMode mode = stateManager.getActiveDatabase().map(BibDatabaseContext::getMode)
                                               .orElse(BibDatabaseMode.BIBLATEX);
            try {
                codeArea.clear();
                codeArea.appendText(getSourceString(getCurrentEntry(), mode, fieldPreferences));
                codeArea.setEditable(true);
                Platform.runLater(this::refreshCodeAreaDecorator);
            } catch (IOException ex) {
                codeArea.setEditable(false);
                codeArea.appendText(ex.getMessage() + "\n\n" +
                        Localization.lang("Correct the entry, and reopen editor to display/edit source."));
                LOGGER.debug("Incorrect entry", ex);
            }
        });
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if ((previousEntry != null) && (codeArea != null)) {
            storeSource(previousEntry, codeArea.getText());
        }
        this.previousEntry = entry;

        updateCodeArea();

        entry.typeProperty().addListener(_ -> updateCodeArea());
        entry.getFieldsObservable().addListener((InvalidationListener) _ -> updateCodeArea());
    }

    private void storeSource(BibEntry outOfFocusEntry, String text) {
        if ((outOfFocusEntry == null) || text.isEmpty()) {
            return;
        }

        BibtexParser bibtexParser = new BibtexParser(importFormatPreferences, fileMonitor);
        ParserResult parserResult;
        try {
            parserResult = bibtexParser.parse(Reader.of(text));
        } catch (IOException ex) {
            validationMessage.setValue(ValidationMessage.error(Localization.lang("Failed to parse Bib(La)TeX: %0", ex.getMessage())));
            LOGGER.debug("Incorrect source", ex);
            return;
        }
        BibDatabase database = parserResult.getDatabase();

        if (database.getEntryCount() > 1) {
            LOGGER.error("More than one entry found.");
            // We use the error dialog as the notification is hidden
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Problem with parsing entry"),
                    Localization.lang("Parsing failed because more than one entry was found. Please check your BibTeX syntax.")
            );
            return;
        }

        if (!database.hasEntries()) {
            if (parserResult.hasWarnings()) {
                LOGGER.warn("Could not store entry: {}", parserResult.warnings());
                String errors = parserResult.getErrorMessage();
                dialogService.showErrorDialogAndWait(errors);
                validationMessage.setValue(ValidationMessage.error(Localization.lang("Failed to parse Bib(La)TeX: %0", errors)));
                return;
            } else {
                LOGGER.warn("No entries found.");
                String errors = Localization.lang("No entries available");
                dialogService.showErrorDialogAndWait(errors);
                validationMessage.setValue(ValidationMessage.error(Localization.lang("Failed to parse Bib(La)TeX: %0", errors)));
                return;
            }
        }

        if (parserResult.hasWarnings()) {
            LOGGER.warn("Failed to parse Bib(La)TeX: {}", parserResult.warnings());
            String errors = parserResult.getErrorMessage();
            dialogService.showErrorDialogAndWait(errors);
            validationMessage.setValue(ValidationMessage.error(Localization.lang("Failed to parse Bib(La)TeX: %0", errors)));
        }

        NamedCompoundEdit compound = new NamedCompoundEdit(Localization.lang("source edit"));
        BibEntry newEntry = database.getEntries().getFirst();
        newEntry.getCitationKey()
                .ifPresentOrElse(
                        outOfFocusEntry::setCitationKey,
                        outOfFocusEntry::clearCitationKey);

        // First, remove fields that the user has removed.
        for (Map.Entry<Field, String> field : outOfFocusEntry.getFieldMap().entrySet()) {
            Field fieldName = field.getKey();
            String fieldValue = field.getValue();

            if (!newEntry.hasField(fieldName)) {
                compound.addEdit(new UndoableFieldChange(outOfFocusEntry, fieldName, fieldValue, null));
                outOfFocusEntry.clearField(fieldName);
            }
        }

        // Then set all fields that have been set by the user.
        for (Map.Entry<Field, String> field : newEntry.getFieldMap().entrySet()) {
            Field fieldName = field.getKey();
            String oldValue = outOfFocusEntry.getField(fieldName).orElse(null);
            String newValue = field.getValue();
            if (!Objects.equals(oldValue, newValue)) {
                // Test if the field is legally set.
                List<String> errors = FieldWriter.checkBalancedBraces(newValue);
                if (!errors.isEmpty()) {
                    validationMessage.setValue(ValidationMessage.error(
                            Localization.lang("Failed to parse Bib(La)TeX: %0", String.join("\n", errors))));
                    return;
                }

                compound.addEdit(new UndoableFieldChange(outOfFocusEntry, fieldName, oldValue, newValue));
                outOfFocusEntry.setField(fieldName, newValue);
            }
        }

        // See if the user has changed the entry type:
        if (!Objects.equals(newEntry.getType(), outOfFocusEntry.getType())) {
            compound.addEdit(new UndoableChangeType(outOfFocusEntry, outOfFocusEntry.getType(), newEntry.getType()));
            outOfFocusEntry.setType(newEntry.getType());
        }
        compound.end();
        undoManager.addEdit(compound);

        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            stateManager.activeTabProperty().get().ifPresent(libraryTab ->
                    libraryTab.getMainTable().clearAndSelect(outOfFocusEntry)
            );
        }

        validationMessage.setValue(null);
    }

    private void listenForSaveKeybinding(KeyEvent event) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            switch (binding) {
                case SAVE_LIBRARY,
                     SAVE_ALL,
                     SAVE_LIBRARY_AS ->
                        storeSource(getCurrentEntry(), codeArea.getText());
            }
        });
    }

    private class EditAction extends SimpleCommand {

        private final StandardActions command;

        public EditAction(StandardActions command) {
            this.command = command;
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY ->
                        codeArea.copy();
                case CUT ->
                        codeArea.cut();
                case PASTE ->
                        codeArea.paste();
                case SELECT_ALL ->
                        codeArea.selectAll();
            }
            codeArea.requestFocus();
        }
    }
}

