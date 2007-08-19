///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile$
//  Purpose:  Atom representation.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg K. Wegner, Gerd Mueller
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
package wsi.ra.tool;

import java.io.*;
import java.util.Vector;

/*==========================================================================*
 * IMPORTS
 *==========================================================================*/
//import org.apache.log4j.*;

/*==========================================================================*
 * CLASS DECLARATION
 *==========================================================================*/

/**
 *  Loads resource file from directory OR jar file. Now it is easier possible to
 *  access resource files in a directory structure or a .jar/.zip file.
 *
 * @author     wegnerj
 * @author     Robin Friedman, rfriedman@TriadTherapeutics.com
 * @author     Gerd Mueller
 * @license GPL
 * @cvsversion    $Revision$, $Date$
 */
public class ResourceLoader
{
    //~ Static fields/initializers /////////////////////////////////////////////

    /**
     *  Obtain a suitable logger.
     */
//    private static Category logger = Category.getInstance(
//            "wsi.ra.tool.ResourceLoader");
    private static ResourceLoader resourceLoader;

    //~ Constructors ///////////////////////////////////////////////////////////

    /*------------------------------------------------------------------------*
     * constructor
     *------------------------------------------------------------------------  */

    /**
     *  Constructor for the ResourceLoader object
     */
    private ResourceLoader()
    {
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    /*-------------------------------------------------------------------------*
     * public methods
     *-------------------------------------------------------------------------  */

    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public static synchronized ResourceLoader instance()
    {
        if (resourceLoader == null)
        {
            resourceLoader = new ResourceLoader();
        }

        return resourceLoader;
    }

    /**
     *  Gets the byte data from a file at the given resource location.
     *
     * @param  resourceLocation  Description of the Parameter
     * @return                   the byte array of file.
     */
    public byte[] getBytesFromResourceLocation(String resourceLocation)
    {
        if (resourceLocation == null)
        {
            return null;
        }
		// to avoid hours of debugging non-found-files under linux with
	    // some f... special characters at the end which will not be shown
	    // at the console output !!!
	    resourceLocation = resourceLocation.trim();

        // is a relative path defined ?
        // this can only be possible, if this is a file resource loacation
        if (resourceLocation.startsWith("..") ||
                resourceLocation.startsWith("/") ||
                resourceLocation.startsWith("\\") ||
                ((resourceLocation.length() > 1) &&
                (resourceLocation.charAt(1) == ':')))
        {
            return getBytesFromFile(resourceLocation);
        }

        InputStream in = ClassLoader
                             .getSystemResourceAsStream(resourceLocation);

        if (in == null)
        {
            // try again for web start applications
            in = this.getClass().getClassLoader().getResourceAsStream(resourceLocation);
        }

        if (in == null)
        {
            return null;
        }

        byte bytes[]=getBytesFromStream(in);

//		if(bytes==null)
//		{
//			URL location = this.getClass().getClassLoader().getSystemResource(resourceLocation);
//			String fileLocation = location.getFile();
//			bytes=getBytesFromFile(fileLocation);
//		}

        return bytes;

        //        //System.out.println(this.getClass().getClassLoader().getSystemResource(resourceLocation));
        //        URL location = this.getClass().getClassLoader().getSystemResource(resourceLocation);
        //
        //        if (location == null)
        //        {
        //            // try again for web start applications
        //            location = this.getClass().getClassLoader().getResource(resourceLocation);
        //        }
        //
        //        if (logger.isDebugEnabled())
        //        {
        //            logger.debug("Trying to get " + resourceLocation + " from URL: " +
        //                location);
        //        }
        //
        //        if (location == null)
        //        {
        //            return null;
        //        }
        //
        //        String locationString = URLDecoder.decode( location.toString() );
        //
        //        int posJAR = locationString.indexOf(".jar!");
        //        int posZIP = locationString.indexOf(".zip!");
        //        int pos = -1;
        //
        //        if ((posJAR > -1) && (posZIP > -1))
        //        {
        //            pos = Math.min(posJAR, posZIP);
        //        }
        //        else if (posJAR > -1)
        //        {
        //            pos = posJAR;
        //        }
        //        else if (posZIP > -1)
        //        {
        //            pos = posZIP;
        //        }
        //
        //        // is the resource file in a zip or a jar file
        //        if (pos > -1)
        //        {
        //            // load it from zip or jar file
        //            String urlToZip = locationString.substring(4, pos + 4);
        //            String internalArchivePath = locationString.substring(pos + 6);
        //
        //            if (logger.isDebugEnabled())
        //            {
        //                logger.debug("Loading " + internalArchivePath +
        //                    " from archive " + urlToZip + ".");
        //            }
        //
        //            return getBytesFromArchive(urlToZip, internalArchivePath);
        //        }
        //        else
        //        {
        //            String fileLocation = location.getFile();
        //
        //            // load it from an unpacked file
        //            if (logger.isDebugEnabled())
        //            {
        //                logger.debug("Loading from file " + fileLocation + ".");
        //            }
        //
        //            return getBytesFromFile(fileLocation);
        //        }
    }

    /**
     *  Description of the Method
     *
     * @param  resourceFile  Description of the Parameter
     * @return               Description of the Return Value
     */
    public static Vector<String> readLines(String resourceFile)
    {
        return readLines(resourceFile, false);
    }

    /**
     *  Description of the Method
     *
     * @param  resourceFile    Description of the Parameter
     * @param  ignoreComments  Description of the Parameter
     * @return                 Description of the Return Value
     */
    public static Vector<String> readLines(String resourceFile,
        boolean ignoreCommentedLines)
    {
        if (resourceFile == null)
        {
            return null;
        }

        byte[] bytes = ResourceLoader.instance().getBytesFromResourceLocation(resourceFile);

        if (bytes == null)
        {
            return null;
        }

        ByteArrayInputStream sReader = new ByteArrayInputStream(bytes);
        LineNumberReader lnr = new LineNumberReader(new InputStreamReader(
                    sReader));

        String line;
        Vector<String> vector = new Vector<String>(100);

        try
        {
            while ((line = lnr.readLine()) != null)
            {
                if (!ignoreCommentedLines)
                {
                    if (!(line.charAt(0) == '#'))
                    {
                        vector.add(line);

                        //		  System.out.println("ADD:"+line);
                    }
                }
                else
                {
                    vector.add(line);
                }
            }
        }
         catch (IOException ex)
        {
            ex.printStackTrace();
        }

        return vector;
    }

    /*-------------------------------------------------------------------------*
     * private methods
     *-------------------------------------------------------------------------  */

    /**
     *  Gets the byte data from a file.
     *
     * @param  fileName  Description of the Parameter
     * @return           the byte array of the file.
     */
    private byte[] getBytesFromFile(String fileName)
    {
        if (fileName.startsWith("/cygdrive/"))
        {
            int length = "/cygdrive/".length();
            fileName = fileName.substring(length, length + 1) + ":" +
                fileName.substring(length + 1);
        }

        //if (logger.isDebugEnabled())
        //{
        //    logger.debug("Trying to get file from " + fileName);
        //}

        File file = new File(fileName);
        FileInputStream fis = null;

        try
        {
            fis = new FileInputStream(file);
        }
         catch (Exception e)
        {
            return null;
        }

        BufferedInputStream bis = new BufferedInputStream(fis);

        // only files with <65536 bytes are allowed
        //if( file.length() > 65536 ) System.out.println("Resource files should be smaller than 65536 bytes...");
        int size = (int) file.length();
        byte[] b = new byte[size];
        int rb = 0;
        int chunk = 0;

        try
        {
            while (((int) size - rb) > 0)
            {
                chunk = bis.read(b, rb, (int) size - rb);

                if (chunk == -1)
                {
                    break;
                }

                rb += chunk;
            }
        }
         catch (Exception e)
        {
            return null;
        }

        return b;
    }

    /**
     *  Gets the byte data from a file.
     *
     * @param  fileName  Description of the Parameter
     * @return           the byte array of the file.
     */
    private byte[] getBytesFromStream(InputStream stream)
    {
        //if (logger.isDebugEnabled())
        //{
        //    logger.debug("Trying to get file from stream");
        //}

        BufferedInputStream bis = new BufferedInputStream(stream);

        try
        {
            int size = (int) bis.available();
            byte[] b = new byte[size];
            int rb = 0;
            int chunk = 0;

            while (((int) size - rb) > 0)
            {
                chunk = bis.read(b, rb, (int) size - rb);

                if (chunk == -1)
                {
                    break;
                }

                rb += chunk;
            }

            return b;
        }
         catch (Exception e)
        {
            return null;
        }
    }
}

/*-------------------------------------------------------------------------*
 * END
 *-------------------------------------------------------------------------*/

///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////
