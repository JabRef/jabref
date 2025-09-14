package org.jabref.model.entry.field;

import java.util.StringJoiner;

import org.jabref.model.strings.StringUtil;

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
        } else if (field == InternalField.KEY_FIELD) {
            return "Citationkey";
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
