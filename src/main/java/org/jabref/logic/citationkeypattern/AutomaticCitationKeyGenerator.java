package org.jabref.logic.citationkeypattern;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.common.eventbus.Subscribe;

public class AutomaticCitationKeyGenerator {
    private final CitationKeyGenerator keyGenerator;
    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public AutomaticCitationKeyGenerator(BibDatabaseContext databaseContext,
                                       CitationKeyPatternPreferences citationKeyPatternPreferences,
                                       FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.keyGenerator = new CitationKeyGenerator(databaseContext, citationKeyPatternPreferences);
        this.filePreferences = filePreferences;
    }

    /**
     * Listens for field changes and regenerates the citation key if a relevant field changed.
     */
    @Subscribe
    public void listen(FieldChangedEvent event) {
        BibEntry entry = event.getBibEntry();
        Field field = event.getField();

        // Check if the changed field affects the citation key generation
        // Typically includes: author, year, title, etc.
        if (isRelevantField(field)) {
            // Regenerate the citation key
            keyGenerator.generateAndSetKey(entry);
        }
    }

    /**
     * Determines if a field is relevant for citation key generation.
     *
     * @param field The field to check
     *
     * @return true if the field affects citation key generation
     */
    private boolean isRelevantField(Field field) {
        // Determine which fields affect the citation key based on the pattern
        return field == StandardField.AUTHOR ||
               field == StandardField.YEAR ||
               field == StandardField.TITLE;
    }
}
