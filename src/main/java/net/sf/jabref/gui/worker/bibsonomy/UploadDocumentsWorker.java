package net.sf.jabref.gui.worker.bibsonomy;

import java.io.File;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

import org.bibsonomy.model.Document;
import org.bibsonomy.model.logic.LogicInterface;

/**
 * Upload documents from the file system directly to the service.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class UploadDocumentsWorker extends AbstractBibSonomyWorker {

	private static final String DEFAULT_FILE_DIRECTORY = System.getProperty("user.dir");

	private static String[] fileLocations = new String[]{
			"",
			JabRefPreferences.getInstance().get("fileDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"),
			JabRefPreferences.getInstance().get("pdfDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"),
			JabRefPreferences.getInstance().get("psDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"),
			DEFAULT_FILE_DIRECTORY + System.getProperty("file.separator")
	};


	private String intrahash;
	private String files;

	public void run() {
		// abort upload if user disabled the option
		if (!BibSonomyProperties.getUploadDocumentsOnExport()) {
			return;
		}

		LogicInterface logic = getLogic();
		// split the filey by ; character
		for (String file : files.split(";")) {
			// get file name
			int firstColonPosition = file.indexOf(":");
			int lastColonPosition = file.lastIndexOf(":");
			String fileName = file.substring(firstColonPosition + 1, lastColonPosition);

			for (String location : fileLocations) {

				// replace all \: in the file name with : - this issues the windows file names
				fileName = fileName.replaceAll("\\\\:", ":");

				File f = new File(location + fileName);
				if (f.exists()) {

					// upload the document
					final Document doc = new Document();
					doc.setFile(f);
					doc.setUserName(BibSonomyProperties.getUsername());
					logic.createDocument(doc, intrahash);

					jabRefFrame.output(Localization.lang("Uploading document %0", fileName));

					break;
				}
			}
		}
		jabRefFrame.output(Localization.lang("Done"));
	}

	public UploadDocumentsWorker(JabRefFrame jabRefFrame, String intrahash, String files) {
		super(jabRefFrame);
		this.intrahash = intrahash;
		this.files = files;
	}
}
