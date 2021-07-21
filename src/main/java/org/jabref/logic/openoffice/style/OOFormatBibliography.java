package org.jabref.logic.openoffice.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationPath;
import org.jabref.model.openoffice.style.CitedKey;
import org.jabref.model.openoffice.style.CitedKeys;

public class OOFormatBibliography {
    private static final OOPreFormatter POSTFORMATTER = new OOPreFormatter();
    private static final Field UNIQUEFIER_FIELD = new UnknownField("uniq");

    private OOFormatBibliography() {
    }

    /**
     * @return The formatted bibliography, including its title.
     */
    public static OOText formatBibliography(CitationGroups cgs,
                                            CitedKeys bibliography,
                                            OOBibStyle style,
                                            boolean alwaysAddCitedOnPages) {

        OOText title = style.getFormattedBibliographyTitle();
        OOText body = formatBibliographyBody(cgs, bibliography, style, alwaysAddCitedOnPages);
        return OOText.fromString(title.toString() + body.toString());
    }

    /**
     * @return Formatted body of the bibliography. Excludes the title.
     */
    public static OOText formatBibliographyBody(CitationGroups cgs,
                                                CitedKeys bibliography,
                                                OOBibStyle style,
                                                boolean alwaysAddCitedOnPages) {

        StringBuilder stringBuilder = new StringBuilder();

        for (CitedKey ck : bibliography.values()) {
            OOText entryText = formatBibliographyEntry(cgs, ck, style, alwaysAddCitedOnPages);
            stringBuilder.append(entryText.toString());
        }

        return OOText.fromString(stringBuilder.toString());
    }

    /**
     * @return A paragraph. Includes label and "Cited on pages".
     */
    public static OOText formatBibliographyEntry(CitationGroups cgs,
                                                 CitedKey ck,
                                                 OOBibStyle style,
                                                 boolean alwaysAddCitedOnPages) {
        StringBuilder sb = new StringBuilder();

        // insert marker "[1]"
        if (style.isNumberEntries()) {
            sb.append(style.getNumCitationMarkerForBibliography(ck).toString());
        } else {
            // !style.isNumberEntries() : emit no prefix
            // Note: We might want [citationKey] prefix for style.isCitationKeyCiteMarkers();
        }

        // Add entry body
        sb.append(formatBibliographyEntryBody(ck, style).toString());

        // Add "Cited on pages"
        if (ck.getLookupResult().isEmpty() || alwaysAddCitedOnPages) {
            sb.append(formatCitedOnPages(cgs, ck).toString());
        }

        // Add paragraph
        OOText entryText = OOText.fromString(sb.toString());
        String parStyle = style.getReferenceParagraphFormat();
        return OOFormat.paragraph(entryText, parStyle);
    }

    /**
     * @return just the body of a bibliography entry. No label, "Cited on pages" or paragraph.
     */
    public static OOText formatBibliographyEntryBody(CitedKey ck, OOBibStyle style) {
        if (ck.getLookupResult().isEmpty()) {
            // Unresolved entry
            return OOText.fromString(String.format("Unresolved(%s)", ck.citationKey));
        } else {
            // Resolved entry, use the layout engine
            BibEntry bibentry = ck.getLookupResult().get().entry;
            Layout layout = style.getReferenceFormat(bibentry.getType());
            layout.setPostFormatter(POSTFORMATTER);

            return formatFullReferenceOfBibEntry(layout,
                                                 bibentry,
                                                 ck.getLookupResult().get().database,
                                                 ck.getUniqueLetter().orElse(null));
        }
    }

    /**
     * Format the reference part of a bibliography entry using a Layout.
     *
     * @param layout     The Layout to format the reference with.
     * @param entry      The entry to insert.
     * @param database   The database the entry belongs to.
     * @param uniquefier Uniqiefier letter, if any, to append to the entry's year.
     *
     * @return OOText The reference part of a bibliography entry formatted as OOText
     */
    private static OOText formatFullReferenceOfBibEntry(Layout layout,
                                                        BibEntry entry,
                                                        BibDatabase database,
                                                        String uniquefier) {

        // Backup the value of the uniq field, just in case the entry already has it:
        Optional<String> oldUniqVal = entry.getField(UNIQUEFIER_FIELD);

        // Set the uniq field with the supplied uniquefier:
        if (uniquefier == null) {
            entry.clearField(UNIQUEFIER_FIELD);
        } else {
            entry.setField(UNIQUEFIER_FIELD, uniquefier);
        }

        // Do the layout for this entry:
        OOText formattedText = OOText.fromString(layout.doLayout(entry, database));

        // Afterwards, reset the old value:
        if (oldUniqVal.isPresent()) {
            entry.setField(UNIQUEFIER_FIELD, oldUniqVal.get());
        } else {
            entry.clearField(UNIQUEFIER_FIELD);
        }

        return formattedText;
    }

    /**
     * Format links to citations of the source (ck).
     *
     * Requires reference marks for the citation groups.
     *
     * - The links are created as references that show page numbers of the reference marks.
     *   - We do not control the text shown, that is provided by OpenOffice.
     */
    private static OOText formatCitedOnPages(CitationGroups cgs, CitedKey ck) {

        if (!cgs.citationGroupsProvideReferenceMarkNameForLinking()) {
            return OOText.fromString("");
        }

        StringBuilder sb = new StringBuilder();

        String prefix = String.format(" (%s: ", Localization.lang("Cited on pages"));
        String suffix = ")";
        sb.append(prefix);

        List<CitationGroup> citationGroups = new ArrayList<>();
        for (CitationPath p : ck.getCitationPaths()) {
            CitationGroupId cgid = p.group;
            Optional<CitationGroup> cg = cgs.getCitationGroup(cgid);
            if (cg.isEmpty()) {
                throw new IllegalStateException();
            }
            citationGroups.add(cg.get());
        }

        // sort the citationGroups according to their indexInGlobalOrder
        citationGroups.sort((a, b) -> {
                Integer aa = a.getIndexInGlobalOrder().orElseThrow(IllegalStateException::new);
                Integer bb = b.getIndexInGlobalOrder().orElseThrow(IllegalStateException::new);
                return (aa.compareTo(bb));
            });

        int i = 0;
        for (CitationGroup cg : citationGroups) {
            if (i > 0) {
                sb.append(", ");
            }
            String markName = cg.getReferenceMarkNameForLinking().orElseThrow(IllegalStateException::new);
            OOText xref = OOFormat.formatReferenceToPageNumberOfReferenceMark(markName);
            sb.append(xref.toString());
            i++;
        }
        sb.append(suffix);
        return OOText.fromString(sb.toString());
    }

}
