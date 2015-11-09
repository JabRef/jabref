/*  Copyright (C) 2003-2015 JabRef contributors.
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

package net.sf.jabref.external.push;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.openoffice.OpenOfficePanel;

public class PushToApplications {
    public static final List<PushToApplication> applications;

    /**
     * Set up the current available choices:
     */
    static {
        applications = new ArrayList<>();

        PushToApplications.applications.add(new PushToEmacs());
        PushToApplications.applications.add(new PushToLatexEditor());
        PushToApplications.applications.add(new PushToLyx());
        PushToApplications.applications.add(OpenOfficePanel.getInstance());
        PushToApplications.applications.add(new PushToTexmaker());
        PushToApplications.applications.add(new PushToTeXstudio());
        PushToApplications.applications.add(new PushToVim());
        PushToApplications.applications.add(new PushToWinEdt());
    }
}
