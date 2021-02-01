package org.jabref.gui.maintable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainTableColumnModelTest {

    private static String testName = "field:author";
    private static MainTableColumnModel.Type testType = MainTableColumnModel.Type.NORMALFIELD;
    private static String testQualifier = "author";

    private static String testTypeOnlyName = "linked_id";
    private static MainTableColumnModel.Type testTypeOnlyType = MainTableColumnModel.Type.LINKED_IDENTIFIER;

    @Test
    public void testMainTableColumnModelParser() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testQualifier);

        assertEquals(testColumnModel.getType(), testType);
        assertEquals(testColumnModel.getQualifier(), testQualifier);
    }

    @Test
    public void testMainTableColumnModelParserFull() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testName);

        assertEquals(testColumnModel.getType(), testType);
        assertEquals(testColumnModel.getQualifier(), testQualifier);
    }

    @Test
    public void testMainTableColumnModelParserTypeOnly() {
        MainTableColumnModel testColumnModel = MainTableColumnModel.parse(testTypeOnlyName);

        assertEquals(testColumnModel.getType(), testTypeOnlyType);
        assertEquals(testColumnModel.getQualifier(), "");
    }
}
