/*
 * Created on 12/10/2004
 */
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * 
 * Uses as input the fields (author or editor) in the LastFirst format. 
 * 
 * This formater enables to abbreviate the authors name in the following way:
 * 
 * Ex: Someone, Van Something will be abbreviated as Someone, V. S.
 * 
 * @author Carlos Silla
 */
public class AuthorAbbreviator implements LayoutFormatter {

	/* (non-Javadoc)
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) 
	{

		String[] authors = fieldText.split(" and ");

		String abbrev = getAbbreviations(authors);
		return abbrev;

	}
				
	/**
	 * Abbreviates the names in the Last First format.
	 * 
	 * @param authors List of authors or editors.
	 * @return the names abbreviated.
	 * @throws RequiredOrderException
	 * 
	 */
	private String getAbbreviations(String[] authors)
	{
		String s = null;
		
		try
		{			
		
			String[] authors_abrv = new String[authors.length];

			int i = 0;

			for(i=0; i<authors.length; i++)
			{
				authors_abrv[i] = getAbbreviation(authors[i]);
			}

			//Faz o merge em um "unico string" usando " and " 
			StringBuffer sb = new StringBuffer();

			for(i=0; i<authors.length-1; i++)
			{
                sb.append(authors_abrv[i]).append(" and ");
			}
			sb.append(authors_abrv[i]);

			 s = new String(sb);		
		}
		catch(Exception e)
		{
                    e.printStackTrace();
                    //	System.out.println(e);
			//System.exit(-1);
		}
		
		return s;
	}

	/**
	 * 
	 * Abbreviates all but the last name of the author.
	 * 
	 * @param string
	 * @return
	 */
	private String getAbbreviation(String string) {
	    StringBuffer sb = new StringBuffer();
	    String[] author = string.split(", ");                
	    char c;

	    if (author.length < 2) {
		// There is no comma in the name. Abbreviate all but the
		// last word.
		author = string.split(" ");
		if (author.length > 1) {
		    for (int i=0; i<author.length-1; i++) if (author[i].length() > 0) {
			c = author[i].charAt(0);
                sb.append(c).append(".");
		    }
		}
            sb.append(" ").append(author[author.length - 1]);
	    }
	    else {               
		//Gets the name:
            sb.append(author[0]).append(", ");
		int index = author[1].indexOf(" ");
		if(index==-1) {
		    //Its a simple name like van Something, Someone or  Something, Someone:
		    c  = author[1].charAt(0);
		    //			System.out.println("Char c: " + c + " Name: " + author[1]);
            sb.append(c).append(".");
		}
		else {
		    //Its a "complex" name like van Something, Someone Something
                    //System.out.println(author[1]);
		    String[] nameParts = author[1].split(" ");
		    
		    int i = 0;
		    
		    for(i=0;i<nameParts.length;i++)
                if (nameParts[i].length() > 0) {
			        c = nameParts[i].charAt(0);
			//			System.out.println("Char c: " + c + " Name: " + nameParts[i]);
                    sb.append(c).append(".");
		        }
		}
	    }
	    //Replaces each "part of the name" for the corresponding Letter Dot Space format:
	    String s = new String(sb);
	    
	    //System.out.println("The Abbreviated name is: " + s);
	    return s;
	}	
}
