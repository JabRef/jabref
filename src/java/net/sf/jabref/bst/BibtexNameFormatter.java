package net.sf.jabref.bst;

import net.sf.jabref.AuthorList;
import net.sf.jabref.AuthorList.Author;

/**
 * From Bibtex:
 * 
 * "The |built_in| function {\.{format.name\$}} pops the
 * top three literals (they are a string, an integer, and a string
 * literal, in that order). The last string literal represents a
 * name list (each name corresponding to a person), the integer
 * literal specifies which name to pick from this list, and the
 * first string literal specifies how to format this name, as
 * described in the \BibTeX\ documentation. Finally, this function
 * pushes the formatted name. If any of the types is incorrect, it
 * complains and pushes the null string."
 * 
 * Sounds easy - is a nightmare... X-(
 * 
 */
public class BibtexNameFormatter {

	public static String formatName(String authorsNameList, int whichName, String formatString, Warn warn){
		AuthorList al = AuthorList.getAuthorList(authorsNameList);
		
		if (whichName < 1 && whichName > al.size()){
			warn.warn("AuthorList " + authorsNameList + " does not contain an author with number " + whichName);
			return "";
		}
		return formatName(al.getAuthor(whichName-1), formatString, warn);
	}
	
	/**
	 * 
	 * @param author
	 * @param format
	 * @param warn may-be-null
	 * @return
	 */
	public static String formatName(Author author, String format, Warn warn) {
		
		StringBuffer sb = new StringBuffer();
		
		char[] c = format.toCharArray();
		int n = c.length;
		int braceLevel = 0;
		int group = 0;
		
		int i = 0;
		while (i < n){
			if (c[i] == '{'){
				group++;
				int groupStart = sb.length();
				i++;
				braceLevel++;
				StringBuffer level1Chars = new StringBuffer();
				StringBuffer wholeChar = new StringBuffer();
				while (i < n && braceLevel > 0){
					wholeChar.append(c[i]);
					if (c[i] == '{'){
						braceLevel++; i++; continue;
					}
					if (c[i] == '}'){
						braceLevel--; i++; continue;
					}
					if (braceLevel == 1){
						if (Character.isLetter(c[i])){
							if ("fvlj".indexOf(c[i]) == -1){
								if (warn != null)
									warn.warn("Format String in format.name$ may only contain fvlj on brace level 1 in group " + group + ": " + format);
							} else 
								level1Chars.append(c[i]);
						}
					}
					i++;
				}
				i--; // unskip last brace (for last i++ at the end)
				String control = level1Chars.toString().toLowerCase();
				
				if (control.length() == 0)
					continue;
				
				if (control.length() > 2 && warn != null)
					warn.warn("Format String in format.name$ may only be one or two character long on brace level 1 in group " + group + ": " + format);
				
				char type = control.charAt(0);
				
				String tokenS;
				switch(type){
					case 'f': tokenS = author.getFirst(); break;
					case 'v': tokenS = author.getVon(); break;
					case 'l': tokenS = author.getLast(); break;
					case 'j': tokenS = author.getJr(); break;
					default: throw new VMException("Internal error");
				}
										
				if (tokenS == null){
					i++;
					continue;
				}
				String[] tokens = tokenS.split(" ");
								
				boolean abbreviateThatIsSingleLetter = true;
				
				if (control.length() == 2){
					if (control.charAt(1) == control.charAt(0)){
						abbreviateThatIsSingleLetter = false;
					} else {
						if (warn != null)
							warn.warn("Format String in format.name$ may only contain one type of vlfj on brace level 1 in group " + group + ": " + format);
					}
				}
				
				// Now we know what to do
				
				if (braceLevel == 0 && wholeChar.charAt(wholeChar.length() - 1) == '}'){
					wholeChar.deleteCharAt(wholeChar.length() - 1);
				}
				
				char[] d = wholeChar.toString().toCharArray();
				
				int bLevel = 1;
				
				String interToken = null;
				
				for (int j = 0; j < d.length; j++){
					
					if (Character.isLetter(d[j]) && bLevel == 1){
						groupStart = sb.length();
						if (!abbreviateThatIsSingleLetter){
							j++;
						}
						if (j+1 < d.length){
							if (d[j+1] == '{'){
								StringBuffer interTokenSb = new StringBuffer();
								j = consumeToMatchingBrace(interTokenSb, d, j+1);
								interToken = interTokenSb.substring(1,interTokenSb.length()-1);
							}
						}
						
						for (int k = 0; k < tokens.length; k++){
							String token = tokens[k];
							if (abbreviateThatIsSingleLetter){
								String[] dashes = token.split("-");
								
								StringBuffer abbToken = new StringBuffer();
								for (int t = 0; t < dashes.length - 1; t++){
									abbToken.append(getFirstCharOfString(dashes[t])).append(".-");
								}
								if (dashes.length > 0)
									abbToken.append(getFirstCharOfString(dashes[dashes.length - 1]));
								 
								token = abbToken.toString();
							} 
							
							// Output token
							sb.append(token);
							
							if (k < tokens.length - 1){
								// Output Intertoken String
								if (interToken == null){
									if (abbreviateThatIsSingleLetter){
										sb.append(".");
									}
									// No clue what this means (What the hell are tokens anyway???
								    // if (lex_class[name_sep_char[cur_token]] = sep_char) then
								    //    append_ex_buf_char_and_check (name_sep_char[cur_token])
								    if (k == tokens.length - 2 || numberOfChars(sb.substring(groupStart, sb.length()), 3) < 3){
								    	sb.append("~");
								    } else {
								    	sb.append(" ");
								    }
								} else {
									sb.append(interToken);
								}
							}
						}
					} else if (d[j] == '}'){
						bLevel--;
						if (bLevel > 0){
							sb.append('}');
						}
					} else if (d[j] == '{'){
						bLevel++;
						sb.append('{');
					} else {
						sb.append(d[j]);
					}
				}
				if (sb.length() > 0){
					boolean noDisTie = false;
					if (sb.charAt(sb.length() - 1) == '~' && 
						(numberOfChars(sb.substring(groupStart, sb.length()), 4) >= 4 ||
						(sb.length() > 1 && (noDisTie = sb.charAt(sb.length() - 2) == '~')))){
						sb.deleteCharAt(sb.length() - 1);
						if (!noDisTie)
							sb.append(' ');
					}
				}	
			} else if (c[i] == '}'){
				if (warn != null)
					warn.warn("Unmatched brace in format string: " + format);
			} else {
				sb.append(c[i]); // verbatim
			}
			i++;
		}
		if (braceLevel != 0)
			warn.warn("Unbalanced brace in format string for nameFormat: " + format);
		
		return sb.toString();
	}
	
