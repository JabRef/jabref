package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.util.LocationDetector;
import org.jabref.logic.util.RegexPatterns;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.BooktitleCleanupAction;
import org.jabref.model.cleanup.BooktitleCleanupField;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NonNull;

public class BooktitleCleanups implements CleanupJob {
    private static final Pattern CLEANUP = Pattern.compile(
            "\\s{2,}"                               // 1. multiple spaces
                    + "|\\s*(?:[,.;!?]\\s*)*([,.;!?])"      // 2. staggered punctuation (with optional spaces)
                    + "|\\s+([)}\\]])"                      // 3. space(s) before a closing bracket/paren
                    + "|([({\\[])[ \\s]+"                   // 4. space(s) after an opening bracket/paren
    );

    private final BooktitleCleanupAction yearAction;
    private final BooktitleCleanupAction monthAction;
    private final BooktitleCleanupAction pageRangeAction;
    private final BooktitleCleanupAction locationAction;
    private final LocationDetector locationDetector;
    private final boolean enabled;

    public BooktitleCleanups(
            BooktitleCleanupAction yearAction,
            BooktitleCleanupAction monthAction,
            BooktitleCleanupAction pageRangeAction,
            BooktitleCleanupAction locationAction,
            LocationDetector locationDetector
    ) {
        this.yearAction = yearAction;
        this.monthAction = monthAction;
        this.pageRangeAction = pageRangeAction;
        this.locationAction = locationAction;
        this.locationDetector = locationDetector;
        this.enabled = true;
    }

