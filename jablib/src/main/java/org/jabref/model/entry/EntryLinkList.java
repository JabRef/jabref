package org.jabref.model.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;

public class EntryLinkList {

    public static final String SEPARATOR = ",";

    private EntryLinkList() {
    }

    public static List<ParsedEntryLink> parse(String fieldValue, BibDatabase database) {
        List<ParsedEntryLink> result = new ArrayList<>();
        if (!StringUtil.isNullOrEmpty(fieldValue)) {
            String[] entries = fieldValue.split(SEPARATOR);
            for (String entry : entries) {
                result.add(new ParsedEntryLink(entry, database));
            }
        }
        return result;
    }

    public static String serialize(List<ParsedEntryLink> list) {
        return list.stream().map(ParsedEntryLink::getKey).collect(Collectors.joining(SEPARATOR));
    }
}
