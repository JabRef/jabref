package org.jabref.logic.citationstyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.stream.Collectors;

import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.integrity.PagesChecker;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.helper.json.StringJsonBuilderFactory;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.DigitStringValue;
import org.jbibtex.Key;

/**
 * Custom {@link ItemDataProvider} that allows to set the data so that we don't have to instantiate a new CSL object
 * every time.
 */
public class JabRefItemDataProvider implements ItemDataProvider {

    private static final BibTeXConverter BIBTEX_CONVERTER = new BibTeXConverter();

    private final StringJsonBuilderFactory stringJsonBuilderFactory;

    private final List<BibEntry> data = new ArrayList<>();

    private BibDatabaseContext bibDatabaseContext;
    private BibEntryTypesManager entryTypesManager;
    private PagesChecker pagesChecker;

    public JabRefItemDataProvider() {
        stringJsonBuilderFactory = new StringJsonBuilderFactory();
    }

    /**
     * Converts the {@link BibEntry} into {@link CSLItemData}.
     *
     * <br>
     * <table>
     * <thead>
     * <tr>
     * <th style="text-align:left">BibTeX</th>
     * <th style="text-align:left">BibLaTeX</th>
     * <th style="text-align:left">EntryPreview/CSL</th>
     * <th style="text-align:left">proposed logic, conditions and info</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <td style="text-align:left">volume</td>
     * <td style="text-align:left">volume</td>
     * <td style="text-align:left">volume</td>
     * <td style="text-align:left"></td>
     * </tr>
     * <tr>
     * <td style="text-align:left">number</td>
     * <td style="text-align:left">issue</td>
     * <td style="text-align:left">issue</td>
     * <td style="text-align:left">For conversion to CSL or BibTeX: BibLaTeX <code>number</code> takes priority and supersedes BibLaTeX <code>issue</code></td>
     * </tr>
     * <tr>
     * <td style="text-align:left">number</td>
     * <td style="text-align:left">number</td>
     * <td style="text-align:left">issue</td>
     * <td style="text-align:left">same as above</td>
     * </tr>
     * <tr>
     * <td style="text-align:left">pages</td>
     * <td style="text-align:left">eid</td>
     * <td style="text-align:left">number</td>
     * <td style="text-align:left">Some journals put the article-number (= eid) into the pages field. If BibLaTeX <code>eid</code> exists, provide csl <code>number</code> to the style. If <code>pages</code> exists, provide csl <code>page</code>.  If <code>eid</code> WITHIN the <code>pages</code> field exists, detect the eid and provide csl <code>number</code>. If both <code>eid</code> and <code>pages</code> exists, ideally provide both csl <code>number</code> and csl <code>page</code>. Ideally the citationstyle should be able to flexibly choose the rendering.</td>
     * </tr>
     * <tr>
     * <td style="text-align:left">pages</td>
     * <td style="text-align:left">pages</td>
     * <td style="text-align:left">page</td>
     * <td style="text-align:left">same as above</td>
     * </tr>
     * </tbody>
     * </table>
     */
    private CSLItemData bibEntryToCSLItemData(BibEntry originalBibEntry, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager) {
        // We need to make a deep copy, because we modify the entry according to the logic presented at
        // https://github.com/JabRef/jabref/issues/8372#issuecomment-1014941935
        BibEntry bibEntry = (BibEntry) originalBibEntry.clone();
        String citeKey = bibEntry.getCitationKey().orElse("");
        BibTeXEntry bibTeXEntry = new BibTeXEntry(new Key(bibEntry.getType().getName()), new Key(citeKey));

        // Not every field is already generated into latex free fields
        RemoveNewlinesFormatter removeNewlinesFormatter = new RemoveNewlinesFormatter();

        Optional<BibEntryType> entryType = entryTypesManager.enrich(bibEntry.getType(), bibDatabaseContext.getMode());

        if (bibEntry.getType().equals(StandardEntryType.Article)) {
            // Patch bibEntry to contain the right BibTeX (not BibLaTeX) fields
            // Note that we do not need to convert from "pages" to "page", because CiteProc already handles it
            // See BibTeXConverter
            if (bibDatabaseContext.isBiblatexMode()) {
                // Map "number" to CSL "issue", unless no number exists
                Optional<String> numberField = bibEntry.getField(StandardField.NUMBER);
                numberField.ifPresent(number -> {
                            bibEntry.setField(StandardField.ISSUE, number);
                            bibEntry.clearField(StandardField.NUMBER);
                        }
                );

                bibEntry.getField(StandardField.EID).ifPresent(eid -> {
                    if (!bibEntry.hasField(StandardField.NUMBER)) {
                        bibEntry.setField(StandardField.NUMBER, eid);
                        bibEntry.clearField(StandardField.EID);
                    }
                });
            } else {
                // BibTeX mode
                bibEntry.getField(StandardField.NUMBER).ifPresent(number -> {
                    bibEntry.setField(StandardField.ISSUE, number);
                    bibEntry.clearField(StandardField.NUMBER);
                });
                bibEntry.getField(StandardField.PAGES).ifPresent(pages -> {
                    if (pages.toLowerCase(Locale.ROOT).startsWith("article ")) {
                        pages = pages.substring("Article ".length());
                        bibEntry.setField(StandardField.NUMBER, pages);
                    }
                });
                bibEntry.getField(StandardField.EID).ifPresent(eid -> {
                    if (!bibEntry.hasField(StandardField.PAGES)) {
                        bibEntry.setField(StandardField.PAGES, eid);
                        bibEntry.clearField(StandardField.EID);
                    }
                });
            }
        }

        SequencedCollection<Field> fields;
        if (entryType.isPresent()) {
            fields = entryType.map(BibEntryType::getAllFields).map(LinkedHashSet::new).get();
            fields.addAll(bibEntry.getFields());
        } else {
            fields = bibEntry.getFields();
        }
        for (Field key : fields) {
            bibEntry.getResolvedFieldOrAlias(key, bibDatabaseContext.getDatabase())
                    .map(removeNewlinesFormatter::format)
                    .map(LatexToUnicodeAdapter::format)
                    .ifPresent(value -> {
                        if (StandardField.MONTH == key) {
                            // Change month from #mon# to mon because CSL does not support the former format
                            value = bibEntry.getMonth().map(Month::getShortName).orElse(value);
                        }
                        bibTeXEntry.addField(new Key(key.getName()), new DigitStringValue(value));
                    });
        }
        return BIBTEX_CONVERTER.toItemData(bibTeXEntry);
    }

