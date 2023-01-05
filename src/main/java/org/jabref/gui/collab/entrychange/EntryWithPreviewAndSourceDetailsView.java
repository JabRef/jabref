package org.jabref.gui.collab.entrychange;

import java.io.IOException;
import java.io.StringWriter;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntryWithPreviewAndSourceDetailsView extends DatabaseChangeDetailsView {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryWithPreviewAndSourceDetailsView.class);

    public EntryWithPreviewAndSourceDetailsView(BibEntry entry, BibDatabaseContext bibDatabaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService) {
        PreviewViewer previewViewer = new PreviewViewer(bibDatabaseContext, dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entry);

        CodeArea codeArea = new CodeArea();
        codeArea.setId("bibtexcodearea");
        codeArea.setWrapText(true);
        codeArea.setDisable(true);

        TabPane tabPanePreviewCode = new TabPane();
        Tab previewTab = new Tab(Localization.lang("Entry preview"), previewViewer);

        try {
            codeArea.appendText(getSourceString(entry, bibDatabaseContext.getMode(), preferencesService.getFieldWriterPreferences()));
        } catch (IOException e) {
            LOGGER.error("Error getting Bibtex", entry);
        }
        codeArea.setEditable(true);
        VirtualizedScrollPane<CodeArea> scrollableCodeArea = new VirtualizedScrollPane<>(codeArea);
        Tab codeTab = new Tab(Localization.lang("%0 source", bibDatabaseContext.getMode().getFormattedName()), scrollableCodeArea);

        tabPanePreviewCode.getTabs().addAll(previewTab, codeTab);

        setLeftAnchor(tabPanePreviewCode, 8d);
        setTopAnchor(tabPanePreviewCode, 8d);
        setRightAnchor(tabPanePreviewCode, 8d);
        setBottomAnchor(tabPanePreviewCode, 8d);

        getChildren().setAll(tabPanePreviewCode);
    }

    private String getSourceString(BibEntry entry, BibDatabaseMode type, FieldWriterPreferences fieldWriterPreferences) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldWriterPreferences);
        new BibEntryWriter(fieldWriter, Globals.entryTypesManager).write(entry, bibWriter, type);
        return writer.toString();
    }
}
