package org.jabref.gui.integrity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.integrity.IntegrityIssue;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrityCheckDialogViewModel extends AbstractViewModel {

    private final Logger LOGGER = LoggerFactory.getLogger(IntegrityCheckDialogViewModel.class);

    private final ListProperty<IntegrityMessage> columnsListProperty;
    private final ObservableList<IntegrityMessage> messages;
    private final ObservableSet<String> entryTypes;

    public IntegrityCheckDialogViewModel(List<IntegrityMessage> messages) {
        this.messages = FXCollections.observableArrayList(messages);

        Set<String> types = messages.stream()
                                    .map(IntegrityMessage::message)
                                    .collect(Collectors.toSet());
        this.entryTypes = FXCollections.observableSet(types);

        this.columnsListProperty = new SimpleListProperty<>(FXCollections.observableArrayList(messages));
    }

    public ListProperty<IntegrityMessage> columnsListProperty() {
        return this.columnsListProperty;
    }

    public ObservableList<IntegrityMessage> getMessages() {
        return messages;
    }

    public Set<String> getEntryTypes() {
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
            case INCORRECT_FORMAT:
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
                replaceNonASCIICharacters(message);
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
        if (title.isEmpty()) {
            return;
        }
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

    public void correctDateFormat(IntegrityMessage message) {
        String date = message.entry().getField(StandardField.DATE).orElse("");
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate parsedDate = LocalDate.parse(date, inputFormat);
            updateField(message, StandardField.DATE, parsedDate.format(targetFormat));
        } catch (DateTimeParseException e) {
            LOGGER.error("Invalid date format: {}", date);
        }
    }

    public void formatPageNumberRange(IntegrityMessage message) {
        String pages = message.entry().getField(StandardField.PAGES).orElse("");
        if (!pages.matches("\\d+(-\\d+)?")) {
            updateField(message, StandardField.PAGES, "1-10");
        }
    }

    public void replaceNonASCIICharacters(IntegrityMessage message) {
        String value = message.entry().getField(StandardField.ABSTRACT).orElse("");
        String normalized = value.replaceAll("[^\\x00-\\x7F]", "");
        updateField(message, StandardField.ABSTRACT, normalized);
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
