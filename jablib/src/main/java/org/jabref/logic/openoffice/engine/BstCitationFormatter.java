package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.openoffice.bst.BSTFormatUtils;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

/// Pure BST parsing and marker formatting shared by the renderer and its parity tests.
@NullMarked
final class BstCitationFormatter {

    private static final Pattern BIBITEM = Pattern.compile("\\\\bibitem(?:\\[(?<label>[^\\]]*)\\])?\\{(?<key>[^}]+)}");
    private static final Pattern BIBLIOGRAPHY_END = Pattern.compile("\\\\end\\{thebibliography}");

    private BstCitationFormatter() {
    }

    static String extractFirstAuthorLastName(BibEntry entry) {
        return entry.getField(StandardField.AUTHOR).map(AuthorList::parse).map(AuthorList::getAsNatbib).orElse("?");
    }

    static String authorYearEntry(BibEntry entry, BibDatabase database) {
        String year = entry.getResolvedFieldOrAlias(StandardField.YEAR, database).map(String::strip).filter(value -> !value.isEmpty()).orElse("n.d.");
        return extractFirstAuthorLastName(entry) + ", " + year;
    }

    static String buildAuthorYearCitation(List<BibEntry> entries, BibDatabaseContext context) {
        StringJoiner text = new StringJoiner("; ", "(", ")");
        entries.forEach(entry -> text.add(authorYearEntry(entry, context.getDatabase())));
        return text.toString();
    }

    static String buildNumericCitationText(List<String> keys, Map<String, Integer> numbers) {
        return buildNumericCitationText(keys, numbers, List.of());
    }

    static String buildNumericCitationText(List<String> keys, Map<String, Integer> numbers, List<String> unresolved) {
        StringJoiner text = new StringJoiner(", ", "[", "]");
        keys.stream().filter(numbers::containsKey).map(numbers::get).sorted().forEach(number -> text.add(number.toString()));
        unresolved.forEach(key -> text.add(RenderData.unresolved(key)));
        return text.toString();
    }

    static String buildStyleDefinedCitationText(List<String> keys, Map<String, String> labels) throws MissingCitationLabelException {
        return buildStyleDefinedCitationText(keys, labels, List.of());
    }

    static String buildStyleDefinedCitationText(List<String> keys, Map<String, String> labels, List<String> unresolved) throws MissingCitationLabelException {
        StringJoiner text = new StringJoiner(", ", "[", "]");
        for (String key : keys) {
            text.add(unresolved.contains(key) ? RenderData.unresolved(key) : StringEscapeUtils.escapeHtml4(getStyleDefinedLabelOrThrow(key, labels)));
        }
        return text.toString();
    }

    static String getStyleDefinedLabelOrThrow(String key, Map<String, String> labels) throws MissingCitationLabelException {
        return Optional.ofNullable(labels.get(key)).filter(label -> !label.isBlank()).orElseThrow(() -> new MissingCitationLabelException(key));
    }

    static StyleOrderAndLabels computeStyleOrderAndLabels(BstVM vm, List<BibEntry> entries, BibDatabase database) throws RenderException {
        for (BibEntry entry : entries) {
            if (entry.getCitationKey().filter(key -> !key.isBlank()).isEmpty()) {
                throw new RenderException("BST rendering requires stable citation keys");
            }
        }
        return computeStyleOrderAndLabels(vm.render(entries.stream().map(BibEntry::new).toList(), database), Map.of());
    }

    static StyleOrderAndLabels computeStyleOrderAndLabels(String bibliography, Map<String, String> citationKeyAliases) {
        List<BibItem> items = parseBibliography(bibliography, citationKeyAliases);
        Map<String, String> labels = new LinkedHashMap<>();
        items.forEach(item -> item.label().ifPresent(label -> labels.put(item.key(), label)));
        return new StyleOrderAndLabels(CitationNumbering.firstAppearance(items.stream().map(BibItem::key).toList()), labels);
    }

    static List<BibItem> parseBibliography(String bibliography) {
        return parseBibliography(bibliography, Map.of());
    }

    private static List<BibItem> parseBibliography(String bibliography, Map<String, String> citationKeyAliases) {
        Matcher matcher = BIBITEM.matcher(bibliography);
        List<String> keys = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();
        while (matcher.find()) {
            String key = citationKeyAliases.getOrDefault(matcher.group("key"), matcher.group("key"));
            keys.add(key);
            starts.add(matcher.start());
            ends.add(matcher.end());
            Optional.ofNullable(matcher.group("label")).map(BSTFormatUtils::normalizeBibItemLabel)
                    .map(String::strip).filter(label -> !label.isEmpty() && !label.contains("\\"))
                    .ifPresent(label -> labels.put(key, label));
        }
        Map<String, BibItem> items = new LinkedHashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            int end = i + 1 == keys.size() ? bibliography.length() : starts.get(i + 1);
            String body = BIBLIOGRAPHY_END.matcher(bibliography.substring(ends.get(i), end)).replaceAll("").replace("\\newblock", " ").strip();
            String key = keys.get(i);
            items.putIfAbsent(key, new BibItem(key, Optional.ofNullable(labels.get(key)), body));
        }
        return List.copyOf(items.values());
    }

    @NullMarked
    record BibItem(String key, Optional<String> label, String latex) {
    }

    @NullMarked
    record StyleOrderAndLabels(Map<String, Integer> identifierToNumberMap, Map<String, String> identifierToLabelMap) {
        StyleOrderAndLabels {
            identifierToNumberMap = Collections.unmodifiableMap(new LinkedHashMap<>(identifierToNumberMap));
            identifierToLabelMap = Collections.unmodifiableMap(new LinkedHashMap<>(identifierToLabelMap));
        }
    }
}
