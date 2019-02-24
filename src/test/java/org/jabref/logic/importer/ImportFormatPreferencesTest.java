package org.jabref.logic.importer;

import org.jabref.logic.importer.fileformat.CustomImporter;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ImportFormatPreferencesTest {

    @Test
    void whenMakeImportFormatPreferencesTest_thenConstructorWorks() {
        Charset charset = StandardCharsets.UTF_8;
        String testPath = "";
        String testClassName = "";
        try {
            CustomImporter testImporter = new CustomImporter(testPath, testClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Set<CustomImporter> testSet = new Set<CustomImporter>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<CustomImporter> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(CustomImporter customImporter) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends CustomImporter> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }
        };
        Character testChar = new Character('a');
        ImportFormatPreferences sut = new ImportFormatPreferences();
    }

    @Test
    void getCustomImportList() {
    }

    @Test
    void whenGetEncoding_thenCharSetEncondingReturned() {

    }

    @Test
    void getKeywordSeparator() {
    }

    @Test
    void getBibtexKeyPatternPreferences() {
    }

    @Test
    void getFieldContentParserPreferences() {
    }

    @Test
    void withEncoding() {
    }

    @Test
    void isKeywordSyncEnabled() {
    }

    @Test
    void getXmpPreferences() {
    }
}
