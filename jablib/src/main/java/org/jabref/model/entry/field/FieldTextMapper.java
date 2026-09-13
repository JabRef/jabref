package org.jabref.model.entry.field;

import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.strings.StringUtil;

@AllowedToUseLogic("Uses StringUtil temporarily")
public class FieldTextMapper {
    public static String getDisplayName(Field field) {
        if (field.isStandardField()) {
            StandardField standardField = (StandardField) field;
            return switch (standardField) {
                case DOI ->
                        "DOI";
                case ISBN ->
                        "ISBN";
                case ISRN ->
                        "ISRN";
                case ISSN ->
                        "ISSN";
                case PDF ->
                        "PDF";
                case PMID ->
                        "PMID";
                case PS ->
                        "PS";
                case URI ->
                        "URI";
                case URL ->
                        "URL";
                default ->
                        StringUtil.capitalizeFirst(field.getName());
            };
        } else if (field instanceof InternalField internalField) {
            // Display names may contain spaces (persistence uses getName(), while UI pickers use fromDisplayName()).
            // Title Case on purpose: column headers are read like "Author/Editor", not like sentences.
            // [impl->req~maintable.column-headers.user-friendly~1]
            // Other internal fields keep their exact name, e.g. the "JabRef" brand casing in INTERNAL_ID_FIELD.
            return switch (internalField) {
                case KEY_FIELD ->
                        "Citation Key";
                case TYPE_HEADER,
                     OBSOLETE_TYPE_HEADER ->
                        "Entry Type";
                case INTERNAL_ALL_FIELD ->
                        "All";
                case INTERNAL_ALL_TEXT_FIELDS_FIELD ->
                        "All text fields";
                default ->
                        field.getName();
            };
        } else if (field instanceof SpecialField specialField) {
            return switch (specialField) {
                case PRINTED ->
                        "Printed";
                case PRIORITY ->
                        "Priority";
                case QUALITY ->
                        "Quality";
                case RANKING ->
                        "Ranking";
                case READ_STATUS ->
                        "Read status";
                case RELEVANCE ->
                        "Relevance";
            };
        }

        return field.getName();
    }

    public static Optional<Field> fromDisplayName(String displayName) {
        if (StringUtil.isBlank(displayName)) {
            return Optional.empty();
        }

        return switch (displayName.trim().toLowerCase(Locale.ROOT)) {
            case "citation key",
                 "citationkey",
                 "bibtexkey" ->
                    Optional.of(InternalField.KEY_FIELD);
            case "entry type",
                 "entrytype",
                 "bibtextype" ->
                    Optional.of(InternalField.TYPE_HEADER);
            case "all" ->
                    Optional.of(InternalField.INTERNAL_ALL_FIELD);
            case "all text fields",
                 "all-text-fields" ->
                    Optional.of(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);
            case "printed" ->
                    Optional.of(SpecialField.PRINTED);
            case "priority" ->
                    Optional.of(SpecialField.PRIORITY);
            case "quality",
                 "qualityassured" ->
                    Optional.of(SpecialField.QUALITY);
            case "ranking" ->
                    Optional.of(SpecialField.RANKING);
            case "read status",
                 "readstatus" ->
                    Optional.of(SpecialField.READ_STATUS);
            case "relevance" ->
                    Optional.of(SpecialField.RELEVANCE);
            default ->
                    Optional.empty();
        };
    }

    public static String getDisplayName(OrFields fields) {
        StringJoiner joiner = new StringJoiner("/");
        for (Field field : fields.getFields()) {
            joiner.add(FieldTextMapper.getDisplayName(field));
        }
        return joiner.toString();
    }
}
