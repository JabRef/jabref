/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

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
package net.sf.jabref.export;

import java.io.*;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.*;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class FileActions
{
    //~ Methods ////////////////////////////////////////////////////////////////

    /**
     * Saves the database to file. Two boolean values indicate whether
     * only entries with a nonzero Globals.SEARCH value and only
     * entries with a nonzero Globals.GROUPSEARCH value should be
     * saved. This can be used to let the user save only the results of
     * a search. False and false means all entries are saved.
     */
    public static void saveDatabase(BibtexDatabase database, MetaData metaData,
        File file, JabRefPreferences prefs, boolean checkSearch,
        boolean checkGroup) throws SaveException
    {
        BibtexEntry be = null;

        try
        {
            String name = file.getName();
            String path = file.getParent();
            File temp = new File(path, name + GUIGlobals.tempExt);

            if (prefs.getBoolean("backup"))
            {
                File back = new File(path, name + GUIGlobals.backupExt);

                if (back.exists())
                {
                    back.renameTo(temp);
                }

                if (file.exists())
                {
                    file.renameTo(back);
                }

                if (temp.exists())
                {
                    temp.delete();
                }
            }
            else
            {
                if (file.exists())
                {
                    file.renameTo(temp);
                }
            }

            // Define our data stream.
            FileWriter fw = new FileWriter(file);

            // Write signature.
            fw.write(Globals.lang(GUIGlobals.SIGNATURE));

            // Write preamble if there is one.
            String preamble = database.getPreamble();

            if (preamble != null)
            {
                fw.write("@PREAMBLE{");
                fw.write(preamble);
                fw.write("}\n\n");
            }

            // Write strings if there are any.
            for (int i = 0; i < database.getStringCount(); i++)
            {
                BibtexString bs = database.getString(i);

                //fw.write("@STRING{"+bs.getName()+" = \""+bs.getContent()+"\"}\n\n");
                fw.write("@STRING{" + bs.getName() + " = ");
                fw.write((new LatexFieldFormatter()).format(bs.getContent(),
                        true));

                //Util.writeField(bs.getName(), bs.getContent(), fw);
                fw.write("}\n\n");
            }

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            String pri = prefs.get("priSort");

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            String sec = prefs.get("secSort");

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            String ter = prefs.get("terSort");
            TreeSet sorter = new TreeSet(new CrossRefEntryComparator(
                        new EntryComparator(prefs.getBoolean("priDescending"),
                            prefs.getBoolean("secDescending"),
                            prefs.getBoolean("terDescending"), pri, sec, ter)));
            Set keySet = database.getKeySet();

            if (keySet != null)
            {
                Iterator i = keySet.iterator();

                for (; i.hasNext();)
                {
                    sorter.add(database.getEntryById((String) (i.next())));
                }
            }

            FieldFormatter ff = new LatexFieldFormatter();

            for (Iterator i = sorter.iterator(); i.hasNext();)
            {
                be = (BibtexEntry) (i.next());

                // Check if the entry should be written.
                boolean write = true;

                if (checkSearch && !nonZeroField(be, Globals.SEARCH))
                {
                    write = false;
                }

                if (checkGroup && !nonZeroField(be, Globals.GROUPSEARCH))
                {
                    write = false;
                }

                if (write)
                {
                    be.write(fw, ff);
                    fw.write("\n");
                }
            }

            // Write meta data.
            if (metaData != null)
            {
                metaData.writeMetaData(fw);
            }

            fw.close();
        }
         catch (Throwable ex)
        {
            ex.printStackTrace();

            // Repair the file with our temp file since saving failed.
            String name = file.getName();

            // Repair the file with our temp file since saving failed.
            String path = file.getParent();
            File temp = new File(path, name + GUIGlobals.tempExt);
            File back = new File(path, name + GUIGlobals.backupExt);

            if (file.exists())
            {
                file.delete();
            }

            if (temp.exists())
            {
                temp.renameTo(file);
            }
            else
            {
                back.renameTo(file);
            }

            throw new SaveException(ex.getMessage(), be);
        }
    }

    /** Returns true iff the entry has a nonzero value in its field.
     */
    private static boolean nonZeroField(BibtexEntry be, String field)
    {
        String o = (String) (be.getField(field));

        return ((o != null) && !o.equals("0"));
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
