/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.exporter.layout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import net.sf.jabref.gui.search.MatchesHighlighter;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.util.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.exporter.layout.format.JournalAbbreviator;
import net.sf.jabref.exporter.layout.format.NameFormatter;
import net.sf.jabref.exporter.layout.format.NotFoundFormatter;
import net.sf.jabref.gui.preftabs.NameFormatterTab;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

class LayoutEntry {

    private List<LayoutFormatter> option;

    // Formatter to be run after other formatters:
    private LayoutFormatter postFormatter;

    private String text;

    private List<LayoutEntry> layoutEntries;

    private final int type;

    private List<String> invalidFormatter;

    private static final Log LOGGER = LogFactory.getLog(LayoutEntry.class);


    public LayoutEntry(StringInt si, JournalAbbreviationRepository repository) {
        type = si.i;

        if (type == LayoutHelper.IS_LAYOUT_TEXT) {
            text = si.s;
        } else if (type == LayoutHelper.IS_SIMPLE_FIELD) {
            text = si.s.trim();
        } else if ((type == LayoutHelper.IS_FIELD_START) || (type == LayoutHelper.IS_FIELD_END)) {
            // Do nothing
        } else if (type == LayoutHelper.IS_OPTION_FIELD) {
            List<String> v = StringUtil.tokenizeToList(si.s, "\n");

            if (v.size() == 1) {
                text = v.get(0);
            } else {
                text = v.get(0).trim();

                option = LayoutEntry.getOptionalLayout(v.get(1), repository);
                // See if there was an undefined formatter:
                for (LayoutFormatter anOption : option) {
                    if (anOption instanceof NotFoundFormatter) {
                        String notFound = ((NotFoundFormatter) anOption).getNotFound();

                        if (invalidFormatter == null) {
                            invalidFormatter = new ArrayList<>();
                        }
                        invalidFormatter.add(notFound);
                    }
                }

            }
        }
    }

    public LayoutEntry(List<StringInt> parsedEntries, int layoutType, JournalAbbreviationRepository repository) {
        List<StringInt> blockEntries = null;
        List<LayoutEntry> tmpEntries = new ArrayList<>();
        LayoutEntry le;
        String blockStart = parsedEntries.get(0).s;
        String blockEnd = parsedEntries.get(parsedEntries.size() - 1).s;

        if (!blockStart.equals(blockEnd)) {
            LOGGER.warn("Field start and end entry must be equal.");
        }

        type = layoutType;
        text = blockEnd;
        for (StringInt parsedEntry : parsedEntries.subList(1, parsedEntries.size() - 1)) {
            if ((parsedEntry.i == LayoutHelper.IS_LAYOUT_TEXT) || (parsedEntry.i == LayoutHelper.IS_SIMPLE_FIELD)) {
                // Do nothing
            } else if ((parsedEntry.i == LayoutHelper.IS_FIELD_START)
                    || (parsedEntry.i == LayoutHelper.IS_GROUP_START)) {
                blockEntries = new ArrayList<>();
                blockStart = parsedEntry.s;
            } else if ((parsedEntry.i == LayoutHelper.IS_FIELD_END) || (parsedEntry.i == LayoutHelper.IS_GROUP_END)) {
                if (blockStart.equals(parsedEntry.s)) {
                    blockEntries.add(parsedEntry);
                    if (parsedEntry.i == LayoutHelper.IS_GROUP_END) {
                        le = new LayoutEntry(blockEntries, LayoutHelper.IS_GROUP_START, repository);
                    } else {
                        le = new LayoutEntry(blockEntries, LayoutHelper.IS_FIELD_START, repository);
                    }
                    tmpEntries.add(le);
                    blockEntries = null;
                } else {
                    LOGGER.warn("Nested field entries are not implemented !!!");
                }
            } else if (parsedEntry.i == LayoutHelper.IS_OPTION_FIELD) {
                // Do nothing
            }

            if (blockEntries == null) {
                tmpEntries.add(new LayoutEntry(parsedEntry, repository));
            } else {
                blockEntries.add(parsedEntry);
            }
        }

        layoutEntries = new ArrayList<>(tmpEntries);

        for (LayoutEntry layoutEntry : layoutEntries) {
            if (layoutEntry.isInvalidFormatter()) {
                if (invalidFormatter == null) {
                    invalidFormatter = new ArrayList<>(1);
                }
                invalidFormatter.addAll(layoutEntry.getInvalidFormatters());
            }

        }

    }

    public void setPostFormatter(LayoutFormatter formatter) {
        this.postFormatter = formatter;
    }

    private String doLayout(BibEntry bibtex, BibDatabase database) {
        return doLayout(bibtex, database, null);
    }

