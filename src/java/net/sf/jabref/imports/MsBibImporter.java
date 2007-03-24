package net.sf.jabref.imports;

import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.IOException;

/**
 * Importer for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed
 *
 * ...
 */
public class MsBibImporter extends ImportFormat {

    public boolean isRecognizedFormat(InputStream in) throws IOException {

        /*
            This method is available for checking if a file can be of the MSBib type.
            The effect of this method is primarily to avoid unnecessary processing of
            files when searching for a suitable import format. If this method returns
            false, the import routine will move on to the next import format.

            The correct behaviour is to return false if it is certain that the file is
            not of the MsBib type, and true otherwise. Returning true is the safe choice
            if not certain.
         */

        return true;
    }

    public List importEntries(InputStream in) throws IOException {

        System.out.println("Hello world MsBibImporter");

        List entries = new ArrayList();
        /*
            This method should read the input stream until the end, and add any entries
            found to a List which is returned. If the stream doesn't contain any useful
            data (is of the wrong format?) you can return null to signal that this is the
            wrong import filter.
         */

        return null;
        //return entries;
    }

    public String getFormatName() {
        // This method should return the name of this import format.
        return "MSBib";
    }

}
