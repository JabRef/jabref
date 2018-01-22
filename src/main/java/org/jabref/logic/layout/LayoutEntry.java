package org.jabref.logic.layout;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.layout.format.AuthorAbbreviator;
import org.jabref.logic.layout.format.AuthorAndToSemicolonReplacer;
import org.jabref.logic.layout.format.AuthorAndsCommaReplacer;
import org.jabref.logic.layout.format.AuthorAndsReplacer;
import org.jabref.logic.layout.format.AuthorFirstAbbrLastCommas;
import org.jabref.logic.layout.format.AuthorFirstAbbrLastOxfordCommas;
import org.jabref.logic.layout.format.AuthorFirstFirst;
import org.jabref.logic.layout.format.AuthorFirstFirstCommas;
import org.jabref.logic.layout.format.AuthorFirstLastCommas;
import org.jabref.logic.layout.format.AuthorFirstLastOxfordCommas;
import org.jabref.logic.layout.format.AuthorLF_FF;
import org.jabref.logic.layout.format.AuthorLF_FFAbbr;
import org.jabref.logic.layout.format.AuthorLastFirst;
import org.jabref.logic.layout.format.AuthorLastFirstAbbrCommas;
import org.jabref.logic.layout.format.AuthorLastFirstAbbrOxfordCommas;
import org.jabref.logic.layout.format.AuthorLastFirstAbbreviator;
import org.jabref.logic.layout.format.AuthorLastFirstCommas;
import org.jabref.logic.layout.format.AuthorLastFirstOxfordCommas;
import org.jabref.logic.layout.format.AuthorNatBib;
import org.jabref.logic.layout.format.AuthorOrgSci;
import org.jabref.logic.layout.format.Authors;
import org.jabref.logic.layout.format.CompositeFormat;
import org.jabref.logic.layout.format.CreateBibORDFAuthors;
import org.jabref.logic.layout.format.CreateDocBookAuthors;
import org.jabref.logic.layout.format.CreateDocBookEditors;
import org.jabref.logic.layout.format.CurrentDate;
import org.jabref.logic.layout.format.DOICheck;
import org.jabref.logic.layout.format.DOIStrip;
import org.jabref.logic.layout.format.DateFormatter;
import org.jabref.logic.layout.format.Default;
import org.jabref.logic.layout.format.EntryTypeFormatter;
import org.jabref.logic.layout.format.FileLink;
import org.jabref.logic.layout.format.FirstPage;
import org.jabref.logic.layout.format.FormatPagesForHTML;
import org.jabref.logic.layout.format.FormatPagesForXML;
import org.jabref.logic.layout.format.GetOpenOfficeType;
import org.jabref.logic.layout.format.HTMLChars;
import org.jabref.logic.layout.format.HTMLParagraphs;
import org.jabref.logic.layout.format.IfPlural;
import org.jabref.logic.layout.format.Iso690FormatDate;
import org.jabref.logic.layout.format.Iso690NamesAuthors;
import org.jabref.logic.layout.format.JournalAbbreviator;
import org.jabref.logic.layout.format.LastPage;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.NameFormatter;
import org.jabref.logic.layout.format.NoSpaceBetweenAbbreviations;
import org.jabref.logic.layout.format.NotFoundFormatter;
import org.jabref.logic.layout.format.Number;
import org.jabref.logic.layout.format.Ordinal;
import org.jabref.logic.layout.format.RTFChars;
import org.jabref.logic.layout.format.RemoveBrackets;
import org.jabref.logic.layout.format.RemoveBracketsAddComma;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.layout.format.RemoveTilde;
import org.jabref.logic.layout.format.RemoveWhitespace;
import org.jabref.logic.layout.format.Replace;
import org.jabref.logic.layout.format.RisAuthors;
import org.jabref.logic.layout.format.RisKeywords;
import org.jabref.logic.layout.format.RisMonth;
import org.jabref.logic.layout.format.ToLowerCase;
import org.jabref.logic.layout.format.ToUpperCase;
import org.jabref.logic.layout.format.WrapContent;
import org.jabref.logic.layout.format.WrapFileLinks;
import org.jabref.logic.layout.format.XMLChars;
import org.jabref.logic.openoffice.OOPreFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LayoutEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutEntry.class);

    private List<LayoutFormatter> option;

    // Formatter to be run after other formatters:
    private LayoutFormatter postFormatter;

    private String text;

    private List<LayoutEntry> layoutEntries;

    private final int type;

    private final List<String> invalidFormatter = new ArrayList<>();

    private final LayoutFormatterPreferences prefs;

    public LayoutEntry(StringInt si, LayoutFormatterPreferences prefs) {
        this.prefs = prefs;
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

    public LayoutEntry(List<StringInt> parsedEntries, int layoutType, LayoutFormatterPreferences prefs) {
        this.prefs = prefs;
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
                    LayoutEntry le = new LayoutEntry(blockEntries, groupType, prefs);
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
                tmpEntries.add(new LayoutEntry(parsedEntry, prefs));
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

    public String doLayout(BibEntry bibtex, BibDatabase database) {
        switch (type) {
        case LayoutHelper.IS_LAYOUT_TEXT:
            return text;
        case LayoutHelper.IS_SIMPLE_FIELD:
            String value = bibtex.getResolvedFieldOrAlias(text, database).orElse("");

            // If a post formatter has been set, call it:
            if (postFormatter != null) {
                value = postFormatter.format(value);
            }
            return value;
        case LayoutHelper.IS_FIELD_START:
        case LayoutHelper.IS_GROUP_START:
            return handleFieldOrGroupStart(bibtex, database);
        case LayoutHelper.IS_FIELD_END:
        case LayoutHelper.IS_GROUP_END:
            return "";
        case LayoutHelper.IS_OPTION_FIELD:
            return handleOptionField(bibtex, database);
        case LayoutHelper.IS_ENCODING_NAME:
            // Printing the encoding name is not supported in entry layouts, only
            // in begin/end layouts. This prevents breakage if some users depend
            // on a field called "encoding". We simply return this field instead:
            return bibtex.getResolvedFieldOrAlias("encoding", database).orElse(null);
        default:
            return "";
        }
    }

    private String handleOptionField(BibEntry bibtex, BibDatabase database) {
        String fieldEntry;

        if (BibEntry.TYPE_HEADER.equals(text)) {
            fieldEntry = bibtex.getType();
        } else if (BibEntry.OBSOLETE_TYPE_HEADER.equals(text)) {
            LOGGER.warn("'" + BibEntry.OBSOLETE_TYPE_HEADER
                    + "' is an obsolete name for the entry type. Please update your layout to use '"
                    + BibEntry.TYPE_HEADER + "' instead.");
            fieldEntry = bibtex.getType();
        } else {
            // changed section begin - arudert
            // resolve field (recognized by leading backslash) or text
            fieldEntry = text.startsWith("\\") ? bibtex
                    .getResolvedFieldOrAlias(text.substring(1), database)
                    .orElse("") : BibDatabase.getText(text, database);
            // changed section end - arudert
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

    private String handleFieldOrGroupStart(BibEntry bibtex, BibDatabase database) {
        Optional<String> field;
        if (type == LayoutHelper.IS_GROUP_START) {
            field = bibtex.getResolvedFieldOrAlias(text, database);
        } else if (text.matches(".*(;|(\\&+)).*")) {
            // split the strings along &, && or ; for AND formatter
            String[] parts = text.split("\\s*(;|(\\&+))\\s*");
            field = Optional.empty();
            for (String part : parts) {
                field = bibtex.getResolvedFieldOrAlias(part, database);
                if (!field.isPresent()) {
                    break;
                }
            }
        } else {
            // split the strings along |, ||  for OR formatter
            String[] parts = text.split("\\s*(\\|+)\\s*");
            field = Optional.empty();
            for (String part : parts) {
                field = bibtex.getResolvedFieldOrAlias(part, database);
                if (field.isPresent()) {
                    break;
                }
            }
        }

        if ((!field.isPresent()) || ((type == LayoutHelper.IS_GROUP_START)
                && field.get().equalsIgnoreCase(LayoutHelper.getCurrentGroup()))) {
            return null;
        } else {
            if (type == LayoutHelper.IS_GROUP_START) {
                LayoutHelper.setCurrentGroup(field.get());
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
                        sb.append(fieldText);
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
            return databaseContext.getDatabaseFile().map(File::getName).orElse("");

        case LayoutHelper.IS_FILEPATH:
            return databaseContext.getDatabaseFile().map(File::getPath).orElse("");

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
        case "AuthorAndToSemicolonReplacer":
            return new AuthorAndToSemicolonReplacer();
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
        case "CreateDocBookEditors":
            return new CreateDocBookEditors();
        case "CurrentDate":
            return new CurrentDate();
        case "DateFormatter":
            return new DateFormatter();
        case "DOICheck":
            return new DOICheck();
        case "DOIStrip":
            return new DOIStrip();
        case "EntryTypeFormatter":
            return new EntryTypeFormatter();
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
            return new JournalAbbreviator(prefs.getJournalAbbreviationLoader(),
                    prefs.getJournalAbbreviationPreferences());
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
            return new RemoveLatexCommandsFormatter();
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
            return new FileLink(prefs.getFileLinkPreferences());
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
            return new WrapFileLinks(prefs.getFileLinkPreferences());
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

        Map<String, String> userNameFormatter = NameFormatter.getNameFormatters(prefs.getNameFormatterPreferences());

        for (List<String> strings : formatterStrings) {

            String nameFormatterName = strings.get(0).trim();

            // Check if this is a name formatter defined by this export filter:

            Optional<String> contents = prefs.getCustomExportNameFormatter(nameFormatterName);
            if (contents.isPresent()) {
                NameFormatter nf = new NameFormatter();
                nf.setParameter(contents.get());
                results.add(nf);
                continue;
            }

            // Try to load from formatters in formatter folder
            try {
                LayoutFormatter f = getLayoutFormatterByName(nameFormatterName);
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
            String formatterParameter = userNameFormatter.get(nameFormatterName);

            if (formatterParameter != null) {
                NameFormatter nf = new NameFormatter();
                nf.setParameter(formatterParameter);
                results.add(nf);
                continue;
            }

            results.add(new NotFoundFormatter(nameFormatterName));
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
                    int bracelevel = 0;

                    if (i < c.length) {
                        if (c[i] == '"') {
                            // Parameter is in format "xxx"

                            // Skip "
                            i++;

                            int startParam = i;
                            i++;
                            boolean escaped = false;
                            while (((i + 1) < c.length)
                                    && !(!escaped && (c[i] == '"') && (c[i + 1] == ')') && (bracelevel == 0))) {
                                if (c[i] == '\\') {
                                    escaped = !escaped;
                                } else if (c[i] == '(') {
                                    bracelevel++;
                                } else if (c[i] == ')') {
                                    bracelevel--;
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

                            while ((i < c.length) && (!((c[i] == ')') && (bracelevel == 0)))) {
                                if (c[i] == '(') {
                                    bracelevel++;
                                } else if (c[i] == ')') {
                                    bracelevel--;
                                }
                                i++;
                            }

                            String param = calls.substring(startParam, i);

                            result.add(Arrays.asList(method, param));
                        }
                    } else {
                        // Incorrectly terminated open brace
                        result.add(Collections.singletonList(method));
                    }
                } else {
                    String method = calls.substring(start, i);
                    result.add(Collections.singletonList(method));
                }
            }
            i++;
        }

        return result;
    }

}
