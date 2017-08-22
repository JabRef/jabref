package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.InvalidFieldValueException;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

public class SourceTab extends EntryEditorTab {

    private static final Log LOGGER = LogFactory.getLog(SourceTab.class);
    private final BibDatabaseMode mode;
    private final BibEntry entry;
    private final BasePanel panel;
    private CodeArea codeArea;
    private BooleanProperty movingToDifferentEntry;

    public SourceTab(BasePanel panel, BibEntry entry, BooleanProperty movingToDifferentEntry) {
        this.mode = panel.getBibDatabaseContext().getMode();
        this.entry = entry;
        this.panel = panel;
        this.movingToDifferentEntry = movingToDifferentEntry;
        this.setText(Localization.lang("%0 source", mode.getFormattedName()));
        this.setTooltip(new Tooltip(Localization.lang("Show/edit %0 source", mode.getFormattedName())));
        this.setGraphic(IconTheme.JabRefIcon.SOURCE.getGraphicNode());
    }

    private static String getSourceString(BibEntry entry, BibDatabaseMode type) throws IOException {
        StringWriter stringWriter = new StringWriter(200);
        LatexFieldFormatter formatter = LatexFieldFormatter
                .buildIgnoreHashes(Globals.prefs.getLatexFieldFormatterPreferences());
        new BibEntryWriter(formatter, false).writeWithoutPrependedNewlines(entry, stringWriter, type);

        return stringWriter.getBuffer().toString();
    }

    public void updateSourcePane() {
        if (codeArea != null) {
            try {
                codeArea.clear();
                codeArea.appendText(getSourceString(entry, mode));
            } catch (IOException ex) {
                codeArea.appendText(ex.getMessage() + "\n\n" +
                        Localization.lang("Correct the entry, and reopen editor to display/edit source."));
                codeArea.setEditable(false);
                LOGGER.debug("Incorrect entry", ex);
            }
        }
    }

    private Node createSourceEditor(BibEntry entry, BibDatabaseMode mode) {
        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.lookup(".styled-text-area").setStyle(
                "-fx-font-size: " + Globals.prefs.getFontSizeFX() + "pt;");
        // store source if new tab is selected (if this one is not focused anymore)
        EasyBind.subscribe(codeArea.focusedProperty(), focused -> {
            if (!focused) {
                storeSource();
            }
        });

        // store source if new entry is selected in the maintable and the source tab is focused
        EasyBind.subscribe(movingToDifferentEntry, newEntrySelected -> {
            if (newEntrySelected && codeArea.focusedProperty().get()) {
                DefaultTaskExecutor.runInJavaFXThread(() -> storeSource());
            }
        });

        try {
            String srcString = getSourceString(entry, mode);
            codeArea.appendText(srcString);
        } catch (IOException ex) {
            codeArea.appendText(ex.getMessage() + "\n\n" +
                    Localization.lang("Correct the entry, and reopen editor to display/edit source."));
            codeArea.setEditable(false);
            LOGGER.debug("Incorrect entry", ex);
        }

        // set the database to dirty when something is changed in the source tab
        EasyBind.subscribe(codeArea.beingUpdatedProperty(), updated -> {
            if (updated) {
                panel.markBaseChanged();
            }
        });

        return new VirtualizedScrollPane<>(codeArea);
    }

    @Override
    public boolean shouldShow() {
        return true;
    }

    @Override
    protected void initialize() {
        this.setContent(createSourceEditor(entry, mode));
    }

    private void storeSource() {
        if (codeArea.getText().isEmpty()) {
            return;
        }

        BibtexParser bibtexParser = new BibtexParser(Globals.prefs.getImportFormatPreferences());
        try {
            ParserResult parserResult = bibtexParser.parse(new StringReader(codeArea.getText()));
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
                entry.setCiteKey(newKey);
            } else {
                entry.clearCiteKey();
            }

            // First, remove fields that the user has removed.
            for (Map.Entry<String, String> field : entry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String fieldValue = field.getValue();

                if (InternalBibtexFields.isDisplayableField(fieldName) && !newEntry.hasField(fieldName)) {
                    compound.addEdit(
                            new UndoableFieldChange(entry, fieldName, fieldValue, null));
                    entry.clearField(fieldName);
                }
            }

            // Then set all fields that have been set by the user.
            for (Map.Entry<String, String> field : newEntry.getFieldMap().entrySet()) {
                String fieldName = field.getKey();
                String oldValue = entry.getField(fieldName).orElse(null);
                String newValue = field.getValue();
                if (!Objects.equals(oldValue, newValue)) {
                    // Test if the field is legally set.
                    new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences())
                            .format(newValue, fieldName);

                    compound.addEdit(new UndoableFieldChange(entry, fieldName, oldValue, newValue));
                    entry.setField(fieldName, newValue);
                }
            }

            // See if the user has changed the entry type:
            if (!Objects.equals(newEntry.getType(), entry.getType())) {
                compound.addEdit(new UndoableChangeType(entry, entry.getType(), newEntry.getType()));
                entry.setType(newEntry.getType());
            }
            compound.end();

        } catch (InvalidFieldValueException | IOException ex) {
            // The source couldn't be parsed, so the user is given an
            // error message, and the choice to keep or revert the contents
            // of the source text field.

            LOGGER.debug("Incorrect source", ex);
            DialogService dialogService = new FXDialogService();
            boolean keepEditing = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Problem with parsing entry"),
                    Localization.lang("Error") + ": " + ex.getMessage(),
                    Localization.lang("Edit"),
                    Localization.lang("Revert to original source")
            );

            if (!keepEditing) {
                // Revert
                try {
                    codeArea.replaceText(0, codeArea.getText().length(), getSourceString(entry, mode));
                } catch (IOException e) {
                    LOGGER.debug("Incorrect source", e);
                }
            }
        }
    }
}
