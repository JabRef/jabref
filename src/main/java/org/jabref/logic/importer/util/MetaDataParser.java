package org.jabref.logic.importer.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataParser.class);
    private static FileUpdateMonitor fileMonitor;

    public MetaDataParser(FileUpdateMonitor fileMonitor) {
        MetaDataParser.fileMonitor = fileMonitor;
    }

    /**
     * Parses the given data map and returns a new resulting {@link MetaData} instance.
     */
    public MetaData parse(Map<String, String> data, Character keywordSeparator) throws ParseException {
        return parse(new MetaData(), data, keywordSeparator);
    }

    /**
     * Parses the data map and changes the given {@link MetaData} instance respectively.
     */
    public MetaData parse(MetaData metaData, Map<String, String> data, Character keywordSeparator) throws ParseException {
        List<String> defaultCiteKeyPattern = new ArrayList<>();
        Map<String, List<String>> nonDefaultCiteKeyPatterns = new HashMap<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            List<String> value = getAsList(entry.getValue());

            if (entry.getKey().startsWith(MetaData.PREFIX_KEYPATTERN)) {
                String entryType = entry.getKey().substring(MetaData.PREFIX_KEYPATTERN.length());
                nonDefaultCiteKeyPatterns.put(entryType, Collections.singletonList(getSingleItem(value)));
                continue;
            } else if (entry.getKey().startsWith(MetaData.FILE_DIRECTORY + '-')) {
                // The user name comes directly after "FILE_DIRECTORY-"
                String user = entry.getKey().substring(MetaData.FILE_DIRECTORY.length() + 1);
                metaData.setUserFileDirectory(user, getSingleItem(value));
                continue;
            } else if (entry.getKey().startsWith(MetaData.SELECTOR_META_PREFIX)) {
                metaData.addContentSelector(ContentSelectors.parse(entry.getKey().substring(MetaData.SELECTOR_META_PREFIX.length()), StringUtil.unquote(entry.getValue(), MetaData.ESCAPE_CHARACTER)));
                continue;
            }

            switch (entry.getKey()) {
                case MetaData.GROUPSTREE:
                case MetaData.GROUPSTREE_LEGACY:
                    metaData.setGroups(GroupsParser.importGroups(value, keywordSeparator, fileMonitor));
                    break;
                case MetaData.SAVE_ACTIONS:
                    metaData.setSaveActions(Cleanups.parse(value));
                    break;
                case MetaData.DATABASE_TYPE:
                    metaData.setMode(BibDatabaseMode.parse(getSingleItem(value)));
                    break;
                case MetaData.KEYPATTERNDEFAULT:
                    defaultCiteKeyPattern = Collections.singletonList(getSingleItem(value));
                    break;
                case MetaData.PROTECTED_FLAG_META:
                    if (Boolean.parseBoolean(getSingleItem(value))) {
                        metaData.markAsProtected();
                    } else {
                        metaData.markAsNotProtected();
                    }
                    break;
                case MetaData.FILE_DIRECTORY:
                    metaData.setDefaultFileDirectory(getSingleItem(value));
                    break;
                case MetaData.SAVE_ORDER_CONFIG:
                    metaData.setSaveOrderConfig(SaveOrderConfig.parse(value));
                    break;
                default:
                    // Keep meta data items that we do not know in the file
                    metaData.putUnkownMetaDataItem(entry.getKey(), value);
            }
        }
        if (!defaultCiteKeyPattern.isEmpty() || !nonDefaultCiteKeyPatterns.isEmpty()) {
            metaData.setCiteKeyPattern(defaultCiteKeyPattern, nonDefaultCiteKeyPatterns);
        }

        return metaData;
    }

    /**
     * Returns the first item in the list.
     * If the specified list does not contain exactly one item, then a {@link ParseException} will be thrown.
     * @param value
     * @return
     */
    private static String getSingleItem(List<String> value) throws ParseException {
        if (value.size() == 1) {
            return value.get(0);
        } else {
            throw new ParseException("Expected a single item but received " + value.toString());
        }
    }

    private static List<String> getAsList(String value) throws ParseException {
        StringReader valueReader = new StringReader(value);
        List<String> orderedValue = new ArrayList<>();

        // We must allow for ; and \ in escape sequences.
        try {
            Optional<String> unit;
            while ((unit = getNextUnit(valueReader)).isPresent()) {
                orderedValue.add(unit.get());
            }
        } catch (IOException ex) {
            LOGGER.error("Weird error while parsing meta data.", ex);
            throw new ParseException("Weird error while parsing meta data.", ex);
        }
        return orderedValue;
    }

    /**
     * Reads the next unit. Units are delimited by ';' (MetaData.SEPARATOR_CHARACTER).
     */
    private static Optional<String> getNextUnit(Reader reader) throws IOException {
        int c;
        boolean escape = false;
        StringBuilder res = new StringBuilder();
        while ((c = reader.read()) != -1) {
            if (escape) {
                res.append((char) c);
                escape = false;
            } else if (c == MetaData.ESCAPE_CHARACTER) {
                escape = true;
            } else if (c == MetaData.SEPARATOR_CHARACTER) {
                break;
            } else {
                res.append((char) c);
            }
        }
        if (res.length() > 0) {
            return Optional.of(res.toString());
        }
        return Optional.empty();
    }
}
