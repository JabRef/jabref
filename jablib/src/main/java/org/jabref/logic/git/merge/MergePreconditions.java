package org.jabref.logic.git.merge;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.git.merge.Refusal.Reason;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NullMarked;

/// The checks a merge has to pass before `current` is rewritten. [Reason] documents what each
/// check protects.
@NullMarked
final class MergePreconditions {

    private static final Pattern UNATTACHED_COMMENT = Pattern.compile(
            "^\\h*%.*(\\R\\s*)*\\R\\s*@(comment|preamble)\\s*[{(]",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private MergePreconditions() {
    }

    /// @param keyPatterns needed to serialize the metadata for comparison
    /// @return one refusal per violated check, empty if the files can be merged
    static List<Refusal> check(ParserResult base, ParserResult current, ParserResult other, GlobalCitationKeyPatterns keyPatterns) throws IOException {
        List<Refusal> refusals = new ArrayList<>();
        for (ParserResult version : List.of(base, current, other)) {
            refusals.addAll(lossOnWriting(version));
        }

        NonEntryContent otherContent = NonEntryContent.of(other, keyPatterns);
        if (!otherContent.equals(NonEntryContent.of(base, keyPatterns)) && !otherContent.equals(NonEntryContent.of(current, keyPatterns))) {
            refusals.add(new Refusal(fileOf(other), Reason.NON_ENTRY_CONTENT_CHANGED_IN_OTHER));
        }
        return refusals;
    }

    /// The content of the file that would not survive being parsed and written back.
    private static List<Refusal> lossOnWriting(ParserResult version) throws IOException {
        Path file = fileOf(version);
        BibDatabase database = version.getDatabase();
        List<Refusal> refusals = new ArrayList<>();
        if (hasDuplicateCitationKeys(database)) {
            refusals.add(new Refusal(file, Reason.DUPLICATE_CITATION_KEYS));
        }
        if (version.hasWarnings()) {
            refusals.add(new Refusal(file, Reason.PARSER_WARNINGS));
        }
        if (database.getEntries().stream().anyMatch(BibEntry::isEmpty)) {
            refusals.add(new Refusal(file, Reason.EMPTY_ENTRY));
        }
        if (hasUnusedCustomEntryTypes(version)) {
            refusals.add(new Refusal(file, Reason.UNUSED_CUSTOM_ENTRY_TYPE));
        }
        if (hasUnattachedComment(file, version)) {
            refusals.add(new Refusal(file, Reason.UNATTACHED_COMMENT));
        }
        return refusals;
    }

    private static boolean hasDuplicateCitationKeys(BibDatabase database) {
        return database.getEntries().stream()
                       .flatMap(entry -> entry.getCitationKey().stream())
                       .anyMatch(database::isDuplicateCitationKeyExisting);
    }

    private static boolean hasUnusedCustomEntryTypes(ParserResult version) {
        Set<EntryType> usedTypes = version.getDatabase().getEntries().stream()
                                          .map(BibEntry::getType)
                                          .collect(Collectors.toSet());
        return version.getEntryTypes().stream()
                      .map(BibEntryType::getType)
                      .anyMatch(type -> !usedTypes.contains(type));
    }

    /// The parser does not keep the comment, so the raw file has to be searched for it.
    private static boolean hasUnattachedComment(Path file, ParserResult version) throws IOException {
        Charset encoding = version.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);
        // Decoding through the constructor replaces unmappable bytes instead of throwing
        String content = new String(Files.readAllBytes(file), encoding);
        return UNATTACHED_COMMENT.matcher(content).find();
    }

    /// The versions are parsed from files, so the path is always known.
    private static Path fileOf(ParserResult version) {
        return version.getPath().orElseThrow();
    }
}
