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

    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings

        Map<String, String> groupNameMapping = new HashMap<>();
        bibDatabaseContext.getMetaData().getGroups().ifPresent(root ->
                pseudonymizeGroupsRecursive(root, groupNameMapping)
        );

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap, groupNameMapping);

        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, stringToIntMap) ->
                stringToIntMap.forEach((value, id) -> valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value)));

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());

        return new Result(result, valueMapping);
    }

    /// @param fieldToValueToIdMap map containing the mapping from field to value to id, will be filled by this method
    private static List<BibEntry> pseudonymizeEntries(BibDatabaseContext bibDatabaseContext, Map<Field, Map<String, Integer>> fieldToValueToIdMap, Map<String, String> groupMapping) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);

            for (Field field : entry.getFields()) {
                String fieldContent = entry.getField(field).get();
                if (field.equals(StandardField.GROUPS)) {
                    String newGroupName = groupMapping.getOrDefault(fieldContent, fieldContent);
                    newEntry.setField(field, newGroupName);
                } else {
                    Map<String, Integer> valueToIdMap = fieldToValueToIdMap.computeIfAbsent(field, k -> new HashMap<>());
                    Integer id = valueToIdMap.computeIfAbsent(fieldContent, k -> valueToIdMap.size() + 1);
                    newEntry.setField(field, field.getName() + "-" + id);
                }
            }
        }
        return newEntries;
    }

    private void pseudonymizeGroupsRecursive(GroupTreeNode node, Map<String, String> groupMapping) {
        if (!node.isRoot()) {
            String oldName = node.getName();
            String newName = "Group-" + (groupMapping.size() + 1);
            node.getGroup().nameProperty().set(newName);
            groupMapping.put(oldName, newName);
        }

        for (GroupTreeNode child : node.getChildren()) {
            pseudonymizeGroupsRecursive(child, groupMapping);
        }
    }
}
