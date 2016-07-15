package net.sf.jabref.logic.cleanup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.entry.BibEntry;


public class ISSNCleanup implements CleanupJob {

    private static final Pattern ISSN_PATTERN_NODASH = Pattern.compile("^(\\d{4})(\\d{3}[\\dxX])$");
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        Optional<String> issn = entry.getFieldOptional("issn");
        if (!issn.isPresent()) {
            return Collections.emptyList();
        }

        Matcher matcher = ISSN_PATTERN_NODASH.matcher(issn.get());
        if (matcher.find()) {
            String result = matcher.replaceFirst("$1-$2");
            entry.setField("issn", result);
            FieldChange change = new FieldChange(entry, "issn", issn.get(), result);
            return Collections.singletonList(change);
        }
        return Collections.emptyList();
    }

}
