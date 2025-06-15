package org.jabref.logic.importer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final char[] ZIP_HEADER_MAGIC_NUMBER = {0x50, 0x4b, 0x03, 0x04};
    // public static final char[] OLE_COMPOUND_MAGIC_NUMBER = {0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1};
    public static final char[] OLE_COMPOUND_MAGIC_NUMBER = {65533, 65533, 17, 2161, 0x1A, 65533, 0x00, 0x00};

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

    public static final List<String> OLE_COMPOUND_FILES_EXTENSIONS = List.of(
            ".doc",
            ".xls",
            ".ppt"
    );

    public static boolean isZip(BufferedReader input) throws IOException {
        return hasMagicNumber(input, ZIP_HEADER_MAGIC_NUMBER);
    }

    public static boolean isOleCompound(BufferedReader input) throws IOException {
        return hasMagicNumber(input, OLE_COMPOUND_MAGIC_NUMBER);
    }

    public static boolean hasMagicNumber(BufferedReader input, char[] magicNumber) throws IOException {
        char[] header = new char[magicNumber.length];
        int nRead = input.read(header);
        return nRead == magicNumber.length && Arrays.equals(header, magicNumber);
    }
}
