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
public class AuthorLastFirstAbbreviator implements LayoutFormatter {

	/* (non-Javadoc)
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) {
		String[] authors = fieldText.split(" and ");
		
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
			sb.append(authors_abrv[i] + " and ");	
		}
		sb.append(authors_abrv[i]);
		
		String s = new String(sb);
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
		String[] author = string.split(", ");
		
		char c;
		String s;
		//Gets the name:
		StringBuffer sb = new StringBuffer(author[0] + ", ");
		
		int index = author[1].indexOf(" ");
		
		if(index==-1) {
			//Its a simple name like van Something, Someone or  Something, Someone:
			c  = author[1].charAt(0);
//			System.out.println("Char c: " + c + " Name: " + author[1]);
			sb.append(c + ".");
		}
		else {
			//Its a "complex" name like van Something, Someone Something
			String[] nameParts = author[1].split(" ");
			
			int i = 0;
			
			for(i=0;i<nameParts.length;i++)
			{
				c = nameParts[i].charAt(0);
	//			System.out.println("Char c: " + c + " Name: " + nameParts[i]);
				sb.append(c + ".");								
			}
		}
		
		//Replaces each "part of the name" for the corresponding Letter Dot Space format:
		s = new String(sb);
		
		//System.out.println("The Abbreviated name is: " + s);
		return s;
	}

}
