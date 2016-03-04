/*
Copyright (C) 2004 R. Nagel
Copyright (C) 2016 JabRef Contributors


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

// created by : r.nagel 13.10.2004
//
// function : handles the subdatabase from aux command line option
//
// modified :

package net.sf.jabref.wizard.auximport;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.logic.util.strings.StringUtil;

public class AuxCommandLine {

    private final String auxName;
    private final BibDatabase bib;


    public AuxCommandLine(String auxFileName, BibDatabase refDBase) {
        auxName = StringUtil.getCorrectFileName(auxFileName, "aux");
        bib = refDBase;
    }

    public BibDatabase perform() {
        BibDatabase back = null;
        if (!auxName.isEmpty() && (bib != null)) {
            AuxFileParser auxParser = new AuxFileParser();
            auxParser.generateBibDatabase(auxName, bib);
            back = auxParser.getGeneratedBibDatabase();

            // print statistics
            System.out.println(auxParser.getInformation(true));
        }
        return back;
    }
}
