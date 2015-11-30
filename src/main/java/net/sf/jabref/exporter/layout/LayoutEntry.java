/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.exporter.layout.format.NameFormatter;
import net.sf.jabref.exporter.layout.format.NotFoundFormatter;
import net.sf.jabref.gui.preftabs.NameFormatterTab;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.util.Util;

class LayoutEntry {

    private LayoutFormatter[] option;

    // Formatter to be run after other formatters:
    private LayoutFormatter postFormatter;

    private String text;

    private LayoutEntry[] layoutEntries;

    private final int type;

    private final String classPrefix;

    private ArrayList<String> invalidFormatter;

    // used at highlighting in preview area.
    // Color chosen similar to JTextComponent.getSelectionColor(), which is
    // used at highlighting words at the editor
    public static final String HIGHLIGHT_COLOR = "#3399FF";

    private static final Log LOGGER = LogFactory.getLog(LayoutEntry.class);


    public LayoutEntry(StringInt si, String classPrefix_) {
        type = si.i;
        classPrefix = classPrefix_;

        if (si.i == LayoutHelper.IS_LAYOUT_TEXT) {
            text = si.s;
        } else if (si.i == LayoutHelper.IS_SIMPLE_FIELD) {
            text = si.s.trim();
        } else if ((si.i == LayoutHelper.IS_FIELD_START) || (si.i == LayoutHelper.IS_FIELD_END)) {
            // Do nothing
        } else if (si.i == LayoutHelper.IS_OPTION_FIELD) {
            Vector<String> v = new Vector<>();
            WSITools.tokenize(v, si.s, "\n");

            if (v.size() == 1) {
                text = v.get(0);
            } else {
                text = v.get(0).trim();

                option = LayoutEntry.getOptionalLayout(v.get(1), classPrefix);
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

    public LayoutEntry(Vector<StringInt> parsedEntries, String classPrefix_, int layoutType) {
        classPrefix = classPrefix_;
        String blockStart;
        String blockEnd;
        StringInt si;
        Vector<StringInt> blockEntries = null;
        Vector<LayoutEntry> tmpEntries = new Vector<>();
        LayoutEntry le;
        si = parsedEntries.get(0);
        blockStart = si.s;
        si = parsedEntries.get(parsedEntries.size() - 1);
        blockEnd = si.s;

        if (!blockStart.equals(blockEnd)) {
            LOGGER.warn("Field start and end entry must be equal.");
        }

        type = layoutType;
        text = si.s;

        for (int i = 1; i < (parsedEntries.size() - 1); i++) {
            si = parsedEntries.get(i);

            // System.out.println("PARSED-ENTRY: "+si.s+"="+si.i);
            if ((si.i == LayoutHelper.IS_LAYOUT_TEXT) || (si.i == LayoutHelper.IS_SIMPLE_FIELD)) {
                // Do nothing
            } else if ((si.i == LayoutHelper.IS_FIELD_START)
                    || (si.i == LayoutHelper.IS_GROUP_START)) {
                blockEntries = new Vector<>();
                blockStart = si.s;
            } else if ((si.i == LayoutHelper.IS_FIELD_END) || (si.i == LayoutHelper.IS_GROUP_END)) {
                if (blockStart.equals(si.s)) {
                    blockEntries.add(si);
                    if (si.i == LayoutHelper.IS_GROUP_END) {
                        le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_GROUP_START);
                    } else {
                        le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_FIELD_START);
                    }
                    tmpEntries.add(le);
                    blockEntries = null;
                } else {
                    LOGGER.warn("Nested field entries are not implemented !!!");
                }
            } else if (si.i == LayoutHelper.IS_OPTION_FIELD) {
                // Do nothing
            }

            // else if (si.i == LayoutHelper.IS_OPTION_FIELD_PARAM)
            // {
            // }
            if (blockEntries == null) {
                // System.out.println("BLOCK ADD: "+si.s+"="+si.i);
                tmpEntries.add(new LayoutEntry(si, classPrefix));
            } else {
                blockEntries.add(si);
            }
        }

        layoutEntries = new LayoutEntry[tmpEntries.size()];

        for (int i = 0; i < tmpEntries.size(); i++) {
            layoutEntries[i] = tmpEntries.get(i);

            // Note if one of the entries has an invalid formatter:
            if (layoutEntries[i].isInvalidFormatter()) {
                if (invalidFormatter == null) {
                    invalidFormatter = new ArrayList<>(1);
                }
                invalidFormatter.addAll(layoutEntries[i].getInvalidFormatters());
            }

        }

    }

    public void setPostFormatter(LayoutFormatter formatter) {
        this.postFormatter = formatter;
    }

    private String doLayout(BibtexEntry bibtex, BibtexDatabase database) {
        return doLayout(bibtex, database, null);
    }

    public String doLayout(BibtexEntry bibtex, BibtexDatabase database, List<String> wordsToHighlight) {
        switch (type) {
        case LayoutHelper.IS_LAYOUT_TEXT:
            return text;
        case LayoutHelper.IS_SIMPLE_FIELD:
            String value = BibtexDatabase.getResolvedField(text, bibtex, database);

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
                field = BibtexDatabase.getResolvedField(text, bibtex, database);
            } else if (text.matches(".*(;|(\\&+)).*")) {
                // split the strings along &, && or ; for AND formatter
                String[] parts = text.split("\\s*(;|(\\&+))\\s*");
                field = null;
                for (String part : parts) {
                    field = BibtexDatabase.getResolvedField(part, bibtex, database);
                    if (field == null) {
                        break;
                    }

                }
            } else {
                // split the strings along |, ||  for OR formatter
                String[] parts = text.split("\\s*(\\|+)\\s*");
                field = null;
                for (String part : parts) {
                    field = BibtexDatabase.getResolvedField(part, bibtex, database);
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

                for (int i = 0; i < layoutEntries.length; i++) {
                    fieldText = layoutEntries[i].doLayout(bibtex, database);

                    if (fieldText == null) {
                        if ((i + 1) < layoutEntries.length) {
                            if (layoutEntries[i + 1].doLayout(bibtex, database).trim().isEmpty()) {
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
                            //System.out.println("ENTRY-BLOCK: " +
                            //layoutEntries[i].doLayout(bibtex));

                            /*
                             * if fieldText is not null and the bibtexentry is marked
                             * as a searchhit, try to highlight the searched words
                             *
                            */
                            if (bibtex.isSearchHit()) {
                                sb.append(highlightWords(fieldText, wordsToHighlight));
                            }
                            else {
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
                fieldEntry = bibtex.getType().getName();
            } else {
                // changed section begin - arudert
                // resolve field (recognized by leading backslash) or text
                String field = text.startsWith("\\") ? BibtexDatabase.getResolvedField(text.substring(1), bibtex, database)
                        : BibtexDatabase.getText(text, database);
                // changed section end - arudert
                if (field == null) {
                    fieldEntry = "";
                } else {
                    fieldEntry = field;
                }
            }

            //System.out.println("OPTION: "+option);
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
                return BibtexDatabase.getResolvedField("encoding", bibtex, database);
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
    public String doLayout(BibtexDatabase database, Charset encoding) {
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
            String field = BibtexDatabase.getText(text, database);
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
        }
        else if (type == LayoutHelper.IS_FILENAME) {
            File f = Globals.prefs.databaseFile;
            return f != null ? f.getName() : "";
        }
        else if (type == LayoutHelper.IS_FILEPATH) {
            File f = Globals.prefs.databaseFile;
            return f != null ? f.getPath() : "";
        }
        return "";
    }


    // added section - end (arudert)

    private static LayoutFormatter getLayoutFormatterByClassName(String className, String classPrefix)
            throws Exception {

        if (!className.isEmpty()) {
            try {
                try {
                    return (LayoutFormatter) Class.forName(classPrefix + className).newInstance();
                } catch (Throwable ex2) {
                    return (LayoutFormatter) Class.forName(className).newInstance();
                }
            } catch (ClassNotFoundException ex) {
                throw new Exception("Formatter not found: " + className);
            } catch (InstantiationException ex) {
                throw new Exception(className + " cannot be instantiated.");
            } catch (IllegalAccessException ex) {
                throw new Exception(className + " cannot be accessed.");
            }
        }
        return null;
    }

    /**
     * Return an array of LayoutFormatters found in the given formatterName
     * string (in order of appearance).
     *
     */
    private static LayoutFormatter[] getOptionalLayout(String formatterName, String classPrefix) {

        ArrayList<String[]> formatterStrings = Util.parseMethodsCalls(formatterName);

        ArrayList<LayoutFormatter> results = new ArrayList<>(formatterStrings.size());

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
                LayoutFormatter f = LayoutEntry.getLayoutFormatterByClassName(className, classPrefix);
                // If this formatter accepts an argument, check if we have one, and
                // set it if so:
                if (f instanceof ParamLayoutFormatter) {
                    if (strings.length >= 2) {
                        ((ParamLayoutFormatter) f).setArgument(strings[1]);
                    }
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

        return results.toArray(new LayoutFormatter[results.size()]);
    }

    public boolean isInvalidFormatter() {
        return invalidFormatter != null;
    }

    public ArrayList<String> getInvalidFormatters() {
        return invalidFormatter;
    }

    /**
     * Will return the text that was called by the method with HTML tags to highlight each word the user has searched
     * for and will skip the highlight process if the first Char isn't a letter or a digit.
     *
     * This check is a quick hack to avoid highlighting of HTML tags It does not always work, but it does its job mostly
     *
     * @param text This is a String in which we search for different words
     * @param wordsToHighlight List of all words which must be highlighted
     * 
     * @return String that was called by the method, with HTML Tags if a word was found
     */
    private String highlightWords(String text, List<String> wordsToHighlight) {
        if (wordsToHighlight == null) {
            return text;
        }

        Matcher matcher = Util.getPatternForWords(wordsToHighlight).matcher(text);

        if (Character.isLetterOrDigit(text.charAt(0))) {
            String hlColor = HIGHLIGHT_COLOR;
            StringBuffer sb = new StringBuffer();
            boolean foundSomething = false;

            String found;
            while (matcher.find()) {
                matcher.end();
                found = matcher.group();
                // color the search keyword	-
                // put first String Part and then html + word + html to a StringBuffer
                matcher.appendReplacement(sb, "<span style=\"background-color:" + hlColor + ";\">" + found + "</span>");
                foundSomething = true;
            }

            if (foundSomething) {
                matcher.appendTail(sb);
                text = sb.toString();
            }

        }
        return text;
    }
}
