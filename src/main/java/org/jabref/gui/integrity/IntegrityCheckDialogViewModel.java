package org.jabref.gui.integrity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<Field> entryTypes;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);

        Set<Field> types = messages.stream()
                                    .map(IntegrityMessage::field)
                                    .collect(Collectors.toSet());
        this.entryTypes = FXCollections.observableSet(types);
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }

    public Set<Field> getEntryTypes() {
        return entryTypes;
    }

    public void removeFromEntryTypes(String entry) {
        entryTypes.remove(entry);
    }

    public void fix(IntegrityIssue issue, IntegrityMessage message) {
        switch (issue) {
            case CAPITAL_LETTER_ARE_NOT_MASKED_USING_CURLY_BRACKETS:
                maskTitle(message);
                break;
            case BIBTEX_FIELD_ONLY_KEY, BIBTEX_FIELD_ONLY_CROSS_REF:
                removeField(message, issue.getField());
                break;
            case INCORRECT_FORMAT_DATE:
                correctDateFormat(message);
                break;
            case NO_INTEGER_AS_VALUE_FOR_EDITION_ALLOWED:
                removeNonIntegerEdition(message);
                break;
            case SHOULD_CONTAIN_AN_INTEGER_OR_A_LITERAL:
                ensureValidEdition(message);
                break;
            case SHOULD_HAVE_THE_FIRST_LETTER_CAPITALIZED:
                capitalizeFirstLetter(message, StandardField.EDITION);
                break;
            case REFERENCED_CITATION_KEY_DOES_NOT_EXIST:
                handleMissingCitationKey(message);
                break;
            case NON_ASCII_ENCODED_CHARACTER_FOUND:
                replaceNonASCIICharacters(message, StandardField.ABSTRACT);
                break;
            case SHOULD_CONTAIN_A_VALID_PAGE_NUMBER_RANGE:
                formatPageNumberRange(message);
                break;
            default:
                break;
        }
    }

    public void maskTitle(IntegrityMessage message) {
        String title = message.entry().getTitle().get();
        StringBuilder result = new StringBuilder();
        for (char ch : title.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append("{").append(ch).append("}");
            } else {
                result.append(ch);
            }
        }
        Map<Field, String> fields = new HashMap<>();
        fields.put(StandardField.TITLE, result.toString());
        message.entry().setField(fields);
    }

    public void removeField(IntegrityMessage message, Field field) {
        Map<Field, String> fields = new HashMap<>();
        fields.put(field, "");
        message.entry().setField(fields);
    }

    public void correctDateFormat(IntegrityMessage message) {
        String date = message.entry().getField(StandardField.DATE).orElse("");
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            updateField(message, StandardField.DATE, "YYYY-MM-DD");
        }
    }

    public void formatPageNumberRange(IntegrityMessage message) {
        String pages = message.entry().getField(StandardField.PAGES).orElse("");
        if (!pages.matches("\\d+(-\\d+)?")) {
            updateField(message, StandardField.PAGES, "1-10");
        }
    }

    public void replaceNonASCIICharacters(IntegrityMessage message, Field field) {
        String value = message.entry().getField(field).orElse("");
        String normalized = value.replaceAll("[^\\x00-\\x7F]", "");
        updateField(message, field, normalized);
    }

    public void handleMissingCitationKey(IntegrityMessage message) {
        updateField(message, StandardField.CROSSREF, "UnknownKey");
    }

    public void capitalizeFirstLetter(IntegrityMessage message, Field field) {
        String value = message.entry().getField(field).orElse("");
        if (!value.isEmpty()) {
            String capitalized = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
            updateField(message, field, capitalized);
        }
    }

    public void ensureValidEdition(IntegrityMessage message) {
        String edition = message.entry().getField(StandardField.EDITION).orElse("");
        if (!edition.matches("\\d+|First|Second|Third")) {
            updateField(message, StandardField.EDITION, "First");
        }
    }

    public void removeNonIntegerEdition(IntegrityMessage message) {
        String edition = message.entry().getField(StandardField.EDITION).orElse("");
        if (!edition.matches("\\d+")) {
            updateField(message, StandardField.EDITION, "");
        }
    }

    public void updateField(IntegrityMessage message, Field field, String newValue) {
        Map<Field, String> fields = new HashMap<>();
        fields.put(field, newValue);
        message.entry().setField(fields);
    }
}
