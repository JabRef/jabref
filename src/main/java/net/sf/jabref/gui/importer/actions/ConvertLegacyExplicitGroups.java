package net.sf.jabref.gui.importer.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupTreeNode;

/**
 * Converts legacy explicit groups, where the group contained a list of assigned entries, to the new format,
 * where the entry stores a list of groups it belongs to.
 */
public class ConvertLegacyExplicitGroups implements PostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult pr) {
        if (pr.getMetaData().getGroups().isPresent()) {
            return !getExplicitGroupsWithLegacyKeys(pr.getMetaData().getGroups().orElse(null)).isEmpty();
        }
        return false;
    }

    @Override
    public void performAction(BasePanel panel, ParserResult pr) {
        Objects.requireNonNull(pr);
        if (!pr.getMetaData().getGroups().isPresent()) {
            return;
        }

        for (ExplicitGroup group : getExplicitGroupsWithLegacyKeys(pr.getMetaData().getGroups().get())) {
            for (String entryKey : group.getLegacyEntryKeys()) {
                for (BibEntry entry : pr.getDatabase().getEntriesByKey(entryKey)) {
                    group.add(entry);
                }
            }
            group.clearLegacyEntryKeys();
        }
    }

    private List<ExplicitGroup> getExplicitGroupsWithLegacyKeys(GroupTreeNode node) {
        Objects.requireNonNull(node);
        List<ExplicitGroup> findings = new ArrayList<>();

        if (node.getGroup() instanceof ExplicitGroup) {
            ExplicitGroup group = (ExplicitGroup) node.getGroup();
            if (!group.getLegacyEntryKeys().isEmpty()) {
                findings.add(group);
            }
        }

        node.getChildren().forEach(child -> findings.addAll(getExplicitGroupsWithLegacyKeys(child)));

        return findings;
    }
}
