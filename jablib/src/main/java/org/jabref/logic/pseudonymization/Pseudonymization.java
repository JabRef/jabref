package org.jabref.logic.pseudonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;

/// This class is used to anonymize a library. It is required to make private libraries available for public use.
///
/// For "just" generating large .bib files, scripts/bib-file-generator.py can be used.
@NullMarked
public class Pseudonymization {

    public record Result(BibDatabaseContext bibDatabaseContext, Map<String, String> valueMapping) {
    }

    /// @param bibDatabaseContext
    /// @return
    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap);

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());

        bibDatabaseContext.getMetaData().getGroups().ifPresent(groups -> {
            Map<String, Integer> groupsMap = fieldToValueToIdMap.computeIfAbsent(StandardField.GROUPS, k -> new HashMap<>());
            GroupTreeNode newGroups = pseudonymizeGroup(groups, groupsMap);
            result.getMetaData().setGroups(newGroups);
        });

        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, stringToIntMap) -> stringToIntMap.forEach((value, id) -> valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value)));

        return new Result(result, valueMapping);
    }

    /// @param fieldToValueToIdMap map containing the mapping from field to value to id, will be filled by this method
    private static List<BibEntry> pseudonymizeEntries(BibDatabaseContext bibDatabaseContext, Map<Field, Map<String, Integer>> fieldToValueToIdMap) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);
            for (Field field : entry.getFields()) {
                Map<String, Integer> valueToIdMap = fieldToValueToIdMap.computeIfAbsent(field, k -> new HashMap<>());
                // TODO: Use {@link org.jabref.model.entry.field.FieldProperty} to distinguish cases.
                //       See {@link org.jabref.model.entry.field.StandardField} for usages.
                String fieldContent = entry.getField(field).get();
                Integer id = valueToIdMap.computeIfAbsent(fieldContent, k -> valueToIdMap.size() + 1);
                newEntry.setField(field, field.getName() + "-" + id);
            }
        }
        return newEntries;
    }

    /// @param node
    /// @param groupNameToIdMap
    /// @return

    private static GroupTreeNode pseudonymizeGroup(GroupTreeNode node, Map<String, Integer> groupNameToIdMap) {
        AbstractGroup oldGroup = node.getGroup();
        AbstractGroup newGroup = oldGroup.deepCopy();

        String name = oldGroup.getName();
        Integer id = groupNameToIdMap.computeIfAbsent(name, k -> groupNameToIdMap.size() + 1);
        newGroup.nameProperty().set("groups-" + id);

        GroupTreeNode newNode = new GroupTreeNode(newGroup);
        for (GroupTreeNode child : node.getChildren()) {
            newNode.addChild(pseudonymizeGroup(child, groupNameToIdMap));
        }
        return newNode;
    }
}
