/*
Copyright (C) 2003 David Weitzman, Morten O. Alver

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

Note:
Modified for use in JabRef.

*/
package net.sf.jabref;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
public abstract class BibtexEntryType
{
    public static final BibtexEntryType OTHER =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Other";
            }

            public String[] getOptionalFields()
            {
                return new String[0];
            }

            public String[] getRequiredFields()
            {
                return new String[0];
            }


            public String describeRequiredFields()
            {
                return "";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return true;
            }
        };


    public static final BibtexEntryType ARTICLE =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Article";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "volume", "number", "pages", "month", "eid", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "journal", "year"
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, JOURNAL and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "author", "title", "journal", "year", "bibtexkey"
                    });
            }
        };

    public static final BibtexEntryType BOOKLET =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Booklet";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "author", "howpublished", "address", "month", "year", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "title"
                };
            }

            public String describeRequiredFields()
            {
                return "TITLE";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "title", "bibtexkey"
                    });
            }
        };


   public static final BibtexEntryType INBOOK =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Inbook";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "volume", "number", "series", "type", "address", "edition", 
		    "month", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "chapter", "pages", "title", "publisher", "year", "editor", 
		    "author"
                };
            }

            public String describeRequiredFields()
            {
                return "TITLE, CHAPTER and/or PAGES, PUBLISHER, YEAR, and an "
		    +"EDITOR and/or AUTHOR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "title", "publisher", "year", "bibtexkey"
                    }) &&
		    (((entry.getField("author") != null) ||
		      (entry.getField("editor") != null)) &&
		     ((entry.getField("chapter") != null) ||
		      (entry.getField("pages") != null)));
            }
        };

    public static final BibtexEntryType BOOK =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Book";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "volume", "number", "series", "address", "edition", "month",
                    "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "title", "publisher", "year", "editor", "author"
                };
            }

            public String describeRequiredFields()
            {
                return "TITLE, PUBLISHER, YEAR, and an EDITOR and/or AUTHOR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "title", "publisher", "year", "bibtexkey"
                    }) &&
                ((entry.getField("author") != null) ||
                (entry.getField("editor") != null));
            }
        };


    public static final BibtexEntryType INCOLLECTION =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Incollection";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "editor", "volume", "number", "series", "type", "chapter",
		    "pages", "address", "edition", "month", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "booktitle", "publisher", "year" 
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, BOOKTITLE, PUBLISHER and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
			"author", "title", "booktitle", "publisher", "year", 
			"bibtexkey"

                    });
            }
        };

    public static final BibtexEntryType INPROCEEDINGS =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Inproceedings";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "editor", "volume", "number", "series", "pages", 
		    "address", "month", "organization", "publisher", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "booktitle", "year" 
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, BOOKTITLE and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
			"author", "title", "booktitle", "year" , "bibtexkey"
                    });
            }
        };

    public static final BibtexEntryType PROCEEDINGS =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Proceedings";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "editor", "volume", "number", "series", "address", 
		    "publisher", "note", "month", "organization"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "title", "year" 
                };
            }

            public String describeRequiredFields()
            {
                return "TITLE and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
			"title", "year", "bibtexkey"
                    });
            }
        };


    public static final BibtexEntryType MANUAL =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Manual";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "author", "organization", "address", "edition",
		    "month", "year", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "title"
                };
            }

            public String describeRequiredFields()
            {
                return "TITLE";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "title", "bibtexkey"
                    });
            }
        };

    public static final BibtexEntryType TECHREPORT =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Techreport";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "type", "number", "address", "month", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "institution", "year"
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, INSTITUTION and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
			"author", "title", "institution", "year",
			"bibtexkey"
                    });
            }
        };


    public static final BibtexEntryType MASTERSTHESIS =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Mastersthesis";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "type", "address", "month", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "school", "year" 
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, SCHOOL and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "author", "title", "school", "year", "bibtexkey"
                    });
            }
        };


    public static final BibtexEntryType PHDTHESIS =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Phdthesis";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "type", "address", "month", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "school", "year" 
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE, SCHOOL and YEAR";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
                        "author", "title", "school", "year", "bibtexkey"
                    });
            }
        };

    public static final BibtexEntryType UNPUBLISHED =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Unpublished";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "month", "year"
                };
            }

            public String[] getRequiredFields()
            {
                return new String[]
                {
                    "author", "title", "note"
                };
            }

            public String describeRequiredFields()
            {
                return "AUTHOR, TITLE and NOTE";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
                return entry.allFieldsPresent(new String[]
                    {
			"author", "title", "note", "bibtexkey"
                    });
            }
        };


    public static final BibtexEntryType MISC =
        new BibtexEntryType()
        {
            public String getName()
            {
                return "Misc";
            }

            public String[] getOptionalFields()
            {
                return new String[]
                {
                    "author", "title", "howpublished", "month", "year", "note"
                };
            }

            public String[] getRequiredFields()
            {
                return null;
            }

            public String describeRequiredFields()
            {
                return "None";
            }

            public boolean hasAllRequiredFields(BibtexEntry entry)
            {
		return entry.allFieldsPresent(new String[]
                    {
			"bibtexkey"
                    });
            }
        };


    public static final Set ALL_TYPES =
        Collections.unmodifiableSet(new HashSet(Arrays.asList(
                    new BibtexEntryType[] { ARTICLE, INBOOK, BOOK, BOOKLET,
					    INCOLLECTION, INPROCEEDINGS, PROCEEDINGS,
					    MANUAL, MASTERSTHESIS, PHDTHESIS, 
					    TECHREPORT, UNPUBLISHED, MISC })));

    public abstract String getName();

    public abstract String[] getOptionalFields();

    public abstract String[] getRequiredFields();

    public String[] getGeneralFields() {
        return new String[] 
	    {"crossref", "keywords", "doi", "url", 
	     "pdf", "abstract", "comment"};
    }

    public abstract String describeRequiredFields();

    public abstract boolean hasAllRequiredFields(BibtexEntry entry);


    public String[] getUtilityFields(){
        return new String[] {"search" } ; 
    }

    
    public boolean isRequired(String field) {
	String[] req = getRequiredFields();
	if (req == null) return false;
	for (int i=0; i<req.length; i++)
	    if (req[i].equals(field)) return true;
	return false;
    }

    public boolean isOptional(String field) {
	String[] opt = getOptionalFields();
	if (opt == null) return false;
	for (int i=0; i<opt.length; i++)
	    if (opt[i].equals(field)) return true;
	return false;
    }

}
