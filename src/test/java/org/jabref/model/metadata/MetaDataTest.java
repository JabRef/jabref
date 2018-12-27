package org.jabref.model.metadata;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataTest {

    private MetaData metaData;

    @BeforeEach
    public void setUp() {
        metaData = new MetaData();
    }

    @Test
    public void emptyGroupsIfNotSet() {
        assertEquals(Optional.empty(), metaData.getGroups());
    }
}
