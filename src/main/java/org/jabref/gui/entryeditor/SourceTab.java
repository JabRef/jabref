package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.IconTheme;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.ObservableRuleBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import org.controlsfx.control.NotificationPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceTab.class);
    private final LatexFieldFormatterPreferences fieldFormatterPreferences;
    private final BibDatabaseMode mode;
    private final JabRefPreferences preferences;
    private final UndoManager undoManager;
    private final ObjectProperty<ValidationMessage> sourceIsValid = new SimpleObjectProperty<>();
    private final ObservableRuleBasedValidator sourceValidator = new ObservableRuleBasedValidator(sourceIsValid);
    private final FileUpdateMonitor fileMonitor;

    public SourceTab(BibDatabaseContext bibDatabaseContext, CountingUndoManager undoManager, LatexFieldFormatterPreferences fieldFormatterPreferences, JabRefPreferences preferences, FileUpdateMonitor fileMonitor) {
        this.mode = bibDatabaseContext.getMode();
        this.setText(Localization.lang("%0 source", mode.getFormattedName()));
        this.setTooltip(new Tooltip(Localization.lang("Show/edit %0 source", mode.getFormattedName())));
        this.setGraphic(IconTheme.JabRefIcon.SOURCE.getGraphicNode());
        this.undoManager = undoManager;
        this.fieldFormatterPreferences = fieldFormatterPreferences;
        this.preferences = preferences;
        this.fileMonitor = fileMonitor;

    }

    private static String getSourceString(BibEntry entry, BibDatabaseMode type, LatexFieldFormatterPreferences fieldFormatterPreferences) throws IOException {
        StringWriter stringWriter = new StringWriter(200);
        LatexFieldFormatter formatter = LatexFieldFormatter.buildIgnoreHashes(fieldFormatterPreferences);
        new BibEntryWriter(formatter, false).writeWithoutPrependedNewlines(entry, stringWriter, type);

        return stringWriter.getBuffer().toString();
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return true;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        TextArea codeArea = new TextArea();
        ScrollPane scrollPane = new ScrollPane(codeArea);
        NotificationPane notificationPane = new NotificationPane(scrollPane);

        notificationPane.setShowFromTop(false);
        sourceValidator.getValidationStatus().getMessages().addListener((ListChangeListener<ValidationMessage>) c -> {
            if (sourceValidator.getValidationStatus().isValid()) {
                notificationPane.hide();
            } else {
                sourceValidator.getValidationStatus().getHighestMessage().ifPresent(validationMessage -> notificationPane.show(validationMessage.getMessage()));
            }
        });
        this.setContent(codeArea);

        // Store source for every change in the source code
        // and update source code for every change of entry field values
        BindingsHelper.bindContentBidirectional(entry.getFieldsObservable(), codeArea.textProperty(), this::storeSource, fields -> {
            DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> {
                codeArea.clear();
                try {
                    codeArea.setText(getSourceString(entry, mode, fieldFormatterPreferences));
                } catch (IOException ex) {
                    codeArea.setEditable(false);
                    codeArea.appendText(ex.getMessage() + "\n\n" +
                            Localization.lang("Correct the entry, and reopen editor to display/edit source."));
                    LOGGER.debug("Incorrect entry", ex);
                }
            });
        });

    }

    private void storeSource(String text) {
        if ((currentEntry == null) || text.isEmpty()) {
            return;
        }

        BibtexParser bibtexParser = new BibtexParser(preferences.getImportFormatPreferences(), fileMonitor);
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

            NamedCompound compound = new NamedCompound(Localization.lang("source edit"));
            BibEntry newEntry = database.getEntries().get(0);
            String newKey = newEntry.getCiteKeyOptional().orElse(null);

            if (newKey != null) {
                currentEntry.setCiteKey(newKey);
            } else {
                currentEntry.clearCiteKey();
            }

            // First, remove fields that the user has removed.
            for (Map.Entry<String, String> field : currentEntry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String fieldValue = field.getValue();

                if (InternalBibtexFields.isDisplayableField(fieldName) && !newEntry.hasField(fieldName)) {
                    compound.addEdit(
                            new UndoableFieldChange(currentEntry, fieldName, fieldValue, null));
                    currentEntry.clearField(fieldName);
                }
            }

            // Then set all fields that have been set by the user.
            for (Map.Entry<String, String> field : newEntry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String oldValue = currentEntry.getField(fieldName).orElse(null);
                String newValue = field.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    // Test if the field is legally set.
                    new LatexFieldFormatter(preferences.getLatexFieldFormatterPreferences())
                            .format(newValue, fieldName);

                    compound.addEdit(new UndoableFieldChange(currentEntry, fieldName, oldValue, newValue));
                    currentEntry.setField(fieldName, newValue);
                }
            }

            // See if the user has changed the entry type:
            if (!Objects.equals(newEntry.getType(), currentEntry.getType())) {
                compound.addEdit(new UndoableChangeType(currentEntry, currentEntry.getType(), newEntry.getType()));
                currentEntry.setType(newEntry.getType());
            }
            compound.end();
            undoManager.addEdit(compound);

            sourceIsValid.setValue(null);
        } catch (InvalidFieldValueException | IllegalStateException | IOException ex) {
            sourceIsValid.setValue(ValidationMessage.error(Localization.lang("Problem with parsing entry") + ": " + ex.getMessage()));
            LOGGER.debug("Incorrect source", ex);
        }
    }
}
