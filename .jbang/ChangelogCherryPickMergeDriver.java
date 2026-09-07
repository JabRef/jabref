///usr/bin/env jbang "$0" "$@" ; exit $?

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

//JAVA 25+
//DEPS org.jspecify:jspecify:1.0.0

/// Git merge driver for `CHANGELOG.md` when a change is ported between `main` and `stable`
/// (see `.github/workflows/port-to-other-branch.yml`).
///
/// [impl->adr~single-stable-branch-with-automatic-ports~1]
///
/// A line-based cherry-pick of a changelog entry always conflicts: the entry's neighbours in
/// `## [Unreleased]` differ between the two branches. No existing tool covers this case:
///
/// - Git's own strategies (`merge=union` from `.gitattributes`, `-X ours`/`-X theirs`,
///   `--ignore-space-change`) are line-based: union duplicates whole blocks of the conflicting
///   hunk, the others drop one side's entries.
/// - [maven-flow/changelog-merge-driver](https://github.com/maven-flow/changelog-merge-driver)
///   (used when merging `main` into a PR branch) merges the
///   two "Unreleased" sections as a union of entries; applied to a cherry-pick, it would copy
///   every unreleased entry of the source branch, not only the one the commit added.
/// - [heylogs](https://github.com/nbbrd/heylogs) and [clparse](https://github.com/marcaddeo/clparse)
///   only read a changelog (lint, extract a version); neither merges.
/// - Generators such as [git-cliff](https://github.com/orhun/git-cliff) or
///   [conventional-changelog](https://github.com/conventional-changelog/conventional-changelog) derive entries from commit
///   messages; JabRef writes its entries by hand in the PR, so there is nothing to generate from.
///
/// This driver is a true three-way merge on entry level: it applies the entries that the
/// cherry-picked commit added to or removed from `## [Unreleased]` (base → theirs) onto ours,
/// per `### Section`. Everything outside `## [Unreleased]` is expected to be unchanged by the
/// commit; if it is not, the driver falls back to `git merge-file` and lets Git report the
/// conflict. Invoked by Git as `driver <base> <ours> <theirs>`; the result replaces `<ours>`.
@NullMarked
public class ChangelogCherryPickMergeDriver {

