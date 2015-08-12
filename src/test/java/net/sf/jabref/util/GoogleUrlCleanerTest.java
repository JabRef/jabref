/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.util;

import org.junit.Assert;
import org.junit.Test;

public class GoogleUrlCleanerTest {

    @Test
    public void testCleanUrl() throws Exception {
        Assert.assertEquals("http://dl.acm.org/citation.cfm?id=321811", GoogleUrlCleaner.cleanUrl("https://www.google.hr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&cad=rja&ved=0CC0QFjAA&url=http%3A%2F%2Fdl.acm.org%2Fcitation.cfm%3Fid%3D321811&ei=gHDRUa-IKobotQbMy4GAAg&usg=AFQjCNEBJPUimu-bAns6lSLe-kszz4AiGA&sig2=DotF0pIZD8OhjDcSHPlBbQ"));
        Assert.assertEquals("http://dl.acm.org/citation.cfm?id=321811", GoogleUrlCleaner.cleanUrl("http://dl.acm.org/citation.cfm?id=321811"));
        Assert.assertEquals("test text", GoogleUrlCleaner.cleanUrl("test text"));
        Assert.assertEquals(" ", GoogleUrlCleaner.cleanUrl(" "));
        Assert.assertEquals("", GoogleUrlCleaner.cleanUrl(""));
        Assert.assertEquals(null, GoogleUrlCleaner.cleanUrl(null));
    }
}