package org.jabref.logic.layout;

import java.nio.charset.Charset;
import java.nio.file.Path;
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
import org.jabref.logic.layout.format.CSLType;
import org.jabref.logic.layout.format.CompositeFormat;
import org.jabref.logic.layout.format.CreateBibORDFAuthors;
import org.jabref.logic.layout.format.CreateDocBook4Authors;
import org.jabref.logic.layout.format.CreateDocBook4Editors;
import org.jabref.logic.layout.format.CreateDocBook5Authors;
import org.jabref.logic.layout.format.CreateDocBook5Editors;
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
import org.jabref.logic.layout.format.MarkdownFormatter;
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
import org.jabref.logic.layout.format.ShortMonthFormatter;
import org.jabref.logic.layout.format.ToLowerCase;
import org.jabref.logic.layout.format.ToUpperCase;
import org.jabref.logic.layout.format.WrapContent;
import org.jabref.logic.layout.format.WrapFileLinks;
import org.jabref.logic.layout.format.XMLChars;
import org.jabref.logic.openoffice.style.OOPreFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.UnknownField;
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
            case LayoutHelper.IS_SIMPLE_COMMAND:
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
                case LayoutHelper.IS_SIMPLE_COMMAND:
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
            case LayoutHelper.IS_SIMPLE_COMMAND:
                String value = bibtex.getResolvedFieldOrAlias(FieldFactory.parseField(text), database).orElse("");

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
                return bibtex.getResolvedFieldOrAlias(new UnknownField("encoding"), database).orElse(null);
            default:
                return "";
        }
    }

    private String handleOptionField(BibEntry bibtex, BibDatabase database) {
        String fieldEntry;

        if (InternalField.TYPE_HEADER.getName().equals(text)) {
            fieldEntry = bibtex.getType().getDisplayName();
        } else if (InternalField.OBSOLETE_TYPE_HEADER.getName().equals(text)) {
            LOGGER.warn("'" + InternalField.OBSOLETE_TYPE_HEADER
                    + "' is an obsolete name for the entry type. Please update your layout to use '"
                    + InternalField.TYPE_HEADER + "' instead.");
            fieldEntry = bibtex.getType().getDisplayName();
        } else {
            // changed section begin - arudert
            // resolve field (recognized by leading backslash) or text
            fieldEntry = text.startsWith("\\") ? bibtex
                    .getResolvedFieldOrAlias(FieldFactory.parseField(text.substring(1)), database)
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
        boolean negated = false;
        if (type == LayoutHelper.IS_GROUP_START) {
            field = bibtex.getResolvedFieldOrAlias(FieldFactory.parseField(text), database);
        } else if (text.matches(".*(;|(\\&+)).*")) {
            // split the strings along &, && or ; for AND formatter
            String[] parts = text.split("\\s*(;|(\\&+))\\s*");
            field = Optional.empty();
            for (String part : parts) {
                negated = part.startsWith("!");
                field = bibtex.getResolvedFieldOrAlias(FieldFactory.parseField(negated ? part.substring(1).trim() : part), database);
                if (field.isPresent() == negated) {
                    break;
                }
            }
        } else {
            // split the strings along |, ||  for OR formatter
            String[] parts = text.split("\\s*(\\|+)\\s*");
            field = Optional.empty();
            for (String part : parts) {
                negated = part.startsWith("!");
                field = bibtex.getResolvedFieldOrAlias(FieldFactory.parseField(negated ? part.substring(1).trim() : part), database);
                if (field.isPresent() ^ negated) {
                    break;
                }
            }
        }

        if ((field.isPresent() == negated) || ((type == LayoutHelper.IS_GROUP_START)
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
     * @param databaseContext Bibtex Database
     */
    public String doLayout(BibDatabaseContext databaseContext, Charset encoding) {
        switch (type) {
            case LayoutHelper.IS_LAYOUT_TEXT:
                return text;

            case LayoutHelper.IS_SIMPLE_COMMAND:
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
            case LayoutHelper.IS_FILEPATH:
                return databaseContext.getDatabasePath().map(Path::toAbsolutePath).map(Path::toString).orElse("");

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

    private LayoutFormatter getLayoutFormatterByName(String name) {
        return switch (name) {
            // For backward compatibility
            case "HTMLToLatexFormatter", "HtmlToLatex" -> new HtmlToLatexFormatter();
            // For backward compatibility
            case "UnicodeToLatexFormatter", "UnicodeToLatex" -> new UnicodeToLatexFormatter();
            case "OOPreFormatter" -> new OOPreFormatter();
            case "AuthorAbbreviator" -> new AuthorAbbreviator();
            case "AuthorAndToSemicolonReplacer" -> new AuthorAndToSemicolonReplacer();
            case "AuthorAndsCommaReplacer" -> new AuthorAndsCommaReplacer();
            case "AuthorAndsReplacer" -> new AuthorAndsReplacer();
            case "AuthorFirstAbbrLastCommas" -> new AuthorFirstAbbrLastCommas();
            case "AuthorFirstAbbrLastOxfordCommas" -> new AuthorFirstAbbrLastOxfordCommas();
            case "AuthorFirstFirst" -> new AuthorFirstFirst();
            case "AuthorFirstFirstCommas" -> new AuthorFirstFirstCommas();
            case "AuthorFirstLastCommas" -> new AuthorFirstLastCommas();
            case "AuthorFirstLastOxfordCommas" -> new AuthorFirstLastOxfordCommas();
            case "AuthorLastFirst" -> new AuthorLastFirst();
            case "AuthorLastFirstAbbrCommas" -> new AuthorLastFirstAbbrCommas();
            case "AuthorLastFirstAbbreviator" -> new AuthorLastFirstAbbreviator();
            case "AuthorLastFirstAbbrOxfordCommas" -> new AuthorLastFirstAbbrOxfordCommas();
            case "AuthorLastFirstCommas" -> new AuthorLastFirstCommas();
            case "AuthorLastFirstOxfordCommas" -> new AuthorLastFirstOxfordCommas();
            case "AuthorLF_FF" -> new AuthorLF_FF();
            case "AuthorLF_FFAbbr" -> new AuthorLF_FFAbbr();
            case "AuthorNatBib" -> new AuthorNatBib();
            case "AuthorOrgSci" -> new AuthorOrgSci();
            case "CompositeFormat" -> new CompositeFormat();
            case "CreateBibORDFAuthors" -> new CreateBibORDFAuthors();
            case "CreateDocBook4Authors" -> new CreateDocBook4Authors();
            case "CreateDocBook4Editors" -> new CreateDocBook4Editors();
            case "CreateDocBook5Authors" -> new CreateDocBook5Authors();
            case "CreateDocBook5Editors" -> new CreateDocBook5Editors();
            case "CurrentDate" -> new CurrentDate();
            case "DateFormatter" -> new DateFormatter();
            case "DOICheck" -> new DOICheck();
            case "DOIStrip" -> new DOIStrip();
            case "EntryTypeFormatter" -> new EntryTypeFormatter();
            case "FirstPage" -> new FirstPage();
            case "FormatPagesForHTML" -> new FormatPagesForHTML();
            case "FormatPagesForXML" -> new FormatPagesForXML();
            case "GetOpenOfficeType" -> new GetOpenOfficeType();
            case "HTMLChars" -> new HTMLChars();
            case "HTMLParagraphs" -> new HTMLParagraphs();
            case "Iso690FormatDate" -> new Iso690FormatDate();
            case "Iso690NamesAuthors" -> new Iso690NamesAuthors();
            case "JournalAbbreviator" -> new JournalAbbreviator(prefs.getJournalAbbreviationRepository());
            case "LastPage" -> new LastPage();
// For backward compatibility
            case "FormatChars", "LatexToUnicode" -> new LatexToUnicodeFormatter();
            case "NameFormatter" -> new NameFormatter();
            case "NoSpaceBetweenAbbreviations" -> new NoSpaceBetweenAbbreviations();
            case "Ordinal" -> new Ordinal();
            case "RemoveBrackets" -> new RemoveBrackets();
            case "RemoveBracketsAddComma" -> new RemoveBracketsAddComma();
            case "RemoveLatexCommands" -> new RemoveLatexCommandsFormatter();
            case "RemoveTilde" -> new RemoveTilde();
            case "RemoveWhitespace" -> new RemoveWhitespace();
            case "RisKeywords" -> new RisKeywords();
            case "RisMonth" -> new RisMonth();
            case "RTFChars" -> new RTFChars();
            case "ToLowerCase" -> new ToLowerCase();
            case "ToUpperCase" -> new ToUpperCase();
            case "XMLChars" -> new XMLChars();
            case "Default" -> new Default();
            case "FileLink" -> new FileLink(prefs.getFileLinkPreferences());
            case "Number" -> new Number();
            case "RisAuthors" -> new RisAuthors();
            case "Authors" -> new Authors();
            case "IfPlural" -> new IfPlural();
            case "Replace" -> new Replace();
            case "WrapContent" -> new WrapContent();
            case "WrapFileLinks" -> new WrapFileLinks(prefs.getFileLinkPreferences());
            case "Markdown" -> new MarkdownFormatter();
            case "CSLType" -> new CSLType();
            case "ShortMonth" -> new ShortMonthFormatter();
            default -> null;
        };
    }

    /**
     * Return an array of LayoutFormatters found in the given formatterName string (in order of appearance).
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
            LayoutFormatter formatter = getLayoutFormatterByName(nameFormatterName);
            if (formatter != null) {
                // If this formatter accepts an argument, check if we have one, and set it if so
                if ((formatter instanceof ParamLayoutFormatter) && (strings.size() >= 2)) {
                    ((ParamLayoutFormatter) formatter).setArgument(strings.get(1));
                }
                results.add(formatter);
                continue;
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

    public String getText() {
        return text;
    }
}
