package org.jabref.model.metadata;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void givenContentSelectorWhenAddContentSelectorThenContentSelectorsIsUpdatedProperly()
    {
        ContentSelectors checkAgainst = new ContentSelectors();
        ContentSelector first = new ContentSelector("keyword", "First");
        ContentSelector second = new ContentSelector("keyword", "Second");
        boolean testResult = true;
        metaData.addContentSelector(first);
        checkAgainst.addContentSelector(first);
        metaData.addContentSelector(second);
        checkAgainst.addContentSelector(second);
        if (testResult) { testResult = metaData.getContentSelectors().equals(checkAgainst);}
        if (testResult) { testResult = metaData.getContentSelectorList().get(0).equals(first);}
        if (testResult) { testResult = metaData.getContentSelectorList().get(1).equals(second);}
        assertTrue(testResult);
    }
}
