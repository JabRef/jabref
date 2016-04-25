package net.sf.jabref.importer;

import java.io.File;
import java.util.Optional;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.model.entry.BibEntry;

/** EntryCreator for any predefined ExternalFileType.
 * This Creator accepts all files with the extension defined in the ExternalFileType.
 */
public class EntryFromExternalFileCreator extends EntryFromFileCreator {

    public EntryFromExternalFileCreator(ExternalFileType externalFileType) {
        super(externalFileType);
    }

    @Override
    public boolean accept(File f) {
        return f.getName().endsWith("." + externalFileType.getExtension());
    }

    @Override
    protected Optional<BibEntry> createBibtexEntry(File file) {
        if (!accept(file)) {
            return Optional.empty();
        }

        return Optional.of(new BibEntry());
    }

    @Override
    public String getFormatName() {
        return externalFileType.getName();
    }
}
