package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.CodeAreaKeyBindings;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.retrieval.Highlighter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.Range;

import de.saxsys.mvvmfx.utils.validation.ObservableRuleBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceTab.class);
    private static final String TEXT_STYLE = "text";
    private static final String SEARCH_STYLE = "search";
    private final FieldPreferences fieldPreferences;
    private final BibDatabaseMode mode;
    private final UndoManager undoManager;
    private final ObjectProperty<ValidationMessage> sourceIsValid = new SimpleObjectProperty<>();
    private final ObservableRuleBasedValidator sourceValidator = new ObservableRuleBasedValidator();
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;
    private final DialogService dialogService;
    private final BibEntryTypesManager entryTypesManager;
    private final KeyBindingRepository keyBindingRepository;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    private Map<Field, Range> fieldPositions;
    private CodeArea codeArea;
    private BibEntry previousEntry;

    public SourceTab(BibDatabaseContext bibDatabaseContext,
                     CountingUndoManager undoManager,
                     FieldPreferences fieldPreferences,
                     ImportFormatPreferences importFormatPreferences,
                     FileUpdateMonitor fileMonitor,
                     DialogService dialogService,
                     BibEntryTypesManager entryTypesManager,
                     KeyBindingRepository keyBindingRepository,
                     OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        this.mode = bibDatabaseContext.getMode();
        this.setText(Localization.lang("%0 source", mode.getFormattedName()));
        this.setTooltip(new Tooltip(Localization.lang("Show/edit %0 source", mode.getFormattedName())));
        this.setGraphic(IconTheme.JabRefIcons.SOURCE.getGraphicNode());
        this.undoManager = undoManager;
        this.fieldPreferences = fieldPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
        this.dialogService = dialogService;
        this.entryTypesManager = entryTypesManager;
        this.keyBindingRepository = keyBindingRepository;
        this.searchQueryProperty = searchQueryProperty;
        searchQueryProperty.addListener((observable, oldValue, newValue) -> highlightSearchPattern());
    }

    private void highlightSearchPattern() {
        if (codeArea == null || searchQueryProperty.get().isEmpty()) {
            return;
        }

        codeArea.setStyleClass(0, codeArea.getLength(), TEXT_STYLE);
        Map<Optional<Field>, List<String>> searchTermsMap = Highlighter.groupTermsByField(searchQueryProperty.get().get());

        searchTermsMap.forEach((optionalField, terms) -> {
            Optional<String> searchPattern = Highlighter.buildSearchPattern(terms);
            if (searchPattern.isEmpty()) {
                return;
            }

            if (optionalField.isPresent()) {
                highlightField(optionalField.get(), searchPattern.get());
            } else {
                fieldPositions.keySet().forEach(field -> highlightField(field, searchPattern.get()));
            }
        });
    }

    private void highlightField(Field field, String searchPattern) {
        Range fieldPosition = fieldPositions.get(field);
        if (fieldPosition == null) {
            return;
        }

        int start = fieldPosition.start();
        int end = fieldPosition.end();
        List<Range> matchedPositions = Highlighter.findMatchPositions(codeArea.getText(start, end), searchPattern);

        for (Range range : matchedPositions) {
            codeArea.setStyleClass(start + range.start() - 1, start + range.end(), SEARCH_STYLE);
        }
    }

    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldPreferences fieldPreferences) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, "\n"); // JavaFX works with LF only
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldPreferences);
        BibEntryWriter bibEntryWriter = new BibEntryWriter(fieldWriter, entryTypesManager);
        bibEntryWriter.write(entry, bibWriter, type, true);
        fieldPositions = bibEntryWriter.getFieldPositions();
        String sourceString = writer.toString();
        writer.close();
        return sourceString;
    }

    /* Work around for different input methods.
     * https://github.com/FXMisc/RichTextFX/issues/146
     */
    private static class InputMethodRequestsObject implements InputMethodRequests {

        @Override
        public String getSelectedText() {
            return "";
        }

        @Override
        public int getLocationOffset(int x, int y) {
            return 0;
        }

        @Override
        public void cancelLatestCommittedText() {
        }

        @Override
        public Point2D getTextLocation(int offset) {
            return new Point2D(0, 0);
        }
    }

    private void setupSourceEditor() {
        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setInputMethodRequests(new InputMethodRequestsObject());
        codeArea.setOnInputMethodTextChanged(event -> {
            String committed = event.getCommitted();
            if (!committed.isEmpty()) {
                codeArea.insertText(codeArea.getCaretPosition(), committed);
            }
        });
        codeArea.setId("bibtexSourceCodeArea");
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> CodeAreaKeyBindings.call(codeArea, event, keyBindingRepository));
        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, this::listenForSaveKeybinding);

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

        sourceValidator.addRule(sourceIsValid);

        sourceValidator.getValidationStatus().getMessages().addListener((InvalidationListener) c -> {
            ValidationStatus sourceValidationStatus = sourceValidator.getValidationStatus();
            if (!sourceValidationStatus.isValid()) {
                sourceValidationStatus.getHighestMessage().ifPresent(message -> {
                    String content = Localization.lang("User input via entry-editor in `{}bibtex source` tab led to failure.")
                            + "\n" + Localization.lang("Please check your library file for wrong syntax.")
                            + "\n\n" + message.getMessage();
                    dialogService.showWarningDialogAndWait(Localization.lang("SourceTab error"), content);
                });
            }
        });

        codeArea.focusedProperty().addListener((obs, oldValue, onFocus) -> {
            if (!onFocus && (currentEntry != null)) {
                storeSource(currentEntry, codeArea.textProperty().getValue());
            }
        });
        VirtualizedScrollPane<CodeArea> scrollableCodeArea = new VirtualizedScrollPane<>(codeArea);
        this.setContent(scrollableCodeArea);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return true;
    }

    private void updateCodeArea() {
        UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
            if (codeArea == null) {
                setupSourceEditor();
            }

            codeArea.clear();
            try {
                codeArea.appendText(getSourceString(currentEntry, mode, fieldPreferences));
                codeArea.setEditable(true);
                highlightSearchPattern();
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
            storeSource(previousEntry, codeArea.textProperty().getValue());
        }
        this.previousEntry = entry;

        updateCodeArea();

        entry.typeProperty().addListener(listener -> updateCodeArea());
        entry.getFieldsObservable().addListener((InvalidationListener) listener -> updateCodeArea());
    }

    private void storeSource(BibEntry outOfFocusEntry, String text) {
        if ((outOfFocusEntry == null) || text.isEmpty()) {
            return;
        }

        BibtexParser bibtexParser = new BibtexParser(importFormatPreferences, fileMonitor);
        try {
            ParserResult parserResult = bibtexParser.parse(new StringReader(text));
            BibDatabase database = parserResult.getDatabase();

            if (database.getEntryCount() > 1) {
                throw new IllegalStateException("More than one entry found.");
            }

            if (!database.hasEntries()) {
                if (parserResult.hasWarnings()) {
                    // put the warning into as exception text -> it will be displayed to the user
                    throw new IllegalStateException(parserResult.warnings().getFirst());
                } else {
                    throw new IllegalStateException("No entries found.");
                }
            }

            if (parserResult.hasWarnings()) {
                // put the warning into as exception text -> it will be displayed to the user
                throw new IllegalStateException(parserResult.getErrorMessage());
            }

            NamedCompound compound = new NamedCompound(Localization.lang("source edit"));
            BibEntry newEntry = database.getEntries().getFirst();
            String newKey = newEntry.getCitationKey().orElse(null);

            if (newKey != null) {
                outOfFocusEntry.setCitationKey(newKey);
            } else {
                outOfFocusEntry.clearCiteKey();
            }

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
                    new FieldWriter(fieldPreferences).write(fieldName, newValue);

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

            sourceIsValid.setValue(null);
        } catch (InvalidFieldValueException | IllegalStateException | IOException ex) {
            sourceIsValid.setValue(ValidationMessage.error(Localization.lang("Problem with parsing entry") + ": " + ex.getMessage()));
            LOGGER.debug("Incorrect source", ex);
        }
    }

    private void listenForSaveKeybinding(KeyEvent event) {
        keyBindingRepository.mapToKeyBinding(event).ifPresent(binding -> {
            switch (binding) {
                case SAVE_DATABASE, SAVE_ALL, SAVE_DATABASE_AS ->
                        storeSource(currentEntry, codeArea.textProperty().getValue());
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
                case COPY -> codeArea.copy();
                case CUT -> codeArea.cut();
                case PASTE -> codeArea.paste();
                case SELECT_ALL -> codeArea.selectAll();
            }
            codeArea.requestFocus();
        }
    }
}
