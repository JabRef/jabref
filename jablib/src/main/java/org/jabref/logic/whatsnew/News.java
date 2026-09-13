package org.jabref.logic.whatsnew;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;

/// The changelog entries a developer has not seen yet, grouped for display by who wrote them.
///
/// Which entries are news is decided against the set of entries announced before, not against a commit range:
/// so nothing is shown twice, nothing is missed while JabRef is closed, and a reworded entry counts as new.
// [impl->req~whats-new.checkout-news~1]
public record News(List<AttributedEntry> items) {
    public static final News NONE = new News(List.of());

    public News {
        items = List.copyOf(items);
    }

    /// The entries of `changelogs` that `announced` does not hold. An entry is known by its text: a release
    /// moves every entry from *Unreleased* under the release's section, and that must not make it news again.
    /// An entry appearing in several changelogs counts once, attributed as the first changelog has it: so the
    /// working tree goes first, and an entry pulled already is not "pushed from another machine".
    public static News pending(Set<ChangelogEntry> announced, List<BlamedChangelog> changelogs) {
        Set<String> announcedTexts = announced.stream().map(ChangelogEntry::text).collect(Collectors.toSet());
        SequencedMap<String, AttributedEntry> fresh = new LinkedHashMap<>();
        for (BlamedChangelog changelog : changelogs) {
            for (AttributedEntry item : changelog.entries()) {
                if (!announcedTexts.contains(item.entry().text())) {
                    fresh.putIfAbsent(item.entry().text(), item);
                }
            }
        }
        return new News(List.copyOf(fresh.values()));
    }

    /// Every entry of `changelogs`, once: what [#pending] does not report after they are announced.
    public static SequencedSet<ChangelogEntry> allEntries(List<BlamedChangelog> changelogs) {
        SequencedSet<ChangelogEntry> entries = new LinkedHashSet<>();
        for (BlamedChangelog changelog : changelogs) {
            changelog.entries().forEach(item -> entries.add(item.entry()));
        }
        return entries;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    /// The items by contributor, in [Contributor#DISPLAY_ORDER]; within a group in changelog order.
    public SequencedMap<Contributor, List<AttributedEntry>> grouped() {
        SequencedMap<Contributor, List<AttributedEntry>> groups = new LinkedHashMap<>();
        items.stream()
             .map(AttributedEntry::by)
             .distinct()
             .sorted(Contributor.DISPLAY_ORDER)
             .forEach(by -> groups.put(by, items.stream().filter(item -> item.by().equals(by)).toList()));
        return groups;
    }

    /// The title of a group, localized.
    public static String groupTitle(Contributor contributor) {
        return switch (contributor) {
            case Contributor.Other other ->
                    Localization.lang("Changes by %0", other.name());
            case Contributor.Me.REMOTE ->
                    Localization.lang("Changes by me (pushed from another machine)");
            case Contributor.Me.LOCAL ->
                    Localization.lang("Changes by me");
        };
    }

    /// The groups as text, one bullet per entry with the Markdown emphasis dropped — a tooltip or a terminal.
    public String asPlainText() {
        StringJoiner groups = new StringJoiner("\n\n");
        grouped().forEach((contributor, entries) -> {
            StringJoiner group = new StringJoiner("\n");
            group.add(groupTitle(contributor));
            entries.forEach(item -> group.add("• " + item.entry().text().replace("**", "")));
            groups.add(group.toString());
        });
        return groups.toString();
    }
}
