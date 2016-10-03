package net.sf.jabref.logic.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.cleanup.FieldFormatterCleanups;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.model.strings.StringUtil;

public class MetaDataSerializer {

    /**
     * Writes all data in the format <key, serialized data>.
     */
    public static Map<String, String> getSerializedStringMap(MetaData metaData,
            GlobalBibtexKeyPattern globalCiteKeyPattern) {

        Map<String, String> serializedMetaData = new TreeMap<>();

        // First write all meta data except groups
        Map<String, List<String>> stringyMetaData = new HashMap<>();
        metaData.getSaveOrderConfig().ifPresent(
                saveOrderConfig -> stringyMetaData.put(MetaData.SAVE_ORDER_CONFIG, saveOrderConfig.getAsStringList()));
        metaData.getSaveActions().ifPresent(
                saveActions -> stringyMetaData.put(MetaData.SAVE_ACTIONS, saveActions.getAsStringList(OS.NEWLINE)));
        if (metaData.isProtected()) {
                stringyMetaData.put(MetaData.PROTECTED_FLAG_META, Collections.singletonList("true"));
        }
        AbstractBibtexKeyPattern citeKeyPattern = metaData.getCiteKeyPattern(globalCiteKeyPattern);
        for (String key : citeKeyPattern.getAllKeys()) {
            if (!citeKeyPattern.isDefaultValue(key)) {
                List<String> data = new ArrayList<>();
                data.add(citeKeyPattern.getValue(key).get(0));
                String metaDataKey = MetaData.PREFIX_KEYPATTERN + key;
                stringyMetaData.put(metaDataKey, data);
            }
        }
        if (citeKeyPattern.getDefaultValue() != null) {
            List<String> data = new ArrayList<>();
            data.add(citeKeyPattern.getDefaultValue().get(0));
            stringyMetaData.put(MetaData.KEYPATTERNDEFAULT, data);
        }
        metaData.getMode().ifPresent(
                mode -> stringyMetaData.put(MetaData.DATABASE_TYPE, Collections.singletonList(mode.getAsString())));
        metaData.getDefaultFileDirectory().ifPresent(
                path -> stringyMetaData.put(MetaData.FILE_DIRECTORY, Collections.singletonList(path.trim())));
        metaData.getUserFileDirectories().forEach(
                (user, path) -> stringyMetaData.put(MetaData.FILE_DIRECTORY + '-' + user, Collections.singletonList(path.trim())));

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

        // Write groups if present.
        // Skip this if only the root node exists (which is always the AllEntriesGroup).
        metaData.getGroups().filter(root -> root.getNumberOfChildren() > 0).ifPresent(
                root -> serializedMetaData.put(MetaData.GROUPSTREE, serializeGroups(root)));
        return serializedMetaData;
    }

    private static String serializeGroups(GroupTreeNode root) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(OS.NEWLINE);

        for (String groupNode : root.getTreeAsString()) {
            stringBuilder.append(StringUtil.quote(groupNode, MetaData.SEPARATOR_STRING, MetaData.ESCAPE_CHARACTER));
            stringBuilder.append(MetaData.SEPARATOR_STRING);
            stringBuilder.append(OS.NEWLINE);
        }
        return stringBuilder.toString();
    }

}
