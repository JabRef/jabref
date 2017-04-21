package org.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jabref.logic.util.OS;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.strings.StringUtil;

public class MetaDataSerializer {

    private MetaDataSerializer() {
    }

    /**
     * Writes all data in the format <key, serialized data>.
     */
    public static Map<String, String> getSerializedStringMap(MetaData metaData,
            GlobalBibtexKeyPattern globalCiteKeyPattern) {

        // First write all meta data except groups
        Map<String, List<String>> stringyMetaData = new HashMap<>();
        metaData.getSaveOrderConfig().ifPresent(
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

        for (ContentSelector selector: metaData.getContentSelectorList()) {
                stringyMetaData.put(MetaData.SELECTOR_META_PREFIX + selector.getFieldName(), selector.getValues());

        }

        Map<String, String> serializedMetaData = serializeMetaData(stringyMetaData);

        // Write groups if present.
        // Skip this if only the root node exists (which is always the AllEntriesGroup).
        metaData.getGroups().filter(root -> root.getNumberOfChildren() > 0).ifPresent(
                root -> serializedMetaData.put(MetaData.GROUPSTREE, serializeGroups(root)));

        // finally add all unknown meta data items to the serialization map
        Map<String, List<String>> unknownMetaData = metaData.getUnknownMetaData();
        for (Map.Entry<String, List<String>> entry : unknownMetaData.entrySet()) {
            StringBuilder value = new StringBuilder();
            value.append(OS.NEWLINE);
            for (String line: entry.getValue()) {
                value.append(line.replaceAll(";", "\\\\;") + MetaData.SEPARATOR_STRING + OS.NEWLINE);
            }
            serializedMetaData.put(entry.getKey(), value.toString());
        }

        return serializedMetaData;
    }

    private static Map<String, String> serializeMetaData(Map<String, List<String>> stringyMetaData) {
        Map<String, String> serializedMetaData = new TreeMap<>();
        for (Map.Entry<String, List<String>> metaItem : stringyMetaData.entrySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String dataItem : metaItem.getValue()) {
                stringBuilder.append(StringUtil.quote(dataItem, MetaData.SEPARATOR_STRING, MetaData.ESCAPE_CHARACTER)).append(MetaData.SEPARATOR_STRING);

                //in case of save actions, add an additional newline after the enabled flag
                if (metaItem.getKey().equals(MetaData.SAVE_ACTIONS)
                        && (FieldFormatterCleanups.ENABLED.equals(dataItem)
                                || FieldFormatterCleanups.DISABLED.equals(dataItem))) {
                    stringBuilder.append(OS.NEWLINE);
                }
            }

            String serializedItem = stringBuilder.toString();
            // Only add non-empty values
            if (!serializedItem.isEmpty() && !MetaData.SEPARATOR_STRING.equals(serializedItem)) {
                serializedMetaData.put(metaItem.getKey(), serializedItem);
            }
        }
        return serializedMetaData;
    }

    private static Map<String, List<String>> serializeCiteKeyPattern(MetaData metaData, GlobalBibtexKeyPattern globalCiteKeyPattern) {
        Map<String, List<String>> stringyPattern = new HashMap<>();
        AbstractBibtexKeyPattern citeKeyPattern = metaData.getCiteKeyPattern(globalCiteKeyPattern);
        for (String key : citeKeyPattern.getAllKeys()) {
            if (!citeKeyPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(citeKeyPattern.getValue(key).get(0));
                String metaDataKey = MetaData.PREFIX_KEYPATTERN + key;
                stringyPattern.put(metaDataKey, data);
            }
        }
        if ((citeKeyPattern.getDefaultValue() != null) && !citeKeyPattern.getDefaultValue().isEmpty()) {
            List<String> data = new ArrayList<>();
            data.add(citeKeyPattern.getDefaultValue().get(0));
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

}
