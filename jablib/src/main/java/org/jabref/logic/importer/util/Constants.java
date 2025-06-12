package org.jabref.logic.importer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final char[] ZIP_HEADER_MAGIC_NUMBER = {0x50, 0x4b, 0x03, 0x04};

    public static final List<String> ZIP_FILES_EXTENSIONS = List.of(
            ".ctv6bak",
            ".zip",
            ".epub",
            ".odt",
            ".docx",
            ".xlsx",
            ".pptx",
            ".ods",
            ".odp"
    );

    public static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    public static boolean isZip(BufferedReader input) throws IOException {
        char[] header = new char[ZIP_HEADER_MAGIC_NUMBER.length];
        int nRead = input.read(header);
        return nRead == ZIP_HEADER_MAGIC_NUMBER.length && Arrays.equals(header, ZIP_HEADER_MAGIC_NUMBER);
    }
}
