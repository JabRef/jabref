package net.sf.jabref.imports;

import java.io.File;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.external.ExternalFileType;

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
	protected BibtexEntry createBibtexEntry(File file) {
		if (!accept(file)) {
			return null;
		}
		BibtexEntry entry = new BibtexEntry();

		return entry;
	}

	@Override
	public String getFormatName() {
		return externalFileType.getName();
	}
}
