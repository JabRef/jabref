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
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
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
            new LatexCleanupFormatter(),
            new MinifyNameListFormatter(),
            new NormalizeDateFormatter(),
            new NormalizeMonthFormatter(),
            new NormalizeNamesFormatter(),
            new NormalizePagesFormatter(),
            new OrdinalsToSuperscriptFormatter(),
            new RemoveBracesFormatter(),
            new UnitsToLatexFormatter(),
            new EscapeUnderscoresFormatter()
    );

    public static final List<Formatter> ALL = new ArrayList<>();

    private Formatters() {
    }

    public static Optional<Formatter> getFormatterForModifier(String modifier) {
        Objects.requireNonNull(modifier);
        Optional<Formatter> formatter = ALL.stream().filter(f -> f.getKey().equals(modifier)).findAny();
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
