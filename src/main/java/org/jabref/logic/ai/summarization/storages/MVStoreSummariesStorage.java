package org.jabref.logic.ai.summarization.storages;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.ai.summarization.SummariesStorage;
import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.ai.util.MVStoreBase;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.NotificationService;

public class MVStoreSummariesStorage extends MVStoreBase implements SummariesStorage {
    private static final String SUMMARIES_MAP_PREFIX = "summaries";

    public MVStoreSummariesStorage(Path path, NotificationService dialogService) {
        super(path, dialogService);
    }

    public void set(Path bibDatabasePath, String citationKey, Summary summary) {
        getMap(bibDatabasePath).put(citationKey, summary);
    }

    public Optional<Summary> get(Path bibDatabasePath, String citationKey) {
        return Optional.ofNullable(getMap(bibDatabasePath).get(citationKey));
    }

    public void clear(Path bibDatabasePath, String citationKey) {
        getMap(bibDatabasePath).remove(citationKey);
    }

    private Map<String, Summary> getMap(Path bibDatabasePath) {
        return mvStore.openMap(SUMMARIES_MAP_PREFIX + "-" + bibDatabasePath.toString());
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
