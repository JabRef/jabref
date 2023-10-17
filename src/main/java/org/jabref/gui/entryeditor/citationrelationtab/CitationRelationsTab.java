package org.jabref.gui.entryeditor.citationrelationtab;

import javax.swing.undo.UndoManager;

import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.SemanticScholarFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GUI for tab displaying an articles citation relations in two lists based on the currently selected BibEntry
 */
public class CitationRelationsTab extends EntryEditorTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationRelationsTab.class);
    private static final RelatedEntriesCache CITATIONS_CACHE = new RelatedEntriesCache();
    private static final RelatedEntriesCache REFERENCES_CACHE = new RelatedEntriesCache();
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final PreferencesService preferencesService;
    private final LibraryTab libraryTab;

    public CitationRelationsTab(DialogService dialogService, BibDatabaseContext databaseContext,
                                UndoManager undoManager, StateManager stateManager, FileUpdateMonitor fileUpdateMonitor,
                                PreferencesService preferencesService, LibraryTab lTab) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.preferencesService = preferencesService;
        this.libraryTab = lTab;
        setText(Localization.lang("Citation relations"));
        setTooltip(new Tooltip(Localization.lang("Show articles related by citation")));
    }

    /**
     * Determines if tab should be shown according to preferences
     *
     * @param entry Currently selected BibEntry
     * @return whether tab should be shown
     */
    @Override
    public boolean shouldShow(BibEntry entry) {
        // TODO: Create a preference and show tab only if preference is enabled
        return true;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        RelatedEntriesComponent citationsComponent = createCitationsComponent(entry);
        RelatedEntriesComponent referencesComponent = createReferencesComponent(entry);

        SplitPane container = new SplitPane(citationsComponent, referencesComponent);

        setContent(container);
    }

    private RelatedEntriesComponent createReferencesComponent(BibEntry entry) {
        RelatedEntriesComponentConfig componentConfig = new RelatedEntriesComponentConfig("References");

        RelatedEntriesRepository referencesRepository =
                new RelatedEntriesRepository(SemanticScholarFetcher.buildReferencesFetcher(), REFERENCES_CACHE);

        return new RelatedEntriesComponent(entry, referencesRepository, libraryTab, componentConfig,
                dialogService, databaseContext, undoManager, stateManager, fileUpdateMonitor, preferencesService);
    }

    private RelatedEntriesComponent createCitationsComponent(BibEntry entry) {
        RelatedEntriesComponentConfig componentConfig = new RelatedEntriesComponentConfig("Citations");

        RelatedEntriesRepository citationsRepository =
                new RelatedEntriesRepository(SemanticScholarFetcher.buildCitationsFetcher(), CITATIONS_CACHE);

        return new RelatedEntriesComponent(entry, citationsRepository, libraryTab, componentConfig,
                dialogService, databaseContext, undoManager, stateManager, fileUpdateMonitor,
                preferencesService);
    }
}
