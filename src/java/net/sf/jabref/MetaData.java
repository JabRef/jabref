/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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
package net.sf.jabref;

import java.io.*;
import java.util.*;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.jabref.groups.GroupTreeNode;

public class MetaData {
    private HashMap metaData = new HashMap();
    private StringReader data;
    private GroupTreeNode groupsRoot = null;

    /**
     * The MetaData object stores all meta data sets in Vectors. To ensure that
     * the data is written correctly to string, the user of a meta data Vector
     * must simply make sure the appropriate changes are reflected in the Vector
     * it has been passed.
     */
    public MetaData(HashMap inData, BibtexDatabase db) {
        this();
        boolean groupsTreePresent = false;
        Vector flatGroupsData = null;
        if (inData != null) for (Iterator i = inData.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            data = new StringReader((String) inData.get(key));
            String unit;
            Vector orderedData = new Vector();
            // We must allow for ; and \ in escape sequences.
            try {
                while ((unit = getNextUnit(data)) != null) {
                    orderedData.add(unit);
                }
            } catch (IOException ex) {
                System.err.println("Weird error while parsing meta data.");
            }
            if (key.equals("groupstree")) {
                putGroups(orderedData,db);
                groupsTreePresent = true;
            } else if (key.equals("groups")) {
                flatGroupsData = orderedData;
            } else {
                putData(key, orderedData);
            }
        }
        if (!groupsTreePresent && flatGroupsData != null)
            groupsRoot = GroupTreeNode.importFlatGroups(flatGroupsData);
    }

    /**
     * The MetaData object can be constructed with no data in it.
     */
    public MetaData() {
        metaData.put(Globals.SELECTOR_META_PREFIX + "keywords", new Vector());
        metaData.put(Globals.SELECTOR_META_PREFIX + "journal", new Vector());
        metaData.put(Globals.SELECTOR_META_PREFIX + "publisher", new Vector());
    }

    public Iterator iterator() {
        return metaData.keySet().iterator();
    }

    public Vector getData(String key) {
        return (Vector) metaData.get(key);
    }

    public void remove(String key) {
        metaData.remove(key);
    }

    /**
     * Stores the specified data in this object, using the specified key. For
     * certain keys (e.g. "groupstree"), the objects in orderedData are
     * reconstructed from their textual (String) representation if they are of
     * type String, and stored as an actual instance.
     */
    public void putData(String key, Vector orderedData) {
        metaData.put(key, orderedData);
    }
    
    private void putGroups(Vector orderedData, BibtexDatabase db) {
        // JZTODO add groups version!!!
        
        try {
            // version 1: orderedData contains exactly 1 element
            groupsRoot = GroupTreeNode.fromString((String) orderedData.firstElement(),db);
        } catch (Exception e) {
            // JZTODO: what now? should never happen, unless 
            // metadata was edited directly in the file
            e.printStackTrace();
        }
    }
    
    public GroupTreeNode getGroups() {
        return groupsRoot;
    }
    
    /**
     * Sets a new group root node. <b>WARNING </b>: This invalidates everything
     * returned by getGroups() so far!!!
     */
    public void setGroups(GroupTreeNode root) {
        groupsRoot = root;
    }

    /**
     * Writes all data to the specified writer, using each object's toString()
     * method.
     */
    public void writeMetaData(Writer out) throws IOException {
        for (Iterator i = metaData.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            StringBuffer sb = new StringBuffer();
            Vector orderedData = (Vector) metaData.get(key);
            if (orderedData.size() > 0) {
                //out.write("@comment{" + GUIGlobals.META_FLAG + key + ":");
                sb.append("@comment{" + GUIGlobals.META_FLAG + key + ":");
                for (int j = 0; j < orderedData.size(); j++) {
                    //out.write(Util.quote((String)orderedData.elementAt(j),";",'\\') + ";");
                    sb.append(Util.quote((String)orderedData.elementAt(j),";",'\\') + ";");
                }
                //out.write("}\n\n");
                sb.append("}\n\n");
            }
            wrapStringBuffer(sb, Globals.METADATA_LINE_LENGTH);
            out.write(sb.toString());
        }
        // write groups if present. skip this if only the root node exists 
        // (which is always the AllEntriesGroup).
        if (groupsRoot != null && groupsRoot.getChildCount() > 0) {
            StringBuffer sb = new StringBuffer();
            sb.append("@comment{" + GUIGlobals.META_FLAG + "groupstree:");
            sb.append(Util.quote(groupsRoot.toString(),";",'\\'));
            sb.append("}\n\n");
            
            wrapStringBuffer(sb, Globals.METADATA_LINE_LENGTH);
            out.write(sb.toString());
        }
    }

    private void wrapStringBuffer(StringBuffer sb, int lineLength) {
        for (int i=lineLength; i<sb.length(); i+=lineLength+1) {
            sb.insert(i, '\n');
        }
    }
    
    /**
     * Reads the next unit. Units are delimited by ';'. 
     */
    private String getNextUnit(Reader reader) throws IOException {
        int c;
        boolean escape = false;
        StringBuffer res = new StringBuffer();
        while ((c = reader.read()) != -1) {
            if (escape) {
                res.append((char)c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == ';') {
                break;
            } else {
                res.append((char)c);
            }
        }
        if (res.length() > 0)
            return res.toString();
        return null;
    }
//
//    private String makeEscape(String s, String specials) {
//        StringBuffer sb = new StringBuffer();
//        int c;
//        for (int i = 0; i < s.length(); i++) {
//            c = s.charAt(i);
//            if ((c == '\\') || specials.indexOf(c) >= 0)
//                sb.append('\\');
//            sb.append((char) c);
//        }
//        return sb.toString();
//    }
}
