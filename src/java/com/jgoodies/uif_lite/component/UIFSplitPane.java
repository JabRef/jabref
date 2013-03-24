/*
 * Copyright (c) 2003, 2004 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.uif_lite.component;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * A <code>JSplitPane</code> subclass that can try to remove the divider border. 
 * Useful if the splitted components render their own borders.
 * Note that this feature is not supported by all look&amp;feels.
 * Some look&amp;feel implementation will always show a divider border, 
 * and conversely, others will never show a divider border.
 * 
 * @author Karsten Lentzsch
 * @version $Revision$
 * 
 * @see javax.swing.plaf.basic.BasicSplitPaneUI
 */

public final class UIFSplitPane extends JSplitPane {
    
    /**
     * Holds the name of the bound property that tries to show or hide
     * the split pane's divider border.
     * 
     * @see #isDividerBorderVisible()
     * @see #setDividerBorderVisible(boolean)
     */
    public static final String PROPERTYNAME_DIVIDER_BORDER_VISIBLE = 
        "dividerBorderVisible";
    
    /**
     * Holds an empty border that is reused for the split pane itself
     * and the divider.
     */
    private static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
    
    /**
     * Determines whether the divider border shall be removed when
     * the UI is updated.
     * 
     * @see #isDividerBorderVisible()
     * @see #setDividerBorderVisible(boolean) 
     */
    private boolean dividerBorderVisible;

   
    // Instance Creation *****************************************************
    
    /**
     * Constructs a <code>UIFSplitPane</code> configured to arrange the child
     * components side-by-side horizontally with no continuous 
     * layout, using two buttons for the components.
     */
    public UIFSplitPane() {
        this(JSplitPane.HORIZONTAL_SPLIT, false,
                new JButton(UIManager.getString("SplitPane.leftButtonText")),
                new JButton(UIManager.getString("SplitPane.rightButtonText")));
    }


    /**
     * Constructs a <code>UIFSplitPane</code> configured with the
     * specified orientation and no continuous layout.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @throws IllegalArgumentException if <code>orientation</code>
     *		is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT.
     */
    public UIFSplitPane(int newOrientation) {
        this(newOrientation, false);
    }


    /**
     * Constructs a <code>UIFSplitPane</code> with the specified
     * orientation and redrawing style.
     *
     * @param newOrientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param newContinuousLayout  a boolean, true for the components to 
     *        redraw continuously as the divider changes position, false
     *        to wait until the divider position stops changing to redraw
     * @throws IllegalArgumentException if <code>orientation</code>
     *		is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public UIFSplitPane(int newOrientation,
                      boolean newContinuousLayout) {
        this(newOrientation, newContinuousLayout, null, null);
    }


    /**
     * Constructs a <code>UIFSplitPane</code> with the specified orientation
     * and the given componenents.
     * 
     * @param orientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param leftComponent the <code>Component</code> that will
     *    appear on the left of a horizontally-split pane, 
     *    or at the top of a vertically-split pane
     * @param rightComponent the <code>Component</code> that will
     *    appear on the right of a horizontally-split pane, 
     *    or at the bottom of a	vertically-split pane
     * @throws IllegalArgumentException if <code>orientation</code>
     *    is not one of: HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public UIFSplitPane(int orientation,
                         Component leftComponent,
                         Component rightComponent) {
        this(orientation, false, leftComponent, rightComponent);
    }
    
    
    /**
     * Constructs a <code>UIFSplitPane</code> with the specified orientation,
     * redrawing style, and given components.
     *
     * @param orientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param continuousLayout  a boolean, true for the components to 
     *        redraw continuously as the divider changes position, false
     *        to wait until the divider position stops changing to redraw
     * @param leftComponent the <code>Component</code> that will
     *		appear on the left
     *        	of a horizontally-split pane, or at the top of a
     *        	vertically-split pane
     * @param rightComponent the <code>Component</code> that will
     *		appear on the right
     *        	of a horizontally-split pane, or at the bottom of a
     *        	vertically-split pane
     * @throws IllegalArgumentException if <code>orientation</code>
     *		is not one of HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public UIFSplitPane(int orientation,
                      boolean continuousLayout,
                      Component leftComponent,
                      Component rightComponent){
        super(orientation, continuousLayout, leftComponent, rightComponent);
        dividerBorderVisible = false;
    }
    
    
    /**
     * Constructs a <code>UIFSplitPane</code>, 
     * i.e. a <code>JSplitPane</code> that has no borders.
     * Also disabled the one touch exandable property.
     * 
     * @param orientation  <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                        <code>JSplitPane.VERTICAL_SPLIT</code>
     * @param leftComponent the <code>Component</code> that will
     *    appear on the left of a horizontally-split pane, 
     *    or at the top of a vertically-split pane
     * @param rightComponent the <code>Component</code> that will
     *    appear on the right of a horizontally-split pane, 
     *    or at the bottom of a	vertically-split pane
     * @throws IllegalArgumentException if <code>orientation</code>
     *    is not one of: HORIZONTAL_SPLIT or VERTICAL_SPLIT
     */
    public static UIFSplitPane createStrippedSplitPane(
             int orientation,
             Component leftComponent,
             Component rightComponent) {
        UIFSplitPane split = new UIFSplitPane(orientation, leftComponent, rightComponent);
        split.setBorder(EMPTY_BORDER);
        split.setOneTouchExpandable(false);
        return split;
    }
    
    
    // Accessing Properties **************************************************
    
    /**
     * Checks and answers whether the divider border shall be visible 
     * or invisible. 
     * Note that this feature is not supported by all look&amp;feels.
     * Some look&amp;feel implementation will always show a divider border, 
     * and conversely, others will never show a divider border.
     * 
     * @return the desired (but potentially inaccurate) divider border visiblity
     */
    public boolean isDividerBorderVisible() {
        return dividerBorderVisible;
    }
    
    
    /**
     * Makes the divider border visible or invisible.
     * Note that this feature is not supported by all look&amp;feels.
     * Some look&amp;feel implementation will always show a divider border, 
     * and conversely, others will never show a divider border.
     * 
     * @param newVisibility   true for visible, false for invisible
     */
    public void setDividerBorderVisible(boolean newVisibility) {
        boolean oldVisibility = isDividerBorderVisible();
        if (oldVisibility == newVisibility)
            return;
        dividerBorderVisible = newVisibility;
        firePropertyChange(PROPERTYNAME_DIVIDER_BORDER_VISIBLE, 
                           oldVisibility, 
                           newVisibility);
    }
    

    // Changing the Divider Border Visibility *********************************
    
    /**
     * Updates the UI and sets an empty divider border. The divider border
     * may be restored by a L&F at UI installation time. And so, we
     * try to reset it each time the UI is changed.
     */
    public void updateUI() {
        super.updateUI();
        if (!isDividerBorderVisible())
            setEmptyDividerBorder();
    }
    

    /**
     * Sets an empty divider border if and only if the UI is 
     * an instance of <code>BasicSplitPaneUI</code>.
     */
    private void setEmptyDividerBorder() {
        SplitPaneUI splitPaneUI = getUI();
        if (splitPaneUI instanceof BasicSplitPaneUI) {
            BasicSplitPaneUI basicUI = (BasicSplitPaneUI) splitPaneUI;
            basicUI.getDivider().setBorder(EMPTY_BORDER);
        }
    }
    
    
}