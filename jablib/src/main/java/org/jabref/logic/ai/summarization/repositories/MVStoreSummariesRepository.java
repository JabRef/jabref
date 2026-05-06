package org.jabref.logic.ai.summarization.repositories;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.ai.summarization.AiSummaryIdentifier;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class MVStoreSummariesRepository extends MVStoreBase implements SummariesRepository {
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    public MVStoreSummariesRepository(NotificationService dialogService, Path path) {
        super(path, dialogService);
    }

    public void set(AiSummaryIdentifier summaryIdentifier, AiSummary aiSummary) {
        Map<String, String> map = getMap(summaryIdentifier.libraryId());

        try {
            map.put(summaryIdentifier.summaryName(), JSON_MAPPER.writeValueAsString(aiSummary));
        } catch (JacksonException e) {
            // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
            // RuntimeException to ignore it.
            throw new RuntimeException(e);
        }
    }

    public Optional<AiSummary> get(AiSummaryIdentifier summaryIdentifier) {
        Map<String, String> map = getMap(summaryIdentifier.libraryId());
        Optional<String> summaryJson = Optional.ofNullable(map.get(summaryIdentifier.summaryName()));

        if (summaryJson.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(JSON_MAPPER.readValue(summaryJson.get(), AiSummary.class));
        } catch (JacksonException e) {
            // NOTE: This is a highly not probable exception, so wrapping in try/catch and turning to a
            // RuntimeException to ignore it.
            throw new RuntimeException(e);
        }
    }

    public void clear(AiSummaryIdentifier summaryIdentifier) {
        getMap(summaryIdentifier.libraryId()).remove(summaryIdentifier.summaryName());
    }

    private Map<String, String> getMap(String libraryId) {
        return mvStore.openMap(libraryId);
    }

    @Override
    protected String errorMessageForOpening() {
        return "An error occurred while opening summary storage. Summaries of entries will not be stored in the next session.";
    }

    @Override
    protected String errorMessageForOpeningLocalized() {
        return Localization.lang("An error occurred while opening summary storage. Summaries of entries will not be stored in the next session.");
    }
}
