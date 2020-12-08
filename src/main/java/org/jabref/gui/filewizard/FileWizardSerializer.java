package org.jabref.gui.filewizard;

import org.jabref.model.entry.BibEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the methods needed for (de-) serializing the list of BibEntries which have already been successfully linked.
 */
public class FileWizardSerializer {
    /**
     *  Serializes the list of already checked BibEntries, making the information survive past runtime.
     */
    public void serializeCheckedFiles(List<String> checkedFilesList, File checkedFilesListFile) {
        try
        {
            FileOutputStream fos = new FileOutputStream(checkedFilesListFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(checkedFilesList);
            oos.close();
            fos.close();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    /**
     *  Deserializes the list of already checked BibEntry, minimizing redundant work for the Wizard.
     */
    public List<String> deserializeCheckedFilesList(File checkedFilesListFile) {
        try
        {
            FileInputStream fis = new FileInputStream(checkedFilesListFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            List<String> result = (ArrayList<String>) ois.readObject();

            ois.close();
            fis.close();

            return result;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }
        catch (ClassNotFoundException c)
        {
            System.out.println("Class not found");
            c.printStackTrace();
            return null;
        }
    }
}
