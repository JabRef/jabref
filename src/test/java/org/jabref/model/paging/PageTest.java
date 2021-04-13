package org.jabref.model.paging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PageTest {
    private Page<String> page1;
    private Page<String> page2;
    private final int testPageNumber = 3;
    private final String testQuery = "anyQuery";
    private Collection<String> testContent = new ArrayList<>();
    private final String[] testStrings = {"str1", "str2", "str3"};

    @BeforeEach
    public void setup() {
        testContent.addAll(Arrays.asList(testStrings));
        testContent = Collections.unmodifiableCollection(testContent);
        page1 = new Page<String>(testQuery, testPageNumber, testContent);
        page2 = new Page<String>(testQuery, testPageNumber);
    }

    @Test
    public void getContentTest() {
        // make sure the collections have the same elements
        List<String> differences = new ArrayList<>(testContent);
        differences.removeAll(page1.getContent());
        assertTrue(differences.isEmpty());

        List<String> differences2 = new ArrayList<>(page1.getContent());
        differences2.removeAll(testContent);
        assertTrue(differences2.isEmpty());

        assertTrue(page2.getContent().isEmpty());
    }

    @Test
    public void getPageNumberTest() {
        assertEquals(testPageNumber, page1.getPageNumber());
    }

    @Test
    public void getQueryTest() {
        assertEquals(testQuery, page1.getQuery());
    }

    @Test
    public void getSizeTest() {
        assertEquals(testContent.size(), page1.getSize());
    }
}
