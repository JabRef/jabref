package org.jabref.logic.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.*;
import org.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.cleanup.Formatter;

public class Formatters {

    public static final List<Formatter> CONVERTERS = Arrays.asList(
            new HtmlToLatexFormatter(),
            new HtmlToUnicodeFormatter(),
            new LatexToUnicodeFormatter(),
            new UnicodeToLatexFormatter()
    );

    public static final List<Formatter> CASE_CHANGERS = Arrays.asList(
            new CapitalizeFormatter(),
            new LowerCaseFormatter(),
            new ProtectTermsFormatter(),
            new SentenceCaseFormatter(),
            new TitleCaseFormatter(),
            new UpperCaseFormatter()
    );

    public static final List<Formatter> OTHERS = Arrays.asList(
            new ClearFormatter(),
            new CleanupURLFormatter(),
            new LatexCleanupFormatter(),
            new MinifyNameListFormatter(),
            new NormalizeDateFormatter(),
            new NormalizeMonthFormatter(),
            new NormalizeNamesFormatter(),
            new NormalizePagesFormatter(),
            new OrdinalsToSuperscriptFormatter(),
            new RegexFormatter(),
            new RemoveBracesFormatter(),
            new UnitsToLatexFormatter(),
            new EscapeUnderscoresFormatter()
    );

    public static final List<Formatter> ALL = new ArrayList<>();

    private static final String REGEX = "regex";

    private static final int LENGTH_OF_REGEX_PREFIX = REGEX.length();

    private Formatters() {
    }

    public static Optional<Formatter> getFormatterForModifier(String modifier) {
        Objects.requireNonNull(modifier);
        Optional<Formatter> formatter;

        if (modifier.matches("regex.*")) {
            String regex = modifier.substring(LENGTH_OF_REGEX_PREFIX);
            RegexFormatter.setRegex(regex);
            formatter = ALL.stream().filter(f -> f.getKey().equals("regex")).findAny();
        } else {
            formatter = ALL.stream().filter(f -> f.getKey().equals(modifier)).findAny();
        }
        if (formatter.isPresent()) {
            return formatter;
        }
        switch (modifier) {
            case "lower":
                return Optional.of(new LowerCaseFormatter());
            case "upper":
                return Optional.of(new UpperCaseFormatter());
            default:
                return Optional.empty();
        }
    }

    static {
        ALL.addAll(CONVERTERS);
        ALL.addAll(CASE_CHANGERS);
        ALL.addAll(OTHERS);
    }
}
