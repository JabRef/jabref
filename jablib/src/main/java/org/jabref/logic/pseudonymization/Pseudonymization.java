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
        // Every ID assignment for a specific field is stored here
        Map<Field, Map<String, Integer>> privacyMap = new HashMap<>();

        // First, anonymize the Group Tree (Metadata). 
        // We do this before entries to ensure that if a group is renamed to "groups-1",
        // the entries belonging to it also get updated to "groups-1".
        Optional<GroupTreeNode> rootGroup = bibDatabaseContext.getMetaData().getGroups();
        if (rootGroup.isPresent()) {
            Map<String, Integer> groupNameMap = new HashMap<>();
            
            GroupTreeNode newRoot = pseudonymizeGroupRecursively(rootGroup.get(), groupNameMap);
            bibDatabaseContext.getMetaData().setGroups(newRoot);
            
            // Here we are filling the IDs generated for group names into the privacy map
            privacyMap.put(StandardField.GROUPS, groupNameMap);
        }

        // Here we process the actual bibliography entries
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, privacyMap);

        // This is the Decoder Mapping that maps pseudonymized values back to original values
        Map<String, String> valueMapping = new HashMap<>();
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

        // reuse existing ID if we've seen this name, otherwise create a new one
        Integer id = groupNameMap.computeIfAbsent(oldName, k -> groupNameMap.size() + 1);
        String newName = "groups-" + id;

        // We use ',' as the separating character for the ExplicitGroup constructor
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
                Map<String, Integer> valueToId = privacyMap.computeIfAbsent(field, k -> new HashMap<>());
                String content = entry.getField(field).get();
                
                Integer id = valueToId.computeIfAbsent(content, k -> valueToId.size() + 1);
                newEntry.setField(field, field.getName() + "-" + id);
            }
        }
        return newEntries;
    }
}