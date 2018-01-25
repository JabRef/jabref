package org.jabref.logic.layout;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for formatting DOCUMENT ME!
 */
public class Layout {

    private static final Logger LOGGER = LoggerFactory.getLogger(Layout.class);

    private final List<LayoutEntry> layoutEntries;

    private final List<String> missingFormatters = new ArrayList<>();


    public Layout(List<StringInt> parsedEntries, LayoutFormatterPreferences prefs) {
        List<LayoutEntry> tmpEntries = new ArrayList<>(parsedEntries.size());

        List<StringInt> blockEntries = null;
        LayoutEntry le;
        String blockStart = null;

        for (StringInt parsedEntry : parsedEntries) {
            switch (parsedEntry.i) {
            case LayoutHelper.IS_LAYOUT_TEXT:
            case LayoutHelper.IS_SIMPLE_FIELD:
            case LayoutHelper.IS_OPTION_FIELD:
                // Do nothing
                break;
            case LayoutHelper.IS_FIELD_START:
            case LayoutHelper.IS_GROUP_START:
                blockEntries = new ArrayList<>();
                blockStart = parsedEntry.s;
                break;
            case LayoutHelper.IS_FIELD_END:
            case LayoutHelper.IS_GROUP_END:
                if ((blockStart != null) && (blockEntries != null)) {
                    if (blockStart.equals(parsedEntry.s)) {
                        blockEntries.add(parsedEntry);
                        le = new LayoutEntry(blockEntries,
                                parsedEntry.i == LayoutHelper.IS_FIELD_END ? LayoutHelper.IS_FIELD_START : LayoutHelper.IS_GROUP_START,
                                prefs);
                        tmpEntries.add(le);
                        blockEntries = null;
                    } else {
                        LOGGER.debug(blockStart + '\n' + parsedEntry.s);
                        LOGGER.warn("Nested field/group entries are not implemented!");
                        Thread.dumpStack();
                    }
                }
                break;
            default:
                break;
            }

            if (blockEntries == null) {
                tmpEntries.add(new LayoutEntry(parsedEntry, prefs));
            } else {
                blockEntries.add(parsedEntry);
            }
        }

        layoutEntries = new ArrayList<>(tmpEntries);

        for (LayoutEntry layoutEntry : layoutEntries) {
            missingFormatters.addAll(layoutEntry.getInvalidFormatters());
        }
    }

    public void setPostFormatter(LayoutFormatter formatter) {
        for (LayoutEntry layoutEntry : layoutEntries) {
            layoutEntry.setPostFormatter(formatter);
        }
    }

    /**
     * Returns the processed bibtex entry. If the database argument is
     * null, no string references will be resolved. Otherwise all valid
     * string references will be replaced by the strings' contents. Even
     * recursive string references are resolved.
     */
    public String doLayout(BibEntry bibtex, BibDatabase database) {
        StringBuilder builder = new StringBuilder(100);

        for (LayoutEntry layoutEntry : layoutEntries) {
            String fieldText = layoutEntry.doLayout(bibtex, database);

            // The following change means we treat null fields as "". This is to fix the
            // problem of whitespace disappearing after missing fields.
            if (fieldText == null) {
                fieldText = "";
            }

            builder.append(fieldText);
        }

        return builder.toString();
    }

    /**
     * Returns the processed text. If the database argument is
     * null, no string references will be resolved. Otherwise all valid
     * string references will be replaced by the strings' contents. Even
     * recursive string references are resolved.
     */
    public String doLayout(BibDatabaseContext databaseContext, Charset encoding) {
        StringBuilder sb = new StringBuilder(100);
        String fieldText;

        for (LayoutEntry layoutEntry : layoutEntries) {
            fieldText = layoutEntry.doLayout(databaseContext, encoding);

            if (fieldText == null) {
                fieldText = "";
            }

            sb.append(fieldText);
        }

        return sb.toString();
    }

    // added section - end (arudert)

    public List<String> getMissingFormatters() {
        return new ArrayList<>(missingFormatters);
    }
}
