package net.sf.jabref.logic.importer.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.sf.jabref.logic.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.groups.GroupsParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.ParseException;
import net.sf.jabref.model.metadata.MetaData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetaDataParser {

    private static final Log LOGGER = LogFactory.getLog(MetaDataParser.class);

    public static MetaData parse(Map<String, String> data, Character keywordSeparator) throws ParseException {
        MetaData metaData = new MetaData();
        metaData.setParsedData(getParsedData(data, keywordSeparator, metaData));
        return metaData;
    }

    public static Map<String, List<String>> getParsedData(Map<String, String> inData, Character keywordSeparator,
            MetaData metaData)
            throws ParseException {
        final Map<String, List<String>> newMetaData = new HashMap<>();
        for (Map.Entry<String, String> entry : inData.entrySet()) {
            StringReader data = new StringReader(entry.getValue());
            List<String> orderedData = new ArrayList<>();
            // We must allow for ; and \ in escape sequences.
            try {
                Optional<String> unit;
                while ((unit = getNextUnit(data)).isPresent()) {
                    orderedData.add(unit.get());
                }
            } catch (IOException ex) {
                LOGGER.error("Weird error while parsing meta data.", ex);
            }
            if (MetaData.GROUPSTREE.equals(entry.getKey())) {
                try {
                    metaData.setGroups(GroupsParser.importGroups(orderedData, keywordSeparator));
                // the keys "groupsversion" and "groups" were used in JabRef versions around 1.3, we will not support them anymore
                } catch (ParseException e) {
                    throw new ParseException(
                            Localization
                                    .lang("Group tree could not be parsed. If you save the BibTeX database, all groups will be lost."),
                            e);
                }
            } else if (MetaData.SAVE_ACTIONS.equals(entry.getKey())) {
                newMetaData.put(MetaData.SAVE_ACTIONS, FieldFormatterCleanups.parse(orderedData).getAsStringList()); // Without MetaDataChangedEvent
            } else if (entry.getKey().startsWith("selector_")) {
                // ignore old content selector metadata
            } else {
                newMetaData.put(entry.getKey(), orderedData);
            }
        }
        return newMetaData;
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
