package org.jabref.migrations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.ObservableList;

import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jspecify.annotations.NonNull;

/// Converts the markings of JabRef 4 and older (field `__markedentry`) to groups.
public class ConvertMarkingToGroups implements PostOpenMigration {

    private static final Pattern MARKING_PATTERN = Pattern.compile("\\[([^\\[\\]]*):(\\d+)\\]");

    @Override
    public boolean isMigrationNecessary(ParserResult parserResult) {
        return parserResult.getDatabase().getEntries().stream().anyMatch(entry -> entry.hasField(InternalField.MARKED_INTERNAL));
    }

    @Override
    public String getId() {
        return "markings";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Entries are marked using the field '__markedentry' (JabRef 4 and older). Convert the markings to groups.");
    }

    @Override
    public void performMigration(@NonNull ParserResult parserResult) {
        ObservableList<BibEntry> entries = parserResult.getDatabase().getEntries();
        Multimap<String, BibEntry> markings = getMarkingWithEntries(entries);
        if (!markings.isEmpty()) {
            GroupTreeNode root = parserResult.getMetaData().getGroups()
                                             .orElseGet(() -> GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup()));
            // Group names are unique per library, so an existing "Markings" tree (e.g., from a partial earlier run) is extended
            GroupTreeNode markingRoot = childNamed(root, Localization.lang("Markings"))
                    .orElseGet(() -> {
                        GroupTreeNode node = GroupTreeNode.fromGroup(new ExplicitGroup(Localization.lang("Markings"), GroupHierarchyType.INCLUDING, ','));
                        root.addChild(node, 0);
                        return node;
                    });
            for (Map.Entry<String, Collection<BibEntry>> marking : markings.asMap().entrySet()) {
                GroupTreeNode markingGroup = childNamed(markingRoot, marking.getKey())
                        .orElseGet(() -> markingRoot.addSubgroup(new ExplicitGroup(marking.getKey(), GroupHierarchyType.INCLUDING, ',')));
                markingGroup.addEntriesToGroup(marking.getValue());
            }
            parserResult.getMetaData().setGroups(root);
        }
        // Blank markings carry no information, but the field has to go, otherwise the migration is offered on every open
        clearMarkings(entries);
    }

    private static Optional<GroupTreeNode> childNamed(GroupTreeNode parent, String name) {
        return parent.getChildren().stream().filter(child -> child.getName().equals(name)).findFirst();
    }

    /// Looks for markings (such as __markedentry = {[Nicolas:6]}) in the given list of entries.
    private Multimap<String, BibEntry> getMarkingWithEntries(List<BibEntry> entries) {
        Multimap<String, BibEntry> markings = MultimapBuilder.treeKeys().linkedListValues().build();

        for (BibEntry entry : entries) {
            Optional<String> marking = entry.getField(InternalField.MARKED_INTERNAL);
            if (marking.isEmpty()) {
                continue;
            }

            // JabRef 2/3 concatenated one "[owner:level]" token per user; older versions stored the bare user name.
            // Every token becomes a group, and whatever is left over is kept as its own group so nothing is lost.
            Matcher matcher = MARKING_PATTERN.matcher(marking.get());
            while (matcher.find()) {
                markings.put(matcher.group(1) + ":" + matcher.group(2), entry);
            }
            String remainder = matcher.replaceAll("").trim();
            if (!remainder.isEmpty()) {
                markings.put(remainder, entry);
            }
        }

        return markings;
    }

    private void clearMarkings(List<BibEntry> entries) {
        entries.forEach(entry -> entry.clearField(InternalField.MARKED_INTERNAL));
    }
}
