package org.jabref.migrations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NonNull;

/// Converts legacy explicit groups, where the group contained a list of assigned entries, to the new format,
/// where the entry stores a list of groups it belongs to.
///
/// Also covers the group tree of JabRef 3.x as a whole (`groupstree` metadata key, no icon/color/description columns):
/// JabRef only writes the current format, so this migration is not optional.
public class ConvertLegacyExplicitGroups implements PostOpenMigration {

    @Override
    public boolean isMigrationNecessary(ParserResult parserResult) {
        return parserResult.getMetaData().isGroupsInLegacyFormat()
                || parserResult.getMetaData().getGroups().map(root -> !getExplicitGroupsWithLegacyKeys(root).isEmpty()).orElse(false);
    }

    @Override
    public String getId() {
        return "legacyGroups";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Groups are stored in the format of JabRef 3. Convert them to the current format, which stores the members of a group in the field 'groups' of each entry.");
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public void performMigration(@NonNull ParserResult parserResult, List<BibEntry> entries) {
        MetaData metaData = parserResult.getMetaData();
        metaData.getGroups().ifPresentOrElse(root -> {
            List<ExplicitGroup> groups = getExplicitGroupsWithLegacyKeys(root);
            if (!groups.isEmpty()) {
                migrateMemberships(groups, entries);
            }
            metaData.markGroupsAsCurrent();
            metaData.setGroups(root);
        }, metaData::markGroupsAsCurrent);
    }

    private void migrateMemberships(List<ExplicitGroup> groups, List<BibEntry> entries) {
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
