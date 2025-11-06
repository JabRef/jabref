package org.jabref.logic.integrity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

import org.jspecify.annotations.NonNull;

public class CitationKeyDeviationChecker implements EntryChecker {

    private final BibDatabaseContext bibDatabaseContext;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public CitationKeyDeviationChecker(@NonNull BibDatabaseContext bibDatabaseContext,
                                       @NonNull CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> valuekey = entry.getCitationKey();
        if (valuekey.isEmpty()) {
            return List.of();
        }

        String key = valuekey.get();

        // generate new key
        String generatedKey = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences).generateKey(entry);

        if (!Objects.equals(key, generatedKey)) {
            return List.of(new IntegrityMessage(
                    Localization.lang("Citation key deviates from generated key"), entry, InternalField.KEY_FIELD));
        }

        return List.of();
    }
}
