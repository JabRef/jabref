package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

/// Extracts a book title and contributors from a Ukrainian
/// cataloguing-in-publication citation block.
///
/// The input is the String returned by [BookCitationBlock#find].
@NullMarked
final class BookTitleAuthorExtractor {

    private static final String SPACE = "[\\s\\h]";

    private static final Pattern TITLE_AUTHOR_SEPARATOR = Pattern.compile("\\h+/\\h+");

    private static final Pattern AREA_SEPARATOR = Pattern.compile("\\.\\h*[—–-]\\h+");

    private static final Pattern CATALOGUE_CODE = Pattern.compile("^\\s*[\\p{Lu}\\p{N}]{1,4}\\d{1,3}\\s+");

    private static final Pattern AUTHOR_HEADING = Pattern.compile("\\p{Lu}[\\p{L}'’ʼ-]+,?\\h+\\p{Lu}\\.(?:\\h*\\p{Lu}\\.)?");

    private static final Pattern EDITOR_LABEL = Pattern.compile("(?iu)^(?:за\\s+ред(?:акцією)?\\.?|під\\s+ред(?:акцією)?\\.?|ред(?:актор(?:и)?)?\\.?)\\s*[:.]?\\s*(.+)$");

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private static final Pattern NON_AUTHOR_LABEL = Pattern.compile("(?iu)^(?:рецензент(?:и)?|reviewer(?:s)?|isbn|удк|ббк)\\b");

    private static final Pattern SPACES = Pattern.compile(SPACE + "+");

    private BookTitleAuthorExtractor() {
    }

    /// Extracts the title before the " / " separator.
    ///
    /// Removes the author heading and catalogue code,
    /// and joins wrapped title lines.
    ///
    /// @param citationBlock the citation block text
    /// @return the extracted title, or empty if not found
    static Optional<String> findTitle(String citationBlock) {
        Optional<String> prefix = beforeSlash(citationBlock);

        if (prefix.isEmpty()) {
            return Optional.empty();
        }

        String title = prefix.orElseThrow().replace('\n', ' ').strip();
        title = CATALOGUE_CODE.matcher(title).replaceFirst("");
        title = SPACES.matcher(title).replaceAll(" ").strip();

        return title.isEmpty()
                ? Optional.empty()
                : Optional.of(title);
    }

    /// Extracts authors appearing after the " / " separator.
    ///
    /// Excludes edition and publication areas.
    /// Explicitly labelled editors are not treated as authors.
    ///
    /// @param citationBlock the citation block text
    /// @return the author string, or empty if not found
    static Optional<String> findAuthors(String citationBlock) {
        Optional<String> suffix = afterSlash(citationBlock);

        if (suffix.isEmpty()) {
            return Optional.empty();
        }

        String contributors = suffix.orElseThrow();

        Matcher editionOrPublication =
                AREA_SEPARATOR.matcher(contributors);

        if (editionOrPublication.find()) {
            contributors = contributors.substring(
                    0, editionOrPublication.start());
        }

        contributors = SPACES.matcher(contributors)
                .replaceAll(" ").strip();

        if (contributors.isEmpty()
                || NON_AUTHOR_LABEL.matcher(contributors).find()
                || EDITOR_LABEL.matcher(contributors).matches()) {
            return Optional.empty();
        }

        return Optional.of(contributors);
    }

    /// Extracts explicitly labelled editors.
    ///
    /// @param citationBlock the citation block text
    /// @return the editor names, or empty if not found
    static Optional<String> findEditors(String citationBlock) {
        Optional<String> suffix = afterSlash(citationBlock);

        if (suffix.isEmpty()) {
            return Optional.empty();
        }

        String contributors = suffix.orElseThrow();

        Matcher end = AREA_SEPARATOR.matcher(contributors);

        if (end.find()) {
            contributors = contributors.substring(0, end.start());
        }

        Matcher editor = EDITOR_LABEL.matcher(
                SPACES.matcher(contributors).replaceAll(" ").strip());

        if (!editor.matches()) {
            return Optional.empty();
        }

        String names = editor.group(1).strip();

        return names.isEmpty()
                ? Optional.empty()
                : Optional.of(names);
    }

    /// Returns the title portion before the author separator.
    private static Optional<String> beforeSlash(String citationBlock) {
        String text = LINE_BREAK.matcher(citationBlock).replaceAll("\n").strip();

        Matcher slash = TITLE_AUTHOR_SEPARATOR.matcher(text);

        if (!slash.find()) {
            return Optional.empty();
        }

        String prefix = text.substring(0, slash.start());
        String[] lines = prefix.split("\\n");

        int start = 0;

        if (lines.length > 1
                && AUTHOR_HEADING.matcher(lines[0].strip()).matches()) {
            start = 1;
        }

        StringBuilder result = new StringBuilder();

        for (int i = start; i < lines.length; i++) {
            if (!lines[i].isBlank()) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(lines[i].strip());
            }
        }

        return result.isEmpty()
                ? Optional.empty()
                : Optional.of(result.toString());
    }

    /// Returns the contributor portion after the author separator.
    private static Optional<String> afterSlash(String citationBlock) {
        Matcher slash = TITLE_AUTHOR_SEPARATOR.matcher(citationBlock);

        if (!slash.find()) {
            return Optional.empty();
        }

        return Optional.of(citationBlock.substring(slash.end()));
    }
}
