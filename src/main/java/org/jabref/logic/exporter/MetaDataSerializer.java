package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.AbstractCitationKeyPattern;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;

/**
 * Reading is done at {@link org.jabref.logic.importer.util.MetaDataParser}
 */
public class MetaDataSerializer {

    private MetaDataSerializer() {
    }

    /**
     * Writes all data in the format &lt;key, serialized data>.
     */
    public static Map<String, String> getSerializedStringMap(MetaData metaData,
                                                             GlobalCitationKeyPattern globalCiteKeyPattern) {

        // metadata-key, list of contents
        //  - contents to be separated by OS.NEWLINE
        //  - each meta data item is written as separate @Comment entry - see org.jabref.logic.exporter.BibtexDatabaseWriter.writeMetaDataItem
        Map<String, List<String>> stringyMetaData = new HashMap<>();

        // First write all meta data except groups
        metaData.getSaveOrder().ifPresent(
                saveOrderConfig -> stringyMetaData.put(MetaData.SAVE_ORDER_CONFIG, saveOrderConfig.getAsStringList()));
        metaData.getSaveActions().ifPresent(
                saveActions -> stringyMetaData.put(MetaData.SAVE_ACTIONS, saveActions.getAsStringList(OS.NEWLINE)));
        if (metaData.isProtected()) {
            stringyMetaData.put(MetaData.PROTECTED_FLAG_META, Collections.singletonList("true"));
        }
        stringyMetaData.putAll(serializeCiteKeyPattern(metaData, globalCiteKeyPattern));
        metaData.getMode().ifPresent(
                mode -> stringyMetaData.put(MetaData.DATABASE_TYPE, Collections.singletonList(mode.getAsString())));
        metaData.getDefaultFileDirectory().ifPresent(
                path -> stringyMetaData.put(MetaData.FILE_DIRECTORY, Collections.singletonList(path.trim())));
        metaData.getUserFileDirectories().forEach((user, path) -> stringyMetaData
                .put(MetaData.FILE_DIRECTORY + '-' + user, Collections.singletonList(path.trim())));
        metaData.getLatexFileDirectories().forEach((user, path) -> stringyMetaData
                .put(MetaData.FILE_DIRECTORY_LATEX + '-' + user, Collections.singletonList(path.toString().trim())));
        metaData.getVersionDBStructure().ifPresent(
                versionDBStructure -> stringyMetaData.put(MetaData.VERSION_DB_STRUCT, Collections.singletonList(versionDBStructure.trim())));

        for (ContentSelector selector : metaData.getContentSelectorList()) {
            stringyMetaData.put(MetaData.SELECTOR_META_PREFIX + selector.getField().getName(), selector.getValues());
        }

        Map<String, String> serializedMetaData = serializeMetaData(stringyMetaData);

        // Write groups if present.
        // Skip this if only the root node exists (which is always the AllEntriesGroup).
        metaData.getGroups().filter(root -> root.getNumberOfChildren() > 0).ifPresent(
                root -> serializedMetaData.put(MetaData.GROUPSTREE, serializeGroups(root)));

        // finally add all unknown meta data items to the serialization map
        Map<String, List<String>> unknownMetaData = metaData.getUnknownMetaData();
        for (Map.Entry<String, List<String>> entry : unknownMetaData.entrySet()) {
            // The last "MetaData.SEPARATOR_STRING" adds compatibility to JabRef v5.9 and earlier
            StringJoiner value = new StringJoiner(MetaData.SEPARATOR_STRING + OS.NEWLINE, OS.NEWLINE, MetaData.SEPARATOR_STRING + OS.NEWLINE);
            for (String line : entry.getValue()) {
                value.add(line.replace(MetaData.SEPARATOR_STRING, "\\" + MetaData.SEPARATOR_STRING));
            }
            serializedMetaData.put(entry.getKey(), value.toString());
        }

        return serializedMetaData;
    }

    private static Map<String, String> serializeMetaData(Map<String, List<String>> stringyMetaData) {
        Map<String, String> serializedMetaData = new TreeMap<>();
        for (Map.Entry<String, List<String>> metaItem : stringyMetaData.entrySet()) {
            List<String> itemList = metaItem.getValue();
            if (itemList.isEmpty()) {
                // Only add non-empty values
                continue;
            }

            boolean isSaveActions = metaItem.getKey().equals(MetaData.SAVE_ACTIONS);
            // The last "MetaData.SEPARATOR_STRING" adds compatibility to JabRef v5.9 and earlier
            StringJoiner joiner = new StringJoiner(MetaData.SEPARATOR_STRING, "", MetaData.SEPARATOR_STRING);
            boolean lastWasSaveActionsEnablement = false;
            for (String dataItem : itemList) {
                String string;
                if (lastWasSaveActionsEnablement) {
                    string = OS.NEWLINE;
                } else {
                    string = "";
                }
                string += StringUtil.quote(dataItem, MetaData.SEPARATOR_STRING, MetaData.ESCAPE_CHARACTER);
                // in case of save actions, add an additional newline after the enabled flag
                lastWasSaveActionsEnablement = isSaveActions
                        && (FieldFormatterCleanups.ENABLED.equals(dataItem)
                        || FieldFormatterCleanups.DISABLED.equals(dataItem));
                joiner.add(string);
            }
            String serializedItem = joiner.toString();
            if (!serializedItem.isEmpty()) {
                // Only add non-empty values
                serializedMetaData.put(metaItem.getKey(), serializedItem);
            }
        }
        return serializedMetaData;
    }

    private static Map<String, List<String>> serializeCiteKeyPattern(MetaData metaData, GlobalCitationKeyPattern globalCitationKeyPattern) {
        Map<String, List<String>> stringyPattern = new HashMap<>();
        AbstractCitationKeyPattern citationKeyPattern = metaData.getCiteKeyPattern(globalCitationKeyPattern);
        for (EntryType key : citationKeyPattern.getAllKeys()) {
            if (!citationKeyPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(citationKeyPattern.getValue(key).get(0));
                String metaDataKey = MetaData.PREFIX_KEYPATTERN + key.getName();
                stringyPattern.put(metaDataKey, data);
            }
        }
        if ((citationKeyPattern.getDefaultValue() != null) && !citationKeyPattern.getDefaultValue().isEmpty()) {
            List<String> data = new ArrayList<>();
            data.add(citationKeyPattern.getDefaultValue().get(0));
            stringyPattern.put(MetaData.KEYPATTERNDEFAULT, data);
        }
        return stringyPattern;
    }

    private static String serializeGroups(GroupTreeNode root) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(OS.NEWLINE);

        for (String groupNode : new GroupSerializer().serializeTree(root)) {
            stringBuilder.append(StringUtil.quote(groupNode, MetaData.SEPARATOR_STRING, MetaData.ESCAPE_CHARACTER));
            stringBuilder.append(MetaData.SEPARATOR_STRING);
            stringBuilder.append(OS.NEWLINE);
        }
        return stringBuilder.toString();
    }

    public static String serializeCustomEntryTypes(BibEntryType entryType) {
        StringBuilder builder = new StringBuilder();
        builder.append(MetaData.ENTRYTYPE_FLAG);
        builder.append(entryType.getType().getName());
        builder.append(": req[");
        builder.append(FieldFactory.serializeOrFieldsList(entryType.getRequiredFields()));
        builder.append("] opt[");
        builder.append(FieldFactory.serializeFieldsList(
                entryType.getOptionalFields()
                         .stream()
                         .map(BibField::field)
                         .collect(Collectors.toList())));
        builder.append("]");
        return builder.toString();
    }
}
