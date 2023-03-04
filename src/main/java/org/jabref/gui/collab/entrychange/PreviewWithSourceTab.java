package org.jabref.gui.collab.entrychange;

import java.io.IOException;
import java.io.StringWriter;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.preview.PreviewViewer;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewWithSourceTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewWithSourceTab.class);

    public TabPane getPreviewWithSourceTab(BibEntry entry, BibDatabaseContext bibDatabaseContext, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager, PreviewViewer previewViewer) {
        return getPreviewWithSourceTab(entry, bibDatabaseContext, preferencesService, entryTypesManager, previewViewer, "");
    }

    public TabPane getPreviewWithSourceTab(BibEntry entry, BibDatabaseContext bibDatabaseContext, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager, PreviewViewer previewViewer, String label) {
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entry);

        CodeArea codeArea = new CodeArea();
        codeArea.setId("bibtexcodearea");
        codeArea.setWrapText(true);
        codeArea.setDisable(true);

        TabPane tabPanePreviewCode = new TabPane();
        Tab previewTab = new Tab();
        previewTab.setContent(previewViewer);

        if (StringUtil.isNullOrEmpty(label)) {
            previewTab.setText(Localization.lang("Entry preview"));
        } else {
            previewTab.setText(label);
        }

        try {
            codeArea.appendText(getSourceString(entry, bibDatabaseContext.getMode(), preferencesService.getFieldWriterPreferences(), entryTypesManager));
        } catch (IOException e) {
            LOGGER.error("Error getting Bibtex: {}", entry);
        }
        codeArea.setEditable(false);
        Tab codeTab = new Tab(Localization.lang("%0 source", bibDatabaseContext.getMode().getFormattedName()), codeArea);

        tabPanePreviewCode.getTabs().addAll(previewTab, codeTab);
        return tabPanePreviewCode;
    }

    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldWriterPreferences fieldWriterPreferences, BibEntryTypesManager entryTypesManager) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldWriterPreferences);
        new BibEntryWriter(fieldWriter, entryTypesManager).write(entry, bibWriter, type);
        return writer.toString();
    }
}
