package net.sf.jabref;
import java.io.File;
import java.io.*;
public class OpenFileFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter {

    String filetype;
    boolean specific = false;

    public OpenFileFilter() {}

    /**
     * Creates a new <code>OpenFileFilter</code> instance which only accepts
     * one specific extension.
     *
     * @param s The extension of the file, e.g. ".bib" or ".html".
     */
    public OpenFileFilter(String s) {
	filetype = s;
	specific = true;
    }



    public boolean accept(File file){
	String filenm = file.getName();
	if (!specific)
	    return (filenm.endsWith(".bib")
		    || file.isDirectory()
		    || filenm.endsWith(".txt")
		    || filenm.endsWith(".ref") // refer/endnote format
		    || filenm.endsWith(".fcgi") // default for pubmed
		    || filenm.endsWith(".bibx") // default for BibTeXML
		    || filenm.endsWith(".xml"));// windows puts ".txt" extentions and for scifinder
	else
	    return (filenm.endsWith(filetype)
		    || file.isDirectory());
    }
    public String getDescription(){
	if (!specific)
	    return "*.bib, *.bibx, *.txt, *.xml, *.ref or *.fcgi";
	else
	    return "*"+filetype;
    }

  public boolean accept(File dir, String name) {
    return accept(new File(dir.getPath()+name));
  }
}
