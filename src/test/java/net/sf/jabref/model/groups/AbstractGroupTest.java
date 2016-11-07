package net.sf.jabref.model.groups;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class AbstractGroupTest {

    private AbstractGroup group;
    private final List<BibEntry> entries = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        group = mock(AbstractGroup.class, Mockito.CALLS_REAL_METHODS);

        entries.add(new BibEntry().withField("author", "author1 and author2"));
        entries.add(new BibEntry().withField("author", "author1"));
    }
}
