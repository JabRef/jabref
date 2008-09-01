///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner
//  Version:  $Revision: 2268 $
//            $Date: 2007-08-20 01:37:05 +0200 (Mon, 20 Aug 2007) $
//            $Author: coezbek $
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


/**
 * Create DocBook authors formatter.
 *
 * @author $author$
 * @version $Revision: 2268 $
 */
public class CreateBibORDFAuthors implements LayoutFormatter
{
    //~ Methods ////////////////////////////////////////////////////////////////

    public String format(String fieldText) {
    	// Yeah, the format is quite verbose... sorry about that :)
    	
//      <bibo:contribution>
//        <bibo:Contribution>
//          <bibo:role rdf:resource="http://purl.org/ontology/bibo/roles/author" />
//          <bibo:contributor><foaf:Person foaf:name="Ola Spjuth"/></bibo:contributor>
//          <bibo:position>1</bibo:position>
//        </bibo:Contribution>
//      </bibo:contribution>

        StringBuffer sb = new StringBuffer(100);

        if (fieldText.indexOf(" and ") == -1)
        {
          singleAuthor(sb, fieldText, 1);
        }
        else
        {
            String[] names = fieldText.split(" and ");
            for (int i=0; i<names.length; i++)
            {
              singleAuthor(sb, names[i], (i+1));
              if (i < names.length -1)
                sb.append("\n");
            }
        }



        fieldText = sb.toString();

        return fieldText;
    }

    /**
     * @param sb
     * @param fieldText
     */
    protected void singleAuthor(StringBuffer sb, String author, int position) {
        sb.append("<bibo:contribution>\n");
        sb.append("  <bibo:Contribution>\n");
        sb.append("    <bibo:role rdf:resource=\"http://purl.org/ontology/bibo/roles/author\" />\n");
        sb.append("    <bibo:contributor><foaf:Person foaf:name=\"" + author + "\"/></bibo:contributor>\n");
        sb.append("    <bibo:position>" + position + "</bibo:position>\n");
        sb.append("  </bibo:Contribution>\n");
        sb.append("</bibo:contribution>\n");
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