    public String doLayout(BibEntry bibtex, BibDatabase database, Optional<Pattern> highlightPattern) {
        switch (type) {
        case LayoutHelper.IS_LAYOUT_TEXT:
            return text;
        case LayoutHelper.IS_SIMPLE_FIELD:
            String value = BibDatabase.getResolvedField(text, bibtex, database);

            if (value == null) {
                value = "";
            }
            // If a post formatter has been set, call it:
            if (postFormatter != null) {
                value = postFormatter.format(value);
            }
            return value;
        case LayoutHelper.IS_FIELD_START:
        case LayoutHelper.IS_GROUP_START: {
            String field;
            if (type == LayoutHelper.IS_GROUP_START) {
                field = BibDatabase.getResolvedField(text, bibtex, database);
            } else if (text.matches(".*(;|(\\&+)).*")) {
                // split the strings along &, && or ; for AND formatter
                String[] parts = text.split("\\s*(;|(\\&+))\\s*");
                field = null;
                for (String part : parts) {
                    field = BibDatabase.getResolvedField(part, bibtex, database);
                    if (field == null) {
                        break;
                    }

                }
            } else {
                // split the strings along |, ||  for OR formatter
                String[] parts = text.split("\\s*(\\|+)\\s*");
                field = null;
                for (String part : parts) {
                    field = BibDatabase.getResolvedField(part, bibtex, database);
                    if (field != null) {
                        break;
                    }
                }
            }

            if ((field == null)
                    || ((type == LayoutHelper.IS_GROUP_START) && field.equalsIgnoreCase(LayoutHelper
                            .getCurrentGroup()))) {
                return null;
            } else {
                if (type == LayoutHelper.IS_GROUP_START) {
                    LayoutHelper.setCurrentGroup(field);
                }
                StringBuilder sb = new StringBuilder(100);
                String fieldText;
                boolean previousSkipped = false;

                for (int i = 0; i < layoutEntries.size(); i++) {
                    fieldText = layoutEntries.get(i).doLayout(bibtex, database);

                    if (fieldText == null) {
                        if ((i + 1) < layoutEntries.size()) {
                            if (layoutEntries.get(i + 1).doLayout(bibtex, database).trim().isEmpty()) {
                                i++;
                                previousSkipped = true;
                                continue;
                            }
                        }
                    } else {

                        // if previous was skipped --> remove leading line
                        // breaks
                        if (previousSkipped) {
                            int eol = 0;

                            while ((eol < fieldText.length())
                                    && ((fieldText.charAt(eol) == '\n') || (fieldText.charAt(eol) == '\r'))) {
                                eol++;
                            }

                            if (eol < fieldText.length()) {
                                sb.append(fieldText.substring(eol));
                            }
                        } else {
                            /*
                             * if fieldText is not null and the bibtexentry is marked
                             * as a searchhit, try to highlight the searched words
                             *
                            */
                            if (bibtex.isSearchHit()) {
                                sb.append(MatchesHighlighter.highlightWordsWithHTML(fieldText, highlightPattern));
                            } else {
                                sb.append(fieldText);
                            }

                        }
                    }

                    previousSkipped = false;
                }

                return sb.toString();
            }
        }
        case LayoutHelper.IS_FIELD_END:
        case LayoutHelper.IS_GROUP_END:
            return "";
        case LayoutHelper.IS_OPTION_FIELD:
            String fieldEntry;

            if ("bibtextype".equals(text)) {
                fieldEntry = bibtex.getType();
            } else {
                // changed section begin - arudert
                // resolve field (recognized by leading backslash) or text
                String field = text.startsWith("\\") ? BibDatabase.getResolvedField(text.substring(1), bibtex, database)
                        : BibDatabase.getText(text, database);
                // changed section end - arudert
                if (field == null) {
                    fieldEntry = "";
                } else {
                    fieldEntry = field;
                }
            }

            if (option != null) {
                for (LayoutFormatter anOption : option) {
                    fieldEntry = anOption.format(fieldEntry);
                }
            }

            // If a post formatter has been set, call it:
            if (postFormatter != null) {
                fieldEntry = postFormatter.format(fieldEntry);
            }

            return fieldEntry;
            case LayoutHelper.IS_ENCODING_NAME:
                // Printing the encoding name is not supported in entry layouts, only
                // in begin/end layouts. This prevents breakage if some users depend
                // on a field called "encoding". We simply return this field instead:
                return BibDatabase.getResolvedField("encoding", bibtex, database);
            default:
            return "";
        }
    }

