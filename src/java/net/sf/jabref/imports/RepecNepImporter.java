/*
 Copyright (C) 2005 Andreas Rudert

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.imports;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Util;


/**
 * Imports a New Economics Papers-Message from the REPEC-NEP Service.
 * 
 * <p>{@link http://www.repec.org RePEc} (Research Papers in Economics) 
 * is a collaborative effort of over 100 volunteers in 49 countries 
 * to enhance the dissemination of research in economics. The heart of 
 * the project is a decentralized database of working papers, journal 
 * articles and software components. All RePEc material is freely available.</p>
 * At the time of writing RePEc holds over 300.000 items.</p>
 * 
 * <p>{@link http://nep.repec.org NEP} (New Economic Papers) is an announcement 
 * service which filters information on new additions to RePEc into edited 
 * reports. The goal is to provide subscribers with up-to-date information 
 * to the research literature.</p>
 * 
 * <p>This importer is capable of importing NEP messages into JabRef.</p>
 * 
 * <p>There is no officially defined message format for NEP. NEP messages are assumed to have 
 * (and almost always have) the form given by the following semi-formal grammar:
 * <pre>
 * NEPMessage:
 *       MessageSection NEPMessage
 *       MessageSection
 *       
 * MessageSection:            
 *       OverviewMessageSection 
 *       OtherMessageSection
 *
 * # we skip the overview
 * OverviewMessageSection:
 *       'In this issue we have: ' SectionSeparator OtherStuff
 *
 * OtherMessageSection:
 *       SectionSeparator  OtherMessageSectionContent
 *
 * # we skip other stuff and read only full working paper references
 * OtherMessageSectionContent:
 *       WorkingPaper EmptyLine OtherMessageSectionContent 
 *       OtherStuff EmptyLine OtherMessageSectionContent
 *       ''
 *       
 * OtherStuff:
 *       NonEmptyLine OtherStuff
 *       NonEmptyLine
 *       
 * NonEmptyLine:
 *       a non-empty String that does not start with a number followed by a '.'
 *       
 * # working papers are recognized by a number followed by a '.' 
 * # in a non-overview section
 * WorkingPaper:
 *       Number'.' WhiteSpace TitleString EmptyLine Authors EmptyLine Abstract AdditionalFields
 *       Number'.' WhiteSpace TitleString AdditionalFields Abstract AdditionalFields
 *       
 * TitleString:
 *       a String that may span several lines and should be joined
 *       
 * # there must be at least one author
 * Authors:
 *       Author '\n' Authors
 *       Author '\n'
 * 
 * # optionally, an institution is given for an author
 * Author:
 *       AuthorName
 *       AuthorName '(' Institution ')'
 *       
 * # there are no rules about the name, it may be firstname lastname or lastname, firstname or anything else
 * AuthorName:
 *       a non-empty String without '(' or ')' characters, not spanning more that one line
 *       
 * Institution:
 *       a non-empty String that may span several lines
 *       
 * Abstract:
 *       a (possibly empty) String that may span several lines
 *
 * AdditionalFields:
 *       AdditionalField '\n' AdditionalFields
 *       EmptyLine AdditionalFields
 *       ''
 *       
 * AdditionalField:
 *       'Keywords:' KeywordList
 *       'URL:' non-empty String
 *       'Date:' DateString
 *       'JEL:' JelClassificationList
 *       'By': Authors
 *       
 * KeywordList:
 *        Keyword ',' KeywordList
 *        Keyword ';' KeywordList
 *        Keyword
 *        
 * Keyword:
 *        non-empty String that does not contain ',' (may contain whitespace)
 *        
 * # if no date is given, the current year as given by the system clock is assumed
 * DateString:
 *        'yyyy-MM-dd'
 *        'yyyy-MM'
 *        'yyyy'
 *        
 * JelClassificationList:
 *        JelClassification JelClassificationList
 *        JelClassification
 *      
 * # the JEL Classifications are set into a new BIBTEX-field 'jel'
 * # they will appear if you add it as a field to one of the BIBTex Entry sections
 * JelClassification:
 *        one of the allowed classes, see http://ideas.repec.org/j/
 *       
 * SectionSeparator:
 *       '\n-----------------------------'
 * </pre>
 * </p>
 * 
 * @see http://nep.repec.org
 * @author andreas_sf at rudert-home dot de
 */
public class RepecNepImporter extends ImportFormat {

  private final static Collection<String> recognizedFields = Arrays.asList(new String[]{"Keywords", "JEL", "Date", "URL", "By"});
  
  private int line = 0;
  private String lastLine = "";
  private String preLine = "";
  private BufferedReader in = null;
  private boolean inOverviewSection = false;
  
