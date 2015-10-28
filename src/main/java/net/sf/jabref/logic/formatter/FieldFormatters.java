package net.sf.jabref.logic.formatter;

import java.util.Arrays;
import java.util.List;

public class FieldFormatters {
    public static final PageNumbersFormatter PAGE_NUMBERS = new PageNumbersFormatter();

    public static final List<Formatter> ALL = Arrays.asList(PAGE_NUMBERS);
}
