package net.sf.jabref;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class MetaDataTest {

    private MetaData metaData;

    @Before
    public void setUp() {
        metaData = new MetaData();
    }

    @Test
    public void serializeNewMetadataReturnsEmptyMap() throws Exception {
        assertEquals(Collections.emptyMap(), metaData.serialize());
    }

    @Test
    public void serializeSingleSaveAction() throws IOException {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true, "title[LowerCaseChanger]");
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + Globals.NEWLINE + "title[LowerCaseChanger]" + Globals.NEWLINE + ";");
        assertEquals(expectedSerialization, metaData.serialize());
    }
}