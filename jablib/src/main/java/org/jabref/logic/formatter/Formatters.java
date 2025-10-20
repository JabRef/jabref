package org.jabref.logic.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.ConvertMSCCodesFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeAmpersandsFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeDollarSignFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeUnderscoresFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeIssn;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import org.jabref.logic.formatter.bibtexfields.RegexFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveWordEnclosingAndOuterEnclosingBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.ShortenDOIFormatter;
import org.jabref.logic.formatter.bibtexfields.TransliterateFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.CamelFormatter;
import org.jabref.logic.formatter.casechanger.CamelNFormatter;
import org.jabref.logic.formatter.casechanger.CapitalizeFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.formatter.casechanger.ShortTitleFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
import org.jabref.logic.formatter.casechanger.UpperCaseFormatter;
import org.jabref.logic.formatter.casechanger.VeryShortTitleFormatter;
import org.jabref.logic.formatter.minifier.MinifyNameListFormatter;
import org.jabref.logic.formatter.minifier.TruncateFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.ReplaceUnicodeLigaturesFormatter;

import org.jspecify.annotations.NonNull;

public class Formatters {
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile("\\Atruncate\\d+\\z");

    private static Map<String, Formatter> keyToFormatterMap;

    static {
        keyToFormatterMap = getAll().stream().collect(Collectors.toMap(Formatter::getKey, f -> f));
    }

    private Formatters() {
    }

    public static List<Formatter> getConverters() {
        return Arrays.asList(
                new HtmlToLatexFormatter(),
                new HtmlToUnicodeFormatter(),
                new LatexToUnicodeFormatter(),
                new UnicodeToLatexFormatter(),
                new ConvertMSCCodesFormatter(),
                new TransliterateFormatter()
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
                new NormalizeIssn(),
                new NormalizeMonthFormatter(),
                new NormalizeNamesFormatter(),
                new NormalizePagesFormatter(),
                new NormalizeUnicodeFormatter(),
                new OrdinalsToSuperscriptFormatter(),
                new RemoveEnclosingBracesFormatter(),
                new RemoveWordEnclosingAndOuterEnclosingBracesFormatter(),
                new UnitsToLatexFormatter(),
                new EscapeUnderscoresFormatter(),
                new EscapeAmpersandsFormatter(),
                new EscapeDollarSignFormatter(),
                new ShortenDOIFormatter(),
                new ReplaceUnicodeLigaturesFormatter(),
                new UnprotectTermsFormatter()
        );
    }

    public static List<Formatter> getTitleChangers() {
        return Arrays.asList(
                new VeryShortTitleFormatter(),
                new ShortTitleFormatter(),
                new CamelFormatter()
        );
    }

    public static List<Formatter> getAll() {
        List<Formatter> all = new ArrayList<>();
        all.addAll(getConverters());
        all.addAll(getCaseChangers());
        all.addAll(getOthers());
        all.addAll(getTitleChangers());
        return all;
    }

    public static Optional<Formatter> getFormatterForKey(@NonNull String name) {
        return keyToFormatterMap.containsKey(name) ? Optional.of(keyToFormatterMap.get(name)) : Optional.empty();
    }

    public static Optional<Formatter> getFormatterForModifier(@NonNull String modifier) {
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
            case "veryshorttitle":
                return Optional.of(new VeryShortTitleFormatter());
            case "shorttitle":
                return Optional.of(new ShortTitleFormatter());
        }

        if (modifier.contains("camel")) {
            modifier = modifier.replace("camel", "");
            if (modifier.isEmpty()) {
                return Optional.of(new CamelFormatter());
            } else {
                int length = Integer.parseInt(modifier);
                return Optional.of(new CamelNFormatter(length));
            }
        } else if (modifier.startsWith(RegexFormatter.KEY)) {
            String regex = modifier.substring(RegexFormatter.KEY.length());
            return Optional.of(new RegexFormatter(regex));
        } else if (TRUNCATE_PATTERN.matcher(modifier).matches()) {
            int truncateAfter = Integer.parseInt(modifier.substring(8));
            return Optional.of(new TruncateFormatter(truncateAfter));
        } else {
            return getFormatterForKey(modifier);
        }
    }
}
