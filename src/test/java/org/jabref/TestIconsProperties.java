package org.jabref;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIconsProperties {

    @Test
    public void testExistenceOfIconImagesReferencedFromIconsProperties() throws IOException {
        String folder = "src/main/resources/images/external";
        String iconsProperties = "Icons.properties";
        String iconsPropertiesPath = "src/main/resources/images/" + iconsProperties;

        // load properties
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Paths.get(iconsPropertiesPath))) {
            properties.load(reader);
        }
        assertFalse(properties.entrySet().isEmpty(), "There must be loaded properties after loading " + iconsPropertiesPath);

        // check that each key references an existing file
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString();

            assertTrue(Files.exists(Paths.get(folder, value)), "Referenced image (" + name + " --> " + value + " does not exist in folder " + folder);
        }

        // check that each image in the folder is referenced by a key
        List<String> imagesReferencedFromProperties = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            imagesReferencedFromProperties.add(entry.getValue().toString());
        }

        try (Stream<Path> pathStream = Files.list(Paths.get(folder))) {
            List<String> fileNamesInFolder = pathStream.map(p -> p.getFileName().toString()).collect(Collectors.toList());
            fileNamesInFolder.removeAll(imagesReferencedFromProperties);
            assertEquals("[red.png]", fileNamesInFolder.toString(), "Images are in the folder that are unused");
        }
    }
}
