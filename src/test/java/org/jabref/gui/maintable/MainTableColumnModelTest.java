package org.jabref.gui.maintable;

import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MainTableColumnModelTest {

    private static String testName = "field:author";
    private static MainTableColumnModel.Type testType = MainTableColumnModel.Type.NORMALFIELD;
    private static String testQualifier = "author";

    private static String testTypeOnlyName = "linked_id";
    private static MainTableColumnModel.Type testTypeOnlyType = MainTableColumnModel.Type.LINKED_IDENTIFIER;

    @BeforeAll
    static void setup() {
        Injector.setModelOrService(PreferencesService.class, mock(PreferencesService.class));
    }

    @Test
    void mainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testQualifier);

        assertEquals(testType, testColumnModel.getType());
    }

    @Test
    void mainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testQualifier);

        assertEquals(testQualifier, testColumnModel.getQualifier());
    }

    @Test
    void fullMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testName);

        assertEquals(testType, testColumnModel.getType());
    }

    @Test
    void fullMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testName);

        assertEquals(testQualifier, testColumnModel.getQualifier());
    }

    @Test
    void typeOnlyMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testTypeOnlyName);

        assertEquals(testTypeOnlyType, testColumnModel.getType());
    }

    @Test
    void typeOnlyMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testTypeOnlyName);

        assertEquals("", testColumnModel.getQualifier());
    }
}
