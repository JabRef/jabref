package org.jabref.logic.journals;

import java.nio.file.Path;

import org.jabref.logic.journals.ltwa.LtwaRepository;

/// Type alias for backward compatibility. Use {@link AbbreviationRepository} instead.
/// @deprecated Use {@link AbbreviationRepository} directly
@Deprecated(forRemoval = true)
public class JournalAbbreviationRepository extends AbbreviationRepository {

    public JournalAbbreviationRepository(Path abbreviationList, LtwaRepository ltwaRepository) {
        super(abbreviationList, ltwaRepository);
    }

    public JournalAbbreviationRepository() {
        super();
    }
}
