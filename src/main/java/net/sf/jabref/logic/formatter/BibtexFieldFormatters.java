package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.*;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;

import java.util.Arrays;
import java.util.List;

public class BibtexFieldFormatters {
    public static final NormalizePagesFormatter NORMALIZE_PAGES = new NormalizePagesFormatter();
    public static final OrdinalsToSuperscriptFormatter
            ORDINALS_TO_LATEX_SUPERSCRIPT = new OrdinalsToSuperscriptFormatter();
    public static final NormalizeDateFormatter NORMALIZE_DATE = new NormalizeDateFormatter();
    public static final NormalizeMonthFormatter NORMALIZE_MONTH = new NormalizeMonthFormatter();
    public static final NormalizeNamesFormatter NORMALIZE_PERSON_NAMES = new NormalizeNamesFormatter();
    public static final LatexCleanupFormatter LATEX_CLEANUP = new LatexCleanupFormatter();
    public static final UnitsToLatexFormatter UNITS_TO_LATEX = new UnitsToLatexFormatter();
    public static final RemoveBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveBracesFormatter();
    public static final HtmlToLatexFormatter HTML_TO_LATEX = new HtmlToLatexFormatter();
    public static final UnicodeToLatexFormatter UNICODE_TO_LATEX = new UnicodeToLatexFormatter();
    public static final LatexToUnicodeFormatter LATEX_TO_UNICODE = new LatexToUnicodeFormatter();
    public static final ClearFormatter CLEAR = new ClearFormatter();

    public static final List<Formatter> ALL = Arrays.asList(NORMALIZE_PAGES, ORDINALS_TO_LATEX_SUPERSCRIPT,
            NORMALIZE_DATE, NORMALIZE_PERSON_NAMES, LATEX_CLEANUP, NORMALIZE_MONTH, UNITS_TO_LATEX,
            REMOVE_BRACES_FORMATTER, HTML_TO_LATEX, UNICODE_TO_LATEX, CLEAR);

    public static final List<Formatter> CONVERTERS = Arrays.asList(HTML_TO_LATEX, UNICODE_TO_LATEX, LATEX_TO_UNICODE);
}
