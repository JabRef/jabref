package org.jabref.migrations;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.ObservableList;

import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

/**
 * Converts legacy explicit groups, where the group contained a list of assigned entries, to the new format,
 * where the entry stores a list of groups it belongs to.
 */
public class ConvertMarkingToGroups implements PostOpenMigration {

    private static final Pattern MARKING_PATTERN = Pattern.compile("\\[(.*):(\\d+)\\]");

    @Override
    public void performMigration(ParserResult parserResult) {
        Objects.requireNonNull(parserResult);

        ObservableList<BibEntry> entries = parserResult.getDatabase().getEntries();
        Multimap<String, BibEntry> markings = getMarkingWithEntries(entries);
        if (!markings.isEmpty()) {
            GroupTreeNode markingRoot = GroupTreeNode.fromGroup(
                    new ExplicitGroup(Localization.lang("Markings"), GroupHierarchyType.INCLUDING, ','));

            for (Map.Entry<String, Collection<BibEntry>> marking : markings.asMap().entrySet()) {
                String markingName = marking.getKey();
                Collection<BibEntry> markingMatchedEntries = marking.getValue();

                GroupTreeNode markingGroup = markingRoot.addSubgroup(
                        new ExplicitGroup(markingName, GroupHierarchyType.INCLUDING, ','));
                markingGroup.addEntriesToGroup(markingMatchedEntries);
            }

            if (!parserResult.getMetaData().getGroups().isPresent()) {
                parserResult.getMetaData().setGroups(GroupTreeNode.fromGroup(DefaultGroupsFactory.getAllEntriesGroup()));
            }
            GroupTreeNode root = parserResult.getMetaData().getGroups().get();
            root.addChild(markingRoot, 0);
            parserResult.getMetaData().setGroups(root);

            clearMarkings(entries);
        }
    }

    /**
     * Looks for markings (such as __markedentry = {[Nicolas:6]}) in the given list of entries.
     */
    private Multimap<String, BibEntry> getMarkingWithEntries(List<BibEntry> entries) {
        Multimap<String, BibEntry> markings = MultimapBuilder.treeKeys().linkedListValues().build();

        for (BibEntry entry : entries) {
            Optional<String> marking = entry.getField(InternalField.MARKED_INTERNAL);
            if (!marking.isPresent()) {
                continue;
            }

            Matcher matcher = MARKING_PATTERN.matcher(marking.get());
            if (matcher.find()) {
                String owner = matcher.group(1);
                String number = matcher.group(2);
                markings.put(owner + ":" + number, entry);
            } else {
                // Not in the expected format, so just add it to not loose information
                markings.put(marking.get(), entry);
            }
        }

        return markings;
    }

    private void clearMarkings(List<BibEntry> entries) {
        entries.forEach(entry -> entry.clearField(InternalField.MARKED_INTERNAL));
    }
}
