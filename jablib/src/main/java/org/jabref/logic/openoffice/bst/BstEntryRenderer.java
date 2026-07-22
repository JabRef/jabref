package org.jabref.logic.openoffice.bst;

import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Renders a single [BibEntry] through a [BstVM] and strips all LaTeX structure that precedes
/// the actual bibliography text, leaving inline markup intact for pandoc to process.
///
/// IEEEtran and similar styles emit a large `\providecommand` preamble before every entry.
/// Rather than matching individual macros with fragile regexes, we take everything **after the
/// last `\bibitem{key}`** — since `BstVM.render(List.of(entry), …)` renders exactly one entry,
/// every line before its `\bibitem` is preamble by construction.
@NullMarked
public class BstEntryRenderer {

    private static final Pattern BIBITEM =
            Pattern.compile("\\\\bibitem(?:\\[[^]]*])?\\{[^}]*}");
    private static final Pattern BIB_ENV_END =
            Pattern.compile("\\\\end\\{thebibliography}");

    private final BstVM bstVM;

    public BstEntryRenderer(BstVM bstVM) {
        this.bstVM = bstVM;
    }

    /// Renders one entry to a LaTeX body string with preamble and structure stripped.
    ///
    /// Returns the inline LaTeX text ready for pandoc — no `\bibitem`, no `\providecommand`,
    /// no `\begin`/`\end{thebibliography}`.
    public String renderEntryToLatex(BibEntry originalEntry, BibDatabase database) {
        BibEntry entry = new BibEntry(originalEntry);
        new ConvertToBibtexCleanup().cleanup(entry);

        String raw = bstVM.render(List.of(entry), database);

        // Find the last \bibitem — everything before it is BST/IEEEtran preamble.
        // This reliably drops \providecommand, \csname, etc. without fragile nested-brace regexes.
        int bibitemIdx = raw.lastIndexOf("\\bibitem");
        if (bibitemIdx >= 0) {
            raw = raw.substring(bibitemIdx);
            // Drop the \bibitem{key} tag itself (may have optional [label])
            raw = BIBITEM.matcher(raw).replaceFirst("");
        }

        // Strip closing \end{thebibliography} if present
        raw = BIB_ENV_END.matcher(raw).replaceAll("");

        raw = raw.replace("\\newblock", " ");
        return raw.trim();
    }
}
