package org.jabref.model.metadata;

import java.util.Optional;

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
    public void emptyGroupsIfNotSet() {
        assertEquals(Optional.empty(), metaData.getGroups());
    }
}
