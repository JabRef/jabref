package net.sf.jabref;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.exporter.FieldFormatterCleanups;
import net.sf.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetaDataTest {

    private MetaData metaData;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        metaData = new MetaData();
    }

    @Test
    public void serializeNewMetadataReturnsEmptyMap() throws Exception {
        assertEquals(Collections.emptyMap(), metaData.getAsStringMap());
    }

    @Test
    public void serializeSingleSaveAction() {
        FieldFormatterCleanups saveActions = new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("title", new LowerCaseFormatter())));
        metaData.setSaveActions(saveActions);

        Map<String, String> expectedSerialization = new TreeMap<>();
        expectedSerialization.put("saveActions",
                "enabled;" + FileUtil.NEWLINE + "title[lower_case]" + FileUtil.NEWLINE + ";");
        assertEquals(expectedSerialization, metaData.getAsStringMap());
    }
}