    // added section - begin (arudert)
    /**
     * Do layout for general formatters (no bibtex-entry fields).
     *
     * @param database
     *            Bibtex Database
     * @return
     */
    public String doLayout(BibDatabase database, Charset encoding) {
        if (type == LayoutHelper.IS_LAYOUT_TEXT) {
            return text;
        } else if (type == LayoutHelper.IS_SIMPLE_FIELD) {
            throw new UnsupportedOperationException(
                    "bibtex entry fields not allowed in begin or end layout");
        } else if ((type == LayoutHelper.IS_FIELD_START) || (type == LayoutHelper.IS_GROUP_START)) {
            throw new UnsupportedOperationException(
                    "field and group starts not allowed in begin or end layout");
        } else if ((type == LayoutHelper.IS_FIELD_END) || (type == LayoutHelper.IS_GROUP_END)) {
            throw new UnsupportedOperationException(
                    "field and group ends not allowed in begin or end layout");
        } else if (type == LayoutHelper.IS_OPTION_FIELD) {
            String field = BibDatabase.getText(text, database);
            if (option != null) {
                for (LayoutFormatter anOption : option) {
                    field = anOption.format(field);
                }
            }
            // If a post formatter has been set, call it:
            if (postFormatter != null) {
                field = postFormatter.format(field);
            }

            return field;
        } else if (type == LayoutHelper.IS_ENCODING_NAME) {
            return encoding.displayName();
        } else if (type == LayoutHelper.IS_FILENAME) {
            File f = Globals.prefs.databaseFile;
            return f == null ? "" : f.getName();
        } else if (type == LayoutHelper.IS_FILEPATH) {
            File f = Globals.prefs.databaseFile;
            return f == null ? "" : f.getPath();
        }
        return "";
    }


    // added section - end (arudert)

    private static LayoutFormatter getLayoutFormatterByClassName(String className,
            JournalAbbreviationRepository repostiory)
            throws Exception {

        if (className.isEmpty()) {
            return null;
        }

        if ("JournalAbbreviator".equals(className)) {
            return new JournalAbbreviator(repostiory);
        }

        try {
            String prefix = "net.sf.jabref.exporter.layout.format.";
            return (LayoutFormatter) Class.forName(prefix + className).newInstance();
        } catch (ClassNotFoundException ex) {
            throw new Exception("Formatter not found: " + className);
        } catch (InstantiationException ex) {
            throw new Exception(className + " cannot be instantiated.");
        } catch (IllegalAccessException ex) {
            throw new Exception(className + " cannot be accessed.");
        }
    }

    /**
     * Return an array of LayoutFormatters found in the given formatterName
     * string (in order of appearance).
     * @param repository
     *
     */
    private static List<LayoutFormatter> getOptionalLayout(String formatterName,
            JournalAbbreviationRepository repository) {

        List<String[]> formatterStrings = Util.parseMethodsCalls(formatterName);

        List<LayoutFormatter> results = new ArrayList<>(formatterStrings.size());

        Map<String, String> userNameFormatter = NameFormatterTab.getNameFormatters();

        for (String[] strings : formatterStrings) {

            String className = strings[0].trim();

            // Check if this is a name formatter defined by this export filter:
            if (Globals.prefs.customExportNameFormatters != null) {
                String contents = Globals.prefs.customExportNameFormatters.get(className);
                if (contents != null) {
                    NameFormatter nf = new NameFormatter();
                    nf.setParameter(contents);
                    results.add(nf);
                    continue;
                }
            }

            // Try to load from formatters in formatter folder
            try {
                LayoutFormatter f = LayoutEntry.getLayoutFormatterByClassName(className, repository);
                // If this formatter accepts an argument, check if we have one, and
                // set it if so:
                if ((f instanceof ParamLayoutFormatter) && (strings.length >= 2)) {
                        ((ParamLayoutFormatter) f).setArgument(strings[1]);
                }
                results.add(f);
                continue;
            } catch (Exception ignored) {
                // Ignored
            }

            // Then check whether this is a user defined formatter
            String formatterParameter = userNameFormatter.get(className);

            if (formatterParameter != null) {
                NameFormatter nf = new NameFormatter();
                nf.setParameter(formatterParameter);
                results.add(nf);
                continue;
            }

            // If not found throw exception...
            //return new LayoutFormatter[] {new NotFoundFormatter(className)};
            results.add(new NotFoundFormatter(className));
            //throw new Exception(Globals.lang("Formatter not found") + ": "+ className);
        }

        return results;
    }

    public boolean isInvalidFormatter() {
        return invalidFormatter != null;
    }

    public List<String> getInvalidFormatters() {
        return invalidFormatter;
    }

}
