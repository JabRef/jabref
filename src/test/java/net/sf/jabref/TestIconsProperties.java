package net.sf.jabref;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertFalse("There must be loaded properties after loading " + iconsPropertiesPath,
                properties.entrySet().isEmpty());

        // check that each key references an existing file
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString();

            assertTrue("Referenced image (" + name + " --> " + value + " does not exist in folder " + folder, Files.exists(Paths.get(folder, value)));
        }

        // check that each image in the folder is referenced by a key
        List<String> imagesReferencedFromProperties = new ArrayList<>();
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
            imagesReferencedFromProperties.add(entry.getValue().toString());
        }

        List<String> fileNamesInFolder = Files.list(Paths.get(folder)).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        fileNamesInFolder.removeAll(imagesReferencedFromProperties);

        assertEquals("Images are in the folder that are unused", "[red.png]", fileNamesInFolder.toString());
    }

}
