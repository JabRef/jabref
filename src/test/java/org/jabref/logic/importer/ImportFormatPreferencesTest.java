package org.jabref.logic.importer;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ImportFormatPreferencesTest {

    //first commit on rebase test branch
    //second commit
    //third commit

    Charset charset = StandardCharsets.UTF_8;
    String testPath = "";
    String testClassName = "";
    boolean testBool = true;
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
    GlobalBibtexKeyPattern keyPattern = null;
    BibtexKeyPatternPreferences testBibtexKeyPat = null; //new BibtexKeyPatternPreferences(testPath, testPath, testBool, testBool, testBool, keyPattern, testChar);
    FieldContentParserPreferences testFieldContentParser = null;
    List<String> testList = new List<String>() {
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
        public Iterator<String> iterator() {
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
        public boolean add(String s) {
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
        public boolean addAll(Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public String get(int index) {
            return null;
        }

        @Override
        public String set(int index, String element) {
            return null;
        }

        @Override
        public void add(int index, String element) {

        }

        @Override
        public String remove(int index) {
            return null;
        }

        @Override
        public int indexOf(Object o) {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            return 0;
        }

        @Override
        public ListIterator<String> listIterator() {
            return null;
        }

        @Override
        public ListIterator<String> listIterator(int index) {
            return null;
        }

        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            return null;
        }
    };
    XmpPreferences testXmpPref = new XmpPreferences(testBool, testList, testChar);

    ImportFormatPreferences sut = new ImportFormatPreferences(testSet, charset, testChar, testBibtexKeyPat, testFieldContentParser, testXmpPref, testBool);

    @Test
    void whenMakeImportFormatPreferencesTest_thenConstructorWorks() {
        sut = new ImportFormatPreferences(testSet, charset, testChar, testBibtexKeyPat, testFieldContentParser, testXmpPref, testBool);
    }

    @Test
    void getCustomImportList() {
    }

    @Test
    void whenGetEncoding_thenCharSetEncondingReturned() {
        Charset testSet = StandardCharsets.UTF_8;
        Charset sutSet = sut.getEncoding();
        assertEquals(testSet, sutSet);
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
