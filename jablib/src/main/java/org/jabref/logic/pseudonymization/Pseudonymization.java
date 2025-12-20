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
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/**
 * This class is used to anonymize a library. It is required to make private libraries available for public use.
 * <p>
 * For "just" generating large .bib files, scripts/bib-file-generator.py can be used.
 */
@NullMarked
public class Pseudonymization {

    private static final String GROUPS_PSEUDONYM_PREFIX = "group";

    public record Result(BibDatabaseContext bibDatabaseContext, Map<String, String> valueMapping) {
    }

    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap);

        Optional<GroupTreeNode> newGroups = pseudonymizeGroups(bibDatabaseContext, fieldToValueToIdMap);

        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, stringToIntMap) ->
                stringToIntMap.forEach((value, id) -> valueMapping.put(getFieldContent(field, id), value)));

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());
        newGroups.ifPresent(result.getMetaData()::setGroups);

        bibDatabaseContext.getMetaData().getGroupSearchSyntaxVersion().ifPresent(result.getMetaData()::setGroupSearchSyntaxVersion);

        return new Result(result, valueMapping);
    }

    /**
     * @param fieldToValueToIdMap map containing the mapping from field to value to id, will be filled by this method
     */
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

                if (field == StandardField.GROUPS) {
                    List<String> groups = splitGroups(fieldContent);
                    String pseudonymizedGroups = pseudonymizeGroupValue(groups, valueToIdMap);
                    newEntry.setField(field, pseudonymizedGroups);
                } else {
                    Integer id = valueToIdMap.computeIfAbsent(fieldContent, k -> valueToIdMap.size() + 1);
                    newEntry.setField(field, getFieldContent(field, id));
                }
            }
        }
        return newEntries;
    }

    /**
     * Pseudonymizes the root group and all subgroups.
     * If no groups exist, returns empty.
     */
    private static Optional<GroupTreeNode> pseudonymizeGroups(BibDatabaseContext bibDatabaseContext, Map<Field, Map<String, Integer>> fieldToValueToIdMap) {
        MetaData metadata = bibDatabaseContext.getMetaData();
        Optional<GroupTreeNode> groupsOpt = metadata.getGroups();

        if (groupsOpt.isEmpty()) {
            return Optional.empty();
        }

        GroupTreeNode originalRoot = groupsOpt.get();
        Map<String, Integer> groupValueMap = fieldToValueToIdMap.computeIfAbsent(StandardField.GROUPS, _ -> new HashMap<>());

        GroupTreeNode newRoot = pseudonymizeGroupNode(originalRoot, groupValueMap);
        return Optional.of(newRoot);
    }

    /**
     * Recursively rewrites a group node and its children.
     * Each original group receives a generated ID, resulting in: original -> "groups-n"
     */
    private static GroupTreeNode pseudonymizeGroupNode(GroupTreeNode node, Map<String, Integer> valueToIdMap) {
        AbstractGroup originalGroup = node.getGroup();
        AbstractGroup groupCopy = originalGroup.deepCopy();

        String originalName = node.getName();
        int id = valueToIdMap.computeIfAbsent(originalName, _ -> valueToIdMap.size() + 1);
        groupCopy.nameProperty().setValue(getFieldContent(StandardField.GROUPS, id));

        GroupTreeNode newNode = new GroupTreeNode(groupCopy);
        for (GroupTreeNode child : node.getChildren()) {
            GroupTreeNode childCopy = pseudonymizeGroupNode(child, valueToIdMap);
            newNode.addChild(childCopy);
        }

        return newNode;
    }

    private static List<String> splitGroups(String content) {
        return List.of(content.split("\\s*,\\s*"));
    }

    private static String pseudonymizeGroupValue(List<String> values, Map<String, Integer> valueToIdMap) {
        List<String> pseudonymized = new ArrayList<>(values.size());

        for (String value : values) {
            Integer id = valueToIdMap.computeIfAbsent(value, k -> valueToIdMap.size() + 1);
            pseudonymized.add(GROUPS_PSEUDONYM_PREFIX + "-" + id);
        }

        return String.join(", ", pseudonymized);
    }

    private static String getFieldContent(Field field, int id) {
        String prefix = field == StandardField.GROUPS
                        ? GROUPS_PSEUDONYM_PREFIX
                        : field.getName().toLowerCase(Locale.ROOT);

        return prefix + "-" + id;
    }
}
