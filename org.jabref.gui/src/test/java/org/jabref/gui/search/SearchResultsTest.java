package org.jabref.gui.search;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.jabref.gui.BasePanel;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.TestUtils;
import org.jabref.testutils.category.GUITest;

import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(GUITest.class)
public class SearchResultsTest extends AssertJSwingJUnitTestCase {

    private FrameFixture frameFixture;


    @BeforeClass
    public static void before() {
        FailOnThreadViolationRepaintManager.uninstall();
    }

    @Override
    protected void onSetUp() {
        TestUtils.initJabRef();
        frameFixture = WindowFinder.findFrame(JFrame.class).withTimeout(15, TimeUnit.SECONDS).using(robot());
    }

    @Override
    protected void onTearDown() {
        frameFixture.close();
        frameFixture = null;

        TestUtils.closeJabRef();
    }

    @Test
    public void testSearchFieldQuery() {
        frameFixture.menuItemWithPath("Search", "Search").click();
        JTextComponentFixture searchField = frameFixture.textBox();
        ComponentFinder finder = robot().finder();
        BasePanel panel = finder.findByType(BasePanel.class);
        Collection<BibEntry> entries = panel.getDatabase().getEntries();

        searchField.deleteText().enterText("");
        Assert.assertEquals(19, entries.size());

        searchField.deleteText().enterText("entrytype=article");
        Assert.assertFalse(entries.stream().noneMatch(entry -> entry.isSearchHit()));
        Assert.assertEquals(5, entries.stream().filter(entry -> entry.isSearchHit()).count());

        searchField.deleteText().enterText("entrytype=proceedings");
        Assert.assertFalse(entries.stream().noneMatch(entry -> entry.isSearchHit()));
        Assert.assertEquals(13, entries.stream().filter(entry -> entry.isSearchHit()).count());

        searchField.deleteText().enterText("entrytype=book");
        Assert.assertFalse(entries.stream().noneMatch(entry -> entry.isSearchHit()));
        Assert.assertEquals(1, entries.stream().filter(entry -> entry.isSearchHit()).count());
    }

    @Test
    public void testSeachWithoutResults() {
        frameFixture.menuItemWithPath("Search", "Search").click();
        JTextComponentFixture searchField = frameFixture.textBox();
        ComponentFinder finder = robot().finder();
        BasePanel panel = finder.findByType(BasePanel.class);
        Collection<BibEntry> entries = panel.getDatabase().getEntries();

        searchField.deleteText().enterText("asdf");
        Assert.assertTrue(entries.stream().noneMatch(entry -> entry.isSearchHit()));
    }

    @Test
    public void testSearchInvalidQuery() {
        frameFixture.menuItemWithPath("Search", "Search").click();
        JTextComponentFixture searchField = frameFixture.textBox();
        ComponentFinder finder = robot().finder();
        BasePanel panel = finder.findByType(BasePanel.class);
        Collection<BibEntry> entries = panel.getDatabase().getEntries();

        searchField.deleteText().enterText("asdf[");
        Assert.assertTrue(entries.stream().noneMatch(entry -> entry.isSearchHit()));
    }

}