    /**
     * Fills the data with all entries in given bibDatabaseContext
     */
    public void setData(BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager) {
        this.setData(bibDatabaseContext.getEntries(), bibDatabaseContext, entryTypesManager);
    }

    public void setData(List<BibEntry> data, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager) {
        this.data.clear();
        this.data.addAll(data);
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;

        // Quick solution to always use BibLaTeX mode at the checker to allow pages ranges with single dash, too
        // Example: pages = {1-2}
        BibDatabaseContext ctx = new BibDatabaseContext();
        ctx.setMode(BibDatabaseMode.BIBLATEX);
        this.pagesChecker = new PagesChecker(ctx);
    }

    @Override
    public CSLItemData retrieveItem(String id) {
        return data.stream()
                   .filter(entry -> entry.getCitationKey().orElse("").equals(id))
                   .map(entry -> bibEntryToCSLItemData(entry, bibDatabaseContext, entryTypesManager))
                   .findFirst().orElse(null);
    }

    @Override
    public Collection<String> getIds() {
        return data.stream()
                   .map(entry -> entry.getCitationKey().orElse(""))
                   .toList();
    }

    public String toJson() {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        this.setData(entries, bibDatabaseContext, entryTypesManager);
        return entries.stream()
                      .map(entry -> bibEntryToCSLItemData(entry, bibDatabaseContext, entryTypesManager))
                      .map(item -> item.toJson(stringJsonBuilderFactory.createJsonBuilder()))
                      .map(String.class::cast)
                      .collect(Collectors.joining(",", "[", "]"));
    }
}
