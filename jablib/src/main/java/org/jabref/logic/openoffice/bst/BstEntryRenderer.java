package org.jabref.logic.openoffice.bst;

import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Renders a single [BibEntry] through a [BstVM] and strips the LaTeX environment structure,
/// leaving inline markup intact for pandoc to process.
@NullMarked
public class BstEntryRenderer {

    private static final Pattern BIB_ENV =
            Pattern.compile("\\\\(begin|end)\\{thebibliography}(\\{[^}]*})?");
    private static final Pattern BIBITEM =
            Pattern.compile("\\\\bibitem\\{([^}]*)}");

    private final BstVM bstVM;

    public BstEntryRenderer(BstVM bstVM) {
        this.bstVM = bstVM;
    }

    /// Renders one entry to a LaTeX body string with structure removed but inline markup intact.
    public String renderEntryToLatex(BibEntry originalEntry, BibDatabase database) {
        BibEntry entry = new BibEntry(originalEntry);
        new ConvertToBibtexCleanup().cleanup(entry);

        String raw = bstVM.render(List.of(entry), database);
        raw = BIB_ENV.matcher(raw).replaceAll("");
        raw = BIBITEM.matcher(raw).replaceAll("");
        raw = raw.replace("\\newblock", " ");
        raw = raw.replace("#2}}", "");
        return raw.trim();
    }
}
