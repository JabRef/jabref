package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class NoBibtexFieldChecker implements Checker {

    /**
     * BibLaTeX package documentation (Section 2.1.1):
     * The title of the periodical is given in the journaltitle field.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.JOURNALTITLE);

        //BibTeX
        if (!value.isPresent()) {
            return Collections.emptyList();
        }
        else {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("BibLaTeX field only"), entry, FieldName.JOURNALTITLE));
        }
    }
}
