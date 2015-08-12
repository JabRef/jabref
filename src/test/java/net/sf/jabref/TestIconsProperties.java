/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref;

import org.junit.Assert;
import org.junit.Test;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestIconsProperties {

    @Test
    public void testExistenceOfIconImagesReferencedFromIconsProperties() throws IOException {
        String folder = "src/main/resources/images/crystal_16";
        String iconsProperties = "Icons.properties";
        String iconsPropertiesPath = folder + "/" + iconsProperties;

        // load properties
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(Paths.get(iconsPropertiesPath))) {
            properties.load(reader);
        }
        assertTrue("There must be loaded properties after loading " + iconsPropertiesPath, !properties.entrySet().isEmpty());

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

        assertEquals("Images are in the folder that are unused", "[Icons.properties]", fileNamesInFolder.toString());
    }

}
