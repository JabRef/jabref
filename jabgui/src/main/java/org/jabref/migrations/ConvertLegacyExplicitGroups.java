package org.jabref.migrations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NonNull;

/// Converts legacy explicit groups, where the group contained a list of assigned entries, to the new format,
/// where the entry stores a list of groups it belongs to.
public class ConvertLegacyExplicitGroups implements PostOpenMigration {

    @Override
    public boolean isMigrationNecessary(ParserResult parserResult) {
        return parserResult.getMetaData().getGroups()
                           .map(root -> !getExplicitGroupsWithLegacyKeys(root).isEmpty())
                           .orElse(false);
    }

    @Override
    public void performMigration(@NonNull ParserResult parserResult, List<BibEntry> entries) {
        if (parserResult.getMetaData().getGroups().isEmpty()) {
            return;
        }

        List<ExplicitGroup> groups = getExplicitGroupsWithLegacyKeys(parserResult.getMetaData().getGroups().orElseThrow());
        if (groups.isEmpty()) {
            return;
        }

        Map<String, List<BibEntry>> entriesByCitationKey = new HashMap<>();
        for (BibEntry entry : entries) {
            entry.getCitationKey().ifPresent(key -> entriesByCitationKey.computeIfAbsent(key, _ -> new ArrayList<>()).add(entry));
        }

        for (ExplicitGroup group : groups) {
            for (String entryKey : group.getLegacyEntryKeys()) {
                for (BibEntry entry : entriesByCitationKey.getOrDefault(entryKey, List.of())) {
                    group.add(entry);
                }
            }
            group.clearLegacyEntryKeys();
        }
    }

    private List<ExplicitGroup> getExplicitGroupsWithLegacyKeys(@NonNull GroupTreeNode node) {
        List<ExplicitGroup> findings = new ArrayList<>();

        if (node.getGroup() instanceof ExplicitGroup group) {
            if (!group.getLegacyEntryKeys().isEmpty()) {
                findings.add(group);
            }
        }

        node.getChildren().forEach(child -> findings.addAll(getExplicitGroupsWithLegacyKeys(child)));

        return findings;
    }
}
