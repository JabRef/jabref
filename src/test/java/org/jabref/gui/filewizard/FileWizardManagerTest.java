package org.jabref.gui.filewizard;

import org.jabref.model.entry.BibEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class FileWizardManagerTest {

    @Test
    void testSerializeCheckedFilesAndDeSerializedCheckedFiles() {
        List<String> entryList = new ArrayList<>();
        File file;

        try {
            file = File.createTempFile("tmp", ".txt");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        BibEntry one = new BibEntry();
        BibEntry two = new BibEntry();
        BibEntry three = new BibEntry();

        one.setCitationKey("id1");
        two.setCitationKey("id2");
        three.setCitationKey("id3");

        entryList.add(one.getCitationKey().get());
        entryList.add(two.getCitationKey().get());
        entryList.add(three.getCitationKey().get());

        FileWizardSerializer serializer = new FileWizardSerializer();
        serializer.serializeCheckedFiles(entryList, file);
        List<String> deserializedList = serializer.deserializeCheckedFilesList(file);

        //System.out.println("Original List: " + entryList.size() + ", Deserialized List: " + deserializedList.size());

        file.delete();

        assertEquals(entryList, deserializedList);
    }
}