    private static final String UNRELEASED_HEADING = "## [Unreleased]";
    private static final List<String> SECTION_ORDER = List.of("### Added", "### Changed", "### Fixed", "### Removed");

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 3) {
            System.err.println("Usage: ChangelogCherryPickMergeDriver <base> <ours> <theirs> | --self-test");
            System.exit(2);
        }
        Path base = Path.of(args[0]);
        Path ours = Path.of(args[1]);
        Path theirs = Path.of(args[2]);
        Optional<String> merged = merge(Files.readString(base), Files.readString(ours), Files.readString(theirs));
        if (merged.isPresent()) {
            Files.writeString(ours, merged.get());
            return;
        }
        // Not a plain entry change - leave the decision (and the conflict markers) to Git's own three-way merge
        int conflicts = new ProcessBuilder("git", "merge-file", ours.toString(), base.toString(), theirs.toString())
                .inheritIO().start().waitFor();
        System.exit(conflicts == 0 ? 0 : 1);
    }

    /// Everything before the "Unreleased" heading, the sections below it, and everything from the next version on.
    record Changelog(List<String> head, LinkedHashMap<String, List<String>> sections, List<String> tail) {
    }

    static Optional<String> merge(String baseText, String oursText, String theirsText) {
        Optional<Changelog> base = parse(baseText);
        Optional<Changelog> ours = parse(oursText);
        Optional<Changelog> theirs = parse(theirsText);
        if (base.isEmpty() || ours.isEmpty() || theirs.isEmpty()) {
            return Optional.empty();
        }
        if (!base.get().head().equals(theirs.get().head()) || !base.get().tail().equals(theirs.get().tail())) {
            // The commit touched a released version, the link references, or the header
            return Optional.empty();
        }

        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        ours.get().sections().forEach((heading, items) -> result.put(heading, new ArrayList<>(items)));

        List<String> headings = new ArrayList<>(theirs.get().sections().keySet());
        base.get().sections().keySet().stream().filter(heading -> !headings.contains(heading)).forEach(headings::add);
        for (String heading : headings) {
            List<String> baseItems = base.get().sections().getOrDefault(heading, List.of());
            List<String> theirItems = theirs.get().sections().getOrDefault(heading, List.of());
            // Multiset difference: a commit that drops one of two equal entries removes exactly one here, too
            List<String> removed = new ArrayList<>(baseItems);
            List<String> added = new ArrayList<>();
            for (String item : theirItems) {
                if (!removed.remove(item)) {
                    added.add(item);
                }
            }
            if (added.isEmpty() && removed.isEmpty()) {
                continue;
            }
            List<String> ourItems = result.get(heading);
            if (ourItems == null) {
                if (added.isEmpty()) {
                    continue;
                }
                ourItems = new ArrayList<>();
                insertInSectionOrder(result, heading, ourItems);
            }
            // An entry removed by the commit that never reached this branch is simply absent here
            removed.forEach(ourItems::remove);
            for (String item : added) {
                if (!ourItems.contains(item)) {
                    ourItems.add(item);
                }
            }
        }
        return Optional.of(render(ours.get().head(), result, ours.get().tail()));
    }

    /// Keeps the Keep-a-Changelog section order when a section only exists on the incoming side.
    private static void insertInSectionOrder(LinkedHashMap<String, List<String>> sections, String heading, List<String> items) {
        int rank = SECTION_ORDER.indexOf(heading);
        LinkedHashMap<String, List<String>> reordered = new LinkedHashMap<>();
        boolean inserted = false;
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            int existingRank = SECTION_ORDER.indexOf(entry.getKey());
            if (!inserted && rank >= 0 && (existingRank < 0 || existingRank > rank)) {
                reordered.put(heading, items);
                inserted = true;
            }
            reordered.put(entry.getKey(), entry.getValue());
        }
        if (!inserted) {
            reordered.put(heading, items);
        }
        sections.clear();
        sections.putAll(reordered);
    }

    static Optional<Changelog> parse(String text) {
        List<String> lines = text.lines().toList();
        int firstVersion = 0;
        while (firstVersion < lines.size() && !lines.get(firstVersion).startsWith("## ")) {
            firstVersion++;
        }
        if (firstVersion == lines.size()) {
            return Optional.empty();
        }
        if (!lines.get(firstVersion).equals(UNRELEASED_HEADING)) {
            // A freshly released branch has no "Unreleased" section yet; ported entries open it
            return Optional.of(new Changelog(lines.subList(0, firstVersion), new LinkedHashMap<>(), lines.subList(firstVersion, lines.size())));
        }
        int end = firstVersion + 1;
        while (end < lines.size() && !lines.get(end).startsWith("## ")) {
            end++;
        }
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        // Collects entries that precede the first "### " heading; any such entry makes the file unparsable
        List<String> beforeFirstSection = new ArrayList<>();
        List<String> items = beforeFirstSection;
        for (String line : lines.subList(firstVersion + 1, end)) {
            if (line.startsWith("### ")) {
                items = sections.computeIfAbsent(line, heading -> new ArrayList<>());
            } else if (line.startsWith("- ")) {
                items.add(line);
            } else if (line.startsWith(" ") && !items.isEmpty()) {
                // Continuation line of a wrapped entry
                items.set(items.size() - 1, items.getLast() + "\n" + line);
            } else if (!line.isBlank()) {
                return Optional.empty();
            }
        }
        if (!beforeFirstSection.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Changelog(lines.subList(0, firstVersion), sections, lines.subList(end, lines.size())));
    }

    private static String render(List<String> head, LinkedHashMap<String, List<String>> sections, List<String> tail) {
        StringBuilder out = new StringBuilder();
        head.forEach(line -> out.append(line).append('\n'));
        out.append(UNRELEASED_HEADING).append("\n\n");
        sections.forEach((heading, items) -> {
            out.append(heading).append("\n\n");
            if (!items.isEmpty()) {
                items.forEach(item -> out.append(item).append('\n'));
                out.append('\n');
            }
        });
        tail.forEach(line -> out.append(line).append('\n'));
        return out.toString();
    }

    private static void selfTest() {
        String base = """
                # Changelog
                
                ## [Unreleased]
                
                ### Added
                
                - We added feature A. [#1](https://github.com/JabRef/jabref/issues/1)
                
                ### Fixed
                
                - We fixed the old bug. [#2](https://github.com/JabRef/jabref/issues/2)
                
                ## [6.0] - 2026-01-01
                
                [Unreleased]: https://github.com/JabRef/jabref/compare/v6.0...HEAD
                """;
        String theirs = base
                .replace("- We added feature A. [#1](https://github.com/JabRef/jabref/issues/1)\n",
                        "- We added feature A. [#1](https://github.com/JabRef/jabref/issues/1)\n- We added feature B. [#3](https://github.com/JabRef/jabref/issues/3)\n")
                .replace("### Fixed\n\n", "### Fixed\n\n- We fixed the crash. [#4](https://github.com/JabRef/jabref/issues/4)\n")
                .replace("- We fixed the old bug. [#2](https://github.com/JabRef/jabref/issues/2)\n", "");
        String ours = """
                # Changelog
                
                ## [Unreleased]
                
                ### Fixed
                
                - We fixed something on stable only. [#9](https://github.com/JabRef/jabref/issues/9)
                
                ## [6.0] - 2026-01-01
                
                [Unreleased]: https://github.com/JabRef/jabref/compare/v6.0...HEAD
                """;
        String expected = """
                # Changelog
                
                ## [Unreleased]
                
                ### Added
                
                - We added feature B. [#3](https://github.com/JabRef/jabref/issues/3)
                
                ### Fixed
                
                - We fixed something on stable only. [#9](https://github.com/JabRef/jabref/issues/9)
                - We fixed the crash. [#4](https://github.com/JabRef/jabref/issues/4)
                
                ## [6.0] - 2026-01-01
                
                [Unreleased]: https://github.com/JabRef/jabref/compare/v6.0...HEAD
                """;
        String actual = merge(base, ours, theirs).orElseThrow();
        if (!expected.equals(actual)) {
            throw new AssertionError("Unexpected merge result:\n" + actual);
        }
        if (!merge(base, ours, theirs.replace("HEAD", "stable")).isEmpty()) {
            throw new AssertionError("A change outside [Unreleased] must fall back to git merge-file");
        }
        String released = ours.replace("## [Unreleased]\n\n### Fixed\n\n- We fixed something on stable only. [#9](https://github.com/JabRef/jabref/issues/9)\n\n", "");
        String opened = merge(base, released, theirs).orElseThrow();
        if (!opened.contains("## [Unreleased]\n\n### Added\n\n- We added feature B.") || !opened.contains("- We fixed the crash. [#4](https://github.com/JabRef/jabref/issues/4)\n\n## [6.0] - 2026-01-01")) {
            throw new AssertionError("A branch without [Unreleased] must get the section created:\n" + opened);
        }
        String duplicated = base.replace("- We fixed the old bug. [#2](https://github.com/JabRef/jabref/issues/2)\n", "- We fixed the old bug. [#2](https://github.com/JabRef/jabref/issues/2)\n".repeat(2));
        String oneCopyDropped = merge(duplicated, duplicated, base).orElseThrow();
        if (!oneCopyDropped.equals(base)) {
            throw new AssertionError("Dropping one of two equal entries must remove exactly one copy:\n" + oneCopyDropped);
        }
        if (!actual.equals(merge(base, actual, theirs).orElseThrow())) {
            throw new AssertionError("Applying the same change twice must be a no-op");
        }
        System.out.println("Self-test passed");
    }
}
