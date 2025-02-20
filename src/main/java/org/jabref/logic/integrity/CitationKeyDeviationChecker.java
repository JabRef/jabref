package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.integrity.IntegrityIssue;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

public class CitationKeyDeviationChecker implements EntryChecker {

    private final BibDatabaseContext bibDatabaseContext;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public CitationKeyDeviationChecker(BibDatabaseContext bibDatabaseContext, CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.citationKeyPatternPreferences = Objects.requireNonNull(citationKeyPatternPreferences);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> valuekey = entry.getCitationKey();
        if (valuekey.isEmpty()) {
            return Collections.emptyList();
        }

        String key = valuekey.get();

        // generate new key
        String generatedKey = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences).generateKey(entry);

        if (!Objects.equals(key, generatedKey)) {
            return Collections.singletonList(new IntegrityMessage(
                    IntegrityIssue.CITATION_KEY_DEVIATES_FROM_GENERATED_KEY.getText(), entry, InternalField.KEY_FIELD));
        }

        return Collections.emptyList();
    }
}
