package org.jabref.logic.pseudonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.StringProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anonymizes bibliographic data while preserving structure.
 * - Entries: replace all field values with deterministic tokens, e.g., "author-1", "title-1".
 * - Groups: replace group names in MetaData with "groups-<id>" tokens (via group tree).
 * - Text fallback: optionally post-process serialized text to pseudonymize group names in "@Comment{jabref-meta: ...}".
 */
@NullMarked
public class Pseudonymization {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pseudonymization.class);

    // Supports legacy and current metadata keys where groups are stored
    private static final Pattern META_BLOCK_PATTERN = Pattern.compile(
            "@Comment\\{\\s*jabref-meta:\\s*(grouping|groupstree|groups)\\s*:\\s*([\\s\\S]*?)\\}",
            Pattern.CASE_INSENSITIVE
    );

    // Example line: "1 KeywordGroup:Used\;0\;keywords\;used\;0\;0\;1\;\;\;\;;"
    // Captures: depth, type, display name, and the remainder starting from the first "\;"
    private static final Pattern GROUP_LINE_PATTERN = Pattern.compile(
            "^(\\d+\\s+)(\\w+Group:)([^\\\\;]*?)(\\\\;.*)$"
    );

    public record Result(BibDatabaseContext bibDatabaseContext,
                         Map<String, String> valueMapping,
                         Map<Field, Map<String, Integer>> fieldToValueToIdMap) {
    }

    /**
     * Pseudonymizes entries and groups.
     * - Entries: values -> "fieldname-<id>" tokens
     * - Groups: rename group tree nodes to "groups-<id>" tokens
     * MetaData is preserved so the saver emits updated groups.
     */
    public Result pseudonymizeLibrary(BibDatabaseContext inputContext) {
        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(inputContext, fieldToValueToIdMap);

        // Build token -> original mapping for entry fields
        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, valueIndex) ->
                valueIndex.forEach((original, id) ->
                        valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, original)));

        // Preserve original MetaData so groups are available and get saved
        BibDatabase newDatabase = new BibDatabase(newEntries);
        BibDatabaseContext resultContext = new BibDatabaseContext(newDatabase, inputContext.getMetaData());
        resultContext.setMode(inputContext.getMode());

        // Pseudonymize groups via group tree (affects saved output)
        pseudonymizeGroupsInMetaData(resultContext.getMetaData(), valueMapping);

        return new Result(resultContext, valueMapping, fieldToValueToIdMap);
    }

    /**
     * Replace each encountered field value with a stable identifier "fieldName-<id>".
     */
    private static List<BibEntry> pseudonymizeEntries(BibDatabaseContext ctx,
                                                      Map<Field, Map<String, Integer>> fieldToValueToIdMap) {
        List<BibEntry> entries = ctx.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);

            for (Field field : entry.getFields()) {
                Map<String, Integer> valueToIdMap = fieldToValueToIdMap.computeIfAbsent(field, k -> new HashMap<>());
                String fieldContent = entry.getField(field).orElse("");
                Integer id = valueToIdMap.computeIfAbsent(fieldContent, k -> valueToIdMap.size() + 1);
                newEntry.setField(field, field.getName() + "-" + id);
            }
        }
        return newEntries;
    }

    /**
     * Renames groups in the MetaData group tree to "groups-<id>" tokens.
     * Skips the root node (usually AllEntriesGroup).
     */
    private static void pseudonymizeGroupsInMetaData(MetaData metaData, Map<String, String> valueMapping) {
        if (metaData == null) {
            return;
        }

        metaData.getGroups().ifPresent(root -> {
            Map<String, Integer> nameToId = new LinkedHashMap<>();
            pseudonymizeGroupNode(root, nameToId, valueMapping);
            // write back the (mutated) root to ensure save emits updated names
            metaData.setGroups(root);
        });
    }

    private static void pseudonymizeGroupNode(GroupTreeNode node,
                                              Map<String, Integer> nameToId,
                                              Map<String, String> valueMapping) {
        boolean isRoot = (node.getParent() == null);
        if (!isRoot) {
            AbstractGroup group = node.getGroup();
            String oldName = group.getName();

            if (oldName != null && !oldName.isEmpty()) {
                int id = nameToId.computeIfAbsent(oldName, k -> nameToId.size() + 1);
                String token = "groups-" + id;

                // Map token -> original name
                valueMapping.putIfAbsent(token, oldName);

                // Try to rename via reflection (setName or 'name' field)
                if (!tryRenameGroup(group, token)) {
                    LOGGER.warn("Could not rename group '{}' (type: {})", oldName, group.getClass().getSimpleName());
                }
            }
        }

        for (GroupTreeNode child : node.getChildren()) {
            pseudonymizeGroupNode(child, nameToId, valueMapping);
        }
    }

    /**
 * Renames a group by accessing its StringProperty "name" field and calling .set() on it.
 */
