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
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
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

    private final Character keywordSeparator;

    public Pseudonymization(Character keywordSeparator) {
        this.keywordSeparator = keywordSeparator;
    }

    public record Result(BibDatabaseContext bibDatabaseContext, Map<String, String> valueMapping) {
    }

    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings

        Map<String, String> valueMapping = new HashMap<>();
        Map<String, String> groupNameMapping = new HashMap<>();

        Optional<GroupTreeNode> groups = bibDatabaseContext.getMetaData().getGroups();
        Optional<GroupTreeNode> pseudonymizedGroups = Optional.empty();
        if (groups.isPresent()) {
            GroupTreeResult groupResult = pseudonymizeGroupTree(groups.get(), valueMapping, groupNameMapping, 1);
            pseudonymizedGroups = Optional.of(groupResult.node());
        }

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap, groupNameMapping, valueMapping);

        fieldToValueToIdMap.forEach((field, stringToIntMap) ->
                stringToIntMap.forEach((value, id) -> valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value)));

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());

        pseudonymizedGroups.ifPresent(newGroups -> result.getMetaData().setGroups(newGroups));

        return new Result(result, valueMapping);
    }

    /// @param fieldToValueToIdMap map containing the mapping from field to value to id, will be filled by this method
    private List<BibEntry> pseudonymizeEntries(BibDatabaseContext bibDatabaseContext,
                                               Map<Field, Map<String, Integer>> fieldToValueToIdMap,
                                               Map<String, String> groupNameMapping,
                                               Map<String, String> valueMapping) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);
            for (Field field : entry.getFields()) {
                if (field.equals(StandardField.GROUPS)) {
                    String fieldContent = entry.getField(field).orElse("");
                    if (!fieldContent.isBlank()) {
                        newEntry.setField(field, pseudonymizeGroupsField(fieldContent, groupNameMapping, valueMapping));
                    }
                    continue;
                }

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

    /// Pseudonymizes the {@link StandardField#GROUPS} field content.
    /// Groups not present in the group tree are also pseudonymized.
    private String pseudonymizeGroupsField(String fieldContent,
                                           Map<String, String> groupNameMapping,
                                           Map<String, String> valueMapping) {
        KeywordList keywords = KeywordList.parse(fieldContent, keywordSeparator);
        KeywordList pseudonymizedKeywords = new KeywordList();
        for (Keyword keyword : keywords) {
            String originalName = keyword.toString();
            String pseudoName = groupNameMapping.get(originalName);
            if (pseudoName == null) {
                pseudoName = "group-" + (groupNameMapping.size() + 1);
                groupNameMapping.put(originalName, pseudoName);
                valueMapping.put(pseudoName, originalName);
            }
            pseudonymizedKeywords.add(pseudoName);
        }
        return pseudonymizedKeywords.getAsString(keywordSeparator);
    }

    private record GroupTreeResult(GroupTreeNode node, int counter) {
    }

    private static GroupTreeResult pseudonymizeGroupTree(GroupTreeNode node,
                                                         Map<String, String> valueMapping,
                                                         Map<String, String> groupNameMapping,
                                                         int counter) {
        AbstractGroup originalGroup = node.getGroup();
        String originalName = originalGroup.getName();
        String pseudoName = "group-" + counter;
        counter++;
        valueMapping.put(pseudoName, originalName);
        groupNameMapping.put(originalName, pseudoName);

        AbstractGroup newGroup = originalGroup.deepCopy();
        newGroup.nameProperty().set(pseudoName);

        GroupTreeNode newNode = new GroupTreeNode(newGroup);
        for (GroupTreeNode child : node.getChildren()) {
            GroupTreeResult childResult = pseudonymizeGroupTree(child, valueMapping, groupNameMapping, counter);
            newNode.addChild(childResult.node());
            counter = childResult.counter();
        }
        return new GroupTreeResult(newNode, counter);
    }
}
