package org.jabref.model.entry.field;

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
            // Display names are never parsed back into a field (persistence uses getName()), so they may contain spaces.
            // Title Case on purpose: column headers are read like "Author/Editor", not like sentences.
            // [impl->req~maintable.column-headers.user-friendly~1]
            // Other internal fields keep their exact name, e.g. the "JabRef" brand casing in INTERNAL_ID_FIELD.
            return switch (internalField) {
                case KEY_FIELD ->
                        "Citation Key";
                case TYPE_HEADER ->
                        "Entry Type";
                default ->
                        field.getName();
            };
        }

        return field.getName();
    }

    public static String getDisplayName(OrFields fields) {
        StringJoiner joiner = new StringJoiner("/");
        for (Field field : fields.getFields()) {
            joiner.add(FieldTextMapper.getDisplayName(field));
        }
        return joiner.toString();
    }
}
