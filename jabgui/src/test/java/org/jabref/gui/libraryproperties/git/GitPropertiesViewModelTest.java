package org.jabref.gui.libraryproperties.git;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitPropertiesViewModelTest {
    private BibDatabaseContext databaseContext;
    private MetaData metaData;
    private GitPropertiesViewModel viewModel;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        metaData = new MetaData();
        databaseContext.setMetaData(metaData);

        viewModel = new GitPropertiesViewModel(databaseContext);
    }

    @Test
    void testInitialStateIsFalse() {
        assertFalse(viewModel.autoCommitProperty().get());
    }

    @Test
    void testSetValuesReadsTrueFromMetadata() {
        metaData.putUnknownMetaDataItem("gitEnabled", Collections.singletonList("true"));

        viewModel.setValues();

        assertTrue(viewModel.autoCommitProperty().get());
    }

    @Test
    void testSetValuesReadsFalseFromMetadata() {
        metaData.putUnknownMetaDataItem("gitEnabled", Collections.singletonList("false"));

        viewModel.setValues();

        assertFalse(viewModel.autoCommitProperty().get());
    }

    @Test
    void testStoreSettingsWritesTrue() {
        viewModel.autoCommitProperty().set(true);
        viewModel.storeSettings();

        Map<String, List<String>> data = metaData.getUnknownMetaData();
        assertTrue(data.containsKey("gitEnabled"));
        assertEquals(Collections.singletonList("true"), data.get("gitEnabled"));
    }

    @Test
    void testStoreSettingsRemovesKeyWhenDisabled() {
        metaData.putUnknownMetaDataItem("gitEnabled", Collections.singletonList("true"));

        viewModel.autoCommitProperty().set(false);
        viewModel.storeSettings();

        assertFalse(metaData.getUnknownMetaData().containsKey("gitEnabled"));
    }
}
