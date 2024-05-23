package org.jabref.gui.maintable;

import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class MainTableColumnModelTest {

    private static final String testName = "field:author";
    private static final MainTableColumnModel.Type testType = MainTableColumnModel.Type.NORMALFIELD;
    private static final String testQualifier = "author";

    private static final String testTypeOnlyName = "linked_id";
    private static final MainTableColumnModel.Type testTypeOnlyType = MainTableColumnModel.Type.LINKED_IDENTIFIER;

    @BeforeAll
    public static void setup() {
        Injector.setModelOrService(PreferencesService.class, mock(PreferencesService.class));
    }

    @Test
    public void mainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testQualifier);

        assertEquals(testColumnModel.getType(), testType);
    }

    @Test
    public void mainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testQualifier);

        assertEquals(testColumnModel.getQualifier(), testQualifier);
    }

    @Test
    public void fullMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testName);

        assertEquals(testColumnModel.getType(), testType);
    }

    @Test
    public void fullMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testName);

        assertEquals(testColumnModel.getQualifier(), testQualifier);
    }

    @Test
    public void typeOnlyMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testTypeOnlyName);

        assertEquals(testColumnModel.getType(), testTypeOnlyType);
    }

    @Test
    public void typeOnlyMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testTypeOnlyName);

        assertEquals("", testColumnModel.getQualifier());
    }
}
