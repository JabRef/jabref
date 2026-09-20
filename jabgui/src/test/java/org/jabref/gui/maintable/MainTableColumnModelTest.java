package org.jabref.gui.maintable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTableColumnModelTest {

    private static final String TEST_NAME = "field:author";
    private static final MainTableColumnModel.Type TEST_TYPE = MainTableColumnModel.Type.NORMALFIELD;
    private static final String TEST_QUALIFIER = "author";

    private static final String TEST_TYPE_ONLY_NAME = "linked_id";
    private static final MainTableColumnModel.Type TEST_TYPE_ONLY_TYPE = MainTableColumnModel.Type.LINKED_IDENTIFIER;

    @Test
    void mainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_QUALIFIER);

        assertEquals(TEST_TYPE, testColumnModel.getType());
    }

    @Test
    void mainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_QUALIFIER);

        assertEquals(TEST_QUALIFIER, testColumnModel.getQualifier());
    }

    @Test
    void fullMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_NAME);

        assertEquals(TEST_TYPE, testColumnModel.getType());
    }

    @Test
    void fullMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_NAME);

        assertEquals(TEST_QUALIFIER, testColumnModel.getQualifier());
    }

    @Test
    void typeOnlyMainTableColumnModelParserRetrievesCorrectType() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_TYPE_ONLY_NAME);

        assertEquals(TEST_TYPE_ONLY_TYPE, testColumnModel.getType());
    }

    @Test
    void typeOnlyMainTableColumnModelParserRetrievesCorrectQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(TEST_TYPE_ONLY_NAME);

        assertEquals("", testColumnModel.getQualifier());
    }

    @Test
    void emptyStringShouldReturnNormalFieldWithEmptyQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse("");

        assertEquals(MainTableColumnModel.Type.NORMALFIELD, testColumnModel.getType());
        assertEquals("", testColumnModel.getQualifier());
    }

    /// A special field's display name is the label of the action that sets it, which is static.
    /// Nothing is registered with the injector here on purpose: preference migrations build column
    /// models before the GUI starts, so a lookup at this point crashes startup.
    @Test
    void specialFieldColumnNamesItselfWithoutAnyRegisteredService() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse("field:printed");

        assertEquals("Printed (Special)", testColumnModel.getDisplayName());
    }

    @Test
    void blankStringShouldReturnNormalFieldWithEmptyQualifier() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse("   ");

        assertEquals(MainTableColumnModel.Type.NORMALFIELD, testColumnModel.getType());
        assertEquals("", testColumnModel.getQualifier());
    }
}
