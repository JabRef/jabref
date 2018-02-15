package org.jabref.logic.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeUnderscoresFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import org.jabref.logic.formatter.bibtexfields.RegexFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.cleanup.Formatter;

public class Formatters {

    private static final List<Formatter> CONVERTERS = Arrays.asList(
            new HtmlToLatexFormatter(),
            new HtmlToUnicodeFormatter(),
            new LatexToUnicodeFormatter(),
            new UnicodeToLatexFormatter()
    );

    private static final List<Formatter> CASE_CHANGERS = Arrays.asList(
            new CapitalizeFormatter(),
            new LowerCaseFormatter(),
            new SentenceCaseFormatter(),
            new TitleCaseFormatter(),
            new UpperCaseFormatter()
    );

    private static final List<Formatter> OTHERS = Arrays.asList(
            new ClearFormatter(),
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

    private static final String REGEX = "regex";

    private static final int LENGTH_OF_REGEX_PREFIX = REGEX.length();

    private Formatters() {
    }

    public static final List<Formatter> getConverters() {
        List<Formatter> converters = new ArrayList<>();
        converters.addAll(CONVERTERS);
        return converters;
    }

    public static final List<Formatter> getCaseChangers() {
        List<Formatter> caseChangers = new ArrayList<>();
        caseChangers.addAll(CASE_CHANGERS);
        return caseChangers;
    }

    public static final List<Formatter> getOthers() {
        List<Formatter> others = new ArrayList<>();
        others.addAll(OTHERS);
        return others;
    }

    public static final List<Formatter> getAll() {
        List<Formatter> all = new ArrayList<>();
        all.addAll(CONVERTERS);
        all.addAll(CASE_CHANGERS);
        all.addAll(OTHERS);
        return all;
    }

    public static Optional<Formatter> getFormatterForModifier(String modifier) {
        Objects.requireNonNull(modifier);
        Optional<Formatter> formatter;
        List<Formatter> all = getAll();

        if (modifier.matches("regex.*")) {
            String regex = modifier.substring(LENGTH_OF_REGEX_PREFIX);
            RegexFormatter.setRegex(regex);
            formatter = all.stream().filter(f -> f.getKey().equals("regex")).findAny();
        } else {
            formatter = all.stream().filter(f -> f.getKey().equals(modifier)).findAny();
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

}
