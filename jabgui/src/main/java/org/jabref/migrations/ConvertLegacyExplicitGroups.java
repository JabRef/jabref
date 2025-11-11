package org.jabref.migrations;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NonNull;

/**
 * Converts legacy explicit groups, where the group contained a list of assigned entries, to the new format,
 * where the entry stores a list of groups it belongs to.
 */
public class ConvertLegacyExplicitGroups implements PostOpenMigration {

    @Override
    public void performMigration(@NonNull ParserResult parserResult) {
        if (parserResult.getMetaData().getGroups().isEmpty()) {
            return;
        }

        for (ExplicitGroup group : getExplicitGroupsWithLegacyKeys(parserResult.getMetaData().getGroups().get())) {
            for (String entryKey : group.getLegacyEntryKeys()) {
                for (BibEntry entry : parserResult.getDatabase().getEntriesByCitationKey(entryKey)) {
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
