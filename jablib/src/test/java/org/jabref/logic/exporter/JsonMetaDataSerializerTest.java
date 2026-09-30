package org.jabref.logic.exporter;

import java.util.Optional;

import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonMetaDataSerializerTest {

    /// Which is what lets a library that has never had such a comment be written exactly as before.
    @Test
    void aLibraryThatKeepsNothingThisWayProducesNoObject() {
        assertEquals(Optional.empty(), JsonMetaDataSerializer.serialize(new MetaData()));
    }

    @Test
    void whatANewerJabRefWroteIsWrittenBackAsItCame() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("somethingElse", "{\"a\":[1,2]}");

        assertEquals(Optional.of("""
                {
                  "somethingElse" : {
                    "a" : [ 1, 2 ]
                  }
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }

    /// The order the file had, so that saving does not reshuffle what somebody edited by hand.
    @Test
    void theKeysKeepTheOrderTheyCameIn() {
        MetaData metaData = new MetaData();
        metaData.putUnknownJsonMetaDataItem("zeta", "1");
        metaData.putUnknownJsonMetaDataItem("alpha", "2");

        assertEquals(Optional.of("""
                {
                  "zeta" : 1,
                  "alpha" : 2
                }"""), JsonMetaDataSerializer.serialize(metaData));
    }
}
