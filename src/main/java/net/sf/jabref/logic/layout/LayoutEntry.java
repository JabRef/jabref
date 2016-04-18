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
package net.sf.jabref.logic.layout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.logic.layout.format.AuthorAbbreviator;
import net.sf.jabref.logic.layout.format.AuthorAndsCommaReplacer;
import net.sf.jabref.logic.layout.format.AuthorAndsReplacer;
import net.sf.jabref.logic.layout.format.AuthorFirstAbbrLastCommas;
import net.sf.jabref.logic.layout.format.AuthorFirstAbbrLastOxfordCommas;
import net.sf.jabref.logic.layout.format.AuthorFirstFirst;
import net.sf.jabref.logic.layout.format.AuthorFirstFirstCommas;
import net.sf.jabref.logic.layout.format.AuthorFirstLastCommas;
import net.sf.jabref.logic.layout.format.AuthorFirstLastOxfordCommas;
import net.sf.jabref.logic.layout.format.AuthorLF_FF;
import net.sf.jabref.logic.layout.format.AuthorLF_FFAbbr;
import net.sf.jabref.logic.layout.format.AuthorLastFirst;
import net.sf.jabref.logic.layout.format.AuthorLastFirstAbbrCommas;
import net.sf.jabref.logic.layout.format.AuthorLastFirstAbbrOxfordCommas;
import net.sf.jabref.logic.layout.format.AuthorLastFirstAbbreviator;
import net.sf.jabref.logic.layout.format.AuthorLastFirstCommas;
import net.sf.jabref.logic.layout.format.AuthorLastFirstOxfordCommas;
import net.sf.jabref.logic.layout.format.AuthorNatBib;
import net.sf.jabref.logic.layout.format.AuthorOrgSci;
import net.sf.jabref.logic.layout.format.Authors;
import net.sf.jabref.logic.layout.format.CompositeFormat;
import net.sf.jabref.logic.layout.format.CreateBibORDFAuthors;
import net.sf.jabref.logic.layout.format.CreateDocBookAuthors;
import net.sf.jabref.logic.layout.format.CurrentDate;
import net.sf.jabref.logic.layout.format.DOICheck;
import net.sf.jabref.logic.layout.format.DOIStrip;
import net.sf.jabref.logic.layout.format.Default;
import net.sf.jabref.logic.layout.format.FileLink;
import net.sf.jabref.logic.layout.format.FirstPage;
import net.sf.jabref.logic.layout.format.FormatPagesForHTML;
import net.sf.jabref.logic.layout.format.FormatPagesForXML;
import net.sf.jabref.logic.layout.format.GetOpenOfficeType;
import net.sf.jabref.logic.layout.format.HTMLChars;
import net.sf.jabref.logic.layout.format.HTMLParagraphs;
import net.sf.jabref.logic.layout.format.IfPlural;
import net.sf.jabref.logic.layout.format.Iso690FormatDate;
import net.sf.jabref.logic.layout.format.Iso690NamesAuthors;
import net.sf.jabref.logic.layout.format.JournalAbbreviator;
import net.sf.jabref.logic.layout.format.LastPage;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.layout.format.NameFormatter;
import net.sf.jabref.logic.layout.format.NoSpaceBetweenAbbreviations;
import net.sf.jabref.logic.layout.format.NotFoundFormatter;
import net.sf.jabref.logic.layout.format.Number;
import net.sf.jabref.logic.layout.format.Ordinal;
import net.sf.jabref.logic.layout.format.RTFChars;
import net.sf.jabref.logic.layout.format.RemoveBrackets;
import net.sf.jabref.logic.layout.format.RemoveBracketsAddComma;
import net.sf.jabref.logic.layout.format.RemoveLatexCommands;
import net.sf.jabref.logic.layout.format.RemoveTilde;
import net.sf.jabref.logic.layout.format.RemoveWhitespace;
import net.sf.jabref.logic.layout.format.Replace;
import net.sf.jabref.logic.layout.format.RisAuthors;
import net.sf.jabref.logic.layout.format.RisKeywords;
import net.sf.jabref.logic.layout.format.RisMonth;
import net.sf.jabref.logic.layout.format.ToLowerCase;
import net.sf.jabref.logic.layout.format.ToUpperCase;
import net.sf.jabref.logic.layout.format.WrapContent;
import net.sf.jabref.logic.layout.format.WrapFileLinks;
import net.sf.jabref.logic.layout.format.XMLChars;
import net.sf.jabref.logic.openoffice.OOPreFormatter;
import net.sf.jabref.logic.search.MatchesHighlighter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class LayoutEntry {

    private List<LayoutFormatter> option;

    // Formatter to be run after other formatters:
    private LayoutFormatter postFormatter;

    private String text;

    private List<LayoutEntry> layoutEntries;

    private final int type;

    private final List<String> invalidFormatter = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(LayoutEntry.class);

    private final JournalAbbreviationRepository repository;

    public LayoutEntry(StringInt si, JournalAbbreviationRepository repository) {
        this.repository = repository;
        type = si.i;
        switch (type) {
        case LayoutHelper.IS_LAYOUT_TEXT:
            text = si.s;
            break;
        case LayoutHelper.IS_SIMPLE_FIELD:
            text = si.s.trim();
            break;
        case LayoutHelper.IS_OPTION_FIELD:
            doOptionField(si.s);
            break;
        case LayoutHelper.IS_FIELD_START:
        case LayoutHelper.IS_FIELD_END:
        default:
            break;
        }
    }

    public LayoutEntry(List<StringInt> parsedEntries, int layoutType, JournalAbbreviationRepository repository) {
        this.repository = repository;
        List<LayoutEntry> tmpEntries = new ArrayList<>();
        String blockStart = parsedEntries.get(0).s;
        String blockEnd = parsedEntries.get(parsedEntries.size() - 1).s;

        if (!blockStart.equals(blockEnd)) {
            LOGGER.warn("Field start and end entry must be equal.");
        }

        type = layoutType;
        text = blockEnd;
        List<StringInt> blockEntries = null;
        for (StringInt parsedEntry : parsedEntries.subList(1, parsedEntries.size() - 1)) {
            switch (parsedEntry.i) {
            case LayoutHelper.IS_FIELD_START:
            case LayoutHelper.IS_GROUP_START:
                blockEntries = new ArrayList<>();
                blockStart = parsedEntry.s;
                break;
            case LayoutHelper.IS_FIELD_END:
            case LayoutHelper.IS_GROUP_END:
                if (blockStart.equals(parsedEntry.s)) {
                    blockEntries.add(parsedEntry);
                    int groupType = parsedEntry.i == LayoutHelper.IS_GROUP_END ? LayoutHelper.IS_GROUP_START :
                            LayoutHelper.IS_FIELD_START;
                    LayoutEntry le = new LayoutEntry(blockEntries, groupType, repository);
                    tmpEntries.add(le);
                    blockEntries = null;
                } else {
                    LOGGER.warn("Nested field entries are not implemented!");
                }
                break;
            case LayoutHelper.IS_LAYOUT_TEXT:
            case LayoutHelper.IS_SIMPLE_FIELD:
            case LayoutHelper.IS_OPTION_FIELD:
            default:
                // Do nothing
                break;
            }

            if (blockEntries == null) {
                tmpEntries.add(new LayoutEntry(parsedEntry, repository));
            } else {
                blockEntries.add(parsedEntry);
            }
        }

        layoutEntries = new ArrayList<>(tmpEntries);

        for (LayoutEntry layoutEntry : layoutEntries) {
            invalidFormatter.addAll(layoutEntry.getInvalidFormatters());
        }

    }

    public void setPostFormatter(LayoutFormatter formatter) {
        this.postFormatter = formatter;
    }

    private String doLayout(BibEntry bibtex, BibDatabase database) {
        return doLayout(bibtex, database, Optional.empty());
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
        case LayoutHelper.IS_GROUP_START:
            return handleFieldOrGroupStart(bibtex, database, highlightPattern);
        case LayoutHelper.IS_FIELD_END:
        case LayoutHelper.IS_GROUP_END:
            return "";
        case LayoutHelper.IS_OPTION_FIELD:
            return handleOptionField(bibtex, database);
        case LayoutHelper.IS_ENCODING_NAME:
            // Printing the encoding name is not supported in entry layouts, only
            // in begin/end layouts. This prevents breakage if some users depend
            // on a field called "encoding". We simply return this field instead:
            return BibDatabase.getResolvedField("encoding", bibtex, database);
        default:
            return "";
        }
    }

    private String handleOptionField(BibEntry bibtex, BibDatabase database) {
        String fieldEntry;

        if ("bibtextype".equals(text)) {
            fieldEntry = bibtex.getType();
        } else {
            // changed section begin - arudert
            // resolve field (recognized by leading backslash) or text
            String fieldText = text.startsWith("\\") ? BibDatabase.getResolvedField(text.substring(1), bibtex,
                    database) : BibDatabase.getText(text, database);
            // changed section end - arudert
            if (fieldText == null) {
                fieldEntry = "";
            } else {
                fieldEntry = fieldText;
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
    }

    private String handleFieldOrGroupStart(BibEntry bibtex, BibDatabase database, Optional<Pattern> highlightPattern) {
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

        if ((field == null) || ((type == LayoutHelper.IS_GROUP_START)
                && field.equalsIgnoreCase(LayoutHelper.getCurrentGroup()))) {
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

    /**
     * Do layout for general formatters (no bibtex-entry fields).
     *
     * @param databaseContext
     *            Bibtex Database
     * @return
     */
    public String doLayout(BibDatabaseContext databaseContext, Charset encoding) {
        switch (type) {
        case LayoutHelper.IS_LAYOUT_TEXT:
            return text;

        case LayoutHelper.IS_SIMPLE_FIELD:
            throw new UnsupportedOperationException("bibtex entry fields not allowed in begin or end layout");

        case LayoutHelper.IS_FIELD_START:
        case LayoutHelper.IS_GROUP_START:
            throw new UnsupportedOperationException("field and group starts not allowed in begin or end layout");

        case LayoutHelper.IS_FIELD_END:
        case LayoutHelper.IS_GROUP_END:
            throw new UnsupportedOperationException("field and group ends not allowed in begin or end layout");

        case LayoutHelper.IS_OPTION_FIELD:
            String field = BibDatabase.getText(text, databaseContext.getDatabase());
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

        case LayoutHelper.IS_ENCODING_NAME:
            return encoding.displayName();

        case LayoutHelper.IS_FILENAME:
            File f = databaseContext.getDatabaseFile();
            return f == null ? "" : f.getName();

        case LayoutHelper.IS_FILEPATH:
            File f2 = databaseContext.getDatabaseFile();
            return f2 == null ? "" : f2.getPath();

        default:
            break;
        }
        return "";
    }

    private void doOptionField(String s) {
        List<String> v = StringUtil.tokenizeToList(s, "\n");

        if (v.size() == 1) {
            text = v.get(0);
        } else {
            text = v.get(0).trim();

            option = getOptionalLayout(v.get(1));
            // See if there was an undefined formatter:
            for (LayoutFormatter anOption : option) {
                if (anOption instanceof NotFoundFormatter) {
                    String notFound = ((NotFoundFormatter) anOption).getNotFound();

                    invalidFormatter.add(notFound);
                }
            }

        }

    }

    private LayoutFormatter getLayoutFormatterByName(String name) throws Exception {

        switch (name) {
        case "HTMLToLatexFormatter": // For backward compatibility
        case "HtmlToLatex":
            return new HtmlToLatexFormatter();
        case "UnicodeToLatexFormatter": // For backward compatibility
        case "UnicodeToLatex":
            return new UnicodeToLatexFormatter();
        case "OOPreFormatter":
            return new OOPreFormatter();
        case "AuthorAbbreviator":
            return new AuthorAbbreviator();
        case "AuthorAndsCommaReplacer":
            return new AuthorAndsCommaReplacer();
        case "AuthorAndsReplacer":
            return new AuthorAndsReplacer();
        case "AuthorFirstAbbrLastCommas":
            return new AuthorFirstAbbrLastCommas();
        case "AuthorFirstAbbrLastOxfordCommas":
            return new AuthorFirstAbbrLastOxfordCommas();
        case "AuthorFirstFirst":
            return new AuthorFirstFirst();
        case "AuthorFirstFirstCommas":
            return new AuthorFirstFirstCommas();
        case "AuthorFirstLastCommas":
            return new AuthorFirstLastCommas();
        case "AuthorFirstLastOxfordCommas":
            return new AuthorFirstLastOxfordCommas();
        case "AuthorLastFirst":
            return new AuthorLastFirst();
        case "AuthorLastFirstAbbrCommas":
            return new AuthorLastFirstAbbrCommas();
        case "AuthorLastFirstAbbreviator":
            return new AuthorLastFirstAbbreviator();
        case "AuthorLastFirstAbbrOxfordCommas":
            return new AuthorLastFirstAbbrOxfordCommas();
        case "AuthorLastFirstCommas":
            return new AuthorLastFirstCommas();
        case "AuthorLastFirstOxfordCommas":
            return new AuthorLastFirstOxfordCommas();
        case "AuthorLF_FF":
            return new AuthorLF_FF();
        case "AuthorLF_FFAbbr":
            return new AuthorLF_FFAbbr();
        case "AuthorNatBib":
            return new AuthorNatBib();
        case "AuthorOrgSci":
            return new AuthorOrgSci();
        case "CompositeFormat":
            return new CompositeFormat();
        case "CreateBibORDFAuthors":
            return new CreateBibORDFAuthors();
        case "CreateDocBookAuthors":
            return new CreateDocBookAuthors();
        case "CurrentDate":
            return new CurrentDate();
        case "DOICheck":
            return new DOICheck();
        case "DOIStrip":
            return new DOIStrip();
        case "FirstPage":
            return new FirstPage();
        case "FormatPagesForHTML":
            return new FormatPagesForHTML();
        case "FormatPagesForXML":
            return new FormatPagesForXML();
        case "GetOpenOfficeType":
            return new GetOpenOfficeType();
        case "HTMLChars":
            return new HTMLChars();
        case "HTMLParagraphs":
            return new HTMLParagraphs();
        case "Iso690FormatDate":
            return new Iso690FormatDate();
        case "Iso690NamesAuthors":
            return new Iso690NamesAuthors();
        case "JournalAbbreviator":
            return new JournalAbbreviator(repository);
        case "LastPage":
            return new LastPage();
        case "FormatChars": // For backward compatibility
        case "LatexToUnicode":
            return new LatexToUnicodeFormatter();
        case "NameFormatter":
            return new NameFormatter();
        case "NoSpaceBetweenAbbreviations":
            return new NoSpaceBetweenAbbreviations();
        case "Ordinal":
            return new Ordinal();
        case "RemoveBrackets":
            return new RemoveBrackets();
        case "RemoveBracketsAddComma":
            return new RemoveBracketsAddComma();
        case "RemoveLatexCommands":
            return new RemoveLatexCommands();
        case "RemoveTilde":
            return new RemoveTilde();
        case "RemoveWhitespace":
            return new RemoveWhitespace();
        case "RisKeywords":
            return new RisKeywords();
        case "RisMonth":
            return new RisMonth();
        case "RTFChars":
            return new RTFChars();
        case "ToLowerCase":
            return new ToLowerCase();
        case "ToUpperCase":
            return new ToUpperCase();
        case "XMLChars":
            return new XMLChars();
        case "Default":
            return new Default();
        case "FileLink":
            return new FileLink();
        case "Number":
            return new Number();
        case "RisAuthors":
            return new RisAuthors();
        case "Authors":
            return new Authors();
        case "IfPlural":
            return new IfPlural();
        case "Replace":
            return new Replace();
        case "WrapContent":
            return new WrapContent();
        case "WrapFileLinks":
            return new WrapFileLinks();
        default:
            return new NotFoundFormatter(name);
        }
    }

    /**
     * Return an array of LayoutFormatters found in the given formatterName
     * string (in order of appearance).
     *
     */
    private List<LayoutFormatter> getOptionalLayout(String formatterName) {

        List<List<String>> formatterStrings = parseMethodsCalls(formatterName);

        List<LayoutFormatter> results = new ArrayList<>(formatterStrings.size());

        Map<String, String> userNameFormatter = NameFormatter.getNameFormatters();

        for (List<String> strings : formatterStrings) {

            String className = strings.get(0).trim();

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
                LayoutFormatter f = getLayoutFormatterByName(className);
                // If this formatter accepts an argument, check if we have one, and
                // set it if so:
                if ((f instanceof ParamLayoutFormatter) && (strings.size() >= 2)) {
                    ((ParamLayoutFormatter) f).setArgument(strings.get(1));
                }
                results.add(f);
                continue;
            } catch (Exception ex) {
                LOGGER.info("Problem with formatter", ex);
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

    public List<String> getInvalidFormatters() {
        return invalidFormatter;
    }

    public static List<List<String>> parseMethodsCalls(String calls) {

        List<List<String>> result = new ArrayList<>();

        char[] c = calls.toCharArray();

        int i = 0;

        while (i < c.length) {

            int start = i;
            if (Character.isJavaIdentifierStart(c[i])) {
                i++;
                while ((i < c.length) && (Character.isJavaIdentifierPart(c[i]) || (c[i] == '.'))) {
                    i++;
                }
                if ((i < c.length) && (c[i] == '(')) {

                    String method = calls.substring(start, i);

                    // Skip the brace
                    i++;

                    if (i < c.length) {
                        if (c[i] == '"') {
                            // Parameter is in format "xxx"

                            // Skip "
                            i++;

                            int startParam = i;
                            i++;
                            boolean escaped = false;
                            while (((i + 1) < c.length) && !(!escaped && (c[i] == '"') && (c[i + 1] == ')'))) {
                                if (c[i] == '\\') {
                                    escaped = !escaped;
                                } else {
                                    escaped = false;
                                }
                                i++;

                            }

                            String param = calls.substring(startParam, i);

                            result.add(Arrays.asList(method, param));
                        } else {
                            // Parameter is in format xxx

                            int startParam = i;

                            while ((i < c.length) && (c[i] != ')')) {
                                i++;
                            }

                            String param = calls.substring(startParam, i);

                            result.add(Arrays.asList(method, param));

                        }
                    } else {
                        // Incorrectly terminated open brace
                        result.add(Arrays.asList(method));
                    }
                } else {
                    String method = calls.substring(start, i);
                    result.add(Arrays.asList(method));
                }
            }
            i++;
        }

        return result;
    }

}
