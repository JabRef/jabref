package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.util.FileType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class FileDialogConfigurationTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWithValidDirectoryString() throws IOException {
        String tempFolder = folder.newFolder().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.of(Paths.get(tempFolder)), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithValidDirectoryPath() throws IOException {
        Path tempFolder = folder.newFolder().toPath();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.of(tempFolder), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithNullStringDirectory() {
        String tempFolder = null;
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.ofNullable(tempFolder), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithNullPathDirectory() {
        Path tempFolder = null;
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.ofNullable(tempFolder), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithNonExistingDirectoryAndParentNull() {
        String tempFolder = "workingDirectory";
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.ofNullable(null), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testSingleExtension() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withDefaultExtension(FileType.BIBTEX_DB).build();

        FileChooser.ExtensionFilter filter = toFilter(FileType.BIBTEX_DB);

        assertEquals(filter.getExtensions(), fileDialogConfiguration.getDefaultExtension().getExtensions());
    }

    @Test
    public void testMultipleExtension() {
        EnumSet<FileType> extensions = EnumSet.allOf(FileType.class);

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilters(extensions).build();

        List<FileChooser.ExtensionFilter> extensionFilters = extensions.stream().map(this::toFilter)
                .collect(Collectors.toList());

        //We use size here as we otherwise would compare object references, as extension filters does not override equals
        assertEquals(extensionFilters.size(), fileDialogConfiguration.getExtensionFilters().size());

    }

    private FileChooser.ExtensionFilter toFilter(FileType extension) {
        return new FileChooser.ExtensionFilter(extension.getDescription(),
                extension.getExtensions().stream().map(ending -> "*." + ending).collect(Collectors.toList()));
    }

}