private static boolean tryRenameGroup(AbstractGroup group, String newName) {
    Class<?> cls = group.getClass();

    // Try to find the StringProperty field named "name"
    for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
        try {
            var nameField = c.getDeclaredField("name");
            nameField.setAccessible(true);
            Object fieldValue = nameField.get(group);
            
            // Check if it's a StringProperty and call .set() on it
            if (fieldValue instanceof javafx.beans.property.StringProperty stringProperty) {
                stringProperty.set(newName);
                return true;
            }
        } catch (NoSuchFieldException ignored) {
            // continue searching up the hierarchy
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("Field 'name' exists but couldn't be accessed", e);
            break;
        }
    }

    return false;
}
    /**
     * Fallback: Post-processes a serialized BibTeX string to anonymize group names in metadata blocks.
     * Useful if operating on raw strings. Saver path should already include updated MetaData via the group tree.
     */
    public static String pseudonymizeBibContent(String originalContent, Result anonymizationResult) {
        if ((originalContent == null) || originalContent.isBlank()) {
            return originalContent;
        }

        Map<String, Integer> groupRegistry = new LinkedHashMap<>();
        Map<String, String> mapping = anonymizationResult.valueMapping();

        Matcher blockMatcher = META_BLOCK_PATTERN.matcher(originalContent);
        StringBuilder out = new StringBuilder();

        int lastEnd = 0;
        while (blockMatcher.find()) {
            out.append(originalContent, lastEnd, blockMatcher.start());

            String metaKey = blockMatcher.group(1);     // grouping | groupstree | groups
            String metadataContent = blockMatcher.group(2);

            String anonymizedMetadata = replaceGroupNames(metadataContent, groupRegistry, mapping);

            out.append("@Comment{jabref-meta: ")
               .append(metaKey)
               .append(":\n")
               .append(anonymizedMetadata)
               .append("}");

            lastEnd = blockMatcher.end();
        }
        out.append(originalContent.substring(lastEnd));

        return out.toString();
    }

    /**
     * Replaces group display names with deterministic tokens within a metadata block.
     * Keeps structure and escape characters intact.
     */
    private static String replaceGroupNames(String metadataBlock,
                                            Map<String, Integer> groupRegistry,
                                            Map<String, String> tokenMapping) {
        String[] lines = metadataBlock.split("\\R", -1);
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = GROUP_LINE_PATTERN.matcher(line);
            if (!m.matches()) {
                output.append(line);
            } else {
                String depth = m.group(1);
                String type = m.group(2);
                String name = m.group(3);
                String tail = m.group(4);

                if ("AllEntriesGroup:".equals(type)) {
                    output.append(line);
                } else {
                    int id = groupRegistry.computeIfAbsent(name, k -> groupRegistry.size() + 1);
                    String token = "groups-" + id;
                    tokenMapping.putIfAbsent(token, name);

                    String replaced = depth + type + token + tail;
                    output.append(replaced);
                }
            }
            if (i < lines.length - 1) {
                output.append("\n");
            }
        }
        return output.toString();
    }
}