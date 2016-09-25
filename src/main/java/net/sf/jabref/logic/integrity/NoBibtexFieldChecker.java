package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class NoBibtexFieldChecker implements Checker {

    private final BibDatabaseContext bibDatabaseContextEdition;


    public NoBibtexFieldChecker(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContextEdition = Objects.requireNonNull(bibDatabaseContext);
    }

    /**
     * BibLaTeX package documentation (Section 4.9.1):
     * The BibLaTeX package will automatically capitalize the first word when required at the beginning of a sentence.
     * Official BibTeX specification:
     * note: Any additional information that can help the reader. The first word should be capitalized.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.JOURNALTITLE);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        //BibTeX
        if (!bibDatabaseContextEdition.isBiblatexMode() && value.isPresent()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("no BibTeX field"), entry, FieldName.JOURNALTITLE));
        }

        return Collections.emptyList();
    }
}
