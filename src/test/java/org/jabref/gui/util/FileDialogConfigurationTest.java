package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

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
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory((String) null).build();

        assertEquals(Optional.empty(), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithNullPathDirectory() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory((Path) null).build();

        assertEquals(Optional.empty(), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testWithNonExistingDirectoryAndParentNull() {
        String tempFolder = "workingDirectory";
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(tempFolder).build();

        assertEquals(Optional.empty(), fileDialogConfiguration.getInitialDirectory());
    }

    @Test
    public void testSingleExtension() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withDefaultExtension(StandardFileType.BIBTEX_DB).build();

        FileChooser.ExtensionFilter filter = toFilter(String.format("%1s %2s", "BibTex", Localization.lang("Library")), StandardFileType.BIBTEX_DB);

        assertEquals(filter.getExtensions(), fileDialogConfiguration.getDefaultExtension().getExtensions());
    }

    private FileChooser.ExtensionFilter toFilter(String description, FileType extension) {
        return new FileChooser.ExtensionFilter(description,
                extension.getExtensions().stream().map(ending -> "*." + ending).collect(Collectors.toList()));
    }

}
