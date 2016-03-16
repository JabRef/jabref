package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.*;
import java.util.Arrays;
import java.util.List;

public class BibtexFieldFormatters {
    public static final PageNumbersFormatter PAGE_NUMBERS = new PageNumbersFormatter();
    public static final SuperscriptFormatter SUPERSCRIPTS = new SuperscriptFormatter();
    public static final DateFormatter DATE = new DateFormatter();
    public static final MonthFormatter MONTH_FORMATTER = new MonthFormatter();
    public static final AuthorsFormatter AUTHORS_FORMATTER = new AuthorsFormatter();
    public static final LatexFormatter LATEX_FORMATTER = new LatexFormatter();
    public static final UnitFormatter UNIT_FORMATTER = new UnitFormatter();
    public static final RemoveBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveBracesFormatter();
    public static final HTMLToLatexFormatter HTML_TO_LATEX = new HTMLToLatexFormatter();
    public static final UnicodeToLatexFormatter UNICODE_TO_LATEX = new UnicodeToLatexFormatter();
    public static final EraseFormatter ERASE = new EraseFormatter();

    public static final List<Formatter> ALL = Arrays.asList(PAGE_NUMBERS, SUPERSCRIPTS, DATE, AUTHORS_FORMATTER,
            LATEX_FORMATTER, MONTH_FORMATTER, UNIT_FORMATTER, REMOVE_BRACES_FORMATTER, HTML_TO_LATEX, UNICODE_TO_LATEX,
            ERASE);
}
