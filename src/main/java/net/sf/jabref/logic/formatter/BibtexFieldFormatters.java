package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.DateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.PageNumbersFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.SuperscriptFormatter;

import java.util.Arrays;
import java.util.List;

public class BibtexFieldFormatters {
    public static final PageNumbersFormatter PAGE_NUMBERS = new PageNumbersFormatter();
    public static final SuperscriptFormatter SUPERSCRIPTS = new SuperscriptFormatter();
    public static final DateFormatter DATE = new DateFormatter();

    public static final List<Formatter> ALL = Arrays.asList(PAGE_NUMBERS, SUPERSCRIPTS, DATE);
}
