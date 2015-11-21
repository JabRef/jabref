package net.sf.jabref.bibtex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.junit.Assert;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.importer.fetcher.GVKParser;
import net.sf.jabref.model.entry.BibtexEntry;

public class BibtexEntryUtil {

    private final static BibtexEntryWriter writer = new BibtexEntryWriter(new LatexFieldFormatter(), true);


    public static String bibtexEntryToString(BibtexEntry entry) {
        StringWriter stringWriter = new StringWriter();
        try {
            writer.write(entry, stringWriter);
        } catch (IOException e) {
            // Quick hack as this should never happen
            return "ERROR: " + e.getMessage();
        }
        return stringWriter.toString();
    }

    public static void doAssertEquals(Class<GVKParser> clazz, String resourceName, BibtexEntry entry)
            throws IOException {
        try (InputStream shouldBeIs = clazz.getResourceAsStream(resourceName)) {
            Assert.assertNotNull(shouldBeIs);
            String shouldBeEntry = CharStreams.toString(new InputStreamReader(shouldBeIs, Charsets.UTF_8));
            Assert.assertEquals(shouldBeEntry, BibtexEntryUtil.bibtexEntryToString(entry));
        }

    }
}
