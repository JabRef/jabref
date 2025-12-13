package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ResourceLock("Localization.lang")
class CitationKeyDeviationCheckerTest {

    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final BibDatabase bibDatabase = mock(BibDatabase.class);
    private final MetaData metaData = mock(MetaData.class);
    private final AbstractCitationKeyPatterns abstractCitationKeyPatterns = mock(AbstractCitationKeyPatterns.class);
    private final GlobalCitationKeyPatterns globalCitationKeyPatterns = mock(GlobalCitationKeyPatterns.class);
    private final CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
    private final CitationKeyDeviationChecker checker = new CitationKeyDeviationChecker(bibDatabaseContext, citationKeyPatternPreferences);

    @BeforeEach
    void setUp() {
        when(bibDatabaseContext.getMetaData()).thenReturn(metaData);
        when(citationKeyPatternPreferences.getKeyPatterns()).thenReturn(globalCitationKeyPatterns);
        when(metaData.getCiteKeyPatterns(citationKeyPatternPreferences.getKeyPatterns())).thenReturn(abstractCitationKeyPatterns);
        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);
    }

    @Test
    void citationKeyDeviatesFromGeneratedKey() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "Knuth2014")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        List<IntegrityMessage> expected = List.of(new IntegrityMessage(
                Localization.lang("Citation key deviates from generated key"), entry, InternalField.KEY_FIELD));
        assertEquals(expected, checker.check(entry));
    }
}
