/// This class is used to anonymize a library. It is required to make private libraries available for testing.
///
/// For "just" generating large .bib files, scripts/bib-file-generator.py can be used.
package org.jabref.logic.pseudonymization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class Pseudonymization {

    public record Result(BibDatabaseContext bibDatabaseContext, Map<String, String> valueMapping) {
    }

    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // Use LinkedHashMap to ensure deterministic ID assignment (fix for unit tests)
        Map<Field, Map<String, Integer>> privacyMap = new LinkedHashMap<>();

        // First, anonymize the Group Tree (Metadata).
        Optional<GroupTreeNode> rootGroup = bibDatabaseContext.getMetaData().getGroups();
        if (rootGroup.isPresent()) {
            Map<String, Integer> groupNameMap = new LinkedHashMap<>();

            GroupTreeNode newRoot = pseudonymizeGroupRecursively(rootGroup.get(), groupNameMap);
            bibDatabaseContext.getMetaData().setGroups(newRoot);

            privacyMap.put(StandardField.GROUPS, groupNameMap);
        }

        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, privacyMap);

        Map<String, String> valueMapping = new LinkedHashMap<>();
        privacyMap.forEach((field, valueToId) ->
                valueToId.forEach((value, id) ->
                        valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value))
        );

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());
        result.setMetaData(bibDatabaseContext.getMetaData());

        return new Result(result, valueMapping);
    }

    private GroupTreeNode pseudonymizeGroupRecursively(GroupTreeNode node, Map<String, Integer> groupNameMap) {
        AbstractGroup oldGroup = node.getGroup();
        String oldName = oldGroup.getName();

        Integer id = groupNameMap.computeIfAbsent(oldName, k -> groupNameMap.size() + 1);
        String newName = "groups-" + id;

        AbstractGroup newGroup = new ExplicitGroup(
                newName,
                oldGroup.getHierarchicalContext(),
                ','
        );
        GroupTreeNode newNode = new GroupTreeNode(newGroup);

        for (GroupTreeNode child : node.getChildren()) {
            newNode.addChild(pseudonymizeGroupRecursively(child, groupNameMap));
        }

        return newNode;
    }

    private static List<BibEntry> pseudonymizeEntries(BibDatabaseContext bibDatabaseContext, Map<Field, Map<String, Integer>> privacyMap) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);

            for (Field field : entry.getFields()) {
                Map<String, Integer> valueToId = privacyMap.computeIfAbsent(field, k -> new LinkedHashMap<>());
                String content = entry.getField(field).get();

                Integer id = valueToId.computeIfAbsent(content, k -> valueToId.size() + 1);
                newEntry.setField(field, field.getName() + "-" + id);
            }
        }
        return newEntries;
    }
}