    public BooktitleCleanups(boolean enabled, LocationDetector locationDetector) {
        this.enabled = enabled;
        this.yearAction = BooktitleCleanupAction.SKIP;
        this.monthAction = BooktitleCleanupAction.SKIP;
        this.pageRangeAction = BooktitleCleanupAction.SKIP;
        this.locationAction = BooktitleCleanupAction.SKIP;
        this.locationDetector = locationDetector;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<BooktitleCleanupField, BooktitleCleanupAction> getConfiguredActions() {
        Map<BooktitleCleanupField, BooktitleCleanupAction> actionMap = new HashMap<>();
        actionMap.put(BooktitleCleanupField.YEAR, yearAction);
        actionMap.put(BooktitleCleanupField.MONTH, monthAction);
        actionMap.put(BooktitleCleanupField.PAGE_RANGE, pageRangeAction);
        actionMap.put(BooktitleCleanupField.LOCATION, locationAction);
        return actionMap;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if (!enabled) {
            return List.of();
        }

        StandardField targetField = StandardField.BOOKTITLE;
        Optional<String> oldTitle = entry.getFieldOrAlias(StandardField.BOOKTITLE);

        if (oldTitle.isEmpty()) {
            oldTitle = entry.getFieldOrAlias(StandardField.JOURNAL);
            targetField = StandardField.JOURNAL;
        }

        if (oldTitle.isEmpty()) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>();
        String locationsRemoved = cleanupLocations(entry, oldTitle.get(), changes, locationAction);
        String yearsRemoved = cleanupYears(entry, locationsRemoved, changes, yearAction);
        String monthsRemoved = cleanupMonths(entry, yearsRemoved, changes, monthAction);
        String pageRangesRemoved = cleanupPageRanges(entry, monthsRemoved, changes, pageRangeAction);
        String finalTitle = cleanupArtifacts(pageRangesRemoved);

        entry.setField(targetField, finalTitle);
        changes.add(new FieldChange(entry, targetField, oldTitle.orElse(""), finalTitle));

        return changes;
    }

    private String cleanupPageRanges(BibEntry entry, String booktitle, List<FieldChange> changes, BooktitleCleanupAction pageRangeAction) {
        if (pageRangeAction == BooktitleCleanupAction.SKIP) {
            return booktitle;
        }

        Matcher pageRangeMatcher = RegexPatterns.PAGE_RANGE_PATTERN.matcher(booktitle);
        if (!pageRangeMatcher.find()) {
            return booktitle;
        }

        String pageRangeFound = pageRangeMatcher.group();
        String pageRangesRemoved = pageRangeMatcher.replaceAll("");
        Optional<String> oldPageRange = entry.getFieldOrAlias(StandardField.PAGES);

        if (shouldSkipReplacement(pageRangeAction, oldPageRange.isPresent())) {
            return pageRangesRemoved;
        }

        entry.setField(StandardField.PAGES, pageRangeFound);
        changes.add(new FieldChange(entry, StandardField.PAGES, oldPageRange.orElse(""), pageRangeFound));

        return pageRangesRemoved;
    }

    private String cleanupMonths(BibEntry entry, String booktitle, List<FieldChange> changes, BooktitleCleanupAction monthAction) {
        if (monthAction == BooktitleCleanupAction.SKIP) {
            return booktitle;
        }

        Matcher monthMatcher = RegexPatterns.MONTHS_PATTERN.matcher(booktitle);
        if (!monthMatcher.find()) {
            return booktitle;
        }

        String monthFound = monthMatcher.group();
        String monthsRemoved = monthMatcher.replaceAll("");
        Optional<String> oldMonth = entry.getField(StandardField.MONTH);

        if (shouldSkipReplacement(monthAction, oldMonth.isPresent())) {
            return monthsRemoved;
        }

        entry.setField(StandardField.MONTH, monthFound);
        changes.add(new FieldChange(entry, StandardField.MONTH, oldMonth.orElse(""), monthFound));

        return monthsRemoved;
    }

    private String cleanupYears(BibEntry entry, String booktitle, List<FieldChange> changes, BooktitleCleanupAction yearAction) {
        if (yearAction == BooktitleCleanupAction.SKIP) {
            return booktitle;
        }

        Matcher yearMatcher = RegexPatterns.YEAR_PATTERN.matcher(booktitle);
        List<String> yearsFound = new ArrayList<>();
        while (yearMatcher.find()) {
            yearsFound.add(yearMatcher.group());
        }
        if (yearsFound.isEmpty()) {
            return booktitle;
        }

        String yearsRemoved = yearMatcher.replaceAll("");
        String foundLatestYear = yearsFound.stream()
                                           .max(java.util.Comparator.naturalOrder())
                                           .get();
        Optional<String> oldDate = entry.getField(StandardField.YEAR);

        if (shouldSkipReplacement(yearAction, oldDate.isPresent())) {
            return yearsRemoved;
        }

        entry.setField(StandardField.YEAR, foundLatestYear);
        changes.add(new FieldChange(entry, StandardField.YEAR, oldDate.orElse(""), foundLatestYear));

        return yearsRemoved;
    }

    private String cleanupLocations(
            BibEntry entry,
            String booktitle,
            List<FieldChange> changes,
            BooktitleCleanupAction locationAction
    ) {
        if (locationAction == BooktitleCleanupAction.SKIP) {
            return booktitle;
        }

        Optional<String> oldLocation = entry.getFieldOrAlias(StandardField.LOCATION);
        Set<String> foundLocations = locationDetector.extractLocations(booktitle);

        if (foundLocations.isEmpty()) {
            return booktitle;
        }

        String locationsRemoved = removeLocationsFromTitle(foundLocations, booktitle);
        if (shouldSkipReplacement(locationAction, oldLocation.isPresent())) {
            return locationsRemoved;
        }

        String newLocation = foundLocations.stream()
                                           .map(BooktitleCleanups::capitalizeAllWords)
                                           .collect(Collectors.joining(", "));
        entry.setField(StandardField.LOCATION, newLocation);
        changes.add(new FieldChange(entry, StandardField.LOCATION, oldLocation.orElse(""), newLocation));

        return locationsRemoved;
    }

    private static String removeLocationsFromTitle(Set<String> foundLocations, String title) {
        // Construct a regex string to match all found locations
        String locationRegex = foundLocations.stream()
                                             .map(location -> Pattern.quote(location).replace("\\ ", "\\\\s+"))
                                             .collect(Collectors.joining("|", "\\b(?:", ")\\b"));

        return Pattern.compile(locationRegex, Pattern.CASE_INSENSITIVE)
                      .matcher(title)
                      .replaceAll("")
                      .strip();
    }

    private static String capitalizeAllWords(String input) {
        return Arrays.stream(input.split(" "))
                     .map(StringUtil::capitalizeFirst)
                     .collect(Collectors.joining(" "));
    }

    private static boolean shouldSkipReplacement(BooktitleCleanupAction cleanupAction, boolean isOldFieldPresent) {
        return cleanupAction == BooktitleCleanupAction.REMOVE_ONLY ||
                (cleanupAction == BooktitleCleanupAction.REPLACE_IF_EMPTY && isOldFieldPresent);
    }

    private String cleanupArtifacts(@NonNull String booktitle) {
        if (StringUtil.isBlank(booktitle)) {
            return "";
        }

        String finalTitle = trimDelimiters(booktitle);
        Matcher cleanupMatcher = CLEANUP.matcher(finalTitle);
        while (cleanupMatcher.find()) {
            finalTitle = cleanupMatcher.replaceAll(mr -> {
                if (mr.group(1) != null) {
                    return mr.group(1);   // keep first punctuation (case 3)
                }
                if (mr.group(2) != null) {
                    return mr.group(2);   // keep closing bracket/paren (case 4)
                }
                if (mr.group(3) != null) {
                    return mr.group(3);   // keep opening bracket/paren (case 5)
                }
                return " ";               // collapse spaces (case 1)
            }).trim();
        }
        // Remove any empty parens/brackets post cleanup
        finalTitle = finalTitle.replaceAll("[({\\[]\\s*[)}\\]]", "").trim();

        return finalTitle;
    }

    private static String trimDelimiters(String candidate) {
        int left = 0;
        int right = candidate.length() - 1;

        // Move left pointer until we find a non-delimiter
        while (left <= right && isDelimiter(candidate.charAt(left))) {
            left++;
        }

        // Move right pointer until we find a non-delimiter
        while (right >= left && isDelimiter(candidate.charAt(right))) {
            right--;
        }

        return left <= right ? candidate.substring(left, right + 1) : "";
    }

    private static boolean isDelimiter(char c) {
        return Character.isWhitespace(c) ||
                c == ',' || c == '_' ||
                c == ':' || c == '.' || c == '-';
    }
}
