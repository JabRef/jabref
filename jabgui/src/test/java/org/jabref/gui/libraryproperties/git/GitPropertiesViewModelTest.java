package org.jabref.gui.libraryproperties.git;

import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void initialStateIsFalse() {
        viewModel.setValues();
        assertFalse(viewModel.autoCommitProperty().get());
        assertFalse(viewModel.autoPullProperty().get());
        assertFalse(viewModel.autoPushProperty().get());
    }

    @Test
    void setValuesReadsGranularKeys() {
        metaData.putUnknownMetaDataItem(MetaData.GIT_AUTO_COMMIT, List.of("true"));

        viewModel.setValues();

        assertTrue(viewModel.autoCommitProperty().get());
        assertFalse(viewModel.autoPullProperty().get());
        assertFalse(viewModel.autoPushProperty().get());
    }

    @Test
    void storeSettingsWritesGranularKeys() {
        // checks "Commit" and "Push"
        viewModel.autoCommitProperty().set(true);
        viewModel.autoPullProperty().set(false);
        viewModel.autoPushProperty().set(true);

        viewModel.storeSettings();

        Map<String, List<String>> data = metaData.getUnknownMetaData();

        assertTrue(data.containsKey(MetaData.GIT_AUTO_COMMIT));
        assertFalse(data.containsKey(MetaData.GIT_AUTO_PULL));
        assertTrue(data.containsKey(MetaData.GIT_AUTO_PUSH));
    }
}
