///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner
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
package net.sf.jabref.export.layout;


/*==========================================================================*
 * IMPORTS
 *==========================================================================*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;


/*==========================================================================*
 * CLASS DECLARATION
 *==========================================================================     */

/**
 * Example for converting molecules.
 *
 * @author     wegnerj
 */
public class LayoutTest
{
    //~ Constructors ///////////////////////////////////////////////////////////

    public LayoutTest()
    {
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    /**
     *  The main program for the TestSmarts class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args)
    {
        LayoutTest test = null;

        test = new LayoutTest();
        if(args.length!=3)
        {
        	System.err.println("Usage: LayoutTest <BibTeX-File> <Layout-File> <Output-File>");
        }
        else
        {
        test.test(args[0], args[1], args[2]);
        }
    }

    /**
     *
     */
    private void test(String bibtexFile, String layoutFile, String outputFile)
    {
        File file = new File(bibtexFile);
		File outFile = new File(outputFile);
        BibtexParser bparser = null;
        BibtexDatabase bibtex = null;
		PrintStream ps=null;

        try
        {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fis);
            ps=new PrintStream(new FileOutputStream(outFile));
            bparser = new BibtexParser(reader);

            ParserResult pr = bparser.parse();
            bibtex = pr.getDatabase();
        }
         catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
         catch (IOException e)
        {
            e.printStackTrace();
        }

        file = new File(layoutFile);
        try
        {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fis);
            LayoutHelper layoutHelper = new LayoutHelper(reader);
            Layout layout = layoutHelper.getLayoutFromText();

            Object[] keys = bibtex.getKeySet().toArray();
            String key;

            for (int i = 0; i < keys.length; i++)
            {
                key = (String) keys[i];

                //System.out.println(key);
                BibtexEntry entry = bibtex.getEntryById(key);
                //System.out.println(layout.doLayout(entry));
                ps.println(layout.doLayout(entry));
            }
        }
         catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
         catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
