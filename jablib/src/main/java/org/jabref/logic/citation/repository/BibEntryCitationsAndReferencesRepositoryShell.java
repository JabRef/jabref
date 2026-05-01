package org.jabref.logic.citation.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class BibEntryCitationsAndReferencesRepositoryShell extends MVStoreBase implements BibEntryCitationsAndReferencesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibEntryCitationsAndReferencesRepositoryShell.class);

    private static final String CITATION_RELATIONS_STORE = "citation-relations.mv";

    private final BibEntryRelationRepository citationsDao;
    private final BibEntryRelationRepository referencesDao;

    public BibEntryCitationsAndReferencesRepositoryShell(Path citationsRelationsDirectory,
                                                         int storeTTL,
                                                         ImportFormatPreferences importFormatPreferences,
                                                         FieldPreferences fieldPreferences,
                                                         BibEntryTypesManager entryTypesManager,
                                                         ObjectProperty<CitationFetcherType> citationFetcherTypeProperty,
                                                         NotificationService notificationService) {
        super(citationsRelationsDirectory.resolve(CITATION_RELATIONS_STORE), notificationService);

        this.referencesDao = new MVStoreBibEntryRelationRepository(mvStore, "references", storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences, citationFetcherTypeProperty);
        this.citationsDao = new MVStoreBibEntryRelationRepository(mvStore, "citations", storeTTL, entryTypesManager, importFormatPreferences, fieldPreferences, citationFetcherTypeProperty);
    }

    @VisibleForTesting
    public BibEntryCitationsAndReferencesRepositoryShell(
            BibEntryRelationRepository citationsDao,
            BibEntryRelationRepository referencesDao
    ) {
        super();
        this.citationsDao = citationsDao;
        this.referencesDao = referencesDao;
    }

    @Override
    public void addCitations(BibEntry entry, List<BibEntry> citations) {
        citationsDao.addRelations(
                entry, Objects.requireNonNullElseGet(citations, List::of)
        );
        commit();
    }

    @Override
    public List<BibEntry> getCitations(@Nullable BibEntry entry) {
        if (entry == null) {
            return List.of();
        }
        return citationsDao.getRelations(entry);
    }

    @Override
    public boolean containsCitations(BibEntry entry) {
        return citationsDao.containsKey(entry);
    }

    @Override
    public boolean isCitationsUpdatable(BibEntry entry) {
        return citationsDao.shouldUpdate(entry);
    }

    @Override
    public void addReferences(BibEntry entry, @Nullable List<BibEntry> references) {
        referencesDao.addRelations(
                entry, Objects.requireNonNullElseGet(references, List::of)
        );
        commit();
    }

    @Override
    public List<BibEntry> getReferences(@Nullable BibEntry entry) {
        if (entry == null) {
            return List.of();
        }
        return referencesDao.getRelations(entry);
    }

    @Override
    public boolean containsReferences(BibEntry entry) {
        return referencesDao.containsKey(entry);
    }

    @Override
    public boolean isReferencesUpdatable(BibEntry entry) {
        return referencesDao.shouldUpdate(entry);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening citation relations storage";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening citation relations storage");
    }
}
