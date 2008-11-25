/*
 Copyright (C) 2006 Raik Nagel <kiar@users.sourceforge.net>
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 * Neither the name of the author nor the names of its contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

// created by : r.nagel 19.04.2006
//
// function : Handling of bibtex fields.
//            All bibtex-field related stuff should be placed here!
//            Because we can export these informations into additional
//            config files -> simple extension and definition of new fields....
//
// todo     : - handling of identically fields with different names
//              e.g. LCCN = lib-congress
//            - group id for each fields, e.g. standard, jurabib, bio....
//            - add a additional properties functionality into the
//              BibtexSingleField class
//
// modified : r.nagel 25.04.2006
//            export/import of some definition from/to a xml file

package net.sf.jabref ;

import java.util.HashMap;
import java.util.Vector;
import java.util.HashSet;

import net.sf.jabref.util.TXMLReader;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class BibtexFields
{
  public static final String KEY_FIELD = "bibtexkey" ;

  // some internal fields
  public static final String
      SEARCH = "__search",
      GROUPSEARCH = "__groupsearch",
      MARKED = "__markedentry",
      OWNER = "owner",
      TIMESTAMP = "timestamp", // it's also definied at the JabRefPreferences class
      ENTRYTYPE = "entrytype",

      // Using this when I have no database open or when I read
      // non bibtex file formats (used by the ImportFormatReader.java)
      DEFAULT_BIBTEXENTRY_ID = "__ID" ;

  public static final String[] DEFAULT_INSPECTION_FIELDS = new String[]
          {"author", "title", "year", KEY_FIELD};


  // singleton instance
  private static final BibtexFields runtime = new BibtexFields() ;

  // contains all bibtex-field objects (BibtexSingleField)
  private HashMap<String, BibtexSingleField> fieldSet;

  // contains all known (and public) bibtex fieldnames
  private String[] PUBLIC_FIELDS = null ;

  private BibtexFields()
  {
    fieldSet = new HashMap<String, BibtexSingleField>();
    BibtexSingleField dummy = null ;

    // FIRST: all standard fields
    // These are the fields that BibTex might want to treat, so these
    // must conform to BibTex rules.
    add( new BibtexSingleField( "address", true, GUIGlobals.SMALL_W  ) ) ;
    // An annotation. It is not used by the standard bibliography styles,
    // but may be used by others that produce an annotated bibliography.
    // http://www.ecst.csuchico.edu/~jacobsd/bib/formats/bibtex.html
    add( new BibtexSingleField( "annote", true, GUIGlobals.LARGE_W  ) ) ;
    add( new BibtexSingleField( "author", true, GUIGlobals.MEDIUM_W, 280 ) ) ;
    add( new BibtexSingleField( "booktitle", true, 175 ) ) ;
    add( new BibtexSingleField( "chapter", true, GUIGlobals.SMALL_W  ) ) ;
    add( new BibtexSingleField( "crossref", true, GUIGlobals.SMALL_W ) ) ;
    add( new BibtexSingleField( "edition", true, GUIGlobals.SMALL_W  ) ) ;
    add( new BibtexSingleField( "editor", true, GUIGlobals.MEDIUM_W, 280  ) ) ;
    add( new BibtexSingleField( "howpublished", true, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "institution", true, GUIGlobals.MEDIUM_W  ) ) ;

    dummy = new BibtexSingleField( "journal", true, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("journalNames");
    add(dummy) ;
    add( new BibtexSingleField( "key", true ) ) ;
    add( new BibtexSingleField( "month", true, GUIGlobals.SMALL_W ) ) ;
    add( new BibtexSingleField( "note", true, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "number", true, GUIGlobals.SMALL_W, 60  ).setNumeric(true) ) ;
    add( new BibtexSingleField( "organization", true, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "pages", true, GUIGlobals.SMALL_W ) ) ;
    add( new BibtexSingleField( "publisher", true, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "school", true, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "series", true, GUIGlobals.SMALL_W  ) ) ;
    add( new BibtexSingleField( "title", true, 400 ) ) ;
    add( new BibtexSingleField( "type", true, GUIGlobals.SMALL_W  ) ) ;
    add( new BibtexSingleField( "language", true, GUIGlobals.SMALL_W  ) ) ;
    add( new BibtexSingleField( "volume", true, GUIGlobals.SMALL_W, 60  ).setNumeric(true) ) ;
    add( new BibtexSingleField( "year", true, GUIGlobals.SMALL_W, 60 ).setNumeric(true) ) ;

    // some semi-standard fields
    dummy = new BibtexSingleField( KEY_FIELD, true ) ;
    dummy.setPrivate();
    add( dummy ) ;

    dummy = new BibtexSingleField( "doi", true, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("external");
    add(dummy) ;
    add( new BibtexSingleField( "eid", true, GUIGlobals.SMALL_W  ) ) ;

    dummy = new BibtexSingleField( "date", true ) ;
    dummy.setPrivate();
    add( dummy ) ;

    add(new BibtexSingleField("pmid", false, GUIGlobals.SMALL_W, 60).setNumeric(true));

    // additional fields ------------------------------------------------------
    dummy =  new BibtexSingleField( "citeseercitationcount", false,
                                                 GUIGlobals.SMALL_W, 75) ;
    dummy.setNumeric(true);
    dummy.setAlternativeDisplayName("Popularity") ;
    add(dummy) ;
    add( new BibtexSingleField( "location", false ) ) ;
    add( new BibtexSingleField( "abstract", false, GUIGlobals.LARGE_W, 400  ) ) ;

    dummy =  new BibtexSingleField( "url", false, GUIGlobals.SMALL_W) ;
    dummy.setExtras("external");
    add(dummy) ;

    dummy = new BibtexSingleField( "citeseerurl", false, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("external");
    add(dummy) ;

    dummy = new BibtexSingleField( "pdf", false, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("browseDoc");
    add(dummy) ;

    dummy = new BibtexSingleField( "ps", false, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("browseDocZip");
    add(dummy) ;
    add( new BibtexSingleField( "comment", false, GUIGlobals.MEDIUM_W  ) ) ;
    add( new BibtexSingleField( "keywords", false, GUIGlobals.SMALL_W  ) ) ;
    //FIELD_EXTRAS.put("keywords", "selector");


    dummy = new BibtexSingleField(GUIGlobals.FILE_FIELD, false);
    dummy.setEditorType(GUIGlobals.FILE_LIST_EDITOR);
    add(dummy);


    add( new BibtexSingleField( "search", false, 75 ) ) ;


    // some internal fields ----------------------------------------------
    dummy = new BibtexSingleField( GUIGlobals.NUMBER_COL, false, 32  ) ;
    dummy.setPrivate() ;
    dummy.setWriteable(false);
    dummy.setDisplayable(false);
    add( dummy ) ;

    dummy = new BibtexSingleField( OWNER, false, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("setOwner");
    dummy.setPrivate();
    add(dummy) ;

    dummy = new BibtexSingleField( TIMESTAMP, false, GUIGlobals.SMALL_W ) ;
    dummy.setExtras("datepicker");
    dummy.setPrivate();
    add(dummy) ;

    dummy =  new BibtexSingleField( ENTRYTYPE, false, 75 ) ;
    dummy.setPrivate();
    add(dummy) ;

    dummy =  new BibtexSingleField( SEARCH, false) ;
    dummy.setPrivate();
    dummy.setWriteable(false);
    dummy.setDisplayable(false);
    add(dummy) ;

    dummy =  new BibtexSingleField( GROUPSEARCH, false) ;
    dummy.setPrivate();
    dummy.setWriteable(false);
    dummy.setDisplayable(false);
    add(dummy) ;

    dummy =  new BibtexSingleField( MARKED, false) ;
    dummy.setPrivate();
    dummy.setWriteable(true); // This field must be written to file!
    dummy.setDisplayable(false);
    add(dummy) ;

     // read external field definitions
    readXML( Globals.additionalFields ) ;

    // collect all public fields for the PUBLIC_FIELDS array
    Vector<String> pFields = new Vector<String>( fieldSet.size()) ;
    for (BibtexSingleField sField : fieldSet.values()){
      if (sField.isPublic() )
      {
        pFields.add( sField.getFieldName() );
        // or export the complet BibtexSingleField ?
        // BibtexSingleField.toString() { return fieldname ; }
      }
    }

    PUBLIC_FIELDS = pFields.toArray(new String[pFields.size()]);
    // sort the entries
    java.util.Arrays.sort( PUBLIC_FIELDS );

  }

    /**
     * Read the "numericFields" string array from preferences, and activate numeric
     * sorting for all fields listed in the array. If an unknown field name is included,
     * add a field descriptor for the new field.
     */
    public static void setNumericFieldsFromPrefs() {
        String[] numFields = Globals.prefs.getStringArray("numericFields");
        if (numFields == null)
            return;
        // Build a Set of field names for the fields that should be sorted numerically:
        HashSet<String> nF = new HashSet<String>();
        for (int i = 0; i < numFields.length; i++) {
            nF.add(numFields[i]);
        }
        // Look through all registered fields, and activate numeric sorting if necessary:
        for (String fieldName : runtime.fieldSet.keySet()) {
            BibtexSingleField field = runtime.fieldSet.get(fieldName);
            if (!field.isNumeric() && nF.contains(fieldName)) {
                field.setNumeric(nF.contains(fieldName));
            }
            nF.remove(fieldName); // remove, so we clear the set of all standard fields.
        }
        // If there are fields left in nF, these must be non-standard fields. Add descriptors for them:
        for (String fieldName : nF) {
            BibtexSingleField field = new BibtexSingleField(fieldName, false);
            field.setNumeric(true);
            runtime.fieldSet.put(fieldName, field);
        }

    }


  /** insert a field into the internal list */
  private void add( BibtexSingleField field )
  {
    // field == null check
    String key = field.name ;
    fieldSet.put( key, field ) ;
  }

  /** read a xml definiton file and put only NEW fields into the field list */
  private void readXML( String resName )
  {
    TXMLReader reader = new TXMLReader(resName) ;
    if (reader.isReady() )
    {
      // get a list of all fields
      NodeList fieldNodes = reader.getNodes("field") ;

      int tagsCount = fieldNodes.getLength() ;
      for (int t = 0 ; t < tagsCount ; t++)
      {
        Element entry = (Element) fieldNodes.item(t) ;
        String fName = reader.readStringAttribute(entry, "name", null) ;
        if (fName != null)  // something found ?
        {
          fName = fName.toLowerCase() ;
          BibtexSingleField dummy = fieldSet.get( fName ) ;
          if (dummy == null)  // unknown field
          {
            dummy = new BibtexSingleField(reader, entry) ;
            fieldSet.put(fName, dummy) ;
          }
        }
      }
    }
  }

  // --------------------------------------------------------------------------
  //  the "static area"
  // --------------------------------------------------------------------------
  private static final BibtexSingleField getField( String name )
  {
    if (name != null)
    {
      return runtime.fieldSet.get(name.toLowerCase()) ;
    }

    return null ;
  }

  public static String getFieldExtras( String name )
  {
    BibtexSingleField sField = getField( name ) ;
    if (sField != null)
    {
      return sField.getExtras() ;
    }
    return null ;
  }


  public static int getEditorType(String name) {
    BibtexSingleField sField = getField( name ) ;
    if (sField != null)
    {
      return sField.getEditorType();
    }
    return GUIGlobals.STANDARD_EDITOR;      
  }

  public static double getFieldWeight( String name )
  {
    BibtexSingleField sField = getField( name ) ;
    if (sField != null)
    {
      return sField.getWeight() ;
    }
    return GUIGlobals.DEFAULT_FIELD_WEIGHT ;
  }

  public static void setFieldWeight( String fieldName, double weight )
  {
    BibtexSingleField sField = getField( fieldName ) ;
    if (sField != null)
    {
      sField.setWeight( weight ) ;
    }
  }

  public static int getFieldLength( String name )
  {
    BibtexSingleField sField = getField( name ) ;
    if (sField != null)
    {
      return sField.getLength() ;
    }
    return GUIGlobals.DEFAULT_FIELD_LENGTH ;
  }

  // returns an alternative name for the given fieldname
  public static String getFieldDisplayName( String fieldName )
  {
    BibtexSingleField sField = getField( fieldName ) ;
    if (sField != null)
    {
      return sField.getAlternativeDisplayName() ;
    }
    return null ;
  }

  public static boolean isWriteableField( String field )
  {
    BibtexSingleField sField = getField( field ) ;
    if (sField != null)
    {
      return sField.isWriteable() ;
    }
    return true ;
  }

  public static boolean isDisplayableField( String field )
  {
    BibtexSingleField sField = getField( field ) ;
    if (sField != null)
    {
      return sField.isDisplayable() ;
    }
    return true ;
  }

  /**
   * Returns true if the given field is a standard Bibtex field.
   *
   * @param field a <code>String</code> value
   * @return a <code>boolean</code> value
   */
  public static boolean isStandardField( String field )
  {
    BibtexSingleField sField = getField( field ) ;
    if (sField != null)
    {
      return sField.isStandard() ;
    }
    return false ;
  }

    public static boolean isNumeric( String field ) {
        BibtexSingleField sField = getField( field ) ;
        if (sField != null)
        {
            return sField.isNumeric() ;
        }
        return false ;
    }

  /** returns an string-array with all fieldnames */
  public static String[] getAllFieldNames()
  {
    return runtime.PUBLIC_FIELDS ;
  }

  /** returns the fieldname of the entry at index t */
  public static String getFieldName( int t )
  {
    return  runtime.PUBLIC_FIELDS[t] ;
  }

  /** returns the number of available fields */
  public static int numberOfPublicFields()
  {
    return runtime.PUBLIC_FIELDS.length ;
  }

  /*
     public static int getPreferredFieldLength(String name) {
     int l = DEFAULT_FIELD_LENGTH;
     Object o = fieldLength.get(name.toLowerCase());
     if (o != null)
     l = ((Integer)o).intValue();
     return l;
     }*/


  // --------------------------------------------------------------------------
  // a container class for all properties of a bibtex-field
  // --------------------------------------------------------------------------
  private static class BibtexSingleField
  {
    private static final int
        STANDARD       = 0x01,  // it is a standard bibtex-field
        PRIVATE        = 0x02,  // internal use, e.g. owner, timestamp
        DISPLAYABLE    = 0x04,  // These fields cannot be shown inside the source editor panel
        WRITEABLE      = 0x08 ; // These fields will not be saved to the .bib file.

    // the fieldname
    private String name ;

    // contains the standard, privat, displayable, writable infos
    // default is: not standard, public, displayable and writable
    private int flag = DISPLAYABLE | WRITEABLE ;

    private int length = GUIGlobals.DEFAULT_FIELD_LENGTH ;
    private double weight = GUIGlobals.DEFAULT_FIELD_WEIGHT ;

    private int editorType = GUIGlobals.STANDARD_EDITOR;

    // a alternative displayname, e.g. used for
    // "citeseercitationcount"="Popularity"
    private String alternativeDisplayName = null ;

    // the extras data
    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    private String extras = null ;

    // This value defines whether contents of this field are expected to be
    // numeric values. This can be used to sort e.g. volume numbers correctly:
    private boolean numeric = false;

      // a comma separated list of alternative bibtex-fieldnames, e.g.
    // "LCCN" is the same like "lib-congress"
    // private String otherNames = null ;

    // a Hashmap for a lot of additional "not standard" properties
    // todo: add the handling in a key=value manner
    // private HashMap props = new HashMap() ;

    // some constructors ;-)
    public BibtexSingleField( String fieldName )
    {
      name = fieldName ;
    }

    public BibtexSingleField( String fieldName, boolean pStandard )
    {
      name = fieldName ;
      setFlag( pStandard, STANDARD) ;
    }

    public BibtexSingleField( String fieldName, boolean pStandard, double pWeight)
    {
      name = fieldName ;
      setFlag( pStandard, STANDARD) ;
      weight = pWeight ;
    }

    public BibtexSingleField( String fieldName, boolean pStandard, int pLength)
    {
      name = fieldName ;
      setFlag( pStandard, STANDARD) ;
      length = pLength ;
    }

    public BibtexSingleField( String fieldName, boolean pStandard,
                              double pWeight, int pLength)
    {
      name = fieldName ;
      setFlag( pStandard, STANDARD) ;
      weight = pWeight ;
      length = pLength ;
    }

    /** the constructor reads all neccessary data from the xml file */
    public BibtexSingleField( TXMLReader reader, Element node)
    {
      // default is: not standard, public, displayable and writable
      flag = DISPLAYABLE | WRITEABLE ;

      name = reader.readStringAttribute(node, "name", "field") ;
      name = name.toLowerCase() ;

      // read the weight
      String wStr = reader.readStringAttribute(node, "weight", null) ;
      if (wStr != null)
      {
        int hCode = wStr.toLowerCase().hashCode() ;
        if (hCode == "small".hashCode())
        {
          weight = GUIGlobals.SMALL_W ;
        }
        else if (hCode == "medium".hashCode())
        {
          weight = GUIGlobals.MEDIUM_W ;
        }
        else if (hCode == "large".hashCode())
        {
          weight = GUIGlobals.LARGE_W ;
        }
        else // try to convert to a double value
        {
          try
          {
            weight = Double.parseDouble(wStr) ;
            if ((weight < 0.0) || (weight > GUIGlobals.MAX_FIELD_WEIGHT))
            {
              weight = GUIGlobals.DEFAULT_FIELD_WEIGHT ;
            }
          }
          catch (Exception e)
          {
            weight = GUIGlobals.DEFAULT_FIELD_WEIGHT ;
          }
        }
      }
      length = reader.readIntegerAttribute( node, "length", GUIGlobals.DEFAULT_FIELD_LENGTH ) ;

      extras = reader.readStringAttribute(node, "extras", null) ;
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------



    private void setFlag( boolean onOff, int flagID)
    {
      if (onOff)  // set the flag
      {
        flag = flag | flagID ;
      }
      else // unset the flag,
      {
        flag = flag & ( 0xff ^ flagID ) ;
      }
    }

    private boolean isSet( int flagID )
    {
      if ( (flag & flagID) == flagID)
        return true ;

      return false ;
    }

    // -----------------------------------------------------------------------
    public boolean isStandard()
    {
      return isSet( STANDARD ) ;
    }

    public void setPrivate()
    {
      flag = flag | PRIVATE ;
    }

    public boolean isPrivate()
    {
      return isSet( PRIVATE ) ;
    }

    public void setPublic()
    {
      setFlag( false, PRIVATE ) ;
    }

    public boolean isPublic()
    {
      return !isSet( PRIVATE ) ;
    }

    public void setDisplayable(boolean value)
    {
      setFlag( value, DISPLAYABLE ) ;
    }

    public boolean isDisplayable()
    {
      return isSet(DISPLAYABLE) ;
    }


    public void setWriteable(boolean value)
    {
      setFlag( value, WRITEABLE ) ;
    }

    public boolean isWriteable()
    {
      return isSet( WRITEABLE ) ;
    }

    // -----------------------------------------------------------------------
    public void setAlternativeDisplayName( String aName)
    {
      alternativeDisplayName = aName ;
    }

    public String getAlternativeDisplayName()
    {
      return alternativeDisplayName ;
    }
    // -----------------------------------------------------------------------

    public void setExtras( String pExtras)
    {
      extras = pExtras ;
    }

    // fieldExtras contains mappings to tell the EntryEditor to add a specific
    // function to this field, for instance a "browse" button for the "pdf" field.
    public String getExtras()
    {
      return extras ;
    }

    public void setEditorType(int type) {
        editorType = type;
    }

    public int getEditorType() {
        return editorType;
    }
    // -----------------------------------------------------------------------

    public void setWeight( double value )
    {
      this.weight = value ;
    }

    public double getWeight()
    {
      return this.weight ;
    }

    // -----------------------------------------------------------------------
    public int getLength()
    {
      return this.length ;
    }

    // -----------------------------------------------------------------------

    public String getFieldName()
    {
      return name ;
    }


      /**
       * Set this field's numeric propery
       * @param numeric true to indicate that this is a numeric field.
       * @return this BibtexSingleField instance. Makes it easier to call this
       *   method on the fly while initializing without using a local variable.
       */
      public BibtexSingleField setNumeric(boolean numeric) {
          this.numeric = numeric;
          return this;
      }

      public boolean isNumeric() {
          return numeric;
      }

  }
}
