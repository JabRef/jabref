/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.export.layout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;

import net.sf.jabref.*;
import net.sf.jabref.export.layout.format.plugin.NameFormat;
import net.sf.jabref.export.layout.format.NotFoundFormatter;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.LayoutFormatterExtension;

/**
 * DOCUMENT ME!
 * 
 * @author $author$
 * @version $Revision$
 */
public class LayoutEntry {
	// ~ Instance fields
	// ////////////////////////////////////////////////////////

	private LayoutFormatter[] option;

    // Formatter to be run after other formatters:
    private LayoutFormatter postFormatter = null;

	private String text;

	private LayoutEntry[] layoutEntries;

	private int type;

	private String classPrefix;

    private ArrayList<String> invalidFormatter = null;
    
	// ~ Constructors
	// ///////////////////////////////////////////////////////////

	public LayoutEntry(StringInt si, String classPrefix_) throws Exception {
		type = si.i;
		classPrefix = classPrefix_;

		if (si.i == LayoutHelper.IS_LAYOUT_TEXT) {
			text = si.s;
		} else if (si.i == LayoutHelper.IS_SIMPLE_FIELD) {
			text = si.s.trim();
		} else if (si.i == LayoutHelper.IS_FIELD_START) {
		} else if (si.i == LayoutHelper.IS_FIELD_END) {
		} else if (si.i == LayoutHelper.IS_OPTION_FIELD) {
			Vector<String> v = new Vector<String>();
			WSITools.tokenize(v, si.s, "\n");

			if (v.size() == 1) {
				text = v.get(0);
			} else {
				text = v.get(0).trim();

				option = getOptionalLayout(v.get(1), classPrefix);
                // See if there was an undefined formatter:
                for (int i = 0; i < option.length; i++) {
                    if (option[i] instanceof NotFoundFormatter) {
                        String notFound = ((NotFoundFormatter)option[i]).getNotFound();
                        
                        if (invalidFormatter == null)
                            invalidFormatter = new ArrayList<String>();
                        invalidFormatter.add(notFound);
                    }
                }

			}
		}
	}
	
	
	public LayoutEntry(Vector<StringInt> parsedEntries, String classPrefix_, int layoutType) throws Exception {
		classPrefix = classPrefix_;
		String blockStart = null;
		String blockEnd = null;
		StringInt si;
		Vector<StringInt> blockEntries = null;
		Vector<LayoutEntry> tmpEntries = new Vector<LayoutEntry>();
		LayoutEntry le;
		si = parsedEntries.get(0);
		blockStart = si.s;
		si = parsedEntries.get(parsedEntries.size() - 1);
		blockEnd = si.s;

		if (!blockStart.equals(blockEnd)) {
			System.err.println("Field start and end entry must be equal.");
		}

		type = layoutType;
		text = si.s;

		for (int i = 1; i < (parsedEntries.size() - 1); i++) {
			si = parsedEntries.get(i);

			// System.out.println("PARSED-ENTRY: "+si.s+"="+si.i);
			if (si.i == LayoutHelper.IS_LAYOUT_TEXT) {
			} else if (si.i == LayoutHelper.IS_SIMPLE_FIELD) {
			} else if ((si.i == LayoutHelper.IS_FIELD_START)
				|| (si.i == LayoutHelper.IS_GROUP_START)) {
				blockEntries = new Vector<StringInt>();
				blockStart = si.s;
			} else if ((si.i == LayoutHelper.IS_FIELD_END) || (si.i == LayoutHelper.IS_GROUP_END)) {
				if (blockStart.equals(si.s)) {
					blockEntries.add(si);
					if (si.i == LayoutHelper.IS_GROUP_END)
						le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_GROUP_START);
					else
						le = new LayoutEntry(blockEntries, classPrefix, LayoutHelper.IS_FIELD_START);
					tmpEntries.add(le);
					blockEntries = null;
				} else {
					System.out.println("Nested field entries are not implemented !!!");
				}
			} else if (si.i == LayoutHelper.IS_OPTION_FIELD) {
			}

			// else if (si.i == LayoutHelper.IS_OPTION_FIELD_PARAM)
			// {
			// }
			if (blockEntries == null) {
				// System.out.println("BLOCK ADD: "+si.s+"="+si.i);
				tmpEntries.add(new LayoutEntry(si, classPrefix));
			} else {
				blockEntries.add(si);
			}
		}

