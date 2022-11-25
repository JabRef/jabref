package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CitationKeyDeviationCheckerTest {

    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final BibDatabase bibDatabase = mock(BibDatabase.class);
    private final MetaData metaData = mock(MetaData.class);
    private final AbstractCitationKeyPattern abstractCitationKeyPattern = mock(AbstractCitationKeyPattern.class);
    private final GlobalCitationKeyPattern globalCitationKeyPattern = mock(GlobalCitationKeyPattern.class);
    private final CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
    private final CitationKeyDeviationChecker checker = new CitationKeyDeviationChecker(bibDatabaseContext, citationKeyPatternPreferences);

    @BeforeEach
    void setUp() {
        when(bibDatabaseContext.getMetaData()).thenReturn(metaData);
        when(citationKeyPatternPreferences.getKeyPattern()).thenReturn(globalCitationKeyPattern);
        when(metaData.getCiteKeyPattern(citationKeyPatternPreferences.getKeyPattern())).thenReturn(abstractCitationKeyPattern);
        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);
    }

    @Test
    void citationKeyDeviatesFromGeneratedKey() {
        BibEntry entry = new BibEntry().withField(InternalField.KEY_FIELD, "Knuth2014")
                                       .withField(StandardField.AUTHOR, "Knuth")
                                       .withField(StandardField.YEAR, "2014");
        List<IntegrityMessage> expected = Collections.singletonList(new IntegrityMessage(
                Localization.lang("Citation key deviates from generated key"), entry, InternalField.KEY_FIELD));
        assertEquals(expected, checker.check(entry));
    }
}
