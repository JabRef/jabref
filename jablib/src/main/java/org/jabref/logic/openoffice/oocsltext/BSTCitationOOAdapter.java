package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.BSTFormatUtils;
import org.jabref.logic.openoffice.bst.BstEntryRenderer;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Inserts BST-styled citations and bibliography into a LibreOffice document.
///
/// In-text citation format is controlled by [OpenOfficePreferences#getBstCitationFormat]:
/// numeric `[n]`, author-year `(Name, Year)`, or style-defined `\bibitem[label]{key}` markers.
/// The bibliography is always rendered by the BST engine regardless of the citation format setting.
@NullMarked
public class BSTCitationOOAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BSTCitationOOAdapter.class);
    private static final Pattern BIBITEM_PATTERN = Pattern.compile("\\\\bibitem(?:\\[(?<label>[^\\]]*)\\])?\\{(?<key>[^}]+)}");

    private final XComponentContext componentContext;
    private final XTextDocument document;
    private final BSTReferenceMarkManager markManager;
    private final PandocLatexConverter pandoc;
    private final OpenOfficePreferences openOfficePreferences;

    private Map<String, String> identifierToLabelMap = Map.of();
    private List<String> cachedLabelIdentifiers = List.of();
    private String cachedBstStylePath = "";
    private boolean styleDefinedLabelsInitialized;

    public BSTCitationOOAdapter(XTextDocument document, XComponentContext componentContext, OpenOfficePreferences openOfficePreferences)
            throws WrappedTargetException, NoSuchElementException {
        this.componentContext = componentContext;
        this.document = document;
        this.openOfficePreferences = openOfficePreferences;
        this.markManager = new BSTReferenceMarkManager(document, componentContext);
        this.pandoc = new PandocLatexConverter(openOfficePreferences.getPandocPath());
        markManager.readAndUpdateExistingMarks();
    }

    /// Inserts an in-text citation mark. Format depends on [OpenOfficePreferences#getBstCitationFormat]:
    /// - [BstCitationFormat#NUMERIC]: `[1]`, `[1, 3]`, ...
    /// - [BstCitationFormat#AUTHOR_YEAR]: `(Cooper et al., 2007)`, ...
    /// - [BstCitationFormat#STYLE_DEFINED]: `[ABC20]`, `[ABC20a]`, ...
    public void insertCitation(XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext ctx)
            throws CreationException, com.sun.star.uno.Exception, MissingStyleDefinedCitationLabelException {
        String citationText = switch (openOfficePreferences.getBstCitationFormat()) {
            case NUMERIC ->
                    buildNumericCitation(entries);
            case AUTHOR_YEAR ->
                    buildAuthorYearCitation(entries, ctx);
            case STYLE_DEFINED ->
                    buildStyleDefinedCitation(entries, ctx);
        };

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(citationText));
        boolean precedingSpaceExists = CitationOOAdapterUtils.hasPrecedingSpace(cursor);
        boolean succeedingSpaceExists = CitationOOAdapterUtils.hasSucceedingSpace(cursor);
        markManager.insertReferenceIntoOO(
                entries, document, cursor, ooText,
                !precedingSpaceExists && openOfficePreferences.getAddSpaceBefore(),
                !succeedingSpaceExists && openOfficePreferences.getAddSpaceAfter(),
                CSLCitationType.NORMAL);
        markManager.setRealTimeNumberUpdateRequired(
                openOfficePreferences.getBstCitationFormat() == BstCitationFormat.NUMERIC);
        markManager.readAndUpdateExistingMarks();
        invalidateStyleDefinedLabels();
    }

    /// Inserts the bibliography by rendering each cited entry through BST → pandoc → OOText.
    /// Entries are sorted by first-appearance (citation-number) order or BST style order.
    public void insertBibliography(XTextCursor cursor, BstStyle style, List<BibEntry> entries,
                                   BibDatabaseContext ctx)
            throws IOException, InterruptedException, com.sun.star.uno.Exception, CreationException {
        if (!pandoc.isAvailable()) {
            throw new IllegalStateException(
                    "pandoc is not available at the configured path. "
                            + "Please ensure pandoc is installed and configure its path in Preferences > OpenOffice/LibreOffice.");
        }

        OOText title = OOFormat.paragraph(
                OOText.fromString(openOfficePreferences.getCslBibliographyTitle()),
                openOfficePreferences.getCslBibliographyHeaderFormat());
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText titleBreak = OOFormat.paragraph(
                OOText.fromString(""),
                openOfficePreferences.getCslBibliographyBodyFormat());
        OOTextIntoOO.write(document, cursor, titleBreak);

        BstVM bstVM;
        try {
            bstVM = style.createBstVM();
        } catch (IOException e) {
            LOGGER.warn("Could not load BST style: {}", style.getPath(), e);
            throw e;
        }
        BstEntryRenderer renderer = new BstEntryRenderer(bstVM);

        BstCitationFormat citationFormat = openOfficePreferences.getBstCitationFormat();
        boolean useNumericBibliographyOrder = citationFormat == BstCitationFormat.NUMERIC;
        boolean useStyleDefinedBibliographyLabels = citationFormat == BstCitationFormat.STYLE_DEFINED;

        List<BibEntry> sorted = new ArrayList<>(entries);
        BibDatabase database = ctx.getDatabase();
        StyleOrderAndLabels styleOrderAndLabels = new StyleOrderAndLabels(Map.of(), Map.of());

        if (useNumericBibliographyOrder) {
            sorted.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(keyOrId(entry))));
        } else {
            styleOrderAndLabels = computeStyleOrderAndLabels(bstVM, sorted, database);
            cacheStyleDefinedLabels(style.getPath(), getCitationSetIdentifiers(sorted), styleOrderAndLabels.identifierToLabelMap());
            Map<String, Integer> identifierToNumberMap = styleOrderAndLabels.identifierToNumberMap();
            sorted.sort(Comparator.comparingInt(entry -> identifierToNumberMap.getOrDefault(keyOrId(entry), Integer.MAX_VALUE)));
        }

        for (BibEntry entry : sorted) {
            String identifier = keyOrId(entry);
            String latex = renderer.renderEntryToLatex(entry, database);

            String norm = BSTFormatUtils.normalizeLegacyForPandoc(latex);

            String html = pandoc.latexToHtml(norm);

            String body = BSTFormatUtils.convertPandocHtmlToOOText(html);

            String finalLine;
            if (useNumericBibliographyOrder) {
                int number = markManager.getCitationNumber(identifier);
                finalLine = "[" + number + "] " + body;
            } else if (useStyleDefinedBibliographyLabels) {
                String label = identifierToLabelMap.getOrDefault(identifier, String.valueOf(markManager.getCitationNumber(identifier)));
                finalLine = "[" + label + "] " + body;
            } else {
                finalLine = body;
            }

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalLine));
            OOTextIntoOO.write(document, cursor, ooText);

            OOText ooBreak = OOFormat.paragraph(
                    OOText.fromString(""),
                    openOfficePreferences.getCslBibliographyBodyFormat());
            OOTextIntoOO.write(document, cursor, ooBreak);
        }
    }

    public void refreshCitationState() throws WrappedTargetException, NoSuchElementException {
        markManager.readAndUpdateExistingMarks();
        invalidateStyleDefinedLabels();
    }

    public List<String> getCitedIdentifiers() throws WrappedTargetException, NoSuchElementException {
        // Use a transient manager here so export only inspects marks. Reusing the adapter's live manager would
        // disturb its cached numbering state for subsequent BST operations before the next full refresh.
        BSTReferenceMarkManager exportMarkManager = new BSTReferenceMarkManager(document, componentContext);
        exportMarkManager.readExistingMarks();

        SequencedSet<String> identifiers = new LinkedHashSet<>();
        for (BSTReferenceMark mark : exportMarkManager.getMarksInOrder().reversed()) {
            identifiers.addAll(mark.getCitationKeys());
        }
        return List.copyOf(identifiers);
    }

    /// Returns `true` if the given entry has already been cited in the document.
    public boolean isCitedEntry(BibEntry entry) {
        return markManager.hasCitationForIdentifier(keyOrId(entry));
    }

    private String buildNumericCitation(List<BibEntry> entries) {
        List<Integer> numbers = new ArrayList<>(entries.size());
        for (BibEntry entry : entries) {
            numbers.add(markManager.getCitationNumber(keyOrId(entry)));
        }
        numbers.sort(Integer::compareTo);
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        numbers.forEach(number -> joiner.add(String.valueOf(number)));
        return joiner.toString();
    }

    private String buildStyleDefinedCitation(List<BibEntry> entries, BibDatabaseContext ctx) throws MissingStyleDefinedCitationLabelException {
        ensureStyleDefinedLabels(getCitedEntriesIncluding(entries, ctx.getDatabase()), ctx.getDatabase());

        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (BibEntry entry : entries) {
            String identifier = keyOrId(entry);
            joiner.add(getStyleDefinedLabelOrThrow(identifier, identifierToLabelMap));
        }
        return joiner.toString();
    }

    private void ensureStyleDefinedLabels(List<BibEntry> entries, BibDatabase database) {
        if (!(openOfficePreferences.getCurrentStyle() instanceof BstStyle style)) {
            invalidateStyleDefinedLabels();
            return;
        }

        List<String> citedIdentifiers = getCitationSetIdentifiers(entries);
        if (styleDefinedLabelsInitialized
                && cachedBstStylePath.equals(style.getPath())
                && cachedLabelIdentifiers.equals(citedIdentifiers)) {
            return;
        }

        try {
            StyleOrderAndLabels styleOrderAndLabels = computeStyleOrderAndLabels(style.createBstVM(), entries, database);
            cacheStyleDefinedLabels(style.getPath(), citedIdentifiers, styleOrderAndLabels.identifierToLabelMap());
        } catch (IOException e) {
            LOGGER.warn("Could not compute BST style-defined citation labels for {}", style.getPath(), e);
            cacheStyleDefinedLabels(style.getPath(), citedIdentifiers, Map.of());
        }
    }

    private List<BibEntry> getCitedEntriesIncluding(List<BibEntry> entries, BibDatabase database) {
        Map<String, BibEntry> entriesByIdentifier = new LinkedHashMap<>();
        for (BibEntry entry : entries) {
            entriesByIdentifier.put(keyOrId(entry), entry);
        }

        SequencedSet<String> citedIdentifiers = new LinkedHashSet<>();
        for (BSTReferenceMark mark : markManager.getMarksInOrder().reversed()) {
            citedIdentifiers.addAll(mark.getCitationKeys());
        }
        citedIdentifiers.addAll(entriesByIdentifier.keySet());

        List<BibEntry> citedEntries = new ArrayList<>(citedIdentifiers.size());
        for (String identifier : citedIdentifiers) {
            BibEntry newEntry = entriesByIdentifier.get(identifier);
            if (newEntry != null) {
                citedEntries.add(newEntry);
                continue;
            }

            database.getEntryByCitationKey(identifier)
                    .or(() -> database.getEntryById(identifier))
                    .ifPresent(citedEntries::add);
        }
        return citedEntries;
    }

    private void cacheStyleDefinedLabels(String stylePath, List<String> citedIdentifiers, Map<String, String> labels) {
        identifierToLabelMap = new LinkedHashMap<>(labels);
        cachedLabelIdentifiers = List.copyOf(citedIdentifiers);
        cachedBstStylePath = stylePath;
        styleDefinedLabelsInitialized = true;
    }

    private void invalidateStyleDefinedLabels() {
        identifierToLabelMap = Map.of();
        cachedLabelIdentifiers = List.of();
        cachedBstStylePath = "";
        styleDefinedLabelsInitialized = false;
    }

    private static List<String> getCitationSetIdentifiers(List<BibEntry> entries) {
        SequencedSet<String> identifiers = new LinkedHashSet<>();
        for (BibEntry entry : entries) {
            identifiers.add(keyOrId(entry));
        }
        return List.copyOf(identifiers);
    }

    @VisibleForTesting
    static String buildAuthorYearCitation(List<BibEntry> entries, BibDatabaseContext ctx) {
        StringJoiner joiner = new StringJoiner("; ", "(", ")");
        for (BibEntry entry : entries) {
            String authorPart = extractFirstAuthorLastName(entry);
            String year = entry.getResolvedFieldOrAlias(StandardField.YEAR, ctx.getDatabase())
                               .map(String::trim)
                               .filter(y -> !y.isEmpty())
                               .orElse("n.d.");
            joiner.add(authorPart + ", " + year);
        }
        return joiner.toString();
    }

    @VisibleForTesting
    static String extractFirstAuthorLastName(BibEntry entry) {
        return entry.getField(StandardField.AUTHOR)
                    .map(AuthorList::parse)
                    .map(AuthorList::getAsNatbib)
                    .orElse("?");
    }

    @VisibleForTesting
    static String keyOrId(BibEntry entry) {
        return entry.getCitationKey().orElse(entry.getId());
    }

    @VisibleForTesting
    static StyleOrderAndLabels computeStyleOrderAndLabels(BstVM bstVM, List<BibEntry> entries, BibDatabase database) {
        List<BibEntry> normalizedEntries = new ArrayList<>(entries.size());
        for (BibEntry entry : entries) {
            BibEntry entryCopy = new BibEntry(entry);
            if (entryCopy.getCitationKey().isEmpty()) {
                entryCopy = entryCopy.withCitationKey(keyOrId(entry));
            }
            normalizedEntries.add(entryCopy);
        }

        String renderedBibliography = bstVM.render(normalizedEntries, database);
        Map<String, String> keyToIdentifier = new LinkedHashMap<>();
        for (BibEntry entry : normalizedEntries) {
            entry.getCitationKey().ifPresent(key -> keyToIdentifier.put(key, keyOrId(entry)));
        }
        return computeStyleOrderAndLabels(renderedBibliography, keyToIdentifier);
    }

    @VisibleForTesting
    static String getStyleDefinedLabelOrThrow(String identifier, Map<String, String> identifierToLabelMap) throws MissingStyleDefinedCitationLabelException {
        String label = identifierToLabelMap.get(identifier);
        if ((label == null) || label.isBlank()) {
            throw new MissingStyleDefinedCitationLabelException();
        }
        return label;
    }

    @VisibleForTesting
    static StyleOrderAndLabels computeStyleOrderAndLabels(String renderedBibliography, Map<String, String> keyToIdentifier) {
        Matcher bibitemMatcher = BIBITEM_PATTERN.matcher(renderedBibliography);
        int order = 1;
        Map<String, Integer> identifierToNumber = new LinkedHashMap<>();
        Map<String, String> identifierToLabel = new LinkedHashMap<>();

        while (bibitemMatcher.find()) {
            String key = bibitemMatcher.group("key");
            String identifier = keyToIdentifier.getOrDefault(key, key);
            if (!identifierToNumber.containsKey(identifier)) {
                identifierToNumber.put(identifier, order++);
            }

            String label = bibitemMatcher.group("label");
            if ((label != null) && !label.isBlank()) {
                identifierToLabel.put(identifier, BSTFormatUtils.normalizeBibItemLabel(label));
            }
        }

        return new StyleOrderAndLabels(identifierToNumber, identifierToLabel);
    }

    @VisibleForTesting
    record StyleOrderAndLabels(Map<String, Integer> identifierToNumberMap, Map<String, String> identifierToLabelMap) {
        StyleOrderAndLabels {
            identifierToNumberMap = new LinkedHashMap<>(identifierToNumberMap);
            identifierToLabelMap = new LinkedHashMap<>(identifierToLabelMap);
        }
    }
}
