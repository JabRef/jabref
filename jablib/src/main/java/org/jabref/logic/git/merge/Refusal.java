package org.jabref.logic.git.merge;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;

/// Why [BibFileMerger] left the files untouched: `file` holds content that JabRef's parser or
/// writer would silently drop, or that the entry-based merge cannot represent.
@NullMarked
public record Refusal(Path file, Reason reason) {
    public enum Reason {
        /// The merge is planned per citation key. With duplicates, the plan would be computed from one entry and applied to another.
        DUPLICATE_CITATION_KEYS,
        /// The parser skipped content it could not read, so the writer cannot reproduce it.
        PARSER_WARNINGS,
        /// The writer drops an entry that has no fields, or automatic fields only.
        EMPTY_ENTRY,
        /// The writer emits the definition of a custom entry type only when an entry uses the type.
        UNUSED_CUSTOM_ENTRY_TYPE,
        /// The parser attaches a comment to the entry or `@String` following it, but drops one in front of `@Comment` or `@Preamble`, as does every JabRef save.
        UNATTACHED_COMMENT,
        /// Only entries with a citation key are merged; everything else is taken from `current` and must therefore not have changed in `other`.
        NON_ENTRY_CONTENT_CHANGED_IN_OTHER
    }
}
