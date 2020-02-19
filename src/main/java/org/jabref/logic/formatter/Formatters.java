package org.jabref.logic.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeAmpersandsFormatter;
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
import org.jabref.logic.formatter.bibtexfields.ShortenDOIFormatter;
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

    private Formatters() {
    }

    public static List<Formatter> getConverters() {
        return Arrays.asList(
                new HtmlToLatexFormatter(),
                new HtmlToUnicodeFormatter(),
                new LatexToUnicodeFormatter(),
                new UnicodeToLatexFormatter()
        );
    }

    public static List<Formatter> getCaseChangers() {
        return Arrays.asList(
                new CapitalizeFormatter(),
                new LowerCaseFormatter(),
                new SentenceCaseFormatter(),
                new TitleCaseFormatter(),
                new UpperCaseFormatter()
        );
    }

    public static List<Formatter> getOthers() {
        return Arrays.asList(
                new ClearFormatter(),
                new CleanupUrlFormatter(),
                new LatexCleanupFormatter(),
                new MinifyNameListFormatter(),
                new NormalizeDateFormatter(),
                new NormalizeMonthFormatter(),
                new NormalizeNamesFormatter(),
                new NormalizePagesFormatter(),
                new OrdinalsToSuperscriptFormatter(),
                new RemoveBracesFormatter(),
                new UnitsToLatexFormatter(),
                new EscapeUnderscoresFormatter(),
                new EscapeAmpersandsFormatter(),
                new ShortenDOIFormatter()
        );
    }

    public static List<Formatter> getAll() {
        List<Formatter> all = new ArrayList<>();
        all.addAll(getConverters());
        all.addAll(getCaseChangers());
        all.addAll(getOthers());
        return all;
    }

    public static Optional<Formatter> getFormatterForModifier(String modifier) {
        Objects.requireNonNull(modifier);

        switch (modifier) {
            case "lower":
                return Optional.of(new LowerCaseFormatter());
            case "upper":
                return Optional.of(new UpperCaseFormatter());
            case "capitalize":
                return Optional.of(new CapitalizeFormatter());
            case "titlecase":
                return Optional.of(new TitleCaseFormatter());
            case "sentencecase":
                return Optional.of(new SentenceCaseFormatter());
        }

        if (modifier.startsWith(RegexFormatter.KEY)) {
            String regex = modifier.substring(RegexFormatter.KEY.length());
            return Optional.of(new RegexFormatter(regex));
        } else {
            return getAll().stream().filter(f -> f.getKey().equals(modifier)).findAny();
        }
    }
}
