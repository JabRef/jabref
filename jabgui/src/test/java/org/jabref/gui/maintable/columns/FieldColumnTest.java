package org.jabref.gui.maintable.columns;

import java.util.List;

import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableTooltip;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
@ExtendWith(JavaFxExtension.class)
@ResourceLock("Localization.lang")
class FieldColumnTest {

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"CHAPTER", "CITATIONCOUNT", "DAY", "DAYFILED", "EDITION", "NUMBER", "PAGETOTAL", "PART", "PMID", "VOLUME", "VOLUMES", "YEAR", "YEARFILED"})
    void sortNumericFieldsAscending(StandardField field) {
        FieldColumn column = new FieldColumn(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, field.getName()), mock(MainTableTooltip.class));

        List<String> sortedValues = List.of("25", "2", "10", "1", "3").stream()
                                        .sorted(column.getComparator())
                                        .toList();

        assertEquals(List.of("1", "2", "3", "10", "25"), sortedValues);
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"CHAPTER", "CITATIONCOUNT", "DAY", "DAYFILED", "EDITION", "NUMBER", "PAGETOTAL", "PART", "PMID", "VOLUME", "VOLUMES", "YEAR", "YEARFILED"})
    void sortNumericFieldsDescending(StandardField field) {
        FieldColumn column = new FieldColumn(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, field.getName()), mock(MainTableTooltip.class));

        List<String> sortedValues = List.of("25", "2", "10", "1", "3").stream()
                                        .sorted(column.getComparator().reversed())
                                        .toList();

        assertEquals(List.of("25", "10", "3", "2", "1"), sortedValues);
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"DOI", "EID", "EPRINT", "ISBN", "ISRN", "ISSN", "ISSUE", "MR_NUMBER", "PAGES", "REPORTNO", "REVISION", "TITLE", "VERSION", "YEARDIVISION", "ZBL_NUMBER"})
    void sortNonnumericFieldsAsStrings(StandardField field) {
        FieldColumn column = new FieldColumn(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, field.getName()), mock(MainTableTooltip.class));

        List<String> sortedValues = List.of("25", "2", "10", "1", "3").stream()
                                        .sorted(column.getComparator())
                                        .toList();

        assertEquals(List.of("1", "10", "2", "25", "3"), sortedValues);
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"DOI", "EID", "EPRINT", "ISBN", "ISRN", "ISSN", "ISSUE", "MR_NUMBER", "PAGES", "REPORTNO", "REVISION", "TITLE", "VERSION", "YEARDIVISION", "ZBL_NUMBER"})
    void sortNonnumericFieldsAsStringsDescending(StandardField field) {
        FieldColumn column = new FieldColumn(new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD, field.getName()), mock(MainTableTooltip.class));

        List<String> sortedValues = List.of("25", "2", "10", "1", "3").stream()
                                        .sorted(column.getComparator().reversed())
                                        .toList();

        assertEquals(List.of("3", "25", "2", "10", "1"), sortedValues);
    }
}
