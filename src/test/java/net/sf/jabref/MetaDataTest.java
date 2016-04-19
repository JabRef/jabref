package net.sf.jabref;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;

import org.junit.Before;
import org.junit.Test;

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
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + Globals.NEWLINE + "title[lower_case]" + Globals.NEWLINE + ";");
        assertEquals(expectedSerialization, metaData.serialize());
    }
}