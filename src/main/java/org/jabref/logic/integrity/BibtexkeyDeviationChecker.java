package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class BibtexkeyDeviationChecker implements Checker {

    private final BibDatabaseContext bibDatabaseContext;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;

    public BibtexkeyDeviationChecker(BibDatabaseContext bibDatabaseContext, BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.bibtexKeyPatternPreferences = Objects.requireNonNull(bibtexKeyPatternPreferences);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> valuekey = entry.getCiteKeyOptional();
        if (!valuekey.isPresent()) {
            return Collections.emptyList();
        }

        String key = valuekey.get();

        // generate new key
        String generatedKey = new BibtexKeyGenerator(bibDatabaseContext, bibtexKeyPatternPreferences).generateKey(entry);

        if (!Objects.equals(key, generatedKey)) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("BibTeX key deviates from generated key"), entry, BibEntry.KEY_FIELD));
        }

        return Collections.emptyList();
    }

}
