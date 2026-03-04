package org.jabref.logic.pseudonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
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

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap);

        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, stringToIntMap) ->
                stringToIntMap.forEach((value, id) -> valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value)));

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());

        Optional<GroupTreeNode> groups = bibDatabaseContext.getMetaData().getGroups();
        if (groups.isPresent()) {
            GroupTreeNode newGroups = pseudonymizeGroupTree(groups.get(), valueMapping, 1).node();
            result.getMetaData().setGroups(newGroups);
        }

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

    private record GroupTreeResult(GroupTreeNode node, int counter) {
    }

    private static GroupTreeResult pseudonymizeGroupTree(GroupTreeNode node, Map<String, String> valueMapping, int counter) {
        AbstractGroup originalGroup = node.getGroup();
        String originalName = originalGroup.getName();
        String pseudoName = "group-" + counter;
        counter++;
        valueMapping.put(pseudoName, originalName);
        AbstractGroup newGroup = new ExplicitGroup(pseudoName, originalGroup.getHierarchicalContext(), ',');
        GroupTreeNode newNode = new GroupTreeNode(newGroup);
        for (GroupTreeNode child : node.getChildren()) {
            GroupTreeResult childResult = pseudonymizeGroupTree(child, valueMapping, counter);
            newNode.addChild(childResult.node());
            counter = childResult.counter();
        }
        return new GroupTreeResult(newNode, counter);
    }
}
