package org.jabref.gui.importer;

import java.nio.file.Path;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.importer.fileformat.CustomImporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImporterViewModelTest {

    private final CustomImporter importer = mock(CustomImporter.class);
    private ImporterViewModel importerViewModel;

    private final StringProperty nameStringProperty = new SimpleStringProperty();
    private final StringProperty classNameStringProperty = new SimpleStringProperty();
    private final StringProperty basePathStringProperty = new SimpleStringProperty();

    @BeforeEach
    void setUp() {
        when(importer.getName()).thenReturn("name");
        when(importer.getClassName()).thenReturn("className");
        when(importer.getBasePath()).thenReturn(Path.of("path"));
        importerViewModel = new ImporterViewModel(importer);
    }

    @Test
    void getLogicTest() {
        assertEquals(importer, importerViewModel.getLogic());
    }

    @Test
    void nameTest() {
        nameStringProperty.setValue("name");
        assertEquals(nameStringProperty.toString(), importerViewModel.name().toString());
    }

    @Test
    void classNameTest() {
        classNameStringProperty.setValue("className");
        assertEquals(classNameStringProperty.toString(), importerViewModel.className().toString());
    }

    @Test
    void basePathTest() {
        basePathStringProperty.setValue("path");
        assertEquals(basePathStringProperty.toString(), importerViewModel.basePath().toString());
    }
}
