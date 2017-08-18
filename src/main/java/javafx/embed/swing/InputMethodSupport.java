/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.embed.swing;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.input.InputMethodHighlight;
import javafx.scene.input.InputMethodTextRun;

import java.awt.Rectangle;
import java.awt.event.InputMethodEvent;
import java.awt.font.TextHitInfo;
import java.awt.im.InputMethodRequests;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class containing the functions to support Input Methods
 */

public class InputMethodSupport {

    public static class InputMethodRequestsAdapter implements InputMethodRequests {

        private final javafx.scene.input.InputMethodRequests fxRequests;

        public InputMethodRequestsAdapter(javafx.scene.input.InputMethodRequests fxRequests) {
            this.fxRequests = fxRequests;
        }

        @Override
        public Rectangle getTextLocation(TextHitInfo offset) {
            Point2D result = fxRequests.getTextLocation(offset.getInsertionIndex());
            return new Rectangle((int) result.getX(), (int) result.getY(), 0, 0);
        }

        @Override
        public TextHitInfo getLocationOffset(int x, int y) {
            int result = fxRequests.getLocationOffset(x, y);
            return TextHitInfo.afterOffset(result);
        }

        @Override
        public int getInsertPositionOffset() {
            if (fxRequests instanceof ExtendedInputMethodRequests) {
                return ((ExtendedInputMethodRequests) fxRequests).getInsertPositionOffset();
            }
            return 0;
        }

        @Override
        public AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex, AttributedCharacterIterator.Attribute[] attributes) {
            String result;
            if (fxRequests instanceof ExtendedInputMethodRequests) {
                result = ((ExtendedInputMethodRequests) fxRequests).getCommittedText(beginIndex, endIndex);
            } else {
                result = "";
            }
            return new AttributedString(result).getIterator();
        }

        @Override
        public int getCommittedTextLength() {
            if (fxRequests instanceof ExtendedInputMethodRequests) {
                return ((ExtendedInputMethodRequests) fxRequests).getCommittedTextLength();
            }
            return 0;
        }

        @Override
        public AttributedCharacterIterator cancelLatestCommittedText(AttributedCharacterIterator.Attribute[] attributes) {
            // Do not support the "Undo Commit" feature
            return null;
        }

        @Override
        public AttributedCharacterIterator getSelectedText(AttributedCharacterIterator.Attribute[] attributes) {
            return new AttributedString(fxRequests.getSelectedText()).getIterator();
        }
    }

    public static ObservableList<InputMethodTextRun> inputMethodEventComposed(String text, int commitCount)
    {
        List<InputMethodTextRun> composed = new ArrayList<>();

        if (commitCount < text.length()) {
            // Create one single segment as UNSELECTED_RAW
            composed.add(new InputMethodTextRun(
                    text.substring(commitCount),
                    InputMethodHighlight.UNSELECTED_RAW));
        }
        return new ObservableListWrapper<>(composed);
    }

    public static String getTextForEvent(InputMethodEvent e) {
        AttributedCharacterIterator text = e.getText();
        if (e.getText() != null) {
            char c = text.first();
            StringBuilder result = new StringBuilder();
            while (c != CharacterIterator.DONE) {
                result.append(c);
                c = text.next();
            }
            return result.toString();
        }
        return "";
    }
}