  /**
   * Return the name of this import format.
   */
  public String getFormatName() {
    return "REPEC New Economic Papers (NEP)";
  }

  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#getCLIId()
   */
  public String getCLIId() {
    return "repecnep";
  }
  
  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#getExtensions()
   */  
  public String getExtensions() {
    return ".txt";
  }
  
  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#getDescription()
   */
  public String getDescription() {
    return 
      "Imports a New Economics Papers-Message (see http://nep.repec.org)\n"
    + "from the REPEC-NEP Service (see http://www.repec.org).\n"
    + "To import papers either save a NEP message as a text file and then import or\n"
    + "copy&paste the papers you want to import and make sure, one of the first lines\n"
    + "contains the line \"nep.repec.org\".";
  }
  
  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#isRecognizedFormat(java.io.InputStream)
   */
  public boolean isRecognizedFormat(InputStream stream) throws IOException {
    // read the first couple of lines
    // NEP message usually contain the String 'NEP: New Economics Papers'
    // or, they are from nep.repec.org
    BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
    String startOfMessage = "";
    String line = in.readLine();
    for (int i = 0; i < 25 && line != null; i++) {
      startOfMessage += line;
      line = in.readLine();
    }
    return startOfMessage.indexOf("NEP: New Economics Papers") >= 0 || startOfMessage.indexOf("nep.repec.org") >= 0;
  }

  private boolean startsWithKeyword(Collection<String> keywords) {
    boolean result = this.lastLine.indexOf(':') > 0;
    if (result) {
      String possibleKeyword = this.lastLine.substring(0, this.lastLine.indexOf(':'));
      result = keywords.contains(possibleKeyword);
    }
    return result;
  }
  
  private void readLine() throws IOException {
    this.line++;
    this.preLine = this.lastLine;
    this.lastLine = this.in.readLine();
  }
  
  /**
   * Read multiple lines.
   * 
   * <p>Reads multiple lines until either
   * <ul>
   *   <li>an empty line</li>
   *   <li>the end of file</li>
   *   <li>the next working paper or</li>
   *   <li>a keyword</li>
   * </ul>
   * is found. Whitespace at start or end of lines is trimmed except for one blank character.</p>
   * 
   * @return  result
   */
  private String readMultipleLines() throws IOException {
    String result = this.lastLine.trim();
    readLine();
    while (this.lastLine != null && !this.lastLine.trim().equals("") && !startsWithKeyword(recognizedFields) && !isStartOfWorkingPaper()) {
      result += this.lastLine.length() == 0 ? this.lastLine.trim() : " " + this.lastLine.trim();
      readLine();
    }
    return result;
  }

  /**
   * Implements grammar rule "TitleString".
   * 
   * @param be
   * @throws IOException
   */
  private void parseTitleString(BibtexEntry be) throws IOException {
    // skip article number
    this.lastLine = this.lastLine.substring(this.lastLine.indexOf('.') + 1, this.lastLine.length());
    be.setField("title", readMultipleLines());
  }
  
  /**
   * Implements grammer rule "Authors"
   * 
   * @param be
   * @throws IOException
   */
  private void parseAuthors(BibtexEntry be) throws IOException {
    // read authors and institutions
    String authors = "";
    String institutions = "";
    while (this.lastLine != null && !this.lastLine.equals("") && !startsWithKeyword(recognizedFields)) {
      
      // read single author
      String author = null;
      String institution = null;
      boolean institutionDone = false;
      if (this.lastLine.indexOf('(') >= 0) {
        author = this.lastLine.substring(0, this.lastLine.indexOf('(')).trim();
        institutionDone = this.lastLine.indexOf(')') > 0;
        institution = this.lastLine.substring(this.lastLine.indexOf('(') + 1, institutionDone && this.lastLine.indexOf(')') > this.lastLine.indexOf('(') + 1 ? this.lastLine.indexOf(')') : this.lastLine.length()).trim();
      } else {
        author = this.lastLine.substring(0, this.lastLine.length()).trim();
        institutionDone = true;
      }
      
      readLine();
      while (!institutionDone && this.lastLine!= null) {
        institutionDone = this.lastLine.indexOf(')') > 0;
        institution += this.lastLine.substring(0, institutionDone ? this.lastLine.indexOf(')') : this.lastLine.length()).trim();
        readLine();
      }
      
      if (author != null) {
        authors += !authors.equals("") ? " and " + author : "" + author;
      }
      if (institution != null) {
        institutions += !institutions.equals("") ? " and " + institution : "" + institution;
      }            
    }
    
    if (!authors.equals("")) {
      be.setField("author", authors);
    }
    if (!institutions.equals("")) {
      be.setField("institution", institutions);
    }
  }
  
  /**
   * Implements grammar rule "Abstract".
   * 
   * @param be
   * @throws IOException
   */
  private void parseAbstract(BibtexEntry be) throws IOException {
    String theabstract = readMultipleLines();
    
    if (!theabstract.equals("")) {
      be.setField("abstract", theabstract);
    }
  }
    
