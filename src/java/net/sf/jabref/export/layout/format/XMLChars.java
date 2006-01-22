///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner, Morten O. Alver
//  Version:  $Revision$
//            $Date$
//            $Author$
//
//  Copyright (c) Dept. Computer Architecture, University of Tuebingen, Germany
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
///////////////////////////////////////////////////////////////////////////////

package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import java.util.regex.*;
import java.util.Iterator;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;

/**
 * Changes {\^o} or {\^{o}} to ?
 *
 * @author $author$
 * @version $Revision$
 */
public class XMLChars implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////
    //Pattern pattern = Pattern.compile(".*\\{..[a-zA-Z].\\}.*");
    Pattern pattern = Pattern.compile(".*\\{\\\\.*[a-zA-Z]\\}.*");
  
    public String format(String fieldText)
    {
 
	fieldText = firstFormat(fieldText);

	//if (!pattern.matcher(fieldText).matches())
	//    return restFormat(fieldText);
        
	for (Iterator i=Globals.HTML_CHARS.keySet().iterator(); i.hasNext();) {
	    String s = (String)i.next();         
            String repl = (String)Globals.XML_CHARS.get(s);
            if (repl != null)
                fieldText = fieldText.replaceAll(s, repl);
	}
	//RemoveBrackets rb = new RemoveBrackets();
	return restFormat(fieldText);
    }

    private String firstFormat(String s) {
	return s.replaceAll("&|\\\\&","&#x0026;").replaceAll("--", "&#x2013;");
    }

    private String restFormat(String s) {
		String fieldText=s.replaceAll("\\}","").replaceAll("\\{","");
		
		// now some copy-paste problems most often occuring in abstracts when copied from PDF
		// AND: this is accepted in the abstract of bibtex files, so are forced to catch those cases
		int code;
		char character;
		StringBuffer buffer=new StringBuffer(fieldText.length()<<1);
    for ( int i = 0; i < fieldText.length(); i++)
    {
    	character = fieldText.charAt(i);
      code = ((int) character);
      //System.out.println(""+character+" "+code);
      if((code<40 && code!=32)||code>125){
      	buffer.append("&#" + code+";");
      }
      else 
      {
      	// TODO: highly inefficient, create look-up array with all 255 codes only once and use code as key!!!
      	int[] forceReplace=new int[]{44,45,63,64,94,95,96,124};
      	boolean alphabet=true;
      	for(int ii=0;ii<forceReplace.length;ii++){
      		if(code==forceReplace[ii]){
      			buffer.append("&#" + code+";");
      			alphabet=false;
      			break;
      		}
      	}
    		// force roundtripping
      	if(alphabet)buffer.append((char)code);
      }
    }
    fieldText=buffer.toString();

		// use common abbreviations for <, > instead of code
		for (Iterator i=Globals.ASCII2XML_CHARS.keySet().iterator(); i.hasNext();) {
	    String ss = (String)i.next();         
            String repl = (String)Globals.ASCII2XML_CHARS.get(ss);
            if (repl != null)
                fieldText = fieldText.replaceAll(ss, repl);
	  }
		
		return fieldText;
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
