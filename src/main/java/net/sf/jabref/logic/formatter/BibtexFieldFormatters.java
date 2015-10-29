package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.PageNumbersFormatter;

import java.util.Arrays;
import java.util.List;

public class BibtexFieldFormatters {
    public static final PageNumbersFormatter PAGE_NUMBERS = new PageNumbersFormatter();

    public static final List<Formatter> ALL = Arrays.asList(PAGE_NUMBERS);
}