  /**
   * Implements grammar rule "AdditionalFields".
   * 
   * @param be
   * @throws IOException
   */
  private void parseAdditionalFields(BibtexEntry be, boolean multilineUrlFieldAllowed) throws IOException {
    
    // one empty line is possible before fields start
    if (this.lastLine != null && this.lastLine.trim().equals("")) {
      readLine();  
    }
    
    // read other fields
    while (this.lastLine != null && !isStartOfWorkingPaper() && (startsWithKeyword(recognizedFields) || this.lastLine.equals(""))) {
      
      // if multiple lines for a field are allowed and field consists of multiple lines, join them
      String keyword = this.lastLine.equals("") ? "" : this.lastLine.substring(0, this.lastLine.indexOf(':')).trim();
      // skip keyword
      this.lastLine = this.lastLine.equals("") ? "" : this.lastLine.substring(this.lastLine.indexOf(':')+1, this.lastLine.length()).trim();
      
      // parse keywords field
      if (keyword.equals("Keywords")) {
        String content = readMultipleLines();
        String[] keywords = content.split("[,;]");
        String keywordStr = "";
        for (int i = 0; i < keywords.length; i++) {
          keywordStr += " '" + keywords[i].trim() + "'";
        }
        be.setField("keywords", keywordStr.trim());
        
      // parse JEL field
      } else if (keyword.equals("JEL")) {
        be.setField("jel", readMultipleLines());
        
      // parse date field
      } else if (keyword.startsWith("Date")) {
        Date date = null;
        String content = readMultipleLines();
        String[] recognizedDateFormats = new String[] {"yyyy-MM-dd","yyyy-MM","yyyy"};
        int i = 0;
        for (; i < recognizedDateFormats.length && date == null; i++) {
          try {            
            date = new SimpleDateFormat(recognizedDateFormats[i]).parse(content);
          } catch (ParseException e) {
            // wrong format
          }
        }
        
        Calendar cal = new GregorianCalendar();              
        cal.setTime(date != null ? date : new Date());
        be.setField("year", "" + cal.get(Calendar.YEAR));
        if (date != null && recognizedDateFormats[i-1].indexOf("MM") >= 0) {
          be.setField("month", "" + cal.get(Calendar.MONTH));
        }
        
      // parse URL field
      } else if (keyword.startsWith("URL")) {
        String content = null;
        if (multilineUrlFieldAllowed) {
          content = readMultipleLines(); 
        } else {
          content = this.lastLine;
          readLine();
        }
        be.setField("url", content);
        
      // authors field
      } else if (keyword.startsWith("By")) {
        // parse authors      
        parseAuthors(be); 
      } else {
        readLine();
      }
    }
  }

  /**
   * if line starts with a string of the form 'x. ' and we are not in the overview
   * section, we have a working paper entry we are interested in
   */
  private boolean isStartOfWorkingPaper() {
    return this.lastLine.matches("\\d+\\.\\s.*") && !this.inOverviewSection && this.preLine.trim().equals("");
  }
  
  /*
   *  (non-Javadoc)
   * @see net.sf.jabref.imports.ImportFormat#importEntries(java.io.InputStream)
   */
  public List<BibtexEntry> importEntries(InputStream stream) throws IOException {    
  	ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
    String paperNoStr = null;
    this.line = 0;
    
    try {
    	this.in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
      
      readLine(); // skip header and editor information
    	while (this.lastLine != null) {
  
        if (this.lastLine.startsWith("-----------------------------")) {
          this.inOverviewSection = this.preLine.startsWith("In this issue we have");
        } 
        if (isStartOfWorkingPaper()) {
          BibtexEntry be = new BibtexEntry(Util.createNeutralId());
          be.setType(BibtexEntryType.getType("techreport"));
          paperNoStr = this.lastLine.substring(0, this.lastLine.indexOf('.'));  
          parseTitleString(be);
          if (startsWithKeyword(recognizedFields)) {
            parseAdditionalFields(be, false);
          } else {
            readLine(); // skip empty line
            parseAuthors(be);
            readLine(); // skip empty line
          }
          if (!startsWithKeyword(recognizedFields)) {
            parseAbstract(be);
          }
          parseAdditionalFields(be, true);
          
          bibitems.add(be);
          paperNoStr = null;
          
        } else {        
          this.preLine = this.lastLine;
          readLine();
        }
      }
      
    } catch (Exception e) {
      String message = "Error in REPEC-NEP import on line " + this.line;
      if (paperNoStr != null) {
        message += ", paper no. " + paperNoStr + ": ";
      }
      message += e.getMessage();
      System.err.println(message);
      if (!(e instanceof IOException)) {
        e.printStackTrace();
        e = new IOException(message);
      }
      throw (IOException)e;
    }

  	return bibitems;	  	
  }
}