	/**
	 * Including the matching brace.
	 * 
	 * @param sb
	 * @param c
	 * @param pos
	 * @return
	 * 
	 * assert c[pos] == '{'
	 */
	public static int consumeToMatchingBrace(StringBuffer sb, char[] c, int pos){
		
		int braceLevel = 0;

		// assert c[pos] == '{';
		
		for (int i = pos; i < c.length; i++){
			if (c[i] == '}'){
				braceLevel--;
				if (braceLevel == 0){
					sb.append('}');
					return i;
				}
			} else if (c[i] == '{'){
				braceLevel++;
			}
			sb.append(c[i]);
		}
		return c.length;
	}
	
	/**
	 * Takes care of special characters too
	 * 
	 * @param s
	 * @return
	 */
	public static String getFirstCharOfString(String s){
		char[] c = s.toCharArray();
		for (int i = 0; i < c.length; i++){
			if (Character.isLetter(c[i])){
				return String.valueOf(c[i]);
			}
			if (c[i] == '{'){
				if (i+1 < c.length && c[i+1] == '\\'){
					StringBuffer sb = new StringBuffer();
					consumeToMatchingBrace(sb, c, i);
					return sb.toString();
				}
			}
		}
		return "";
	}
	
	public static int numberOfChars(String token, int stop) {
		
		if (stop < 0)
			stop = Integer.MAX_VALUE;
		
		int result = 0;
		int i = 0;
		char[] c = token.toCharArray();
		int n = c.length;
		
		int braceLevel = 0;
		while (i < n && result < stop){
			i++;
			if (c[i-1] == '{'){
				braceLevel++;
				if (braceLevel == 1 && i < n && c[i] == '\\'){
					i++;
					while (i < n && braceLevel > 0){
						if (c[i] == '}'){
							braceLevel--;
						} else if (c[i] == '{'){
							braceLevel++;
						}
						i++;
					}
				}
			} else if (c[i-1] == '}'){
				braceLevel--;
			}
			result++;
		}
		return result;
	}
	

}
