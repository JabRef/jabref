package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;

/// The changelog entries announced to the developer so far, kept in a file: one entry per line, its section,
/// heading and text separated by tabs. A changelog line never contains a tab, so no escaping is needed.
///
/// The file lives in the checkout's git directory, so it follows the worktree and survives JabRef being closed.
@NullMarked
public final class AnnouncedEntries {

    private static final String FIELD_SEPARATOR = "\t";

    private final Path file;

    public AnnouncedEntries(Path file) {
        this.file = file;
    }

    /// The announced entries, or empty when nothing has been announced yet — the first run in a checkout.
    public Optional<Set<ChangelogEntry>> read() throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllLines(file).stream()
                                .map(AnnouncedEntries::fromLine)
                                .flatMap(Optional::stream)
                                .collect(Collectors.toUnmodifiableSet()));
    }

    /// Replaces the announced entries: from now on, only entries outside `entries` are news.
    public void write(Collection<ChangelogEntry> entries) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, entries.stream().map(AnnouncedEntries::toLine).toList());
    }

    private static String toLine(ChangelogEntry entry) {
        return String.join(FIELD_SEPARATOR, entry.section(), entry.heading(), entry.text());
    }

    private static Optional<ChangelogEntry> fromLine(String line) {
        String[] fields = line.split(FIELD_SEPARATOR, -1);
        if (fields.length != 3) {
            return Optional.empty();
        }
        return Optional.of(new ChangelogEntry(fields[0], fields[1], fields[2]));
    }
}
