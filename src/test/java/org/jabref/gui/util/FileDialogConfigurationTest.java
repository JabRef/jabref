package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BasicFileType;
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
                .withDefaultExtension(BasicFileType.BIBTEX_DB).build();

        FileChooser.ExtensionFilter filter = toFilter(String.format("%1s %2s", "BibTex", Localization.lang("Library")), BasicFileType.BIBTEX_DB);

        assertEquals(filter.getExtensions(), fileDialogConfiguration.getDefaultExtension().getExtensions());
    }

    @Test
    public void testMultipleExtension() {
        Map<String, FileType> extensions = EnumSet.allOf(BasicFileType.class).stream().collect(Collectors.toMap(Object::toString, Function.identity()));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilters(extensions).build();


        List<FileChooser.ExtensionFilter> extensionFilters = extensions.entrySet().stream().map(entry -> toFilter(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        //We use size here as we otherwise would compare object references, as extension filters does not override equals
        assertEquals(extensionFilters.size(), fileDialogConfiguration.getExtensionFilters().size());

    }

    private FileChooser.ExtensionFilter toFilter(String description, FileType extension) {
        return new FileChooser.ExtensionFilter(description,
                extension.getExtensions().stream().map(ending -> "*." + ending).collect(Collectors.toList()));
    }

}
