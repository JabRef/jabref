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
package net.sf.jabref.gui.help;

import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.View;
import javax.swing.text.html.ParagraphView;

class HTMLParagraphView extends ParagraphView {

    private static final int MAX_VIEW_SIZE = 100;


    public HTMLParagraphView(Element elem) {
        super(elem);
        strategy = new HTMLParagraphView.HTMLFlowStrategy();
    }


    private static class HTMLFlowStrategy extends FlowStrategy {

        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View res = super.createView(fv, startOffset, spanLeft, rowIndex);
            if (res.getEndOffset() - res.getStartOffset() > HTMLParagraphView.MAX_VIEW_SIZE) {
                res = res.createFragment(startOffset, startOffset + HTMLParagraphView.MAX_VIEW_SIZE);
            }
            return res;
        }

    }


    @Override
    public int getResizeWeight(int axis) {
        return 0;
    }
}
