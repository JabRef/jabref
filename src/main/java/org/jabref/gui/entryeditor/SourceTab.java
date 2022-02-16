package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
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
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.ObservableRuleBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceTab.class);
    private final FieldWriterPreferences fieldWriterPreferences;
    private final BibDatabaseMode mode;
    private final UndoManager undoManager;
    private final ObjectProperty<ValidationMessage> sourceIsValid = new SimpleObjectProperty<>();
    private final ObservableRuleBasedValidator sourceValidator = new ObservableRuleBasedValidator();
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private Optional<Pattern> searchHighlightPattern = Optional.empty();
    private final KeyBindingRepository keyBindingRepository;
    private CodeArea codeArea;

    private BibEntry previousEntry;

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

    public SourceTab(BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager, FieldWriterPreferences fieldWriterPreferences, ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor, DialogService dialogService, StateManager stateManager, KeyBindingRepository keyBindingRepository) {
        this.mode = bibDatabaseContext.getMode();
        this.setText(Localization.lang("%0 source", mode.getFormattedName()));
        this.setTooltip(new Tooltip(Localization.lang("Show/edit %0 source", mode.getFormattedName())));
        this.setGraphic(IconTheme.JabRefIcons.SOURCE.getGraphicNode());
        this.undoManager = undoManager;
        this.fieldWriterPreferences = fieldWriterPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.keyBindingRepository = keyBindingRepository;

        stateManager.activeSearchQueryProperty().addListener((observable, oldValue, newValue) -> {
            searchHighlightPattern = newValue.flatMap(SearchQuery::getPatternForWords);
            highlightSearchPattern();
        });
    }

    private void highlightSearchPattern() {
        if (searchHighlightPattern.isPresent() && (codeArea != null)) {
            codeArea.setStyleClass(0, codeArea.getLength(), "text");
            Matcher matcher = searchHighlightPattern.get().matcher(codeArea.getText());
            while (matcher.find()) {
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    codeArea.setStyleClass(matcher.start(), matcher.end(), "search");
                }
            }
        }
    }

    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldWriterPreferences fieldWriterPreferences) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldWriterPreferences);
        new BibEntryWriter(fieldWriter, Globals.entryTypesManager).write(entry, bibWriter, type);
        return writer.toString();
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
            return;
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

        ActionFactory factory = new ActionFactory(keyBindingRepository);
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
                sourceValidationStatus.getHighestMessage().ifPresent(message ->
                        dialogService.showErrorDialogAndWait(message.getMessage()));
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
        DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> {
            if (codeArea == null) {
                setupSourceEditor();
            }

            codeArea.clear();
            try {
                codeArea.appendText(getSourceString(currentEntry, mode, fieldWriterPreferences));
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
                    throw new IllegalStateException(parserResult.warnings().get(0));
                } else {
                    throw new IllegalStateException("No entries found.");
                }
            }

            if (parserResult.hasWarnings()) {
                // put the warning into as exception text -> it will be displayed to the user

                throw new IllegalStateException(parserResult.getErrorMessage());
            }

            NamedCompound compound = new NamedCompound(Localization.lang("source edit"));
            BibEntry newEntry = database.getEntries().get(0);
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
                    new FieldWriter(fieldWriterPreferences).write(fieldName, newValue);

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
                case SAVE_DATABASE, SAVE_ALL, SAVE_DATABASE_AS -> {
                    storeSource(currentEntry, codeArea.textProperty().getValue());
                }
            }
        });
    }

}
