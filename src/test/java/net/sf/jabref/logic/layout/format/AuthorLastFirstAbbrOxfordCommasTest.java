/*
 * Copyright (C) 2006 Jabref-Team
 * 
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLastFirstAbbrOxfordCommasTest {

    /**
     * Test method for {@link net.sf.jabref.logic.layout.format.AuthorLastFirstAbbrOxfordCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirstAbbrOxfordCommas();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("Someone, V. S.", a.format("Van Something Someone"));

        // Two names
        Assert.assertEquals("von Neumann, J. and Black Brown, P.", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        Assert.assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
