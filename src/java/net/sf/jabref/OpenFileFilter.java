package net.sf.jabref;
import java.io.File;
public class OpenFileFilter extends javax.swing.filechooser.FileFilter{
    public boolean accept(File file){
	String filenm = file.getName();
	return (filenm.endsWith(".bib")
		|| file.isDirectory()
		|| filenm.endsWith(".txt")
		|| filenm.endsWith(".ref") // refer/endnote format
		|| filenm.endsWith(".fcgi") // default for pubmed
		|| filenm.endsWith(".xml"));// windows puts ".txt" extentions and for scifinder
    }
    public String getDescription(){
	return "*.bib, *.txt, *.xml, *.ref or *.fcgi";
    }
}
