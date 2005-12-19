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

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;
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
 *       Number'.' WhiteSpace TitleString EmptyLine Authors EmptyLine Abstract EmptyLine AdditionalFields
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
 *       ''
 *       
 * AdditionalField:
 *       'Keywords:' KeywordList
 *       'URL:' non-empty String
 *       'Date:' DateString
 *       'JEL:' JelClassificationList
 *       
 * KeywordList:
 *        Keyword ',' KeywordList
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

  private final static Collection recognizedFields = Arrays.asList(new String[]{"Keywords", "JEL", "Date", "URL"});
  
  private int line = 0;
  
  /**
   * Return the name of this import format.
   */
  public String getFormatName() {
    return "REPEC New Economic Papers (NEP)";
  }

  /**
   * Check whether the source is in the correct format for this importer.
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

  private boolean startsWithKeyword(String s, Collection keywords) {
    boolean result = s.indexOf(':') > 0;
    if (result) {
      String possibleKeyword = s.substring(0, s.indexOf(':'));
      result = keywords.contains(possibleKeyword);
    }
    return result;
  }
  
  private String readLine(BufferedReader in) throws IOException {
    this.line++;
    return in.readLine();
  }
  
  /**
   * Parse the entries in the source, and return a List of BibtexEntry
   * objects.
   */
  public List importEntries(InputStream stream) throws IOException {    
  	ArrayList bibitems = new ArrayList();
    String paperNoStr = null;
    this.line = 0;
    
    try {
    	String s = "";
    	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
      
      String prevLine = "";
      boolean inOverviewSection = false;
      // skip header and editor information
      s = readLine(in);
    	while (s != null) {
  
        if (s.startsWith("-----------------------------")) {
          inOverviewSection = prevLine.startsWith("In this issue we have");
        } 
        
        // if line starts with a string of the form 'x. ' and we are not in the overview
        // section, we have a working paper entry we are interested in
        if (s.matches("\\d+\\.\\s.*") && !inOverviewSection) {
          paperNoStr = s.substring(0, s.indexOf('.'));
  
          BibtexEntry be = new BibtexEntry(Util.createNeutralId());
          be.setType(BibtexEntryType.getType("techreport"));
          
          // read title
          String title = s.substring(s.indexOf('.') + 1, s.length()).trim();
          s = readLine(in).trim();
          while (s != null && !s.equals("")) {
            title += " " + s;
            s = readLine(in).trim();
          }
          be.setField("title", title);
          
          // skip empty line
          s = readLine(in);
          
          // read authors and institutions
          String authors = "";
          String institutions = "";
          while (s != null && !s.equals("")) {
            
            // read single author
            String author = null;
            String institution = null;
            boolean institutionDone = false;
            if (s.indexOf('(') >= 0) {
              author = s.substring(0, s.indexOf('(')).trim();
              institutionDone = s.indexOf(')') > 0;
              institution = s.substring(s.indexOf('(') + 1, institutionDone? s.indexOf(')') : s.length()).trim();
            } else {
              author = s.substring(0, s.length()).trim();
              institutionDone = true;
            }
            s = readLine(in).trim();
            while (!institutionDone && s!= null) {
              institutionDone = s.indexOf(')') > 0;
              institution = s.substring(s.indexOf('(') + 1, institutionDone ? s.indexOf(')') : s.length()).trim();
              s = readLine(in).trim();
            }
            
            if (author != null) {
              authors += !authors.equals("") ? " and " + author : "" + author;
            }
            if (institution != null) {
              institutions += !institutions.equals("") ? "and " + institution : "" + institution;
            }            
          }
          
          if (!authors.equals("")) {
            be.setField("author", authors);
          }
          if (!institutions.equals("")) {
            be.setField("institution", institutions);
          }
  
          // skip empty line
          s = readLine(in);
          
          // read abstract
          String theabstract = "";
          while (s != null && !s.equals("") && !startsWithKeyword(s,recognizedFields)) {
            theabstract += " " + s.trim();
            s = readLine(in);
          }
          theabstract = theabstract.trim();
          
          if (!theabstract.equals("")) {
            be.setField("abstract", theabstract);
          }
          
          // read other fields
          while (startsWithKeyword(s, recognizedFields)) {
            
            // if field consists of multiple lines, join them
            String field = s;
            s = readLine(in);
            while (s != null && !s.equals("") && !startsWithKeyword(s,recognizedFields)) {
              field += " " + s.trim();
              s = readLine(in);
            }
            String content = field.substring(field.indexOf(':')+1, field.length()).trim();
            
            // now field keyword-content
            if (field.startsWith("Keywords:")) {
              String[] keywords = content.split(",");
              String keywordStr = "";
              for (int i = 0; i < keywords.length; i++) {
                keywordStr += " '" + keywords[i].trim() + "'";
              }
              be.setField("keywords", keywordStr.trim());
              
            } else if (field.startsWith("JEL:")) {
              be.setField("jel", content);
              
            } else if (field.startsWith("Date:")) {
              Date date = null;
              try {
                date = new SimpleDateFormat("yyyy-MM-dd").parse(content);
              } catch (ParseException e) {
                // wrong format
              }
              
              if (date == null) {
                try {
                  date = new SimpleDateFormat("yyyy-MM").parse(content);
                } catch (ParseException e) {
                  // wrong format
                }                  
              }
              
              Calendar cal = new GregorianCalendar();              
              if (date != null) {
                cal.setTime(date);
              } else {
                cal.setTime(new Date());
              }
              int year = cal.get(Calendar.YEAR);
              int month = cal.get(Calendar.MONTH);
              
              be.setField("year", ""+year);
              be.setField("month", ""+month);
              
            } else if (field.startsWith("URL:")) {
              be.setField("url", content);
            }
          }
          
          bibitems.add(be);
          paperNoStr = null;
        }
        
        prevLine = s;
        s = readLine(in);
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