		layoutEntries = new LayoutEntry[tmpEntries.size()];

		for (int i = 0; i < tmpEntries.size(); i++) {
			layoutEntries[i] = tmpEntries.get(i);

            // Note if one of the entries has an invalid formatter:
            if (layoutEntries[i].isInvalidFormatter()) {
                if (invalidFormatter == null)
                    invalidFormatter = new ArrayList<String>(1);
                invalidFormatter.addAll(layoutEntries[i].getInvalidFormatters());
            }

        }
		
	}

    public void setPostFormatter(LayoutFormatter formatter) {
        this.postFormatter = formatter;
    }
    
    public String doLayout(BibtexEntry bibtex, BibtexDatabase database) {
    	return doLayout(bibtex, database, null); 
    }
    
	public String doLayout(BibtexEntry bibtex, BibtexDatabase database, ArrayList<String> wordsToHighlight ) {
		switch (type) {
		case LayoutHelper.IS_LAYOUT_TEXT:
			return text;
		case LayoutHelper.IS_SIMPLE_FIELD:
			String value = BibtexDatabase.getResolvedField(text, bibtex, database);
			
            if (value == null)
                value = "";
            // If a post formatter has been set, call it:
            if (postFormatter != null)
                value = postFormatter.format(value);
            return value;
		case LayoutHelper.IS_FIELD_START:
		case LayoutHelper.IS_GROUP_START: {
            String field;
            if (type == LayoutHelper.IS_GROUP_START) {
                field = BibtexDatabase.getResolvedField(text, bibtex, database);
            } else if(text.matches(".*(;|(\\&+)).*")) {
		// split the strings along &, && or ; for AND formatter
                String[] parts = text.split("\\s*(;|(\\&+))\\s*");
                field = null;
                for (int i = 0; i < parts.length; i++) {
                    field = BibtexDatabase.getResolvedField(parts[i], bibtex, database);
                    if (field == null)
                        break;
                    
                }
            } else { 
		// split the strings along |, ||  for OR formatter
                String[] parts = text.split("\\s*(\\|+)\\s*");
                field = null;
                for (int i = 0; i < parts.length; i++) {
                    field = BibtexDatabase.getResolvedField(parts[i], bibtex, database);
                    if (field != null)
                        break;
                }
	    }
            
			if ((field == null)
				|| ((type == LayoutHelper.IS_GROUP_START) && (field.equalsIgnoreCase(LayoutHelper
					.getCurrentGroup())))) {
				return null;
			} else {
				if (type == LayoutHelper.IS_GROUP_START) {
					LayoutHelper.setCurrentGroup(field);
				}
				StringBuffer sb = new StringBuffer(100);
				String fieldText;
				boolean previousSkipped = false;

				for (int i = 0; i < layoutEntries.length; i++) {
					fieldText = layoutEntries[i].doLayout(bibtex, database);

					if (fieldText == null) {
						if ((i + 1) < layoutEntries.length) {
							if (layoutEntries[i + 1].doLayout(bibtex, database).trim().length() == 0) {
								i++;
								previousSkipped = true;
								continue;
							}
						}
					} else {
						
						// if previous was skipped --> remove leading line
						// breaks
						if (previousSkipped) {
							int eol = 0;

							while ((eol < fieldText.length())
								&& ((fieldText.charAt(eol) == '\n') || (fieldText.charAt(eol) == '\r'))) {
								eol++;
							}

							if (eol < fieldText.length()) {
								sb.append(fieldText.substring(eol));
							}
						} else {
							//System.out.println("ENTRY-BLOCK: " +
							//layoutEntries[i].doLayout(bibtex));
							
							/*
							 * if fieldText is not null and the bibtexentry is marked
							 * as a searchhit, try to highlight the searched words
							 * 
							*/
			                if (bibtex.isSearchHit()) {
			                    sb.append(highlightWords(fieldText,wordsToHighlight));
			                }
			                else{
			                	sb.append(fieldText);
			                	
			                }
			               

						}
					}

					previousSkipped = false;
				}
				
				return sb.toString();
			}
		}
		case LayoutHelper.IS_FIELD_END:
		case LayoutHelper.IS_GROUP_END:
			return "";
		case LayoutHelper.IS_OPTION_FIELD: {
			String fieldEntry;

			if (text.equals("bibtextype")) {
				fieldEntry = bibtex.getType().getName();
			} else {
				// changed section begin - arudert
				// resolve field (recognized by leading backslash) or text
				String field = text.startsWith("\\") ? BibtexDatabase.getResolvedField(text.substring(1), bibtex, database)
					: BibtexDatabase.getText(text, database);
				// changed section end - arudert
				if (field == null) {
					fieldEntry = "";
				} else {
					fieldEntry = field;
				}
			}

			//System.out.println("OPTION: "+option);
			if (option != null) {
				for (int i = 0; i < option.length; i++) {
					fieldEntry = option[i].format(fieldEntry);
				}
			}

            // If a post formatter has been set, call it:
            if (postFormatter != null)
                fieldEntry = postFormatter.format(fieldEntry);

			return fieldEntry;
		}
        case LayoutHelper.IS_ENCODING_NAME: {
            // Printing the encoding name is not supported in entry layouts, only
            // in begin/end layouts. This prevents breakage if some users depend
            // on a field called "encoding". We simply return this field instead:
            return BibtexDatabase.getResolvedField("encoding", bibtex, database);
        }
        default:
			return "";
		}
	}

	// added section - begin (arudert)
	/**
	 * Do layout for general formatters (no bibtex-entry fields).
	 * 
	 * @param database
	 *            Bibtex Database
	 * @return
	 */
	public String doLayout(BibtexDatabase database, String encoding) {
		if (type == LayoutHelper.IS_LAYOUT_TEXT) {
			return text;
		} else if (type == LayoutHelper.IS_SIMPLE_FIELD) {
			throw new UnsupportedOperationException(
				"bibtex entry fields not allowed in begin or end layout");
		} else if ((type == LayoutHelper.IS_FIELD_START) || (type == LayoutHelper.IS_GROUP_START)) {
			throw new UnsupportedOperationException(
				"field and group starts not allowed in begin or end layout");
		} else if ((type == LayoutHelper.IS_FIELD_END) || (type == LayoutHelper.IS_GROUP_END)) {
			throw new UnsupportedOperationException(
				"field and group ends not allowed in begin or end layout");
		} else if (type == LayoutHelper.IS_OPTION_FIELD) {
			String field = BibtexDatabase.getText(text, database);
			if (option != null) {
				for (int i = 0; i < option.length; i++) {
					field = option[i].format(field);
				}
			}
            // If a post formatter has been set, call it:
            if (postFormatter != null)
                field = postFormatter.format(field);

			return field;
		} else if (type == LayoutHelper.IS_ENCODING_NAME) {
            // Try to translate from Java encoding name to common name:
            String commonName = Globals.ENCODING_NAMES_LOOKUP.get(encoding);
            return commonName != null ? commonName : encoding;
        }
        else if (type == LayoutHelper.IS_FILENAME) {
            File f = Globals.prefs.databaseFile;
            return f != null ? f.getName() : "";
        }
        else if (type == LayoutHelper.IS_FILEPATH) {
            File f = Globals.prefs.databaseFile;
            return f != null ? f.getPath() : "";
        }
		return "";
	}

	// added section - end (arudert)

	static Map<String, LayoutFormatter> pluginLayoutFormatter;
	
	public static LayoutFormatter getLayoutFormatterFromPlugins(String formatterName){
		if (pluginLayoutFormatter == null){
			pluginLayoutFormatter = new HashMap<String, LayoutFormatter>();
            JabRefPlugin plugin = JabRefPlugin.getInstance(PluginCore.getManager());
			if (plugin != null){
				for (LayoutFormatterExtension e : plugin.getLayoutFormatterExtensions()){
					LayoutFormatter formatter = e.getLayoutFormatter();
					String name = e.getName();
					if (name == null)
						name = e.getId();
					
					if (formatter != null){
						pluginLayoutFormatter.put(name, formatter);
					}
				}
			}
		}
        // We need to make a new instance of this LayoutFormatter, in case it is a
        // parameter-accepting layout formatter:
        if (pluginLayoutFormatter.containsKey(formatterName)) {
            Class<? extends LayoutFormatter> c = pluginLayoutFormatter.get(formatterName).getClass();
            try {
                return c.getConstructor().newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return pluginLayoutFormatter.get(formatterName);
        }
        else return null;
	}
	
	public static LayoutFormatter getLayoutFormatterByClassName(String className, String classPrefix)
		throws Exception {

		if (className.length() > 0) {
			try {
				try {
					return (LayoutFormatter) Class.forName(classPrefix + className).newInstance();
				} catch (Throwable ex2) {
					return (LayoutFormatter) Class.forName(className).newInstance();
				}
			} catch (ClassNotFoundException ex) {
				throw new Exception(Globals.lang("Formatter not found") + ": " + className);
			} catch (InstantiationException ex) {
				throw new Exception(className + " can not be instantiated.");
			} catch (IllegalAccessException ex) {
				throw new Exception(className + " can't be accessed.");
			}
		}
		return null;
	}

	/**
	 * Return an array of LayoutFormatters found in the given formatterName
	 * string (in order of appearance).
	 * 
	 */
	public static LayoutFormatter[] getOptionalLayout(String formatterName,
			String classPrefix) throws Exception {

		ArrayList<String[]> formatterStrings = Util
				.parseMethodsCalls(formatterName);

		ArrayList<LayoutFormatter> results = new ArrayList<LayoutFormatter>(
				formatterStrings.size()); 

		Map<String, String> userNameFormatter = NameFormatterTab.getNameFormatters();

		for (String[] strings : formatterStrings) {

            String className = strings[0].trim();
                        
            // Check if this is a name formatter defined by this export filter:
            if (Globals.prefs.customExportNameFormatters != null) {
                String contents = Globals.prefs.customExportNameFormatters.get(className);
                if (contents != null) {
                    NameFormat nf = new NameFormat();
                    nf.setParameter(contents);
                    results.add(nf);
                    continue;
                }
            }

            // Try to load from formatters in formatter folder
			try {
				LayoutFormatter f = getLayoutFormatterByClassName(className,
						classPrefix);
                // If this formatter accepts an argument, check if we have one, and
                // set it if so:
                if (f instanceof ParamLayoutFormatter) {
                    if (strings.length >= 2)
                        ((ParamLayoutFormatter)f).setArgument(strings[1]);
                }
                results.add(f);
				continue;
			} catch (Exception e) {
			}

			// Then check whether this is a user defined formatter
			String formatterParameter = userNameFormatter
					.get(className);

			if (formatterParameter != null) {
				NameFormat nf = new NameFormat();
				nf.setParameter(formatterParameter);
				results.add(nf);
				continue;
			}

			// Last load from plug-ins
			LayoutFormatter f = getLayoutFormatterFromPlugins(className);
			if (f != null) {
                // If this formatter accepts an argument, check if we have one, and
                // set it if so:
                if (f instanceof ParamLayoutFormatter) {
                    if (strings.length >= 2) {
                        ((ParamLayoutFormatter)f).setArgument(strings[1]);
                    }
                }
				results.add(f);
				continue;
			}

			// If not found throw exception...
            //return new LayoutFormatter[] {new NotFoundFormatter(className)};
            results.add(new NotFoundFormatter(className));
			//throw new Exception(Globals.lang("Formatter not found") + ": "+ className);
		}

		return results.toArray(new LayoutFormatter[] {});
	}


    public boolean isInvalidFormatter() {
        return invalidFormatter != null;
    }

    public ArrayList<String> getInvalidFormatters() {
        return invalidFormatter;
    }
    
    /** 
     * Will return the text that was called by the method with HTML tags 
     * to highlight each word the user has searched for and will skip
     * the highlight process if the first Char isn't a letter or a digit.
     * 
     * This check is a quick hack to avoid highlighting of HTML tags
     * It does not always work, but it does its job mostly  
     * 
     * @param text This is a String in which we search for different words
     * @param toHighlight List of all words which must be highlighted
     * 
     * @return String that was called by the method, with HTML Tags if a word was found 
     */
    private String highlightWords(String text, ArrayList<String> toHighlight){
    	if (toHighlight == null)
    		return text;
    	
		Matcher matcher = Globals.getPatternForWords(toHighlight).matcher(text);

    	if (Character.isLetterOrDigit(text.charAt(0))) {
			String hlColor=Globals.highlightColor;
			StringBuffer sb = new StringBuffer();
			boolean foundSomething = false;

			String found;
			while (matcher.find())
			{
				matcher.end();
				found = matcher.group();
				// color the search keyword	-
				// put first String Part and then html + word + html to a StringBuffer
				matcher.appendReplacement(sb, "<span style=\"background-color:"+hlColor+";\">" + found + "</span>");
				foundSomething = true;
			}
			
			if (foundSomething)
			{
				matcher.appendTail(sb);
				text = sb.toString();
			}

    	}
    	return text;
    }
}
