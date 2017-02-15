package net.sf.jabref.logic.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import net.sf.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import net.sf.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.model.cleanup.Formatter;

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
            new UnitsToLatexFormatter()
    );

    public static final List<Formatter> ALL = new ArrayList<>();

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
