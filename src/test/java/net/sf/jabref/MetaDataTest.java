package net.sf.jabref;

import net.sf.jabref.exporter.SaveActions;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

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
    public void serializeSingleSaveAction() throws Exception {
        SaveActions saveActions = new SaveActions(true, "title[LowerCaseChanger]");
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + Globals.NEWLINE + "title[LowerCaseChanger]" + Globals.NEWLINE + ";");
        assertEquals(expectedSerialization, metaData.serialize());
    }
}